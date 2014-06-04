/*
 * File: FindFragment.java
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
package org.hfoss.posit.android.api.fragment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.LocaleManager;
import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.api.plugin.AddFindPluginCallback;
import org.hfoss.posit.android.api.plugin.FindPlugin;
import org.hfoss.posit.android.api.plugin.FindPluginManager;
import org.hfoss.posit.android.api.plugin.FunctionPlugin;
import org.hfoss.posit.android.plugin.csv.CsvListFindsFragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/**
 * A fragment which is used to display a find or used to create a find.
 *
 */
public class FindFragment extends OrmLiteFragment<DbManager>
		implements OnClickListener, OnItemClickListener, LocationListener{

	private static final String TAG = "FindFragment";

	private boolean mGeoTagEnabled;
	private LocationManager mLocationManager = null;
	protected Location mCurrentLocation = null;

	// UI Elements
	private EditText mNameET = null;
	private EditText mDescriptionET = null;
	private TextView mGuidTV = null;
	private TextView mGuidRealTV = null;
	private TextView mTimeTV = null;
	private TextView mLatTV = null;
	private TextView mLatitudeTV = null;
	private TextView mLongTV = null;
	private TextView mLongitudeTV = null;
	private TextView mAdhocTV = null;
	
	private Find mFind = null; // For action=update or bundled Finds


	private ArrayList<FunctionPlugin> mAddFindMenuPlugins = null;
	
	/**
	 * This may be invoked by a FindActivity subclass, which may or may not have
	 * latitude and longitude fields.
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {

		super.onActivityCreated(savedInstanceState);
		
		// Sets listeners for various UI elements
		initializeListeners();

		mAddFindMenuPlugins = FindPluginManager
				.getFunctionPlugins(FindPluginManager.ADD_FIND_MENU_EXTENSION);
		
		setHasOptionsMenu(true);

		// Initialize all UI elements for later uses
		mNameET = (EditText) getView().findViewById(R.id.nameEditText);
		mDescriptionET = (EditText) getView().findViewById(R.id.descriptionEditText);
		mGuidTV = (TextView) getView().findViewById(R.id.guidValueTextView);
		mGuidRealTV = (TextView) getView().findViewById(R.id.guidRealValueTextView);
		mTimeTV = (TextView) getView().findViewById(R.id.timeValueTextView);
		mLatTV = (TextView) getView().findViewById(R.id.latitudeTextView);
		mLatitudeTV = (TextView) getView().findViewById(R.id.latitudeValueTextView);
		mLongTV = (TextView) getView().findViewById(R.id.longitudeTextView);
		mLongitudeTV = (TextView) getView().findViewById(R.id.longitudeValueTextView);
		mAdhocTV = (TextView) getView().findViewById(R.id.isAdhocTextView);

		// Check if settings allow Geotagging
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		mGeoTagEnabled = prefs.getBoolean("geotagKey", true);

		// If enabled, get location manager and provider
		if (mGeoTagEnabled) {
			mLocationManager = (LocationManager) getActivity()
					.getSystemService(Context.LOCATION_SERVICE);
		}

		// Set the content of UI elements, either auto-generated or retrieved
		// from a Find
		Bundle extras = getArguments();

		if (extras != null && !(extras.size() == 1 && extras.containsKey("ACTION"))) {
			// Existing Find
			if (getAction().equals(Intent.ACTION_EDIT)) { 
				int id = extras.getInt(Find.ORM_ID);
				Log.i(TAG, "ORM_id = " + id);
//				Find find = getHelper().getFindById(id);
				mFind = getHelper().getFindById(id);
				Log.i(TAG, "Updating: " + mFind);
				displayContentInView(mFind);
			} else 
				
			// Bundled Find (from SMS)
			if (getAction().equals(Intent.ACTION_INSERT_OR_EDIT)) {
				// Pull a Bundle corresponding to a Find from the Intent and put
				// that in the view
				Bundle findBundle = extras.getBundle("findbundle");
				//Find find;
				try {
					FindPlugin plugin = FindPluginManager.mFindPlugin;
					if (plugin == null) {
						Log.e(TAG, "Could not retrieve Find Plugin.");
						Toast.makeText(getActivity(), "A fatal error occurred while trying to start FindActivity", 
								Toast.LENGTH_LONG).show();
						getActivity().finish();
						return;
					}
					mFind = plugin.getmFindClass().newInstance();
				} catch (IllegalAccessException e) {
					Toast.makeText(getActivity(), "A fatal error occurred while trying to start FindActivity", 
							Toast.LENGTH_LONG).show();
					getActivity().finish();
					return;
				} catch (java.lang.InstantiationException e) {
					Toast.makeText(getActivity(), "A fatal error occurred while trying to start FindActivity", 
							Toast.LENGTH_LONG).show();
					getActivity().finish();
					return;
				}
				mFind.updateObject(findBundle);
				displayContentInView(mFind);
			} else 
			// CSV Find	
			if (getAction().equals(CsvListFindsFragment.ACTION_CSV_FINDS)) {
				
			}
		// New Find
		} else {  
			Log.i("TAG", "new find");
			// Set real GUID
			if (mGuidRealTV != null)
				mGuidRealTV.setText(UUID.randomUUID().toString());
			// Set displayed GUID
			if (mGuidTV != null)
				mGuidTV.setText(mGuidRealTV.getText().toString()
						.substring(0, 8)
						+ "...");
			// Set Time
			if (mTimeTV != null) {
				SimpleDateFormat dateFormat = new SimpleDateFormat(
						"yyyy/MM/dd HH:mm:ss");
				Date date = new Date();
				mTimeTV.setText(dateFormat.format(date));
			}

			if (mGeoTagEnabled) {
				// Set Longitude and Latitude
				mLocationManager.requestLocationUpdates(
						LocationManager.NETWORK_PROVIDER, 60000, 0, this);
				mLocationManager.requestLocationUpdates(
						LocationManager.GPS_PROVIDER, 60000, 0, this);

				Location netLocation = mLocationManager
						.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
				Location gpsLocation = mLocationManager
						.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

				if (gpsLocation != null) {
					mCurrentLocation = gpsLocation;
				} else {
					mCurrentLocation = netLocation;
				}

				if (mCurrentLocation == null) {
					Log.i(TAG, "Location issue, mCurrentLocation = "
							+ mCurrentLocation);
					if (mLongitudeTV != null)
						mLongitudeTV.setText("0.0");
					if (mLatitudeTV != null)
						mLatitudeTV.setText("0.0");
					// Toast.makeText(this, "Unable to retrieve GPS info." +
					// " Please make sure your Data or Wi-Fi is enabled.",
					// Toast.LENGTH_SHORT).show();
					// Log.i(TAG,
					// "Cannot request location updates; Data or Wifi might not be enabled.");
				} else {
					if (mLongitudeTV != null)
						mLongitudeTV.setText(String.valueOf(mCurrentLocation
								.getLongitude()));
					if (mLatitudeTV != null)
						mLatitudeTV.setText(String.valueOf(mCurrentLocation
								.getLatitude()));
				}
			} else {
				if (mLongitudeTV != null && mLongTV != null) {
					mLongitudeTV.setVisibility(TextView.INVISIBLE);
					mLongTV.setVisibility(TextView.INVISIBLE);
				}
				if (mLatitudeTV != null && mLatTV != null) {
					mLatitudeTV.setVisibility(TextView.INVISIBLE);
					mLatTV.setVisibility(TextView.INVISIBLE);
				}
			}
		}
	}

	/**
	 * Function which imitates Activity.getIntent().getAction()
	 * 
	 * @return the action for the fragment
	 */
	protected String getAction() {
		return getArguments().getString("ACTION");
	}

	/**
	 * Creates the view which is displayed in the fragment
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Get the custom add find layout from the plugin settings, if there us one
		int resId  = getResources().getIdentifier(
				FindPluginManager.mFindPlugin.mAddFindLayout, "layout",
				getActivity().getPackageName());
		
		return inflater.inflate(resId, container, false);
	}
	
	/**
	 * Request Updates whenever the activity is paused.
	 */
	@Override
	public void onResume() {
		super.onResume();
		
		LocaleManager.setDefaultLocale(getActivity()); // Locale Manager should
		
		if (mGeoTagEnabled) { 
			mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 0, this);
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 0, this);
			
			Location netLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			Location gpsLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			
			if (gpsLocation != null) {
				mCurrentLocation = gpsLocation;
			} else {
				mCurrentLocation = netLocation;
			}

			if (mCurrentLocation == null) {
				Toast
						.makeText(
								getActivity(),
								"Unable to retrieve GPS info."
										+ " Please make sure your Data or Wi-Fi is enabled.",
								Toast.LENGTH_SHORT).show();
				Log
						.i(TAG,
								"Cannot request location updates; Data or Wifi might not be enabled.");
			}
		}
	}

	/**
	 * Remove Updates whenever the activity is paused.
	 */
	@Override
	public void onPause() {
		super.onPause();
		if (mLocationManager != null)
			mLocationManager.removeUpdates(this);
	}

	/**
	 * Remove Updates whenever the activity is finished.
	 */
	@Override
	public void onDestroyView() {
		Log.i(TAG, "onFinish()");
		if (mGeoTagEnabled)
			mLocationManager.removeUpdates(this);
		mLocationManager = null;
		mCurrentLocation = null;
		doFinishCallbacks();
		super.onDestroyView();
	}

	/**
	 * Sets listeners for various UI elements.
	 */
	protected void initializeListeners() {
		Button saveButton = ((Button) getView().findViewById(R.id.saveButton));
		if (saveButton != null)
			saveButton.setOnClickListener(this);
	}

	/**
	 * Typical onClick stuff--shouldn't need to override anything here for the
	 * most basic functionality, but you can! (non-Javadoc)
	 * 
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.saveButton:
			if (saveFind()) {
				getActivity().finish();
			}
			break;
		}
	}

	/**
	 * Creates the menu for this activity by inflating a menu resource file.
	 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		
		inflater.inflate(R.menu.add_finds_menu, menu);

		if (getAction().equals(Intent.ACTION_INSERT)) {
			menu.removeItem(R.id.delete_find_menu_item);
		}

		// Add menu options based on function plug-ins
		for (FunctionPlugin plugin : mAddFindMenuPlugins) {
				// For all other function plug-ins
				MenuItem item = menu.add(plugin.getmMenuTitle());
				int resId = getResources().getIdentifier(plugin.getmMenuIcon(),
						"drawable", getActivity().getPackageName());
				item.setIcon(resId);
				if (resId != 0) // icon found; display it on the action bar
				    item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
				else // icon not found
				    item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		}
	}

	/**
	 * Handles the various menu item actions.
	 * 
	 * @param featureId
	 *            is unused
	 * @param item
	 *            is the MenuItem selected by the user
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		Log.i(TAG, "onOptionsItemSelected()");
		
		switch (item.getItemId()) {
		case R.id.save_find_menu_item:
			if (saveFind()) {
				getActivity().finish();
			}
			break;

		case R.id.delete_find_menu_item:
			showDialog(DeleteFindsDialogFragment.CONFIRM_DELETE_FIND_DIALOG);
			break;

		default:
			for (FunctionPlugin plugin : mAddFindMenuPlugins) {
				if (item.getTitle().equals(plugin.getmMenuTitle())) {

					Intent intent = new Intent(getActivity(), plugin.getmMenuActivity());

					Log.i(TAG, "plugin=" + plugin);
					Class<AddFindPluginCallback> callbackClass = null;
					Object o;
					try {
						Find find = retrieveContentFromView();
						View view = getView();
						String className = plugin.getAddFindCallbackClass();
						if (className != null) {
							callbackClass = (Class<AddFindPluginCallback>) Class.forName(className);
							o = (AddFindPluginCallback) callbackClass.newInstance();
							((AddFindPluginCallback) o).menuItemSelectedCallback(
									getActivity().getApplication(),
									find,
									view,
									intent);
						}
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (java.lang.InstantiationException e) {
						e.printStackTrace();
					}

					// Put Find information in Intent so that it may be
					// utilized by the plugin.
					// This is done by creating a find and then extracting
					// the ContentValues object from it
					// because I want to make sure that we have the same
					// behaviour as retrieveContentFromView()
					// without having to duplicate code.
					Find find = retrieveContentFromView();
					Bundle bundle = find.getDbEntries();
					intent.putExtra("DbEntries", bundle);
					if (plugin.getActivityReturnsResult())
						startActivityForResult(intent, plugin
								.getActivityResultAction());
					else
						startActivity(intent);
				}
			}
			break;
		}
		return true;
	}

	/**
	 * Called to remove any data when the Activity which contains the fragment is ended.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		// Go through all Function Plug-in to find the
		// one that matches the intent request code
		for (FunctionPlugin plugin : mAddFindMenuPlugins) {
			if (requestCode == plugin.getActivityResultAction()) {
				Log.i(TAG, "plugin=" + plugin);
				Class<AddFindPluginCallback> callbackClass = null;
				Object o;
				try {
					Find find = retrieveContentFromView();
					View view = getView();
					String className = plugin.getAddFindCallbackClass();
					if (className != null) {
						callbackClass = (Class<AddFindPluginCallback>) Class.forName(className);
						o = (AddFindPluginCallback) callbackClass.newInstance();
						((AddFindPluginCallback) o).onActivityResultCallback(
								getActivity().getApplication(),
								find,
								view,
								intent);
					}
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (java.lang.InstantiationException e) {
					e.printStackTrace();
				}

//				if (plugin.getmMenuTitle().equals("Capture Media")) {
//					if (intent != null) {
//						// do we get an image back?
//						if (intent.getStringExtra("Photo") != null) {
//							img_str = intent.getStringExtra("Photo");
//							byte[] c = Base64.decode(img_str, Base64.DEFAULT);
//							Bitmap bmp = BitmapFactory.decodeByteArray(c, 0,
//									c.length);
//							photo.setImageBitmap(bmp);// display the retrieved
//							// image
//							photo.setVisibility(View.VISIBLE);
//						}
//					}
//				} 
//				else {
//					// Do something specific for other function plug-ins
//				}
			}
		}
	}

	/**
	 * Retrieves values from the View fields and stores them in a Find instance.
	 * This method is invoked from the Save menu item. It also marks the find
	 * 'unsynced' so it will be updated to the server.
	 * 
	 * @return a new Find object with data from the view.
	 */
	protected Find retrieveContentFromView() {

		// Get the appropriate find class from the plug-in
		// manager and make an instance of it
		Class<Find> findClass = FindPluginManager.mFindPlugin.getmFindClass();
		Find find = null;
		
		if (getAction().equals(Intent.ACTION_EDIT) ||
				getAction().equals(Intent.ACTION_INSERT_OR_EDIT)) {
			find = mFind;
		} else {
			try {
				find = findClass.newInstance();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (java.lang.InstantiationException e) {
				e.printStackTrace();
			}
		}

		// Set GUID
		// NOTE: Some derived finds may not have a GUID field. But the Guid must
		// be
		// set anyway because it used as the Find ID by the Posit server.
		if (mGuidRealTV != null) {
			find.setGuid(mGuidRealTV.getText().toString());
		} else {
			find.setGuid(UUID.randomUUID().toString());
		}

		// Set Time
		if (mTimeTV != null) {
			SimpleDateFormat dateFormat = new SimpleDateFormat(
					"yyyy/MM/dd HH:mm:ss");
			String value = mTimeTV.getText().toString();
			if (value.length() == 10) {
				dateFormat = new SimpleDateFormat("yyyy/MM/dd");
			}
			try {
				find.setTime(dateFormat.parse(value));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		// Set Name
		if (mNameET != null) {
			find.setName(mNameET.getText().toString());
		}

		// Set Description
		if (mDescriptionET != null) {
			find.setDescription(mDescriptionET.getText().toString());
		}

		// Set Longitude and Latitude
		if (mLatitudeTV != null && mLongitudeTV != null) {
			if (mGeoTagEnabled && (!mLatitudeTV.getText().equals("Getting latitude...") && !mLongitudeTV.getText().equals("Getting longitude..."))) {
				find.setLatitude(Double.parseDouble(mLatitudeTV.getText()
						.toString()));
				find.setLongitude(Double.parseDouble(mLongitudeTV.getText()
						.toString()));
			} else {
				find.setLatitude(0);
				find.setLongitude(0);
			}
		}

		if (mAdhocTV != null) {
			find.setIs_adhoc(Integer.parseInt((String) mAdhocTV.getText()));
		}
		// Set Project ID
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		int projectId = prefs.getInt(getString(R.string.projectPref), 0);
		find.setProject_id(projectId);

		// Mark the find unsynced
		// find.setSynced(Find.NOT_SYNCED);

		return find;
	}

	/**
	 * Retrieves values from a Find object and puts them in the View.
	 * 
	 * @param a
	 *            Find object
	 */
	@SuppressWarnings("unchecked")
	protected void displayContentInView(Find find) {

		// Set real GUID
		if (mGuidRealTV != null) {
			mGuidRealTV.setText(find.getGuid());
		}

		// Set displayed GUID
		if (mGuidTV != null) {
			String id = mGuidRealTV.getText().toString();
			mGuidTV.setText(id.substring(0, Math.min(8, id.length())) + "...");
		}

		// Set Time
		if (mTimeTV != null) {
			SimpleDateFormat dateFormat = new SimpleDateFormat(
					"yyyy/MM/dd HH:mm:ss");
			String time = dateFormat.format(find.getTime());
			mTimeTV.setText(time);
		}

		// Set Name
		if (mNameET != null) {
			mNameET.setText(find.getName());
		}

		// Set Description
		if (mDescriptionET != null) {
			mDescriptionET.setText(find.getDescription());
		}

		// Set Longitude and Latitude
		if (mLongitudeTV != null && mLatitudeTV != null) {
			mLongitudeTV.setText(String.valueOf(find.getLongitude()));
			mLatitudeTV.setText(String.valueOf(find.getLatitude()));
		}

		/**
		 * For each plugin, call its displayFindInViewCallback.
		 */
		for (FunctionPlugin plugin : mAddFindMenuPlugins) {
			Log.i(TAG, "plugin=" + plugin);
			Class<AddFindPluginCallback> callbackClass = null;
			Object o;
			try {
				View view = getView();
				String className = plugin.getAddFindCallbackClass();
				if (className != null) {
					callbackClass = (Class<AddFindPluginCallback>) Class.forName(className);
					o = (AddFindPluginCallback) callbackClass.newInstance();
					((AddFindPluginCallback) o).displayFindInViewCallback(
							getActivity().getApplication(),
							find,
							view);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (java.lang.InstantiationException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * When we get a fresh location, update our class variable..
	 */
	public void onLocationChanged(Location location) {
		if (isBetterLocation(location, mCurrentLocation)) {
			mCurrentLocation = location;
			// if we are creating a new find update the location as we get updates
			if (getAction().equals(Intent.ACTION_INSERT)) {
			    if (mLongitudeTV != null)
			        mLongitudeTV.setText(String.valueOf(mCurrentLocation
			                .getLongitude()));
			    if (mLatitudeTV != null)
			        mLatitudeTV.setText(String.valueOf(mCurrentLocation
			                .getLatitude()));
				
			}
			Log.i(TAG, "Got a new location: " + mCurrentLocation.getLatitude()
					+ "," + mCurrentLocation.getLongitude());
		}
	}

	public void onProviderDisabled(String provider) {
	}

	public void onProviderEnabled(String provider) {
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
	}
	
	public void showDialog(int num) {
		
		// DialogFragment.show() will take care of adding the fragment
	    // in a transaction.  We also want to remove any currently showing
	    // dialog, so make our own transaction and take care of that here.
	    FragmentTransaction ft = getFragmentManager().beginTransaction();
	    Fragment prev = getFragmentManager().findFragmentByTag("dialog");
	    if (prev != null) {
	        ft.remove(prev);
	    }
	    ft.addToBackStack(null);

	    // Create and show the dialog.
	    DeleteFindsDialogFragment newFragment = DeleteFindsDialogFragment.newInstance(num, getArguments().getInt(Find.ORM_ID));
	    newFragment.show(ft, "dialog");
	}

	/**
	 * Save the find which is displayed in the fragment
	 * 
	 * @return	success of saving the find
	 */
	@SuppressWarnings("unchecked")
	protected boolean saveFind() {
		Log.i(TAG, "saveFind()");
		int rows = 0;
		Find find = retrieveContentFromView();
		prepareForSave(find);

		// A valid GUID is required
		if (!isValidGuid(find.getGuid())) {
			Toast.makeText(getActivity(), "You must provide a valid Id for this Find.",
					Toast.LENGTH_LONG).show();
			return false;
		}

		// A name is not always required in derived classes
		String name = find.getName();
		if (name != null && name.equals("")) {
			// if (find.getName().equals("")){
			Toast.makeText(getActivity(), "You must provide a name for this Find.",
					Toast.LENGTH_LONG).show();
			return false;
		}

		// Either create a new Find or update the existing Find
		if (getAction().equals(Intent.ACTION_INSERT))
			rows = getHelper().insert(find);
		else if (getAction().equals(Intent.ACTION_EDIT)) {
			find.setId(getArguments().getInt(Find.ORM_ID));
			find.setStatus(Find.IS_NOT_SYNCED);
			rows = getHelper().update(find);
		} else if (getAction().equals(Intent.ACTION_INSERT_OR_EDIT)) {
			// Check if a Find with the same GUID already exists and update it if so
			Find sameguid = getHelper().getFindByGuid(find.getGuid());
			if (sameguid == null) {
				rows = getHelper().insert(find);
			} else {
				find.setId(sameguid.getId());
				rows = getHelper().update(find);
			}
		} else
			rows = 0; // Something wrong with intent
		
		if (rows > 0) {
			Log.i(TAG, "Find " + getAction() + " successful: " + find);
		} else
			Log.e(TAG, "Find " + getAction() + " not successful: " + find);

		/**
		 * For each plugin, call its displayFindInViewCallback.
		 */
		for (FunctionPlugin plugin : mAddFindMenuPlugins) {
			Log.i(TAG, "plugin=" + plugin);
			Class<AddFindPluginCallback> callbackClass = null;
			Object o;
			try {
				View view = getView();
				String className = plugin.getAddFindCallbackClass();
				if (className != null) {
					callbackClass = (Class<AddFindPluginCallback>) Class.forName(className);
					o = (AddFindPluginCallback) callbackClass.newInstance();
					((AddFindPluginCallback) o).afterSaveCallback(
							getActivity().getApplication(),
							find,
							view,
							rows > 0);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (java.lang.InstantiationException e) {
				e.printStackTrace();
			}
		}
		return rows > 0;
	}

	protected void prepareForSave(Find find) {
		// Stub : meant to be overridden in subclass
	}

	/**
	 * By default a Guid must not be the empty string. This method can be
	 * overridden in the plugin extension.
	 * 
	 * @param guid
	 * @return
	 */
	protected boolean isValidGuid(String guid) {
		return guid.length() != 0;
	}

	/**
	 * Determines whether one Location reading is better than the current
	 * Location fix
	 * 
	 * @param location The new Location that you want to evaluate
	 * 
	 * @param currentBestLocation The current Location fix, to which you want to
	 * compare the new one
	 */
	protected boolean isBetterLocation(Location location,
			Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > 1000 * 60 * 2;
		boolean isSignificantlyOlder = timeDelta < -1000 * 60 * 2;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
				.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate
				&& isFromSameProvider) {
			return true;
		}
		return false;
	}

	/**
	 *  Checks whether two providers are the same
	 */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}
	
	/**
	 * Performs a finish callback on all function plugins
	 */
	private void doFinishCallbacks() {
		for (FunctionPlugin plugin : mAddFindMenuPlugins) {
			Class<AddFindPluginCallback> callbackClass = null;
			Object o;
			try {
				Find find = retrieveContentFromView();
				View view = getView();
				
				String className = plugin.getAddFindCallbackClass();
				if (className != null) {
					callbackClass = (Class<AddFindPluginCallback>) Class.forName(className);
					o = (AddFindPluginCallback) callbackClass.newInstance();
					((AddFindPluginCallback) o).finishCallback(getActivity().getApplication(), find, view);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (java.lang.InstantiationException e) {
				e.printStackTrace();
			}
		}
	}
}
