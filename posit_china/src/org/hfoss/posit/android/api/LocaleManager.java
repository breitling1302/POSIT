/*
 * File: LocaleManager.java
 * 
 * Copyright (C) 2009 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Source Information Tool.
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

package org.hfoss.posit.android.api;

import java.util.Locale;

import android.app.Activity;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Handles locale changes. Should be called in 
 * onResume() of all Activities.
 *
 */
public class LocaleManager {
	
	public static final String TAG = "LocaleManager";
	
	public static void setDefaultLocale(Activity activity) {
		String localePref = PreferenceManager.getDefaultSharedPreferences(activity).getString("locale", "");
		Log.i(TAG, "Locale = " + localePref);
		Locale locale = new Locale(localePref); 
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
		activity.getBaseContext().getResources().updateConfiguration(config, null);
	}

}
