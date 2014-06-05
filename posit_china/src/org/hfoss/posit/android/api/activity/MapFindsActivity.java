/*
 * File: MapFindsActivity.java
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


import java.util.List;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.api.fragment.ListFindsFragment;
import org.hfoss.posit.android.plugin.csv.CsvListFindsFragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
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
import android.widget.Toast;

import com.actionbarsherlock.ActionBarSherlock;
import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.map.MapController;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationOverlay;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.OverlayItem;
import com.baidu.platform.comapi.basestruct.GeoPoint;

/**
 *  This class retrieves Finds from the Db or from a Csv List and
 *  displays them as an overlay on a Google map. When clicked, 
 *  the finds start a FindActivity. Allowing them to be edited.
 *
 */
public class MapFindsActivity extends OrmLiteBaseMapActivity<DbManager>  {

	private static final String TAG = "MapFindsActivity";
	
	private static List<? extends Find> finds = null;

	BMapManager mBMapMan = null;  
	MapView mMapView = null;
	private MapController mapController;
	private MyLocationOverlay myLocationOverlay;
	private List<Overlay> mapOverlays;
	private Drawable drawable;
	private boolean zoomFirst = true;
	private LinearLayout searchLayout;
	private Button search_first_Btn;
	private Button search_next_Btn;
	private Button search_prev_Btn;
	private Button search_last_Btn;
	
	private int mSearchIndex = 0;
	
	ActionBarSherlock actionBarSherlock = ActionBarSherlock.wrap(this);
	

