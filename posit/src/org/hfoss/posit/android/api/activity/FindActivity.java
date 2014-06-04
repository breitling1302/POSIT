/*
 * File: FindActivity.java
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
package org.hfoss.posit.android.api.activity;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.api.fragment.FindFragment;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

/**
 *  This class retrieves an existing Find or creates a new one and displays a 
 *  form allowing the user to modify the find.
 *
 */
public class FindActivity extends OrmLiteBaseFragmentActivity<DbManager> {// Activity
	protected FindFragment find;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.find);
		
		Bundle extras = getIntent().getExtras();
		if(extras == null) {
			extras = new Bundle();
		}
		
		extras.putString("ACTION", getIntent().getAction());
		
		Log.i(TAG, "Find: " + find);
		if (find == null) {
			find = new FindFragment();
		}
		
		find.setArguments(extras);
		
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.replace(R.id.find, find);
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.commit();
	}
}
