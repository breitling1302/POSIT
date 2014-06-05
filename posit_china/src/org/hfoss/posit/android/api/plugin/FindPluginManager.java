/*
 * File: FindPluginManager.java
 * 
 * Copyright (C) 2011 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Source Information Tool. 
 *
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License (LGPL) as published 
 * by the Free Software Foundation; either version 3.0 of the License, or (at
 * your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU LGPL along with this program; 
 * if not visit http://www.gnu.org/licenses/lgpl.html.
 * 
 */

package org.hfoss.posit.android.api.plugin;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.hfoss.posit.android.R;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

/**
 * This class reads Plugin specifications and preferences from the
 * raw XML file, plugins-preferences.xml, and constructs a list 
 * of currently active plugins. Its static methods are used by Posit
 * to access plugin activities and other plugin elements
 *
 */
public class FindPluginManager {
	private static FindPluginManager sInstance = null;
	
	private static final String TAG = "FindPluginManager";

	public static final String MAIN_MENU_EXTENSION = "mainMenu";
	/* Function Button Begins */
	public static final String MAIN_BUTTON_EXTENSION = "mainButton";
	/* Function Button Ends */
	public static final String LIST_MENU_EXTENSION = "listMenu";
	/* To-Do Begins */
	public static final String ADD_FIND_MENU_EXTENSION = "addFindMenu";
	/* To-Do Ends */
	public static final String MAIN_LOGIN_EXTENSION = "mainLogin"; 
	
	public static final String IS_PLUGIN = "isPlugin"; // used in subclasses to
														// indicate
	
	public static final String FUNC_PLUGIN_PREFS = "funcPluginPrefs";
	
	// that in onCreate() we are in a plugin.. not sure

	// Mostly for Function Plugins
	private static ArrayList<Plugin> plugins = null; // new ArrayList<Plugin>();
	
	private int pluginsPreferencesId;
	
	// Our one and only (sometimes) Find Plugin
	public static FindPlugin mFindPlugin = null;
	public static List<String> mFindPluginNames = null;
	public static String mDefaultFindPluginName = "";
	private Activity mMainActivity = null;


	private List<ActiveFuncPluginChangeEventListener> ActiveFuncPluginChangeEventListenerList = 
			new ArrayList<ActiveFuncPluginChangeEventListener>();
	
	private FindPluginManager(Activity activity) {
		this.mMainActivity = activity;
	}

	public static FindPluginManager initInstance(Activity activity) {
		sInstance = new FindPluginManager(activity);
		sInstance.initFromResource(activity, R.raw.plugins_preferences);
		return sInstance;
	}

	public static FindPluginManager getInstance() {
		assert (sInstance != null);
		return sInstance;
	}

	/**
	 * Reads the plugins_xml file and creates plugin objects, storing
	 * them in the plugins list.
	 * @param context
	 * @param plugins_xml
	 */
	public void initFromResource(Context context, int plugins_xml){	
		this.pluginsPreferencesId = plugins_xml;
		
		plugins = new ArrayList<Plugin>();	
		mFindPluginNames = new ArrayList<String>();
		
		try{
			HashSet<String> FunctionPluginNames = new HashSet<String>();
			
			SharedPreferences funcPluginPrefs = context.getSharedPreferences(FUNC_PLUGIN_PREFS, 
					Context.MODE_WORLD_READABLE | Context.MODE_WORLD_WRITEABLE);
			SharedPreferences.Editor funcPluginPrefsEditor = funcPluginPrefs.edit();
			
			
			NodeList plugin_nodes = GetNodeListFromPluginsPreferences();
			if (plugin_nodes == null)
			{
				return;
			}
			for(int k = 0; k < plugin_nodes.getLength(); ++k){
				
				String curPluginName = plugin_nodes.item(k).getAttributes().getNamedItem("name").getTextContent();
				String curPluginType = plugin_nodes.item(k).getAttributes().getNamedItem("type").getTextContent();
				boolean curPluginIsSelectableByUser = plugin_nodes.item(k).getAttributes().getNamedItem("user_can_select").getTextContent().compareTo("true") == 0;
				
				Plugin p = null;
				if (curPluginType.equals("find") ) {
					boolean curPluginIsDefault = plugin_nodes.item(k).getAttributes().getNamedItem("default_find_plugin").getTextContent().compareTo("true") == 0;
					
					if(curPluginIsSelectableByUser)
					{
						mFindPluginNames.add(curPluginName);
					}
					
					if (curPluginIsDefault)  {
						//Ensure that there is not more then one default find plugin
						assert(mDefaultFindPluginName.equals(""));
						mDefaultFindPluginName = curPluginName;
						
						p = new FindPlugin(this.mMainActivity, plugin_nodes.item(k));
						mFindPlugin = (FindPlugin) p;
						plugins.add(mFindPlugin);	
					}
				} else if (curPluginType.equals("function") ) {
					boolean curPluginIsActive = plugin_nodes.item(k).getAttributes().getNamedItem("active_by_default").getTextContent().compareTo("true") == 0;

					FunctionPluginNames.add(curPluginName);
					
					//Add the current function plugin to the shared preferences 
					//if it is not already there and the user is allowed to select it.
					if (funcPluginPrefs.contains(curPluginName) == false && 
						curPluginIsSelectableByUser == true)
					{
						funcPluginPrefsEditor.putBoolean(curPluginName, curPluginIsActive);
						funcPluginPrefsEditor.commit();
					}
					
					if (curPluginIsActive)  {
						p = new FunctionPlugin(this.mMainActivity, plugin_nodes.item(k));
						plugins.add(p);
						Log.i(TAG, "Plugin " + p.toString());
					}
				}
				else {
					// Do sth for other types in the future			
				}
				
			}
			//Ensure that there is at least one default find plugin
			assert(mDefaultFindPluginName.equals("") == false);
			
			//Remove from the DB any function plugins that were not found in plugins_preferences.xml
			Map<String, ?> funcPluginPrefsMap = funcPluginPrefs.getAll();
			for(String funcPluginNameInPrefs : funcPluginPrefsMap.keySet()){
				if (FunctionPluginNames.contains(funcPluginNameInPrefs) == false)
				{
					funcPluginPrefsEditor.remove(funcPluginNameInPrefs);
					funcPluginPrefsEditor.commit();
				}
			}
			
			Log.i(TAG, "# of plugins = " + plugins.size());			
		} catch (Exception e) {
			HandlePluginPreferencesXmlError(e);
		}
	}

