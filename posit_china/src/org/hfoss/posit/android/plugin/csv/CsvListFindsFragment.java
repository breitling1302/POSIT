/*
 * File: CsvListFindsFragment.java
 * 
 * Copyright (C) 2012 The Humanitarian FOSS Project (http://www.hfoss.org)
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
package org.hfoss.posit.android.plugin.csv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.activity.MapFindsActivity;
import org.hfoss.posit.android.api.fragment.FindFragment;
import org.hfoss.posit.android.api.fragment.ListFindsFragment;
import org.hfoss.posit.android.api.plugin.FindPlugin;
import org.hfoss.posit.android.api.plugin.FindPluginManager;
import org.hfoss.posit.android.api.plugin.FunctionPlugin;
import org.hfoss.posit.android.functionplugin.sms.ObjectCoder;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class CsvListFindsFragment extends ListFindsFragment {
	
	public static final String TAG="CsvListFindsActivity";
	public static final String DEFAULT_FILE = "Museums_and_Hours.csv";
	public static final String DEFAULT_DIRECTORY = "csv";
	public static final String FILENAME_TAG = "filename";
	public static final String ACTION_CSV_FINDS = "csv_finds";
	public static final String CSV_ID = "csv_id";
	
	protected static CsvListAdapter mAdapter = null;

	protected static List<CsvFind> finds;
	protected String fileName;

	/*
	 * Called when the Activity starts.
	 * 
	 * @param savedInstanceState
	 *            contains the Activity's previously frozen state. In this case
	 *            it is unused.
	 * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		Bundle arguments = getArguments();
		
		fileName = arguments.getString(FILENAME_TAG);
		if (finds == null && fileName != null) {
			File file = new File(fileName);
			finds = readFindsFromFile(file);
		} else if (finds == null) {
			File file = new File(Environment.getExternalStorageDirectory() + "/"
					+ DEFAULT_DIRECTORY + "/" + DEFAULT_FILE);
			finds = readFindsFromFile(file);
		}
	}
	
	/**
	 * Displays the find which the user selects
	 */
	@Override
	protected void displayFind(int index, String action, Bundle extras, FindFragment findFragment) {
		super.displayFind(index, action, extras, new CsvFindFragment());
	}
	
	/**
	 * Display the finds or show error message
	 */
	@Override
	public void onResume() {
		super.onResume();
		if (finds != null) {
			mAdapter = (CsvListAdapter) setUpAdapter(finds);
			fillList(mAdapter);
		} else {
			Log.i(TAG, "Cannot parse " + fileName);
			Toast.makeText(getActivity(), "Sorry, cannot parse " + fileName,
					Toast.LENGTH_SHORT).show();
		}
	}
	
	/**
	 * Creates the menus for this activity.
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if (mListMenuPlugins.size() > 0) {
			for (FunctionPlugin plugin: mListMenuPlugins) {
				MenuItem item = menu.add(plugin.getmMenuTitle());
				int id = getResources().getIdentifier(
						plugin.getmMenuIcon(), "drawable", "org.hfoss.posit.android");
				Log.i(TAG, "icon =  " + plugin.getmMenuIcon() + " id =" + id);
				item.setIcon(id);
				//item.setIcon(android.R.drawable.ic_menu_mapmode);				
			}
		}
		inflater.inflate(R.menu.csv_list_finds_menu, menu);		
	}

	/**
	 * Handles the various menu item actions.
	 * 
	 * @param featureId
	 *            is unused
	 * @param item
	 *            is the MenuItem selected by the user
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "onMenuitemSelected()");

		Intent intent;
		switch (item.getItemId()) {
		
		case R.id.map_finds_menu_item:
			Log.i(TAG, "Map finds menu item");
			intent = new Intent();
			intent.setAction(ACTION_CSV_FINDS);
			intent.setClass(getActivity(), MapFindsActivity.class);			
			startActivity(intent);
			break;
			
		default:
			if (mListMenuPlugins.size() > 0){
				for (FunctionPlugin plugin: mListMenuPlugins) {
					if (item.getTitle().equals(plugin.getmMenuTitle()))
						startActivity(new Intent(getActivity(), plugin.getmMenuActivity()));
				}
			}
			break;
		}
		return true;
	} // onMenuItemSelected

	
	public static List<? extends Find> getFinds() {
		return finds;
	}
	
	/**
	 * Returns the find with GUID=n, where GUIDs are numbered 1, 2, 3...
	 * But Finds are stored indexed 0, 1, ....  That's why we substract 1.
	 * @param n
	 * @return
	 */
	public static Find getFind(int n) {
		return finds.get(n-1);
	}
	
	
	protected CsvListAdapter setUpAdapter(List<? extends Find> finds) {

		int resId = getResources().getIdentifier("tracker_row", "layout", getActivity().getPackageName());
		CsvListAdapter adapter = new CsvListAdapter(getActivity(), resId, finds);

		return adapter;
	}


	/**
	 * Reads a CSV file and parses the contents as Finds.
	 * @param file, should be a comma-delimited CSV file, with no internal
	 * commas in any of the columns. 
	 * 
	 * The file should have the following header lines:
	 * #OBJECTID,LATITUDE,LONGITUDE,NAME,TEL,URL,ADRESS1,ADDRESS2,CITY,ZIP
	 * #FORMAT:guid,latitude,longitude,name,phone,url,address1,address2,city,zip
	 * #MAP:guid,latitude,longitude,name,phone,url,description,description,description,description
	 * 
	 * The first line is the CSV header, as found in the raw data file.
	 * The second line is the CSV header with some POSIT attribute names
	 * The third line is POSIT attributes that each item in 2nd line should be mapped to.
	 *
	 * @return
	 */
	protected List<CsvFind> readFindsFromFile(File file) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;

			FindPlugin plugin = FindPluginManager.mFindPlugin;
			if (plugin == null) {
				Log.e(TAG, "Could not retrieve Find Plugin.");
				return null;
			}

			int project_id = PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt(this.getString(R.string.projectPref), 0);
			
			String formatStr = br.readLine().toLowerCase();                  // Header Line #1
			formatStr = formatStr.substring(1);			

			// Parse each line into a CsvFind and add it to a list.
			finds = new ArrayList<CsvFind>();
			while ((line = br.readLine()) != null) {
				CsvFind f = parseCsvFind(line, formatStr);
				if (f != null) {
					f.setProject_id(project_id);
					finds.add(f);
				}
			}
		} catch (IOException e) {
			Log.e(TAG, "IO Exception reading from file "
					+ e.getMessage());
			e.printStackTrace();
			Toast.makeText(getActivity(), "Error occurred reading from file",
					Toast.LENGTH_LONG).show();
			getActivity().finish();
		}
		return finds;
	}
	
	/**
	 * Attempts to parse a comma-delimited String as a CsvFind object.
	 * This is a simplified version of the method Ryan McLeod wrote for
	 * Sms Plugin.
	 * @param csv The comma delimited string
	 * @return A CsvFind object, or null if it couldn't be parsed.
	 */
	private CsvFind parseCsvFind(String csv, String mapstring) {
		String map[] = mapstring.split(",");
		String values[] = csv.split(",");

		Log.i(TAG, csv);

		// Attempt to construct a Find
		CsvFind find;
		FindPlugin plugin = FindPluginManager.mFindPlugin;
		if (plugin == null) {
			Log.e(TAG, "Could not retrieve Find Plugin.");
			return null;
		}
		find = new CsvFind();
		Log.i(TAG,"Processing " + find.getClass());

		// Need to get attributes for the Find
		Bundle bundle = find.getDbEntries();
		List<String> keys = new ArrayList<String>(bundle.keySet());

		Log.i(TAG,"mapstring=" + mapstring);
		Log.i(TAG,"keys=" + keys);
		Log.i(TAG,"values=" + values);

		for (int i = 0; i < values.length; i++) {
			String key = map[i];

			// Get type of this entry
			Class<Object> type = null;
			if (keys.contains(key)) {
				try {
					type = find.getType(key);
				} catch (NoSuchFieldException e) {
					Log.e(TAG, "Encountered no such field exception on field: " + key);
					e.printStackTrace();
					continue;
				}
			} else {
				continue;
			}
			// See if we can decode this value. If not, then we can't make a Find.
			Serializable obj;
			try {
				obj = (Serializable) ObjectCoder.decode(values[i], type);
			} catch (IllegalArgumentException e) {
				Log.e(TAG, "Failed to decode value for attribute \"" + key
						+ "\", string was \"" + values[i] + "\"");
				return null;
			}

			// Decode successful!
			bundle.putSerializable(key, obj);
		}
		// Make Find
		find.updateObject(bundle);
		return find;
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
				int find_id = Integer.parseInt( (String) ((TextView)view.findViewById(R.id.id)).getText());

				Find find = new Find();
				find.setName((String) ((TextView)view.findViewById(R.id.name)).getText());
				find.setDescription((String) ((TextView)view.findViewById(R.id.description_id)).getText());
				find.setLatitude(Double.parseDouble((String) ((TextView)view.findViewById(R.id.latitude)).getText()));
				find.setLongitude(Double.parseDouble((String) ((TextView)view.findViewById(R.id.longitude)).getText()));
				Intent intent = new Intent(parent.getContext(), CsvFindActivity.class);

				find.setGuid((String) ((TextView) view.findViewById(R.id.id)).getText());

				intent.setAction(ACTION_CSV_FINDS);
				intent.putExtra(ACTION_CSV_FINDS, Integer.parseInt(find.getGuid())); // CsvFind use integer guids, 1, 2, 3 ...
				intent.putExtra("findbundle",  find.getDbEntries());
				startActivity(intent);
			}
		});
	}

	/**
	 * Customized Adapter class for CsvFinds.
	 */
	protected class CsvListAdapter extends ArrayAdapter<Find> {
		protected List<? extends Find> items;
		Context context;

		public CsvListAdapter(Context context, int textViewResourceId, List list) {
			super(context, textViewResourceId, list);
			Log.i(TAG, "FileViewListAdapter constructor");
			this.items = list;
			this.context = context;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;

			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				int resId = getResources().getIdentifier("csv_list_row", "layout", getActivity().getPackageName());
				v = vi.inflate(resId, null);

			}
			Find find = items.get(position);
			if (find != null) {
				TextView tv = (TextView) v.findViewById(R.id.name);
				if (tv != null) 
					tv.setText("" + find.getName());
				tv = (TextView) v.findViewById(R.id.description_id);
				if (tv != null)
					tv.setText(((CsvFind)find).getFullAddress());
				tv = (TextView) v.findViewById(R.id.latitude);
				if (tv != null) {
					tv.setVisibility(View.GONE);
					tv.setText("" + find.getLatitude());
				}
				tv = (TextView) v.findViewById(R.id.longitude);
				if (tv != null) {
					tv.setVisibility(View.GONE);
					tv.setText("" + find.getLongitude());
				}
				tv = (TextView) v.findViewById(R.id.id);
				if (tv != null) {
					tv.setText(find.getGuid());
					tv.setVisibility(View.GONE);
				}
				
				ImageView iv = (ImageView) v.findViewById(R.id.find_image);
				if (iv != null && Integer.parseInt(find.getGuid()) % 2 == 0) {
					iv.setImageResource(R.drawable.museum_icon_32);
					v.setBackgroundColor(Color.DKGRAY);
				}
				else if (iv != null) {
					iv.setImageResource(R.drawable.museum_icon);
					v.setBackgroundColor(Color.BLACK);
				}
			
			}
			return v;
		}

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
}
