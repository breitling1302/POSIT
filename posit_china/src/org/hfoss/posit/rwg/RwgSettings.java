/*
 * File: RwgSettings.java
 * 
 * Copyright (C) 2010 The Humanitarian FOSS Project (http://www.hfoss.org)
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
package org.hfoss.posit.rwg;

import org.hfoss.posit.android.Log;
import org.hfoss.posit.android.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;


public class RwgSettings extends PreferenceActivity {

	public static final String TAG = "Adhoc";

	// These settable RWG preferences have to be identical to String resources
	public static final String RWG_GROUP_PREFERENCE = 
		"Group Size"; // @string/rwg_group_size
	public static final String MRWG_SSID_PREFERENCE = 
		"SSID"; // @string/rwgSSID


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(TAG, "RwgSettings, onCreate()");

		PreferenceManager.getDefaultSharedPreferences(this);

		addPreferencesFromResource(R.xml.rwg_preferences);	
	}
}
