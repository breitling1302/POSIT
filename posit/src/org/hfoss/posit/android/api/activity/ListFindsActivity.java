/*
 * File: ListFindsActivity.java
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
import org.hfoss.posit.android.api.fragment.ListFindsFragment;

import com.actionbarsherlock.view.MenuItem;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;

/**
 * This activity shows a list of all the finds and allows interaction with find currently
 * stored on the phone.
 * 
 *
 */
public class ListFindsActivity extends OrmLiteBaseFragmentActivity<DbManager> {
	protected ListFindsFragment finds;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_finds);
		
		Bundle extras = getIntent().getExtras();
		if(extras == null)
			extras = new Bundle();
		
		if (finds == null) {
			finds = new ListFindsFragment();
		}
		
		finds.setArguments(extras);
		
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.replace(R.id.finds, finds);
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.commit();
	}
	
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		return super.onMenuItemSelected(featureId, item);
	}
}
