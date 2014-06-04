/*
 * File: TrackerSettings.java
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
package org.hfoss.posit.android;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;


/**
 * Lets the user adjust Tracker parameters. The tracker preferences are
 * loaded from an XML resource file.  Whenever the user changes a preference,
 * all Listeners that are registered with the PREFERENCES_NAME settings, will
 * be notified and can take action.  TrackerActivity is the only registered
 * listener.
 *  
 * @author rmorelli
 * @see http://code.google.com/p/mytracks/
 */
public class TrackerSettings extends PreferenceActivity {
	
	public static final String TAG = "PositTracker";
	
	  public static final String PREFERENCES_NAME = "TrackerSettings";

	  // Default settings -- some of these settable in shared preferences
	  public static final int DEFAULT_MIN_RECORDING_DISTANCE = 3; // meters, sp
	  public static final int DEFAULT_MIN_RECORDING_INTERVAL = 2000; 
	  public static final int DEFAULT_MIN_REQUIRED_ACCURACY = 200; // Unused
	  public static final int DEFAULT_SWATH_WIDTH = 50; // sp
	  
	  public static final int MIN_LOCAL_EXP_ID = 10000;  // lowest random exp id number
	  public static final int LOCAL_EXP_ID_RANGE = 10000; // range of random ids
	  
	  public static final int IDLE = 0;
	  public static final int RUNNING = 1;
	  public static final int PAUSED = 2;  // Currently unused
	  public static final int VIEWING_MODE = 3;
	  public static final int SYNCING_POINTS = 4;

	  public static final String TRACKER_STATE_PREFERENCE = "TrackerState";
	  public static final String POSIT_PROJECT_PREFERENCE = "PROJECT_ID";
	  public static final String POSIT_SERVER_PREFERENCE = "SERVER_ADDRESS";
	  public static final String ROW_ID_EXPEDITION_BEING_SYNCED = "RowIdExpeditionBeingSynced";
	  
	  // These settable Tracker preferences have to be identical to String resources
	  public static final String SWATH_PREFERENCE = 
		  "swathWidth"; // @string/swath_width
	  public static final String MINIMUM_DISTANCE_PREFERENCE = 
		  "minDistance"; // @string/min_recording_distance

	  /**
	   * Simply tells this Activity what preferences are being updated
	   * and provides the XML for the layout. The PreferenceActivity handles
	   * the rest. Whenever the user changes a preference value, the
	   * preference manager notifies any listeners that have been 
	   * registered with it.
	   * 
	   * In this case we use DefaultSharedPreferences where all other 
	   * Posit preferences are stored. It might make sense to define
	   * a "TrackerSettings" preference??
	   */
	  @Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			
			Log.d(TAG, "TrackerSettings, onCreate()");
			
			PreferenceManager.getDefaultSharedPreferences(this);

		    addPreferencesFromResource(R.xml.tracker_preferences);	
		}
}