	/* (non-Javadoc)
	 * @see com.google.android.maps.MapActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		mBMapMan=new BMapManager(getApplication());  
		mBMapMan.init(null);
		setContentView(R.layout.map_finds);
		
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		int pid = sp.getInt(getString(R.string.projectPref), 0);
		
		// Check if this is a CSV Finds case
		Intent intent = getIntent();
		if (intent != null) {
			String action = intent.getAction();
			if (action != null && action.equals(CsvListFindsFragment.ACTION_CSV_FINDS)) {
				finds = CsvListFindsFragment.getFinds();
			} else if (action != null && action.equals(ListFindsFragment.ACTION_LIST_FINDS)) {
				finds = ListFindsFragment.getFinds();
			}
		} else {
			finds = this.getHelper().getFindsByProjectId(pid);
		}
		
		mMapView = (MapView) findViewById(R.id.mapView);
		if (mMapView == null){
			Log.i(TAG,"MapView is NULL");
		}else{
			Log.i(TAG,"successfully inflated mapView");
		}
		mMapView.setBuiltInZoomControls(true);
		
		mapController=mMapView.getController();  
		// 得到mMapView的控制权,可以用它控制和驱动平移和缩放  
		GeoPoint point =new GeoPoint((int)(39.915* 1E6),(int)(116.404* 1E6));  
		//用给定的经纬度构造一个GeoPoint，单位是微度 (度 * 1E6)  
		mapController.setCenter(point);//设置地图中心点  
		mapController.setZoom(18);//设置地图zoom级别  

		
		// Create a mylocation overlay, add it and refresh it
	    myLocationOverlay = new MyLocationOverlay(mMapView);
	    mMapView.getOverlays().add(myLocationOverlay);
	    mMapView.postInvalidate();
	    		
		searchLayout = (LinearLayout) findViewById(R.id.search_finds_layout);
		search_first_Btn = (Button) findViewById(R.id.search_finds_first);
		search_next_Btn = (Button) findViewById(R.id.search_finds_next);
		search_prev_Btn = (Button) findViewById(R.id.search_finds_previous);
		search_last_Btn = (Button) findViewById(R.id.search_finds_last);
		
		search_first_Btn.setOnClickListener(search_first_lstn);
		search_next_Btn.setOnClickListener(search_next_lstn);
		search_prev_Btn.setOnClickListener(search_prev_lstn);
		search_last_Btn.setOnClickListener(search_last_lstn);		
	}
    
	/** 
	 * This method is called when the activity is ready to start 
	 *  interacting with the user. It is at the top of the Activity
	 *  stack.
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		mMapView.onResume();  
        if(mBMapMan!=null){  
                mBMapMan.start();  
        }  

		super.onResume();
		
		// Enable my location
	    //myLocationOverlay.enableMyLocation();
		myLocationOverlay.enableCompass();	
		mapFinds();
	}
	
	@Override  
	protected void onDestroy(){  
	        mMapView.destroy();  
	        if(mBMapMan!=null){  
	                mBMapMan.destroy();  
	                mBMapMan=null;  
	        }  
	        super.onDestroy();  
	}  
		
	/**
	 * Called when the system is about to resume some other activity.
	 *  It can be used to save state, if necessary.  In this case
	 *  we close the cursor to the DB to prevent memory leaks.
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onPause(){
		mMapView.onPause();  
        if(mBMapMan!=null){  
                mBMapMan.stop();  
        }  

		super.onPause();
		//myLocationOverlay.disableMyLocation();
	}

	/**
	 * Gets the Finds to be mapped.  These may have been passed in from
	 * Csv plugin, in which case the finds list is non-null.  Otherwise
	 * get the Finds from the Db.
	 */
	private void mapFinds() {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		int pid = sp.getInt(getString(R.string.projectPref), 0);
		
		if (finds == null) 
			finds = this.getHelper().getFindsByProjectId(pid);
		if (finds.size() <= 0) { // No finds
			Toast.makeText(this, "No finds to display", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		mapOverlays = mMapView.getOverlays();
		//mapOverlays.add(mapLayoutItems(finds));	
		mapController = mMapView.getController();
		
		centerFinds();
	}

	/**
	 * Displays each Find in the map View.
	 * @param finds, a List of Finds
	 * @return
	 */
	private  FindOverlay mapLayoutItems(List<? extends Find> finds) {
		int latitude = 0;
		int longitude = 0;
		int id = 0;

		drawable = this.getResources().getDrawable(R.drawable.bubble);
		FindOverlay mPoints = new FindOverlay(drawable, this, true, this.getIntent().getAction());

		for(Find find  : finds) {
			latitude = (int) (find.getLatitude()*1E6);
			longitude = (int) (find.getLongitude()*1E6);
//			Log.i(TAG, "(" + latitude + "," + longitude + ") ");

			id = find.getId();
			Intent intent = getIntent();
			if (intent != null) {
				String action = intent.getAction();
				if (action != null && action.equals(CsvListFindsFragment.ACTION_CSV_FINDS)) {
					id = Integer.parseInt(find.getGuid());
				}
			}
		
			String description = find.getGuid() + "\n" + find.getName() + "\n" + find.getDescription(); 

			Log.i(TAG, "(" + latitude + "," + longitude + ") " + description);
			
			//mPoints.addOverlay(new OverlayItem(new GeoPoint(latitude,longitude),String.valueOf(id),description));
		}
		return mPoints;
	}

	/**
	 * Only works on phones with a keyboard. Should we keep?
	 */
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
// 			showDialog();
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
		GeoPoint location = null;
		switch (item.getItemId()) {
		/*case R.id.my_location_mapfind_menu_item:
			if (myLocationOverlay.isMyLocationEnabled()) {
				myLocationOverlay.disableMyLocation();
				myLocationOverlay.disableCompass();
				item.setTitle(R.string.my_location_on);
			} else {
				myLocationOverlay.enableMyLocation();
				myLocationOverlay.enableCompass();
				location = myLocationOverlay.getMyLocation();
				if(location != null)
					mapController.setCenter(location);
				item.setTitle(R.string.my_location_off);
			}
			break;*/
		case R.id.search_finds_mapfind_menu_item:
			searchFinds();
			break;
		//case R.id.toggle_tracks_mapfind_menu_item:
		//	toggleTracks();
		//	break;
		case R.id.center_finds_mapfind_menu_item:
			centerFinds();
			break;
		default:
			return false;
		}
		return true;
	} // onMenuItemSelected
	
	
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
	
	private void centerSelectedFind(Context context) {
       	Find find = finds.get(mSearchIndex);
    	double latitude = find.getLatitude();
    	double longitude = find.getLongitude();
    	Toast.makeText(context, "" + (mSearchIndex + 1) + "/" + finds.size()
    			+ " " + find.getName(), Toast.LENGTH_SHORT).show();
		mapController.setCenter(new GeoPoint((int)(latitude *1E6), (int)(longitude *1E6)));
	}
	
	private OnClickListener search_first_lstn = new OnClickListener() {
        public void onClick(View v) {
            // On Click for Search Finds First
        	mSearchIndex = 0;
        	centerSelectedFind(v.getContext());
         }
    };

    private OnClickListener search_next_lstn = new OnClickListener() {
        public void onClick(View v) {
            // On Click for Search Finds Next
        	mSearchIndex = (mSearchIndex + 1) % finds.size();
        	centerSelectedFind(v.getContext());
        }
    };
    
    private OnClickListener search_prev_lstn = new OnClickListener() {
        public void onClick(View v) {
            // On Click for Search Finds Previous
        	mSearchIndex = (mSearchIndex - 1);
        	if (mSearchIndex <= 0)
        		mSearchIndex = finds.size()-1;
        	centerSelectedFind(v.getContext());
        }
    };
    
    private OnClickListener search_last_lstn = new OnClickListener() {
        public void onClick(View v) {
            // On Click for Search Finds Last
           	mSearchIndex = finds.size()-1;
        	centerSelectedFind(v.getContext());
         }
    };	
		
	/**
	 * Center and scale the map to show all of the views in the current project.
	 *  CsvFind which doesn't store Finds in the Db.
	 */
	private void centerFinds() {
		if (finds == null) 
			return;
		int minLat;
		int maxLat;
		int minLong;
		int maxLong;
		
		int latitude;
		int longitude;
		
		if (finds.size() <= 0) {
			// No finds at all
			mapController.setZoom(1);
		} else {
			minLat = (int)(+81 * 1E6);
			maxLat = (int)(-81 * 1E6);
			minLong = (int)(+181 * 1E6);
			maxLong = (int)(-181 * 1E6);
			// Go through all finds
			for (Find find : finds) {
				latitude = (int) (find.getLatitude()*1E6);
				longitude = (int) (find.getLongitude()*1E6);
//				Log.i(TAG, "lat=" + latitude + " min=" + minLat + " max=" + maxLat);
//				Log.i(TAG, "long=" + longitude + " min=" + minLong + " max=" + maxLong);
				
				// Find min and max for all latitudes and longitudes
				if (latitude != 0 && longitude != 0) {
					minLat = (minLat > latitude) ? latitude : minLat;
					maxLat = (maxLat < latitude) ? latitude : maxLat;
					minLong = (minLong > longitude) ? longitude : minLong;
					maxLong = (maxLong < longitude) ? longitude : maxLong;
				}
			}
			Log.i(TAG, "difflat,difflong " + (maxLat - minLat) + "," + (maxLong - minLong) );
			mapController.zoomToSpan(maxLat - minLat, maxLong - minLong);
			mapController.animateTo(new GeoPoint((maxLat + minLat)/2,(maxLong + minLong)/2 ));
		}
		
	} // centerFinds
}