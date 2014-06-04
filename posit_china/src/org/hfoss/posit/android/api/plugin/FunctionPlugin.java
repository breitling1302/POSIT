/*
 * File: FunctionPlugin.java
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

import java.util.ArrayList;

import org.hfoss.posit.android.api.activity.SettingsActivity;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

import android.app.Activity;
import android.app.Service;
import android.util.Log;

/**
 * Function plugins implement various functions and are
 * (supposedly) independent of each other. Function plugins
 * attach to POSIT at pre-defined extension points.
 *
 */
public class FunctionPlugin extends Plugin {
		
	// Extension point in PositMain
	protected String mExtensionPoint = null;
	protected Class<Activity> mMenuActivity;
	protected String mMenuIcon;
	protected String mMenuTitle;
	protected Boolean activityReturnsResult = false;
	protected int activityResultAction = 0;
	
	protected String listFindCallbackClass = null;
	protected String addFindCallbackClass = null;

	// Function plugins can start services.
 	protected ArrayList<Class<Service>> mServices = new ArrayList<Class<Service>>();
	
	public FunctionPlugin (Activity activity, Node node) throws DOMException, ClassNotFoundException {
		mMainActivity = activity;
		
		// Perhaps this can be done more generally, rather than for each possible node
		Node aNode = null;

		for (int i = 0; i < node.getAttributes().getLength(); i++) {
			aNode = node.getAttributes().item(i);
			if (aNode.getNodeName().equals("name")) {
				name = aNode.getTextContent();
			}
			if (aNode.getNodeName().equals("type")) {
				type = aNode.getTextContent();
			}
			if (aNode.getNodeName().equals("activity")) {
				this.activity = (Class<Activity>) Class.forName(aNode.getTextContent());
			}
			if (aNode.getNodeName().equals("extensionPoint")) {
				mExtensionPoint = aNode.getTextContent();
			}
			if (aNode.getNodeName().equals("menuActivity")) {
				mMenuActivity = (Class<Activity>) Class.forName(aNode.getTextContent());
			}
			if (aNode.getNodeName().equals("menuIcon")) {
				mMenuIcon = aNode.getTextContent();
			}
			if (aNode.getNodeName().equals("menuTitle")) {
				mMenuTitle = aNode.getTextContent();
			}
			if (aNode.getNodeName().equals("activity_returns_result")) {
				activityReturnsResult = Boolean.valueOf(aNode.getTextContent());
			}
			if (aNode.getNodeName().equals("activity_result_action")) {
				activityResultAction = Integer.parseInt(aNode.getTextContent());
			}
			/* BEGINS - Function Plugin now has a preference */
			if (aNode.getNodeName().equals("preferences_xml")) {
				mPreferences = aNode.getTextContent();
				SettingsActivity.loadPluginPreferences(mMainActivity, mPreferences);
			}
			/* ENDS - Function Plugin now has a preference */
			/* BEGINS - Function Plugin now has a preference */
			if (aNode.getNodeName().equals("service")) {
				Class<Service> service = (Class<Service>) Class.forName(aNode.getTextContent());
				mServices.add(service);
			}
			/* ENDS - A list of all services this function plug-in requires*/
		
			if (aNode.getNodeName().equals("list_find_callback")) {
				listFindCallbackClass = aNode.getTextContent();
			}
			
			if (aNode.getNodeName().equals("add_find_callback")) {
				addFindCallbackClass = aNode.getTextContent();
			}
		}
		
	}
	
	public Boolean getActivityReturnsResult() {
		return activityReturnsResult;
	}

	public void setActivityReturnsResult(Boolean activityReturnsResult) {
		this.activityReturnsResult = activityReturnsResult;
	}

	public String getmExtensionPoint() {
		return mExtensionPoint;
	}

	public void setmExtensionPoint(String mExtensionPoint) {
		this.mExtensionPoint = mExtensionPoint;
	}

	public Class<Activity> getmMenuActivity() {
		return mMenuActivity;
	}

	public void setmMenuActivity(Class<Activity> mMenuActivity) {
		this.mMenuActivity = mMenuActivity;
	}

	public String getmMenuIcon() {
		return mMenuIcon;
	}

	public void setmMenuIcon(String mMenuIcon) {
		this.mMenuIcon = mMenuIcon;
	}

	public String getmMenuTitle() {
		return mMenuTitle;
	}

	public void setmMenuTitle(String mMenuTitle) {
		this.mMenuTitle = mMenuTitle;
	}
	
	public int getActivityResultAction() {
		return activityResultAction;
	}

	public void setActivityResultAction(int activityResultAction) {
		this.activityResultAction = activityResultAction;
	}
	
	public ArrayList<Class<Service>> getmServices() {
		return mServices;
	}


	public String getListFindCallbackClass() {
		return listFindCallbackClass;
	}

	public void setListFindCallbackClass(String listFindCallbackClass) {
		this.listFindCallbackClass = listFindCallbackClass;
	}
	
	public String getAddFindCallbackClass() {
		return addFindCallbackClass;
	}

	public void setAddFindCallbackClass(String addFindCallbackClass) {
		this.addFindCallbackClass = addFindCallbackClass;
	}

	public String toString() {
		return super.toString() + " " + mExtensionPoint;
	}
	
	

}