	/**
	 * Returns the NodeList object for the plugins_preferences.xml file.
	 * 
	 */
	private NodeList GetNodeListFromPluginsPreferences()
	{
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try{
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputStream istream = this.mMainActivity.getResources().openRawResource(pluginsPreferencesId);
			Document document = builder.parse(istream);
			XPath xpath = XPathFactory.newInstance().newXPath();
			
			return (NodeList)xpath.evaluate("PluginsPreferences/FindPlugins/Plugin", document, XPathConstants.NODESET);
	
		} catch (Exception e) {
			HandlePluginPreferencesXmlError(e);
		}
		
		return null;
	}
	
	/**
	 * Print the appropriate stack trace when an exception is thrown as a result of parsing 
	 * plugins_preferences.xml
	 * @param e The exception that was thrown
	 */
	private void HandlePluginPreferencesXmlError(Exception e)
	{
		Log.e(TAG, "Failed to load plugin");
		Log.e(TAG, "reason: " + e);
		Log.e(TAG, "stack trace: ");
		e.printStackTrace();
		Toast.makeText(this.mMainActivity, "POSIT failed to load plugin. Please fix this in plugins_preferences.xml.",
				Toast.LENGTH_LONG).show();
		this.mMainActivity.finish();
	}
	
	/**
	 * Returns all plugins
	 * @return
	 */
	public ArrayList<Plugin> getPlugins() {
		return plugins;
	}
	
	/**
	 * Returns a FunctionPlugin by extension point
	 * @return
	 */
	public static FunctionPlugin getFunctionPlugin(String extensionType) {
		FunctionPlugin plugin = null;
		for (Plugin p : plugins) {
			if (p instanceof FunctionPlugin) {
//				Log.i(TAG, "Function plugin " + p.toString());
				if ( ((FunctionPlugin) p).mExtensionPoint.equals(extensionType))
					plugin = (FunctionPlugin) p;
			}
		}
		return plugin;
	}

	
	/**
	 * Returns FunctionPlugins by extension point
	 * @return
	 */
	public static ArrayList<FunctionPlugin> getFunctionPlugins(String extensionType) {
		ArrayList<FunctionPlugin> list = new ArrayList<FunctionPlugin>();
		if (plugins == null) return list;
		for (Plugin plugin : plugins) {
			if (plugin instanceof FunctionPlugin) {
//				Log.i(TAG, "Function plugin " + plugin.toString());
				FunctionPlugin fPlugin = (FunctionPlugin) plugin;
				if (fPlugin.mExtensionPoint.equals(extensionType))
					list.add(fPlugin);
			}
		}
		return list;
	}
	
	/**
	 * Returns all FunctionPlugins
	 * @return
	 */
	public static ArrayList<FunctionPlugin> getFunctionPlugins() {
		ArrayList<FunctionPlugin> list = new ArrayList<FunctionPlugin>();
		if (plugins == null) return list;
		for (Plugin plugin : plugins) {
			if (plugin instanceof FunctionPlugin) {
//				Log.i(TAG, "Function plugin " + plugin.toString());
				FunctionPlugin fPlugin = (FunctionPlugin) plugin;
				list.add(fPlugin);
			}
		}
		return list;
	}
	
