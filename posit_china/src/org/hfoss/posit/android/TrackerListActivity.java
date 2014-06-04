/*
 * File: TrackerListActivity.java
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

import org.hfoss.posit.android.provider.PositDbHelper;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;

/**
 * Provides a clickable list of Expeditions currently stored on the phone.
 * @author rmorelli
 *
 */
public class TrackerListActivity extends ListActivity implements ViewBinder {
	private static final String TAG = "PositTracker";
	
	private int mProjectId;
	private SharedPreferences mSharedPrefs;
	private PositDbHelper mDbHelper;
	private Cursor mCursor;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mProjectId = mSharedPrefs.getInt(TrackerSettings.POSIT_PROJECT_PREFERENCE, -1);
		mDbHelper = new PositDbHelper(this);
		Log.d(TAG, "TrackerListActivity, Created TrackerList for project_id = " + mProjectId);
	}

	/**
	 * The list of expeditions is retrieved from the Db and displayed whenever
	 * the Activity is resumed. 
	 */
	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG,"TrackerListActivity,  Resuming");
		displayTracks();
	}

	/*
	 * Helper method to display the expeditions. It uses a SimpleCursorAdapter
	 * to managed the list. (Remember to close the cursor when exiting this
	 * Activity.)
	 */
	private void displayTracks() {
		
		// These arrays are defined in PositDbHelper to associate columns from the
		// Expedition table (columns) with the Views that will display their data.
		// columns[0] is displayed in views[0], etc.  See PositDbHelper for their defs.
		
		String[] columns = PositDbHelper.track_data;
		int [] views = PositDbHelper.track_views;
		
		mCursor = mDbHelper.fetchExpeditionsByProjectId(mProjectId);
		
		if (mCursor.getCount() == 0) { // No tracks
			setContentView(R.layout.tracker_list);
			mCursor.close();
			return;
		}
		
		startManagingCursor(mCursor); // NOTE: Can't close DB while managing cursor

		// CursorAdapter binds the data in 'columns' to the views in 'views' 
		// It repeatedly calls ViewBinder.setViewValue() (see below) for each column
		// NOTE: The columns and views are defined in PositDBHelper.  For each column
		// there must be a view and vice versa.
		
		SimpleCursorAdapter adapter = 
			new SimpleCursorAdapter(this, R.layout.tracker_row, mCursor, columns, views);
		adapter.setViewBinder(this);
		setListAdapter(adapter); 
	}
	
	/**
	 * This can be used to set each view's value.  We use it here to extract
	 * the expedition id's from the views and save them in an array. We retrieve
	 * them in onListItemClicked().
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		Intent result = new Intent();
		result.putExtra(PositDbHelper.EXPEDITION_ROW_ID, id);
		setResult(Activity.RESULT_OK, result);
		mCursor.close();
		Log.d(TAG, "TrackerListActivity, onListItemClick position= " + position + " id = " +  id);
		finish();
	}

	/**
	 * Required for the ViewBinder interface.  Unused at the moment. It could
	 * be used to modify the view as the data are being displayed.  As it stands
	 * data from the Cursor are simply placed in their corresponding Views using
	 * the arrays provided above to SimpleCursorAdapter.
	 */
	public boolean setViewValue(View v, Cursor cursor, int colIndex) {
		return false;
	}

	@Override
	protected void onPause() {
		super.onPause();
		mCursor.close();
		Log.d(TAG, "TrackerListActivity, onPause()");

	}

	@Override
	protected void onStop() {
		super.onStop();
		mCursor.close();
		Log.d(TAG, "TrackerListActivity, onStop()");
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
		mCursor.close();
		Log.d(TAG, "TrackerListActivity, onDestroy()");
	}

}
