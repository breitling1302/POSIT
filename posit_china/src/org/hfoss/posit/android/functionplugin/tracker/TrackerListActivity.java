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

package org.hfoss.posit.android.functionplugin.tracker;

import java.text.SimpleDateFormat;
import java.util.List;

import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.database.DbHelper;
import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.api.plugin.FindPluginManager;
import org.hfoss.posit.android.R;

import com.j256.ormlite.android.apptools.OrmLiteBaseListActivity;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleCursorAdapter.ViewBinder;

/**
 * Provides a clickable list of Expeditions currently stored on the phone.
 * @author rmorelli
 *
 */
public class TrackerListActivity extends OrmLiteBaseListActivity<DbManager> implements ViewBinder {
	private static final String TAG = "PositTracker";
	
	private int mProjectId;
	private SharedPreferences mSharedPrefs;
	
	List<? extends Expedition> tracks;
	protected static TrackerListAdapter mAdapter = null;


	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mProjectId = mSharedPrefs.getInt(TrackerSettings.POSIT_PROJECT_PREFERENCE, -1);
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
		mAdapter = (TrackerListAdapter) setUpAdapter();
		fillList(mAdapter);
	}
	
	
	/**
	 * Puts the items from the DB table into the rows of the view.
	 */
	private void fillList(ListAdapter adapter) {
		setListAdapter(adapter);

		ListView lv = getListView();
		lv.setTextFilterEnabled(true);
		lv.setOnItemClickListener(new OnItemClickListener() {

			/**
			 * Returns the expedition Id to the TrackerActivity
			 */
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent result = new Intent();
				int exp_id = Integer.parseInt( (String) ((TextView)view.findViewById(R.id.expedition_id)).getText());
				result.putExtra(getHelper().EXPEDITION_ROW_ID, exp_id);
				setResult(Activity.RESULT_OK, result);
				Log.d(TAG, "TrackerListActivity, onListItemClick position= " + position + " id = " +  exp_id);
				finish();
			}
		});
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
		Log.d(TAG, "TrackerListActivity, onPause()");

	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d(TAG, "TrackerListActivity, onStop()");
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "TrackerListActivity, onDestroy()");
	}

	
	protected TrackerListAdapter setUpAdapter() {

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		int projectId = prefs.getInt(getString(R.string.projectPref), 0);
		
		tracks = this.getHelper().fetchExpeditionsByProjectId(projectId);
		Log.i(TAG, "# tracks = " + tracks.size());

		int resId = getResources().getIdentifier("tracker_row", "layout", getPackageName());
		TrackerListAdapter adapter = new TrackerListAdapter(this, resId, tracks);

		return adapter;
	}

	protected class TrackerListAdapter extends ArrayAdapter<Expedition> {
		protected List<? extends Expedition> items;

		public TrackerListAdapter(Context context, int textViewResourceId, List list) {
			super(context, textViewResourceId, list);
			Log.i(TAG, "TrackerListAdapter constructor");
			this.items = list;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

				int resId = getResources().getIdentifier("tracker_row", "layout", getPackageName());
				v = vi.inflate(resId, null);

			}
			Expedition expedition = items.get(position);
			if (expedition != null) {
				TextView tv = (TextView) v.findViewById(R.id.expedition_id);
				tv.setText("" + expedition.expedition_num);
				tv = (TextView) v.findViewById(R.id.project_id);
				tv.setText("" + expedition.project_id);
			}
			return v;
		}
		
	}
	
	

}
