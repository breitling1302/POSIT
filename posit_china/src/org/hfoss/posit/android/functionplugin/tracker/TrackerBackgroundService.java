/*
 * File: TrackerBackgroundService.java
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

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.database.DbHelper;
import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.sync.Communicator;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.baidu.platform.comapi.basestruct.GeoPoint;
import com.j256.ormlite.android.apptools.OrmLiteBaseService;


/**
 * This service manages the tracking of the device in the background.  It uses a
 * Async Tasks to manage communication with the POSIT server.  It listens for 
 * Location updates and uses a call back method to update the View in TrackerActivity.
 * 
 * For a nice example of how to set up a Service that uses an Async thread, see the src in
 * @ http://github.com/commonsguy/cw-android/tree/master/Service/WeatherPlus/
 * @author rmorelli
 *
 */ 
public class TrackerBackgroundService extends OrmLiteBaseService<DbManager> implements LocationListener {

	private static final String TAG = "PositTracker";
	private static final int START_STICKY = 1;
	
	// The static variable allows us to send the Activity a reference to the service through the
	// 
	// See http://stackoverflow.com/questions/2463175/how-to-have-android-service-communicate-with-activity
	private static TrackerBackgroundService serviceInstance; 
	
	private static TrackerActivity TRACKER_ACTIVITY;  // The UI
	public static ServiceUpdateUIListener UI_UPDATE_LISTENER; // The Listener for the UI
	
	private Communicator mCommunicator;
	private ConnectivityManager mConnectivityMgr;

	TrackerDbManager dbManager;

	private LocationManager mLocationManager;
	private Location mLocation  = null;
	private TrackerState mState;
	
	private int mRowId;
	
	/**
	 * These next two STATIC methods allow data to pass between Service and Activity (UI)
	 * @param activity
	 */
	public static void setMainActivity(TrackerActivity activity) {
	  TRACKER_ACTIVITY = activity;
	}

	public static void setUpdateListener(ServiceUpdateUIListener l) {
	  UI_UPDATE_LISTENER = l;
	}
	
	/**
	 * This method is called by the TrackerActivity to get a reference to the Service.
	 * The value of serviceInstance is set in onCreate().
	 * 
	 * See http://stackoverflow.com/questions/2463175/how-to-have-android-service-communicate-with-activity
	 * @return
	 */
	public static TrackerBackgroundService getInstance() {
		return serviceInstance;
	}
	
	/**
	 * This method is called repeatedly by the Activity to get
	 * data about the current Track (expedition) that is then
	 * displayed in the UI.
	 * @return
	 */
	public TrackerState getTrackerState() {
		return mState;
	}
	
	public void stopListening() {
		// Shouldn't be null unless Tracker Service fails to start
		if (mState != null)
			mState.isRunning = false;
	}
	
		  
	public void onCreate() {
		Log.d(TAG, "TrackerService, onCreate()");
		// Set up the Service so it can communicate with the Activity (UI)
		serviceInstance = this;      // Reference to this Service object used by the UI
		
		// Create a network manager
		mConnectivityMgr = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		// Create a database helper
		dbManager = new TrackerDbManager(TRACKER_ACTIVITY);
		
		// Let the UI know about this Tracker service.
		if (TRACKER_ACTIVITY != null)
			TrackerActivity.setTrackerService(this);
	}

	/**
	 * This method is used only if the Service does inter process calls (IPCs), 
	 * which ours does not.
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	// This is the old onStart method that will be called on the pre-2.0
	// platform.  On 2.0 or later we override onStartCommand() so this
	// method will not be called.
	@Override
	public void onStart(Intent intent, int startId) {
	    handleCommand(intent);
		Log.i(TAG, "TrackerService,  Started, id " + startId + " minDistance: " + mState.mMinDistance);
	}
	
	/**
	 * Called when the service is started in TrackerActivity. Note that this method 
	 * sometimes executes  BEFORE onCreate(). This is important for the use of objects
	 * such as the TrackerState.
	 * 
	 * The Service is passed a TrackerState object from the Activity and this object
	 * is used during tracking to communicate data back and forth. It is used by the
	 * Activity to display data in the View. It is used by the Service to store the
	 * points that are gathered. 
	 * 
	 * @param intent Used to pass a TrackerState object to the service
	 * @param flags Unused
	 * @param startId Unused
	 * @return START_STICKY so the service persists
	 */
	public int onStartCommand(Intent intent, int flags, int startId) {
		handleCommand(intent);
		Log.i(TAG, "TrackerService,  Started, id " + startId + " minDistance: " + mState.mMinDistance);

		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}
	
