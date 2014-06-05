
/*
 * File: FindOverlay.java
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


import java.util.ArrayList;

import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.activity.FindActivity;
import org.hfoss.posit.android.api.plugin.FindActivityProvider;
import org.hfoss.posit.android.api.plugin.FindPluginManager;
import org.hfoss.posit.android.plugin.csv.CsvFindActivity;
import org.hfoss.posit.android.plugin.csv.CsvListFindsActivity;
import org.hfoss.posit.android.plugin.csv.CsvListFindsFragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

/**
 * Object for creating and adding overlays on the map, making them
 * tappable with clickable icons.
 */
public class FindOverlay extends ItemizedOverlay {
	private static final String TAG = "ItemizedOverlay";

	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	private Context mContext;
	private boolean isTappable;
	private String action;

	/**
	 * @param defaultMarker
	 */
	public FindOverlay(Drawable defaultMarker, Context c, boolean isTappable, String action) {
		super(boundCenterBottom(defaultMarker));
		mContext = c;
		this.isTappable = isTappable;
		this.action = action;
	}

	/* (non-Javadoc)
	 * @see com.google.android.maps.ItemizedOverlay#createItem(int)
	 */
	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	/* (non-Javadoc)
	 * @see com.google.android.maps.ItemizedOverlay#size()
	 */
	@Override
	public int size() {
		return mOverlays.size();
	}

	public void addOverlay(OverlayItem overlay) {
		mOverlays.add(overlay);
		populate();
	}

	/**
	 * Called when the user clicks on one of the Find icons
	 *   in the map. It shows a description of the Find
	 * @param pIndex is the Find's index in the ArrayList
	 */
	@Override
	protected boolean onTap(int pIndex) {
		// show the description
		// Toast.makeText(mContext, mOverlays.get(pIndex).getSnippet(), Toast.LENGTH_LONG).show();
		if (!isTappable)
			return false;
		Intent intent = new Intent();
		int id = Integer.parseInt(mOverlays.get(pIndex).getTitle());
		Log.i(TAG, "id= " + id);
		if (action != null && action.equals(CsvListFindsFragment.ACTION_CSV_FINDS)) {
			intent.setAction(action);
			intent.putExtra(action, id); // Pass the RowID to FindActivity
			intent.setClass(mContext, CsvFindActivity.class);
		}  else  {
			intent.setAction(Intent.ACTION_EDIT);
			intent.putExtra(Find.ORM_ID, id); // Pass the RowID to FindActivity
			intent.setClass(mContext, FindPluginManager.mFindPlugin.getmFindActivityClass());
		}
		mContext.startActivity(intent);
		return true;
	}
}