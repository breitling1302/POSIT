/*
 /*
 * File: SettingsActivity.java
 * 
 * Copyright (C) 2009 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Search and Identification Tool.
 *
 * POSIT is free software; you can redistribute it and/or modify
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
package org.hfoss.posit.android.api.activity;

import java.io.IOException;
import java.util.ArrayList;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.LocaleManager;
import org.hfoss.posit.android.sync.Communicator;
import org.hfoss.posit.android.sync.SyncAdapter;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Manages preferences for core POSIT preferences and all plugins. Plugin
 * preferences are merged with POSIT preferences.
 * 
 * Here's how to specify a preference XML for a plugin.
 * 
 * Preferences (EditPreference, ListPreference, etc) that simply set a
 * preference value are handled automatically by PreferenceActivity. These can
 * be coded as usual: <ListPreference android:key="@string/s4"
 * android:defaultValue="s5" android:summary="@string/s6"
 * android:entries="@array/a1" android:entryValues="@array/a2" />
 * 
 * Preferences that start an Activity must include an "activity_class"
 * attribute:
 * 
 * <Preference android:key="testpref" android:title="About Me"
 * activity_class="org.hfoss.posit.android.AboutActivity"
 * android:summary="Click me and see!" />
 * 
 */
public class SettingsActivity extends SherlockPreferenceActivity implements
		OnPreferenceClickListener, OnSharedPreferenceChangeListener {
	private static final String TAG = "API Settings";
	public static final String SETTINGS_ACTION = "settings_action";
	
	private static SettingsActivity mInstance;

	public static SettingsActivity getInstance(Context context,
			String prefsXmlFileName) {
		mInstance = new SettingsActivity();
		SettingsActivity.init(context, prefsXmlFileName);
		// assert(mInstance != null);
		return mInstance;
	}

	public static void init(Context context, String prefsXmlFileName) {

	}

	/**
	 * Associates preferences with activities.
	 */
	private static ArrayList<PluginSettings> pluginXmlList = new ArrayList<PluginSettings>();

	/**
	 * Inner class for a plugin setting, which consists of a preference name and
	 * and associated activity of service. A PluginSetting is created only for those
	 * preferences that require an associated Activity, not for preferences that
	 * are handled automatically by PreferenceActivity.
	 * 
	 * @author rmorelli
	 * 
	 */
	static class PluginSetting {
		public static final String TYPE_SERVICE = "service";
		public static final String TYPE_ACTIVITY = "activity";
		public String prefName;
		public String activityOrServiceName;
		public String type;                   // Either activity or service

		public PluginSetting(String prefName, String activityOrServiceName, String type) {
			this.prefName = prefName;
			this.activityOrServiceName = activityOrServiceName;
			this.type = type;
		}

		@Override
		public String toString() {
			return prefName + "," + activityOrServiceName + ", " + type;
		}
	}

	/**
	 * Inner class for PluginSettings. Stores a record for each plugin
	 * consisting of the plugin's preferences XML file (in res/xml) and
	 * key/activity  or key/service pairs for each preference that requires an 
	 * Activity or Service launch.
	 * 
	 * @author rmorelli
	 * 
	 */
	static class PluginSettings {
		private String preferencesXmlFile;
		private ArrayList<PluginSetting> preferencesList;

		PluginSettings(String preferencesXmlFile) {
			this.preferencesXmlFile = preferencesXmlFile;
			preferencesList = new ArrayList<PluginSetting>();
		}

		public void put(String prefName, String activityName, String type) {
			preferencesList.add(new PluginSetting(prefName, activityName, type));
		}

		public String getPreferencesXmlFile() {
			return preferencesXmlFile;
		}

		public ArrayList<PluginSetting> getPreferencesList() {
			return preferencesList;
		}

		@Override
		public String toString() {
			return preferencesXmlFile + " table=" + preferencesList.toString();
		}

		@Override
		public boolean equals(Object o) {
			return preferencesXmlFile.equals(((PluginSettings) o)
					.getPreferencesXmlFile());
		}
	}

	/**
	 * Parses the XML preferences file and loads the key/activity pairs for all
	 * preferences that require an Activity. Uses XmlPullParser. Called from
	 * PluginManager.
	 * 
	 * @see http
	 *      ://android-er.blogspot.com/2010/04/read-xml-resources-in-android-
	 *      using.html
	 * @param context
	 * @param prefsXmlFileName
	 */
	public static void loadPluginPreferences(Context context,
			String prefsXmlFileName) {
		Log.i(TAG, "Loading " + prefsXmlFileName + " preferences for Settings Activity");

		PluginSettings settingsObject = getKeyActivityPairs(context,
				prefsXmlFileName);
		if (!pluginXmlList.contains(settingsObject))
			pluginXmlList.add(settingsObject);
	}

	/**
	 * Utility method parses an XML preferences file pulling out domain-specific
	 * attributes that associate a Preference key with an Activity or Service.
	 * 
	 * @param context
	 *            this Activity
	 * @param prefsXmlFileName
	 *            the name of the XML file
	 * @return an PluginSettings object that stores the data for a particular
	 *         XML file.
	 */
	private static PluginSettings getKeyActivityPairs(Context context,
			String prefsXmlFileName) {
		PluginSettings settingsObject = new PluginSettings(prefsXmlFileName);
		int resId = context.getResources().getIdentifier(prefsXmlFileName,
				"xml", "org.hfoss.posit.android");

		XmlResourceParser xpp = context.getResources().getXml(resId);
		try {
			xpp.next();
			int eventType = xpp.getEventType();

			while (eventType != XmlPullParser.END_DOCUMENT) {
				if (eventType == XmlPullParser.START_TAG) {
//					if (xpp.getName().equals("Preference")) {
					 Log.i(TAG," xml pref tag = " + xpp.getName());
					if (xpp.getName().endsWith("Preference")) {
						String preference_name = "";
						String activity_or_service_name = "";
						String type = "";
						for (int k = 0; k < xpp.getAttributeCount(); k++) {
							String attribute = xpp.getAttributeName(k);
							// Log.i(TAG,"Attribute = " + attribute);
							if (attribute.equals("key")) {
								preference_name = parsePreferenceName(context,
										xpp.getAttributeValue(k));
								Log.i(TAG, "pref: " + preference_name);
							} else if (attribute.equals("activity_class")) {
								activity_or_service_name = xpp.getAttributeValue(k);
								type = PluginSetting.TYPE_ACTIVITY;
							} else if (attribute.equals("service_class")) {
								activity_or_service_name = xpp.getAttributeValue(k);
								type = PluginSetting.TYPE_SERVICE;
							}
						}
						// Log.i(TAG,"Settings = " + preference_name + " " +
						// activity_name);
						settingsObject.put(preference_name, activity_or_service_name, type);

					}
				}
				eventType = xpp.next();
			}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return settingsObject;
	}

	/**
	 * Because we are parsing a compiled file, android replaces String resource
	 * strings with integer resource IDs. This method parses out the resource ID
	 * and looks up the actual string resource.
	 * 
	 * @param name
	 * @return the string
	 */
	protected static String parsePreferenceName(Context context, String name) {
		if (name.charAt(0) == '@') {
			int resourceId = Integer.parseInt(name.substring(1));
			return context.getString(resourceId);
		}
		return name;
	}
	
	/**
	 * Have all list preferences show their choice in the summary section.
	 */	
	private void updatePrefSummary(Preference p){
         if (p instanceof ListPreference) {
             ListPreference listPref = (ListPreference) p; 
             p.setSummary(listPref.getEntry()); 
         }
    }

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Log.i(TAG, "onCreate()");

		// Add POSIT's core preferences to the Settings (if not already added)
		PluginSettings settingsObject = getKeyActivityPairs(this,
				"posit_preferences");
		if (!pluginXmlList.contains(settingsObject))
			pluginXmlList.add(0, settingsObject);

		// For each plugin add its preferences to the Settings
		for (int k = 0; k < pluginXmlList.size(); k++) {
			Log.i(TAG, pluginXmlList.get(k).toString());

			// Merge its preference with POSIT core preferences.
			String pluginFile = pluginXmlList.get(k).getPreferencesXmlFile();
			int resID = getResources().getIdentifier(pluginFile, "xml",
					"org.hfoss.posit.android");
			this.addPreferencesFromResource(resID);

			// For each preference that starts an Activity set its Listener
			ArrayList<PluginSetting> settings = pluginXmlList.get(k)
					.getPreferencesList();
			for (int j = 0; j < settings.size(); j++) {
				Log.i(TAG, "current pref: " + settings.get(j).prefName);
				this.findPreference(settings.get(j).prefName)
						.setOnPreferenceClickListener(this);
			}
		}
		// Register this activity as a preference change listener
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(this);
		sp.registerOnSharedPreferenceChangeListener(this);

		controlSettingsVisibility(sp);
		
		PreferenceManager manager = this.getPreferenceManager();
		Preference p = manager.findPreference(getString(R.string.projectNamePref));
		p.setSummary(sp.getString(getString(R.string.projectNamePref), "None"));
		p = manager.findPreference(getString(R.string.serverPref));
		p.setSummary(sp.getString(getString(R.string.serverPref), getString(R.string.defaultServer)));
		p = manager.findPreference(getString(R.string.localePref));
		updatePrefSummary(p);			
	}

	/**
	 * Set visibility OFF for menus that the normal user is not supposed to
	 * control.
	 * 
	 * @param sp
	 */
	private void controlSettingsVisibility(SharedPreferences sp) {
		// // NOTE: This seems to throw an exception for non-string preference
		// values.
		// // Initialize the summary strings
		// // SharedPreferences.Editor spe = sp.edit(); // 7/25/11
		//
		// int userTypeOrdinal = sp.getInt(AcdiVocaUser.USER_TYPE_KEY, -1);
		// Log.i(TAG, "Control settings, UserTypeKey = " + userTypeOrdinal);
		//
		// Map<String,?> prefs = sp.getAll();
		// // PreferenceGroup spg = PreferenceGroup(this,
		// xml.acdivoca_preferences); // 7/25/11
		// Iterator it = prefs.keySet().iterator();
		// while (it.hasNext()) {
		// try {
		// String key = (String) it.next();
		// Preference p = findPreference(key);
		// String value = sp.getString(key, null);
		// if (p!= null && value != null) {
		// Log.i(TAG, "Preference = " + p.toString());
		// if (key.equals(getString(R.string.distribution_point_key))){ // New
		// code for distribution points
		// p.setSummary(AttributeManager.getMapping(value)); // 7/15/11
		// }
		//
		// else{
		// p.setSummary(value);
		// }
		//
		// if (key.equals(getString(R.string.distribution_point_key))) {
		// if (AppControlManager.isAgronUser()) {
		// ListPreference listDistrPoint = (ListPreference)
		// findPreference(getString(R.string.distribution_point_key));
		// Log.i(TAG, "List Preference = " + listDistrPoint);
		// PreferenceCategory mCategory = (PreferenceCategory)
		// findPreference(getString(R.string.acdivocaprefs));
		// if (mCategory.removePreference(listDistrPoint))
		// Log.i(TAG, "Should have removed preference = " + listDistrPoint);
		// }
		// }
		//
		// // if (userTypeOrdinal == UserType.USER.ordinal()
		// if ((AppControlManager.isRegularUser() ||
		// AppControlManager.isAgriUser() /*|| AppControlManager.isAgronUser()
		// **/)
		// // 7/25/11
		// && (key.equals(getString(R.string.smsPhoneKey))
		// || key.equals(getString(R.string.distribution_point_key))
		// || key.equals(getString(R.string.distribution_event_key))
		// || key.equals(getString(R.string.commune_section_key))
		// )) {
		// Log.i(TAG, "############################"); // 7/25/11
		// p.setEnabled(false);
		// // onPrepareForRemoval()
		// // sp.remove(key);
		// // int id = p.getLayoutResource();
		// // findViewById(id).setVisibility(false);
		//
		// // PreferenceCategory mCategory = (PreferenceCategory)
		// findPreference(getString(R.string.acdivocaprefs));
		// // Preference mPreference =
		// getPreferenceScreen().findPreference(key);
		// // if(mCategory==null)
		// // Log.i(TAG, "Category not being found");
		// // int id = p.getLayoutResource();
		// // Preference mPreference = mCategory.getPreference(id);
		// // if(mPreference==null)
		// // Log.i(TAG, "Preference not being found in category");
		// // if(!mCategory.remove(mPreference))
		// // Log.i(TAG,
		// "Something is wrong with mCategory.removePreference()"); //Doesn't
		// work here
		// // ListPreference listDistrPoint = (ListPreference)
		// findPreference(key);
		// Log.i(TAG, "Disabling USER setting for key = " + key);
		// }
		// // 7/25/11 Allows Agron users to change commune section
		//
		// if ((AppControlManager.isAgronUser())
		// && (key.equals(getString(R.string.smsPhoneKey))
		// || key.equals(getString(R.string.distribution_point_key))
		// || key.equals(getString(R.string.distribution_event_key))
		// )) {
		// p.setEnabled(false);
		// //this.getPreferenceScreen().removePreference(p); Doesn't work here
		// Log.i(TAG, "Disabling USER setting for key = " + key);
		// }
		//
		//
		// // if (userTypeOrdinal == UserType.ADMIN.ordinal()
		// // 8/3/11 This code prevents admin users from changing the commune
		// section
		// if ((AppControlManager.isAdminUser()
		// && key.equals(getString(R.string.commune_section_key))
		// )) {
		// p.setEnabled(false);
		// Log.i(TAG, "Disabling ADMIN setting for key = " + key);
		// }
		//
		// if ((AppControlManager.isAdminUser() ||
		// AppControlManager.isAgronUser())
		// && key.equals(getString(R.string.distribution_event_key))) {
		// p.setEnabled(false);
		//
		//
		//
		// //this.getPreferenceScreen().removePreference(p); Doesn't work here
		// Log.i(TAG, "Disabling ADMIN setting for key = " + key);
		// }
		// }
		// } catch (ClassCastException e) {
		// Log.e(TAG, "Initialize summary strings ClassCastException");
		// Log.e(TAG, e.getStackTrace().toString());
		// continue;
		// }
		// }
	}
	
	/**
	 * Called automatically when a preference is clicked in the View. Go through
	 * all the plugins and see if one with an associated activity was clicked
	 * and, if so, start the activity.
	 * 
	 * NOTE: This method is not invoked for CheckboxPreference, ListPreference, etc.
	 * only for Preference.
	 */
	public boolean onPreferenceClick(Preference preference) {
		Log.i(TAG, "API onPreferenceClick " + preference.toString());

		// For each plugin
		for (int k = 0; k < pluginXmlList.size(); k++) {
			Log.i(TAG, pluginXmlList.get(k).toString());

			ArrayList<PluginSetting> list = pluginXmlList.get(k)
					.getPreferencesList();
			Log.i(TAG, "list = " + list.toString());
			for (int j = 0; j < list.size(); j++) {
				if (preference.getKey().equals(list.get(j).prefName)) {
					String className = list.get(j).activityOrServiceName;
					String type = list.get(j).type;
					Log.i(TAG, "Class = " + className + " type = " + type);
					if (type.equals(PluginSetting.TYPE_ACTIVITY)) {
						try {
							Class activity = Class.forName(className);
							Log.i(TAG, "Class = " + activity);
							Intent intent = new Intent(this, activity);
							intent.setAction(SETTINGS_ACTION);
							startActivity(intent);
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						} 
					} else  {
						try {
							Class service = Class.forName(className);
							Log.i(TAG, "Class = " + service);
							Intent intent = new Intent(this, service);
							intent.setAction(SETTINGS_ACTION);
							startService(intent);              // This will either stop or start it
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						} 

					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Adjusts the summary string for certain preferences when a shared preference is changed.
	 *  
	 * NOTE: Should this method also handle plugin string keys?  Integer or boolean keys?
	 * 
	 * @see android.content.SharedPreferences.OnSharedPreferenceChangeListener#onSharedPreferenceChanged(android.content.SharedPreferences,
	 *      java.lang.String)
	 */
	public void onSharedPreferenceChanged(SharedPreferences sp, String key) {

		try {

			// Note: How can we tell whether key has a String value?
			Log.i(TAG, "onSharedPreferenceChanged, key= " + key 
//				+ " value = " + sp.getString(key, "") 
			);

			Log.i(TAG, "Preferences= " + sp.getAll().toString());
			PreferenceManager manager = this.getPreferenceManager();
			Preference p = manager.findPreference(key);
			
			Log.i(TAG, "p = " + p);
			
			updatePrefSummary(findPreference(key));	
			
			if (p!= null && key.equals(getString(R.string.localeKey))) {
				LocaleManager.setDefaultLocale(this);
			}
			
			if (p!= null && key.equals(getString(R.string.serverPref))) {
				String server = sp.getString(key, "");
				if (server != null) {
					Log.i(TAG, "new server = " + server);
					p.setSummary(server);
					
					// Changing the server invalidates the account.
					Communicator.removeAccount(this, SyncAdapter.ACCOUNT_TYPE);

					Toast.makeText(this, "You must authenticate on the new server.", Toast.LENGTH_LONG).show();
					Log.i(TAG, "Server change invalidates the account");
					
					SharedPreferences.Editor editor = sp.edit();	
					editor.remove(getString(R.string.projectPref));
					editor.remove(getString(R.string.projectNamePref));
					//editor.putInt(getString(R.string.projectPref), 0);
					//editor.putString(getString(R.string.projectNamePref), "");
					editor.commit();
					p = manager.findPreference(getString(R.string.projectNamePref));
					p.setSummary("None");
				}
			}

			if (p != null && key.equals(getString(R.string.projectNamePref))) {
				
				String value = sp.getString(key, "");
				Log.i(TAG, "value = " + value);
				if (value != null)
					p.setSummary(value);
			}
		} catch (ClassCastException e) {
			Log.e(TAG, "Class Cast Exception on " + key);
			//e.printStackTrace();
		}
	}
}