	private void handleCommand(Intent intent) {
		// Get the TrackerState object or create a new one from scratch
		if (intent != null) {
			Bundle b = intent.getBundleExtra(TrackerState.BUNDLE_NAME);
			mState = new TrackerState(b);
			mState.setSaved(false);
		} else  {
			mState = new TrackerState(this);
			Log.e(TrackerActivity.TAG, "TrackerBackgroundService null intent error");
		}
		 			

		// Request location updates
 		mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE); 
 		 		
 		if (mLocationManager != null) {
	 		Log.i(TrackerActivity.TAG, "TrackerBackgroundService Requesting updates");
	 		
//	 		mLocationManager.requestLocationUpdates(
//	 				mLocationManager.getBestProvider(new Criteria(), true), 
//	 				TrackerSettings.DEFAULT_MIN_RECORDING_INTERVAL, 
//	 				 mState.mMinDistance, 
//	 				 this);

	 		mLocationManager.requestLocationUpdates(
	 				mLocationManager.GPS_PROVIDER, 
	 				TrackerSettings.DEFAULT_MIN_RECORDING_INTERVAL, 
	 				 mState.mMinDistance, 
	 				 this);	 		
	 		
//			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, TrackerSettings.DEFAULT_MIN_RECORDING_INTERVAL, mState.mMinDistance, this);
//			mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, TrackerSettings.DEFAULT_MIN_RECORDING_INTERVAL, mState.mMinDistance, this);
//
//			Location netLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//			Location gpsLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//
//			if (gpsLocation != null) {
//				mLocation = gpsLocation;
//			} else {
//				mLocation = netLocation;
//			}
	 		
// 			mLocationManager.requestLocationUpdates(
// 					LocationManager.GPS_PROVIDER, 
// 					TrackerSettings.DEFAULT_MIN_RECORDING_INTERVAL, 
// 					mState.mMinDistance, 
// 					this);
 		}
	