	/**
	 * Returns a list of all services created by plugins.
	 * @return
	 */
	public static ArrayList<Class<Service>> getAllServices() {
		ArrayList<Class<Service>> list = (ArrayList<Class<Service>>) new ArrayList<Class<Service>>();
		for (Plugin plugin : plugins) {
			if (plugin instanceof FunctionPlugin) {
//				Log.i(TAG, "Function plugin " + plugin.toString());
				FunctionPlugin fPlugin = (FunctionPlugin) plugin;
				if (fPlugin.getmServices().size() > 0) {
					list.addAll(fPlugin.getmServices());
				}
			}
		}
		return list;
	}
	
	/**
	 * A class that implements ActiveFuncPluginChangeEventListener should call this to be notified 
	 * when a function is enabled/disabled.
	 * @param listener	the instance of the class implementing ActiveFuncPluginChangeEventListener.
	 */
	public synchronized void addActiveFuncPluginChangeEventListener(
			ActiveFuncPluginChangeEventListener listener)  
	{
		this.ActiveFuncPluginChangeEventListenerList.add(listener);
	}
	
	/**
	 * Notify any classes that have registered through addActiveFuncPluginChangeEventListener()
	 * that a function plugin has been enabled/disabled
	 * @param plugin	the plugin that has been enabled/disabled
	 * @param enabled	specifies whether plugin has been enabled or disabled
	 */
	private synchronized void TriggerActiveFuncPluginChangeEvent(FunctionPlugin plugin, boolean enabled) {
		for (ActiveFuncPluginChangeEventListener listener : this.ActiveFuncPluginChangeEventListenerList)
		{
			listener.handleActiveFuncPluginChangeEvent(plugin, enabled);
		}
	}
	
	/**
	 * This will cause the enabled status of a function plugin to be set to the value of the 
	 * <code>enabled</code> parameter
	 * @param pluginName	The name of the plugin to enable/disable
	 * @param enabled		Specifies wheather the plugin should be enabled or disabled
	 */
	public void UpdateFuncPluginEnabledState(String pluginName, boolean enabled)
	{		
		SharedPreferences funcPluginPrefs = this.mMainActivity.getSharedPreferences(FindPluginManager.FUNC_PLUGIN_PREFS, 
				Context.MODE_WORLD_READABLE | Context.MODE_WORLD_WRITEABLE);
		SharedPreferences.Editor funcPluginPrefsEditor = funcPluginPrefs.edit();
		
		funcPluginPrefsEditor.putBoolean(pluginName, enabled);
		funcPluginPrefsEditor.commit();
		
		boolean pluginAlreadyExists = false;
		int existingPluginIndex = -1;
		
		for (int i = 0; i < plugins.size(); i++) {
			Plugin curPlugin = plugins.get(i);
			if (curPlugin instanceof FunctionPlugin) {
				if(curPlugin.getName().equals(pluginName))
				{
					pluginAlreadyExists = true;
					existingPluginIndex = i;
					break;
				}
				
			}
		}
		
		//Remove existing plugin if necessary
		if (pluginAlreadyExists == true && enabled == false)
		{
			FunctionPlugin plugin = (FunctionPlugin)plugins.get(existingPluginIndex);
			plugins.remove(existingPluginIndex);
			TriggerActiveFuncPluginChangeEvent(plugin, enabled);
		}
		
		//Add new plugin if necessary
		if (pluginAlreadyExists == false && enabled == true)
		{
			Node staticPluginInfo = GetStaticPluginInfoFromPluginName(pluginName);
			if (staticPluginInfo == null)
			{
				return;
			}
			
			FunctionPlugin plugin = null;
			try {
				plugin = new FunctionPlugin(this.mMainActivity, staticPluginInfo);
			} catch (Exception e) {
				HandlePluginPreferencesXmlError(e);
			}

			plugins.add(plugin);	
			TriggerActiveFuncPluginChangeEvent(plugin, enabled);
		}
	}

	/**
	 * Returns the node in plugin_preferences.xml that refers to the plugin specified by pluginName
	 */
	public Node GetStaticPluginInfoFromPluginName(String pluginName)
	{
		NodeList plugin_nodes = GetNodeListFromPluginsPreferences();
		if (plugin_nodes == null)
		{
			return null;
		}
		
		for(int k = 0; k < plugin_nodes.getLength(); ++k){
			
			String curPluginName = plugin_nodes.item(k).getAttributes().getNamedItem("name").getTextContent();
			
			if (pluginName.equals(curPluginName))
			{
				return plugin_nodes.item(k);
			}
		}
		
		return null;
	}
}
