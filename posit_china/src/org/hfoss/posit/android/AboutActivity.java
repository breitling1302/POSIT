/*
 * File: AboutActivity.java
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
package org.hfoss.posit.android;

import org.hfoss.posit.android.R;

import com.actionbarsherlock.app.SherlockActivity;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TextView;


/**
 * Displays basic information about POSIT, including name,
 *  developer (HFOSS), and the names of development team members.
 */
public class AboutActivity extends SherlockActivity {
	private static final String TAG = "AboutActivity";
	 
	/**
	 * To change 'about posit' edit the about_copyright XML.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		setContentView(R.layout.about_copyright);
        // Prompt for new users
		String server = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.serverPref), "");
        if (server == null)
        	server = getString(R.string.defaultServer);
        TextView serverTView = (TextView) findViewById(R.id.currentServer);
        serverTView.setText("Synced with:\n" + server);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
}

