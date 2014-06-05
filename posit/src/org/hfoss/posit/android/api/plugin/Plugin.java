/*
 * File: Plugin.java
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

import android.app.Activity;

/**
 * Plugin superclass with two known subclasses, FindPlugin and 
 * FunctionPlugin. Defines elements common to all plugins.
 */
public class Plugin {

	protected static final String TAG = "Plugin";
	
	public static String mPreferences = null;
	protected String name;
	protected String type;
	protected Class<Activity> activity;
	
	protected Activity mMainActivity;
	public static String getmPreferences() {
		return mPreferences;
	}
	public static void setmPreferences(String mPreferences) {
		Plugin.mPreferences = mPreferences;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Activity getmMainActivity() {
		return mMainActivity;
	}
	public void setmMainActivity(Activity mMainActivity) {
		this.mMainActivity = mMainActivity;
	}
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}

	public Class<Activity> getActivity() {
		return activity;
	}
	public void setActivity(Class<Activity> activity) {
		this.activity = activity;
	}
	public String toString() {
		return name + " " + type;
	}
}
