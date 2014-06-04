/*
 * File: MapFindsActivity.java
 * 
 * Copyright (C) 2009 The Humanitarian FOSS Project (http://www.hfoss.org)
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

import java.util.List;

import org.hfoss.posit.android.provider.PositDbHelper;
import org.hfoss.posit.android.utilities.MyItemizedOverlay;
import org.hfoss.posit.android.utilities.Utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ZoomControls;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

/**
 *  This class retrieves Finds from the POSIT DB and
 *  displays them as an overlay on a Google map. When clicked, 
 *  the finds start a FindActivity. Allowing them to be edited.
 *
 */
public class MapFindsActivity extends MapActivity implements LocationListener {

	private static final String TAG = "MapFindsActivity";
	private MapView mMapView;
	private MapController mapController;
	private ZoomControls mZoom;
	private LinearLayout linearLayout;
	private List<Overlay> mapOverlays;
	private Drawable drawable;
	private boolean zoomFirst;
	private LinearLayout searchLayout;
	private Button search_first_Btn;
	private Button search_next_Btn;
	private Button search_prev_Btn;
	private Button search_last_Btn;

	private Cursor mCursor;  // Used for DB accesses
	private PositDbHelper mDbHelper;
	private LocationManager mLocationManager;

	/* (non-Javadoc)
	 * @see com.google.android.maps.MapActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.map_finds);
		linearLayout = (LinearLayout) findViewById(R.id.zoomView);
		mMapView = (MapView) findViewById(R.id.mapView);
		mZoom = (ZoomControls) mMapView.getZoomControls();
		linearLayout.addView(mZoom);
		zoomFirst = true;

		searchLayout = (LinearLayout) findViewById(R.id.search_finds_layout);
		search_first_Btn = (Button) findViewById(R.id.search_finds_first);
		search_next_Btn = (Button) findViewById(R.id.search_finds_next);
		search_prev_Btn = (Button) findViewById(R.id.search_finds_previous);
		search_last_Btn = (Button) findViewById(R.id.search_finds_last);
		
		search_first_Btn.setOnClickListener(search_first_lstn);
		search_next_Btn.setOnClickListener(search_next_lstn);
		search_prev_Btn.setOnClickListener(search_prev_lstn);
		search_last_Btn.setOnClickListener(search_last_lstn);
		
		// Update location every 60 seconds
		mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
	}
    
	/** 
	 * This method is called when the activity is ready to start 
	 *  interacting with the user. It is at the top of the Activity
	 *  stack.
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		mLocationManager.requestLocationUpdates(
				LocationManager.GPS_PROVIDER, 
				60000 /* Updates Every minute */, 
				50, 
				this);
		mapFinds();
		
		if (zoomFirst) {
			// Zoom into current position
			Log.d("MapFindsActivity:onResume", "Zoom into currrent location");
			int latitude = 0;
			int longitude = 0;
			
			LocationManager mlocManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
			
			Location loc = mlocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			
			Log.d("MapFindsActivity:onResume", "Got Location " + loc);
				
			if (loc != null) {
				latitude = (int) (loc.getLatitude()*1E6);
				longitude = (int) (loc.getLongitude()*1E6);
				mapController.setCenter(new GeoPoint(latitude, longitude));
				mapController.setZoom(14);
				zoomFirst = false;
			} else {
				// Move to first find
				if (!mCursor.moveToFirst()) {
					// No Finds
					Utils.showToast(this, "No Finds.");
					mapController.setZoom(1);
				} else {
					// Get first "Find"
					Utils.showToast(this, "Could not retrieve GPS position\nMoving to first find. ");
					latitude = (int) (mCursor.getDouble(mCursor.getColumnIndex(PositDbHelper.FINDS_LATITUDE))*1E6);
					longitude = (int) (mCursor.getDouble(mCursor.getColumnIndex(PositDbHelper.FINDS_LONGITUDE))*1E6);
					mapController.setCenter(new GeoPoint(latitude, longitude));
					mapController.setZoom(14);
					zoomFirst = false;
				}
			}
		}
		
		searchLayout.setVisibility(LinearLayout.INVISIBLE);
	}

	/*
	 * Called every 60 seconds for map updates.
	 * (non-Javadoc)
	 * @see android.location.LocationListener#onLocationChanged(android.location.Location)
	 */
	public void onLocationChanged(Location location) {
		
	}
	
	/**
	 * Called when the system is about to resume some other activity.
	 *  It can be used to save state, if necessary.  In this case
	 *  we close the cursor to the DB to prevent memory leaks.
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onPause(){
		super.onPause();
		stopManagingCursor(mCursor);
		mDbHelper.close(); // NOTE WELL: Can't close while managing cursor
		mCursor.close();
		if (mLocationManager != null) {
			mLocationManager.removeUpdates(this);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		mDbHelper.close();
		mCursor.close();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mDbHelper.close();
		mCursor.close();
	}

	private void mapFinds() {
		mDbHelper = new PositDbHelper(this);

		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		
		mCursor = mDbHelper.fetchFindsByProjectId(sp.getInt("PROJECT_ID", 0));		
		if (mCursor.getCount() == 0) { // No finds
			Utils.showToast(this, "No Finds to display");
			finish();
			return;
		}

		mapOverlays = mMapView.getOverlays();
		mapOverlays.add(mapLayoutItems(mCursor));	
		mapController = mMapView.getController();

		mDbHelper.close();
	}

	private  MyItemizedOverlay mapLayoutItems(Cursor c) {
		int latitude = 0;
		int longitude = 0;

		drawable = this.getResources().getDrawable(R.drawable.androidmarker);
		MyItemizedOverlay mPoints = new MyItemizedOverlay(drawable, this, true);
		c.moveToFirst();

		do {
			latitude = (int) (c.getDouble(c
					.getColumnIndex(PositDbHelper.FINDS_LATITUDE))*1E6);
			longitude = (int) (c.getDouble(c
					.getColumnIndex(PositDbHelper.FINDS_LONGITUDE))*1E6);

			String itemIdStr = "" + c.getString(c.getColumnIndex(PositDbHelper.FINDS_ID));
			String description = itemIdStr + "\n" 
			+ c.getString(c.getColumnIndex(PositDbHelper.FINDS_NAME));
			description += "\n" + c.getString(c.getColumnIndex(PositDbHelper.FINDS_DESCRIPTION));

			Log.i(TAG, latitude+" "+longitude+" "+description);
			mPoints.addOverlay(new OverlayItem(new GeoPoint(latitude,longitude),itemIdStr,description));
		} while (c.moveToNext());

		return mPoints;
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_I) {
			// Zoom In
			mapController.zoomIn();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_O) {
			// Zoom Out
			mapController.zoomOut();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_S) {
			// Switch on the satellite images
			mMapView.setSatellite(!mMapView.isSatellite());		
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_T) {
			// Switch on traffic overlays
			mMapView.setTraffic(!mMapView.isTraffic());
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_BACK) {
			finish();
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
		}
		
		return false;
	}
	
	/** 
	 * Creates the menu for this activity by inflating a menu resource file.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.map_finds_menu, menu);
		return true;
	} // onCreateOptionsMenu()
	
	/** 
	 * Handles the various menu item actions.
	 * @param featureId is unused
	 * @param item is the MenuItem selected by the user
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.my_location_mapfind_menu_item:
			myLocation();
			break;
		case R.id.search_finds_mapfind_menu_item:
			searchFinds();
			break;
		case R.id.center_finds_mapfind_menu_item:
			centerFinds();
			break;
		default:
			return false;
		}
		return true;
	} // onMenuItemSelected
	
	
	/**
	 * Zoom in a center on your current GPS position.
	 * If no GPS position is found it will present a toast.
	 */
	private void myLocation() {
		int latitude = 0;
		int longitude = 0;
		
		LocationManager mlocManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

		Location loc = mlocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		
		Log.d("MapFindsActivity:onResume", "Got Location " + loc);
		
		if (mlocManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			if (loc != null) {
				latitude = (int) (loc.getLatitude()*1E6);
				longitude = (int) (loc.getLongitude()*1E6);
				mapController.setCenter(new GeoPoint(latitude, longitude));
				mapController.setZoom(14);
			} else {
				Utils.showToast(this, "Could not retrieve GPS position. ");
			}
		} else {
			if (loc != null) {
				Utils.showToast(this, "GPS is disabled\nMoving to last known position. ");
				latitude = (int) (loc.getLatitude()*1E6);
				longitude = (int) (loc.getLongitude()*1E6);
				mapController.setCenter(new GeoPoint(latitude, longitude));
				mapController.setZoom(14);
			} else {
				Utils.showToast(this, "GPS is disabled\nCurrent position is unknown. ");
			}
		}
		
	} // myLocation
	
	/**
	 * Toggle arrows that will allow you to move between different finds.
	 * Also shows a toast to reflect the find that you are currently looking at.
	 */
	private void searchFinds() {
		if (searchLayout.getVisibility() == LinearLayout.VISIBLE) {
			searchLayout.setVisibility(LinearLayout.INVISIBLE);
		} else {
			searchLayout.setVisibility(LinearLayout.VISIBLE);
		}
	} // searchFinds
	
	private OnClickListener search_first_lstn = new OnClickListener() {
        public void onClick(View v) {
            // On Click for Search Finds First
        	if (!mCursor.moveToFirst()) {
				// No Finds
				Utils.showToast(v.getContext(), "No finds.");
        	} else {
        		int latitude;
        		int longitude;
        		
        		Utils.showToast(v.getContext(), "Find " + (mCursor.getPosition() + 1) + " of " + mCursor.getCount());
        		latitude = (int) (mCursor.getDouble(mCursor.getColumnIndex(PositDbHelper.FINDS_LATITUDE))*1E6);
				longitude = (int) (mCursor.getDouble(mCursor.getColumnIndex(PositDbHelper.FINDS_LONGITUDE))*1E6);
				mapController.setCenter(new GeoPoint(latitude, longitude));
        	}
        }
    };

    private OnClickListener search_next_lstn = new OnClickListener() {
        public void onClick(View v) {
            // On Click for Search Finds Next
        	if (!mCursor.moveToNext()) {
				// No further Finds
        		mCursor.moveToLast();
				Utils.showToast(v.getContext(), "No further finds.");
        	} else {
        		int latitude;
        		int longitude;
        		
        		Utils.showToast(v.getContext(), "Find " + (mCursor.getPosition() + 1) + " of " + mCursor.getCount());
        		latitude = (int) (mCursor.getDouble(mCursor.getColumnIndex(PositDbHelper.FINDS_LATITUDE))*1E6);
				longitude = (int) (mCursor.getDouble(mCursor.getColumnIndex(PositDbHelper.FINDS_LONGITUDE))*1E6);
				mapController.setCenter(new GeoPoint(latitude, longitude));
        	}
        }
    };
    
    private OnClickListener search_prev_lstn = new OnClickListener() {
        public void onClick(View v) {
            // On Click for Search Finds Previous
        	if (!mCursor.moveToPrevious()) {
				// No further Finds
        		mCursor.moveToFirst();
				Utils.showToast(v.getContext(), "No further finds.");
        	} else {
        		int latitude;
        		int longitude;
        		
        		Utils.showToast(v.getContext(), "Find " + (mCursor.getPosition() + 1) + " of " + mCursor.getCount());
        		latitude = (int) (mCursor.getDouble(mCursor.getColumnIndex(PositDbHelper.FINDS_LATITUDE))*1E6);
				longitude = (int) (mCursor.getDouble(mCursor.getColumnIndex(PositDbHelper.FINDS_LONGITUDE))*1E6);
				mapController.setCenter(new GeoPoint(latitude, longitude));
				//mapController.setZoom(14);
        	}
        }
    };
    
    private OnClickListener search_last_lstn = new OnClickListener() {
        public void onClick(View v) {
            // On Click for Search Finds Last
        	if (!mCursor.moveToLast()) {
				// No further Finds
				Utils.showToast(v.getContext(), "No finds.");
        	} else {
        		int latitude;
        		int longitude;
        		
        		Utils.showToast(v.getContext(), "Find " + (mCursor.getPosition() + 1) + " of " + mCursor.getCount());
        		latitude = (int) (mCursor.getDouble(mCursor.getColumnIndex(PositDbHelper.FINDS_LATITUDE))*1E6);
				longitude = (int) (mCursor.getDouble(mCursor.getColumnIndex(PositDbHelper.FINDS_LONGITUDE))*1E6);
				mapController.setCenter(new GeoPoint(latitude, longitude));
        	}
        }
    };	
	
	/**
	 * Toggle the display of tracks on the map overlay.
	 */
	private void toggleTracks() {
		startActivity(new Intent(this, TrackerActivity.class));
	} // toggleTracks
	
	
	/**
	 * Center and scale the map to show all of the views in the current project.
	 */
	private void centerFinds() {
		int minLat;
		int maxLat;
		int minLong;
		int maxLong;
		
		int latitude;
		int longitude;
		
		// Move to first find
		if (!mCursor.moveToFirst()) {
			// No finds at all
			mapController.setZoom(1);
		} else {
			latitude = (int) (mCursor.getDouble(mCursor.getColumnIndex(PositDbHelper.FINDS_LATITUDE))*1E6);
			minLat = latitude;
			maxLat = latitude;
			longitude = (int) (mCursor.getDouble(mCursor.getColumnIndex(PositDbHelper.FINDS_LONGITUDE))*1E6);
			minLong = longitude;
			maxLong = longitude;
			// Go through all finds
			while (mCursor.moveToNext()) {
				latitude = (int) (mCursor.getDouble(mCursor.getColumnIndex(PositDbHelper.FINDS_LATITUDE))*1E6);
				longitude = (int) (mCursor.getDouble(mCursor.getColumnIndex(PositDbHelper.FINDS_LONGITUDE))*1E6);
				// Find min and max for all latitudes and longitudes
				if (latitude < minLat)
					minLat = latitude;
				if (latitude > maxLat)
					maxLat = latitude;
				if (longitude < minLong)
					minLong = longitude;
				if (longitude > maxLong)
					maxLong = longitude;
			}
			mapController.zoomToSpan(maxLat - minLat, maxLong - minLong);
			mapController.zoomOut(); // One extra zoom out so it doesn't cut off icons
			mapController.setCenter(new GeoPoint((minLat + maxLat) / 2, (minLong + maxLong) / 2));
		}
		
	} // centerFinds

	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
}