		// Register a new expedition and update the UI
		mCommunicator = new Communicator();
//		mCommunicator.setContext(this);
		registerExpedition();
		if (UI_UPDATE_LISTENER != null) {
			UI_UPDATE_LISTENER.updateUI(mState);
		}	
	}
	
	/**
	 * Registers the expedition with the POSIT server or, if there is no
	 * network connectivity, creates a temporary expedition with ID > 10000.
	 */
	private void registerExpedition() {
		// Timed-wait until we have WIFI or MOBILE (MOBILE works best of course)	
		NetworkInfo info = mConnectivityMgr.getActiveNetworkInfo();
		long startTime = System.currentTimeMillis();
		long elapsedTime = 0;
		int expId = 0;

		// If no network, wait here for up to 5 seconds
		while (info == null && elapsedTime < 5000 ) {  
			try {
				Thread.sleep(500);  // Half a second
				info = mConnectivityMgr.getActiveNetworkInfo();
				elapsedTime = System.currentTimeMillis() - startTime;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		if (info != null) {
			// Register a new expedition
			// TODO: Embed this in an error check or try/catch block and possibly
			// clean up the communicator method
			
			expId = mCommunicator.registerExpeditionId(TRACKER_ACTIVITY, mState.mProjId);
			Log.d(TrackerActivity.TAG, "Async: Register Expedition expId = " + expId);
			if (expId != -1) {
				mState.isRegistered = true;
				mState.isInLocalMode = false;
			}
		}
		// Either there is no network connectivity or there's a problem with the server
		// and an expId of -1 was returned. 
		if (info == null || expId == -1) {
			expId = TrackerSettings.MIN_LOCAL_EXP_ID 
				+ (int)(Math.random() * TrackerSettings.LOCAL_EXP_ID_RANGE);  // Create a random expId
			mState.isInLocalMode = true;
		}

		// Create a record in Db for this expedition
		try {
			insertExpeditionToDb(expId);
		} catch (Exception e) {
			Log.e(TrackerActivity.TAG, "TrackerService.Async, registeringExpedition " + e.getMessage());
			e.printStackTrace();
		}

		mState.mExpeditionNumber =  expId; 
	}		
	 
	/**
	 * Helper method to insert the expedition into the db.
	 * @param expId The expedition's id.
	 */
	private synchronized void insertExpeditionToDb (int expId) {
        ContentValues values = new ContentValues();

        DbManager dbHelper = DbHelper.getDbManager(TRACKER_ACTIVITY);
        
		values.put(DbManager.EXPEDITION_NUM, expId);
		values.put(DbManager.EXPEDITION_PROJECT_ID, mState.mProjId);
					
		if (mState.isRegistered)
			values.put(DbManager.EXPEDITION_REGISTERED, DbManager.EXPEDITION_IS_REGISTERED);
		else 
			values.put(DbManager.EXPEDITION_REGISTERED, DbManager.EXPEDITION_NOT_REGISTERED);
		
		try {
			//long id = getHelper().addNewExpedition(values);
			int rows = dbHelper.addNewExpedition(values);

			if (rows > 0) {
				Log.i(TAG, "TrackerService, saved expedition " 
						+ expId + " projid= " + mState.mProjId);
				//			Utils.showToast(this, "Saved expedition " + mState.mExpeditionNumber);
				if (mState != null)
					mState.setSaved(true);
			} else {
				Log.i(TAG, "TrackerService, Db Error: exped=" + expId + " proj=" 
						+ mState.mProjId);
				//			Utils.showToast(this, "Oops, something went wrong when saving " + mState.mExpeditionNumber);
			}
		} catch (Exception e) {
			Log.e(TrackerActivity.TAG, "TrackerService.Async, insertExpeditionToDb " + e.getMessage());
			e.printStackTrace();
		}
		DbHelper.releaseDbManager();
	}
	
	/**
	 * Helper method to update the Db when a point is synced with server.  Must be syncrhonized
	 * to allow multiple threads to access Db.
	 * @param rowId  The point's rowid
	 * @param vals  Columns to update for the point
	 * @param isSynced  Synced or not
	 * @param expId  The expedition's id
	 */
	private synchronized void updatePointAndExpedition(int rowId, ContentValues vals, int isSynced, int expId) {
		// Update the point in the database
		DbManager dbHelper = DbHelper.getDbManager(TRACKER_ACTIVITY);
		try {
//			boolean success = getHelper().updateGPSPoint(rowId, vals);
			boolean success = dbHelper.updateGPSPoint(rowId, vals);
			if (success) {
				Log.i(TAG, "TrackerService.Async, Updated point# " + rowId + " synced = " + isSynced);

				// Update the expedition record
				ContentValues expVals = new ContentValues();
				expVals.put(DbManager.EXPEDITION_SYNCED, mState.mSynced);
				//dbHelper.updateExpedition(mState.mExpeditionNumber, expVals);
				int rows = dbHelper.updateExpedition(mState.mExpeditionNumber, expVals);
				if (rows != 0) {
					Log.i(TrackerActivity.TAG, "Updated Expedition " + mState.mExpeditionNumber);
				} else {
					Log.i(TrackerActivity.TAG, "Failed to update Expedition " + mState.mExpeditionNumber);
				}
				//expVals.put(getHelper().EXPEDITION_SYNCED, mState.mSynced);
				//getHelper().updateExpedition(mState.mExpeditionNumber, expVals);
			} else
				Log.i(TAG, "TrackerService.Async, Oops. Failed to update point# " + rowId + " synced = " + isSynced);
		} catch (Exception e) {
			Log.e(TrackerActivity.TAG, "TrackerService.Async, updatePointAndExpedition " + e.getMessage());
			e.printStackTrace();
		}
		DbHelper.releaseDbManager();
	}

	/**
	 *   Reports to the user and updates the Expedition record in the Db.
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (mLocationManager != null) {
			mLocationManager.removeUpdates(this);

			Toast.makeText(this, "Tracker stopped ---- " +
					"\n#updates = " + mState.mUpdates + 
					" #sent = " + mState.mSent +
					" #synced = " + mState.mSynced,  Toast.LENGTH_LONG).show();

			Log.i(TAG,"TrackerService, Tracker destroyed, " + //updatedDb= " + success + 
					" #updates = " + mState.mUpdates + 
					" #sent = " + mState.mSent +
					" #synced = " + mState.mSynced);
		}
	}
	
	
	/**
	 * Called from TrackerActivity when the user changes preferences. 
	 * @param sp
	 * @param key
	 */
	public void changePreference(SharedPreferences sp, String key) {
		Log.d(TAG, "TrackerService, Shared Preference Changed key = " + key);
		if (key != null) {
			if (key.equals(getString(R.string.swath_width))) {
				mState.mSwath = Integer.parseInt(
						sp.getString(key, ""+TrackerSettings.DEFAULT_SWATH_WIDTH));
				if (mState.mSwath <= 0) 
					mState.mSwath = TrackerSettings.DEFAULT_SWATH_WIDTH;
			} else if (key.equals(getString(R.string.min_recording_distance))) {
				mState.mMinDistance = Integer.parseInt(
						sp.getString(key, ""+TrackerSettings.DEFAULT_MIN_RECORDING_DISTANCE));
				if (mState.mMinDistance < 0) 
					mState.mMinDistance = 0;
			}	   
		}
	}
	
	/**
	 * Called whenever a new location is received
	 * @param loc  The new location
	 */
    private synchronized void handleNewLocation(Location loc) {
		// Update the TrackerState object, used to keep track of things and send data to UI
		// Each new point is recorded in the TrackerState object
    	
		mState.mLocation = loc;
		if (loc == null) {
			return;
		}
		double latitude = loc.getLatitude();
		double longitude = loc.getLongitude();
		long time = loc.getTime();
		
		// The SQLite Db is happier if we store these as strings instead of doubles.
		String latStr = String.valueOf(latitude);
		String longStr = String.valueOf(longitude);
		//Log.i(TAG, "TrackerService, Lat,long as strings " + latStr + "," + longStr);
		
		// Add the point to the ArrayList (used for mapping the points)
		mState.mPoints++;
		mState.addGeoPointAndTime(new GeoPoint(
				(int)(latitude*1E6), 
				(int)(longitude*1E6)),
				time);
		
		// Create a ContentValues for the Point
		DbManager dbHelper = DbHelper.getDbManager(TRACKER_ACTIVITY);
        ContentValues resultGPSPoint = new ContentValues();
        resultGPSPoint.put(DbManager.EXPEDITION, mState.mExpeditionNumber); // Will be -1 if no network
        resultGPSPoint.put(DbManager.GPS_POINT_LATITUDE, latStr);
        resultGPSPoint.put(DbManager.GPS_POINT_LONGITUDE, longStr);
        resultGPSPoint.put(DbManager.GPS_POINT_ALTITUDE, loc.getAltitude());
        resultGPSPoint.put(DbManager.GPS_POINT_SWATH, mState.mSwath);
        resultGPSPoint.put(DbManager.GPS_TIME, time);
        
        try {
        	// Insert the point to the phone's database and update the expedition record
        	mRowId = dbHelper.addNewGPSPoint(resultGPSPoint);
        	Log.i(TAG, "New point " + resultGPSPoint);
        	
        	// Update the expedition record
        	
			ContentValues expVals = new ContentValues();
			expVals.put(DbManager.EXPEDITION_POINTS, mState.mPoints);
//			dbHelper.updateExpedition(mState.mExpeditionNumber, expVals);
			int rows = dbHelper.updateExpedition(mState.mExpeditionNumber, expVals);
			if (rows != 0) {
				Log.i(TrackerActivity.TAG, "Updated Expedition " + mState.mExpeditionNumber);
			} else {
				Log.i(TrackerActivity.TAG, "Failed to update Expedition " + mState.mExpeditionNumber);
			}
				
			
			//getHelper().updateExpedition(mState.mExpeditionNumber, expVals);
			
        } catch (Exception e) {
        	Log.e(TrackerActivity.TAG, "TrackerService, handleNewLocation " + e.getMessage());
        	e.printStackTrace();
        }

        
		// Call the UI's Listener. This will update the View if it is visible
		if (UI_UPDATE_LISTENER != null && loc != null) {
			UI_UPDATE_LISTENER.updateUI(mState);
		}	
 
		// Don't start a thread to send the point unless there is network
		if (mConnectivityMgr.getActiveNetworkInfo() != null) {
			
			// Pass the row id to the Async thread
			resultGPSPoint.put(DbManager.EXPEDITION_GPS_POINT_ROW_ID, mRowId);

			// Send the point to the POSIT server using a background Async Thread
			new SendExpeditionPointTask().execute(resultGPSPoint);
		} else {
			Log.i(TrackerActivity.TAG, "Caching: no network. Not sending point to server.");
		}
		
        DbHelper.releaseDbManager();

	}
	
//	/**
//	 * This method is called whenever a new location update is received from the GPS. It
//	 * updates the TrackerState object (mState) and sends the GeoPoint to the POSIT Server.
//	 * @param newLocation
//	 */
//	private void setCurrentGpsLocation(Location newLocation) {
//		if (mLocation == null || mLocation.distanceTo(newLocation) >= mState.mMinDistance) {
//			// Remember the new location
//			mLocation = newLocation;
//		}
//	}
	
	// These are the location listener methods. 
	
	/**
	 * Called whenever a new location update is received from the GPS service. It
	 * updates the TrackerState object (mState) and sends the GeoPoint to the POSIT Server.
	 * @param location, the updated lcoation
	 */
	public void onLocationChanged(Location location) {
		if (mLocation == null || mLocation.distanceTo(location) >= mState.mMinDistance) {
			// Remember the new location and handle the change
			mLocation = location;
			++mState.mUpdates;
			handleNewLocation(location);
		}
		
		Log.d(TAG, "TrackerService, point found");			
	}

	public void onProviderDisabled(String provider) {
		// Required for location listener interface. Not used
	}

	public void onProviderEnabled(String provider) {
		// Required for location listener interface. Not used
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.i(TAG, "onStatusChanged " + provider + " " + status);
		// Required for location listener interface. Not used
	}
	
	
	/**
	 * This class creates a new Thread to handle sending GPS points to the POSIT server.
	 * Note that the thread will wait until there is network connectivity and until
	 * the Expedition has been duly registered on the server--i.e., has an Expedition id != -1.
	 *  
	 * @author rmorelli
	 */
	public class SendExpeditionPointTask extends AsyncTask<ContentValues, Void, Void> {


	@Override
	protected Void doInBackground(ContentValues... values) {
		
		String result;
		for (ContentValues vals : values) {

			try {			
				NetworkInfo info = mConnectivityMgr.getActiveNetworkInfo();

				// Wait until we have WIFI or MOBILE (MOBILE works best of course)	
				// or it's decided that we'll operate in local mode

				while (info == null || mState.mExpeditionNumber == -1) {
					try {
						Thread.sleep(500);  // Wait for 1/2 seconds
						info = mConnectivityMgr.getActiveNetworkInfo();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				// If we're running in local mode (= no server) exit without
				//  trying to sync the point. The point is marked NOT synced by default.
				if (mState.isInLocalMode) {
					Log.i(TAG, "TrackerService.Async, In local mode, NOT sending point");
					return null;
				}
				
				// Send the point to the Server
				result = mCommunicator.registerExpeditionPoint(TRACKER_ACTIVITY,
						vals.getAsDouble(DbManager.GPS_POINT_LATITUDE),
						vals.getAsDouble(DbManager.GPS_POINT_LONGITUDE), 
						vals.getAsDouble(DbManager.GPS_POINT_ALTITUDE), 
						vals.getAsInteger(DbManager.GPS_POINT_SWATH), 
						mState.mExpeditionNumber,  //  We need to use the newly made expedition number
						vals.getAsLong(DbManager.GPS_TIME));
				
				// Successful result has the form mmm,nnnn where mmm = expediton_id
				String s = result.substring(0, result.indexOf(","));
				 
				// Get this point's row id
				int rowId = vals.getAsInteger(DbManager.EXPEDITION_GPS_POINT_ROW_ID);
				
				++mState.mSent;	
				Log.i(TAG, "TrackerService.Async, Sent  point " + mState.mSent + " rowId=" + rowId + " to server, result = |" + result + "|");

				vals = new ContentValues();
				int isSynced = 0;
				if (s.equals("" + mState.mExpeditionNumber)) {
					++mState.mSynced;
					isSynced = DbManager.FIND_IS_SYNCED;
					vals.put(DbManager.GPS_SYNCED, isSynced);
				} else {
					isSynced = DbManager.FIND_NOT_SYNCED;
					vals.put(DbManager.GPS_SYNCED, isSynced);
				}
				
				// Mark the point as synced or not -- probably not necessary in the NOT case
				// because the default value for a point is NOT synced
				updatePointAndExpedition(rowId, vals, isSynced, mState.mExpeditionNumber);


			} catch (Exception e) {
				Log.i(TAG, "TrackerService.Async, Error handleMessage " + e.getMessage());
				e.printStackTrace();
				// finish();
			}
		}
		return null;
	}

}
		 
}
