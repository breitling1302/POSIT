/*
 * File: TrackerActivity.java
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

import java.util.ArrayList;
import java.util.List;

import org.hfoss.posit.android.TrackerState.PointAndTime;
import org.hfoss.posit.android.provider.PositDbHelper;
import org.hfoss.posit.android.utilities.Utils;
import org.hfoss.posit.android.web.Communicator;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;

/**
 * This class tracks and maps the phone's location in real time.  The track is displayed
 * on this activity's View.  Listening for Location updates and sending points to the POSIT
 * server are handled by the TrackerBackgroundService. 
 * 
 * @see http://www.calvin.edu/~jpr5/android/tracker.html
 *
 */

public class TrackerActivity extends MapActivity 
	implements ServiceUpdateUIListener, 
		View.OnClickListener,
		OnSharedPreferenceChangeListener { 

	public static final String TAG = "PositTracker";
		
	private static final boolean ENABLED_ONLY = true;
	private static final String NO_PROVIDER = "No location service";
	private static final boolean RESUMING_SYNC = true;

	public static final int SET_MINIMUM_DISTANCE = 0;

	public static final int GET_EXPEDITION_ROW_ID = 1;
	
	private int mExecutionState = TrackerSettings.IDLE;
	private TextView mPointsTextView;

    private SharedPreferences mPreferences ;
    private SharedPreferences.Editor spEditor;
    
	private ConnectivityManager mConnectivityMgr;
	private int mNetworkType;
	private NotificationManager mNotificationMgr;
	private MapController mMapController;

	private static TrackerBackgroundService mBackgroundService; 
	private PositDbHelper mDbHelper;
	private Communicator mCommunicator;
	
	// View stuff
	private MapView mapView;
	private MyLocationOverlay myLocationOverlay;
	private TrackerOverlay mTrackerOverlay;
	private List<Overlay> mOverlays;
	private TextView mLocationTextView;
	private TextView mStatusTextView;
	private TextView mExpeditionTextView;
	private TextView mSwathTextView;
	private TextView mMinDistTextView;
	private Button mTrackerButton;
	private Button mSettingsButton;
	private Button mListButton;

	 
	// The current track
	private TrackerState mTrack;
	
	// Used when ACTION_VIEW intent -- i.e., for displaying existing expeditions
	private int mRowIdExpeditionBeingSynced;
	private int mExpId;
	private int mPoints;
	private int mSynced;
	private int mRegistered;

	 
	/** 
	 * Called when the activity is first created. Note that if the "Back" key is used while this
	 *  (or any) activity is running, the Activity will be stopped and destroyed.  So if it is started
	 *  again, onCreate() will be called. 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
						
	    // Get our preferences and register as a listener for changes to tracker preferences. 
	    // The Tracker's execution state (RUNNING, IDLE, VIEWING_MODE, SYNCING) is saved as 
	    // a preference.
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mPreferences.registerOnSharedPreferenceChangeListener(this);
	    spEditor = mPreferences.edit();
		
		// Abort the Tracker if GPS is unavailable
		if (!hasNecessaryServices())  {
			this.finish();
			return;
		}
		
		// Create a network manager
		mConnectivityMgr = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

		// Initialize call backs so that the Tracker Service can pass data to this UI
		TrackerBackgroundService.setUpdateListener(this);   // The Listener for the Service
	    TrackerBackgroundService.setMainActivity(this);
	    
	    // Create a new track
	    mTrack = new TrackerState(this);
	    
		// Make sure the phone is registered with a Server and has a project selected
		if (mTrack.mProjId == -1) {
			Utils.showToast(this,"Cannot start Tracker:\nDevice must be registered with a project.");
			Log.e(TAG, "Cannot start Tracker -- device not registered with a project.");
			return;
		}
		
	    // Get a notification manager
		mNotificationMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		setUpTheUI();

		mExecutionState = mPreferences.getInt(TrackerSettings.TRACKER_STATE_PREFERENCE, TrackerSettings.IDLE);
		
		if (mExecutionState == TrackerSettings.SYNCING_POINTS) {
				mRowIdExpeditionBeingSynced = mPreferences.getInt(TrackerSettings.ROW_ID_EXPEDITION_BEING_SYNCED, -1);
			displayExistingExpedition(mRowIdExpeditionBeingSynced, RESUMING_SYNC); 
		}
		
		Log.i(TAG,"TrackerActivity,  created with state = " + 
				mPreferences.getInt(TrackerSettings.TRACKER_STATE_PREFERENCE, -1));
	}
	
	/**
	 * Handles updating the UI when the Activity is resumed. It also 
	 * distinguishes between this activity started from the main POSIT menu
	 * (the default) and started from the TrackerListActivity (action = ACTION_VIEW).
	 * In the latter case the track is just viewable--the Tracker is IDLE.
	 */
	@Override
	public void onResume() {
		super.onResume();
		
		mExecutionState = mPreferences.getInt(TrackerSettings.TRACKER_STATE_PREFERENCE, TrackerSettings.IDLE);

		// Create a network manager
		mConnectivityMgr = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

		myLocationOverlay.enableMyLocation();
		myLocationOverlay.enableCompass();

		if (mConnectivityMgr.getActiveNetworkInfo() == null) {
			Log.i(TrackerActivity.TAG, "TrackerActivity there is NO network connectivity");
			Utils.showToast(this, "Note: Tracker currently has NO network connectivity.");
		}
		
		// See whether the Tracker service is running and if so restore the state
		mExecutionState = mPreferences.getInt(
				TrackerSettings.TRACKER_STATE_PREFERENCE, 
				TrackerSettings.IDLE);
		if (mExecutionState == TrackerSettings.RUNNING)  {
			Utils.showToast(this, "The Tracker is RUNNING.");
			restoreExpeditionState();
			if (mTrack != null)  
				updateUI(mTrack);
			else 
				updateViewTrackingMode();
		} else if (mExecutionState == TrackerSettings.IDLE){
			updateViewTrackingMode();
			Utils.showToast(this, "The Tracker is IDLE.");
		} else if (mExecutionState == TrackerSettings.VIEWING_MODE)
			Utils.showToast(this, "Viewing an existing track.");
		else  { // Syncing state 
			ArrayList<ContentValues> points = mDbHelper.fetchExpeditionPointsUnsynced(mExpId);
			Log.d(TrackerActivity.TAG, "TrackerActivity.onresume points= " + points.size() + " to sync");

			if (points.size() == 0) { // There were probably a few lost points 
				Log.d(TrackerActivity.TAG, "TrackerActivity, Stopping sync " + points.size() + " to sync");
				mSettingsButton.setVisibility(View.GONE);
				mListButton.setEnabled(true);
				mListButton.setClickable(true);
				spEditor.putInt(TrackerSettings.TRACKER_STATE_PREFERENCE, TrackerSettings.IDLE);
				spEditor.commit();
			} else 
				Utils.showToast(this, "This expedition is being synced with the server.");
		}

		Log.i(TAG,"TrackerActivity,  resumed in state " + mExecutionState);
	}
	
	/**
	 * A utility method to organize the activity's state transition machine into a 
	 * single method. Controls the Tracker's execution state. If setFromNewState is true
	 * the method sets the new state to newState.  If it is false, it figures out the
	 * new state from the current state and the context. In either case, it saves the
	 * state in shared Preferences. 
	 * 
	 * @param newState Specifies the new state if setFromNewState is true
	 * @param setFromNewState Forces the state change to new state when true
	 * @return The new execution state
	 */
	private int updateExecutionState(int newState, boolean setFromNewState) {
		if (setFromNewState)  {
			mExecutionState = newState;
			spEditor.putInt(TrackerSettings.TRACKER_STATE_PREFERENCE, newState);
			spEditor.commit();
			return newState;
		}
		switch (mExecutionState) {
		
		}
		return TrackerSettings.IDLE;
	}
	
	/*
	 * Sets the layout for tracking mode
	 */
	private void setUpTheUI() {
		// Set up the UI -- first the text views
		setContentView(R.layout.tracker);
		mPointsTextView = (TextView)findViewById(R.id.trackerPoints);
		mLocationTextView = (TextView)findViewById(R.id.trackerLocation);
		mStatusTextView = (TextView)findViewById(R.id.trackerStatus);
		mExpeditionTextView = (TextView)findViewById(R.id.trackerExpedition);
		mSwathTextView = (TextView)findViewById(R.id.trackerSwath);
		mMinDistTextView = (TextView)findViewById(R.id.trackerMinDistance);
	    mTrackerButton = (Button)findViewById(R.id.idTrackerButton);
	    mTrackerButton.setOnClickListener(this);
	    mSettingsButton = (Button)findViewById(R.id.idTrackerSettingsButton);
	    mSettingsButton.setOnClickListener(this);
	    mListButton = (Button)findViewById(R.id.idTrackerListButton);
	    mListButton.setOnClickListener(this);
	    mListButton.setText("List");
	    
		// Set up the UI -- now the map view and its current location overlay. 
		// The points overlay is created in updateView, after the Tracker Service is started. 
		mapView = (MapView) findViewById(R.id.mapView);
		mapView.setSatellite(false);
		mMapController = mapView.getController();
		mapView.setBuiltInZoomControls(true);
		
		// Add an overlay for my location
		mOverlays = mapView.getOverlays();
		if (mOverlays.contains(myLocationOverlay)) {
			mOverlays.remove(myLocationOverlay);
		}
		myLocationOverlay = new MyLocationOverlay(this, mapView);
		mOverlays.add(myLocationOverlay);
		
		LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		Criteria crit = new Criteria();
		crit.setAccuracy(Criteria.ACCURACY_COARSE);
		String provider = lm.getBestProvider(crit, true);

		Location loc = lm.getLastKnownLocation(provider);
		if (loc != null) {
			GeoPoint point = new GeoPoint((int)(loc.getLatitude()*1E6), (int)(loc.getLongitude()*1E6));
			mMapController.animateTo(point);			
		}
	}
	
	/*
	 * Displays a static expedition, one that was previously collected and stored in the Db. 
	 * Call this with -1 (from onCreate() or onResume() if already in VIEWING_MODE. That means
	 * the Async thread is already syncing the points.  We just want to reset the display.
	 * 
	 * @param rowId Either the rowId of the expedition to be displayed or -1 to indicate
	 * that we are already displaying the track. 
	 */
	private void displayExistingExpedition(long rowId, boolean isResuming) {
		Log.d(TAG, "TrackerActivity, displayExistingExpedition() ");
			
		if (rowId != -1) { // Unless we are already in VIEWING_MODE
			// Retrieve data from this expedition
			mDbHelper = new PositDbHelper(this);

			mExpId = Integer.parseInt(mDbHelper.fetchExpeditionData(rowId, PositDbHelper.EXPEDITION_NUM));
			mPoints = Integer.parseInt(mDbHelper.fetchExpeditionData(rowId, PositDbHelper.EXPEDITION_POINTS));
			mSynced = Integer.parseInt(mDbHelper.fetchExpeditionData(rowId, PositDbHelper.EXPEDITION_SYNCED));
			mRegistered = Integer.parseInt(mDbHelper.fetchExpeditionData(rowId, PositDbHelper.EXPEDITION_REGISTERED));
			Log.d(TrackerActivity.TAG, "TrackerActivity.displayExisting mExpId " + mExpId +
					" mPoints=" + mPoints + " mScynced=" + mSynced +  " mRegistered= " + mRegistered);
			mDbHelper.fetchExpeditionPointsByExpeditionId(mExpId, mTrack);
		}
	
		// Initialize the View
		mPointsTextView = (TextView)findViewById(R.id.trackerPoints);
		mLocationTextView = (TextView)findViewById(R.id.trackerLocation);
		mStatusTextView = (TextView)findViewById(R.id.trackerStatus);
		mExpeditionTextView = (TextView)findViewById(R.id.trackerExpedition);
		mSwathTextView = (TextView)findViewById(R.id.trackerSwath);
		mMinDistTextView = (TextView)findViewById(R.id.trackerMinDistance);
	    mTrackerButton = (Button)findViewById(R.id.idTrackerButton);
	    mTrackerButton.setOnClickListener(this);
	    mSettingsButton = (Button)findViewById(R.id.idTrackerSettingsButton);
	    mSettingsButton.setOnClickListener(this);
	    mListButton = (Button)findViewById(R.id.idTrackerListButton);
	    mListButton.setOnClickListener(this);
		
		// Set up the View for this expedition
		mExpeditionTextView.setText(""+ mExpId);
		mPointsTextView.setText("" + mPoints);
		mTrackerButton.setClickable(false);
		mTrackerButton.setEnabled(false);
		mTrackerButton.setVisibility(View.GONE);
		mSettingsButton.setClickable(false);
		mSettingsButton.setEnabled(false);	
		mListButton.setText("Delete");
		mListButton.setClickable(true);
		mListButton.setEnabled(true);	
		
		// Make invisible the controls and data fields used in the default Intent.

		((TextView)findViewById(R.id.trackerSwathLabel)).setText("Synced");
		((TextView)findViewById(R.id.trackerSwath)).setText("" + mSynced);		
		
		((TextView)findViewById(R.id.trackerMinDistLabel)).setVisibility(View.GONE);
		((TextView)findViewById(R.id.trackerMinDistance)).setVisibility(View.GONE);
	
		((TextView)findViewById(R.id.trackerStatusLabel)).setVisibility(View.GONE);
		((TextView)findViewById(R.id.trackerStatus)).setVisibility(View.GONE);
		((TextView)findViewById(R.id.trackerLabel)).setVisibility(View.GONE);
		((TextView)findViewById(R.id.trackerLocation)).setVisibility(View.GONE);
		
		if (rowId != -1) {
			// Get the unsynced points
			ArrayList<ContentValues> points = mDbHelper.fetchExpeditionPointsUnsynced(mExpId);
			// Get middle point in track

			if (mPoints != 0) {
				List<PointAndTime> pointsList = mTrack.getPoints();
				PointAndTime aPoint = pointsList.get(pointsList.size()/2);
				GeoPoint point = aPoint.getGeoPoint();
				mMapController.animateTo(point);
			}

			// If there are points to sync and we have a network connection now
			if (points.size() > 0 && mConnectivityMgr.getActiveNetworkInfo() != null) {

				if (mRegistered == PositDbHelper.EXPEDITION_IS_REGISTERED)
					mSettingsButton.setText("Sync");
				else  {
					mSettingsButton.setText("Register");
					Utils.showToast(this, "This expedition needs to be registered with the server. " +
					"Please click the register button");

				}
				if (isResuming) {
					mSettingsButton.setClickable(false);
					mSettingsButton.setEnabled(false);	
					mListButton.setClickable(false);
					mListButton.setEnabled(false);

				} else {
					mSettingsButton.setClickable(true);
					mSettingsButton.setEnabled(true);	
				}
			} else {
				mSettingsButton.setVisibility(View.GONE);
			}
		}
		
		// Display the expedition as an overlay
		if (mapView.getOverlays().contains(mTrackerOverlay)) {
			   mapView.getOverlays().remove(mTrackerOverlay);
			 }
		mTrackerOverlay = new TrackerOverlay(mTrack);
		mOverlays.add(mTrackerOverlay);
		Log.d(TrackerActivity.TAG, "TrackerActivity.displayExisting mExpId " + mExpId +
				" mPoints=" + mPoints + " mScynced=" + mSynced +  " mRegistered= " + mRegistered);
	}
	  
	/**
	 * Checks for network and GPS service.
	 * 
	 * @return
	 */
	private boolean hasNecessaryServices() {		
		// First check that we have GPS enabled 
		LocationManager locationManager = 
			(LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		List<String> providers = locationManager.getProviders(ENABLED_ONLY);
		if (!providers.contains(LocationManager.GPS_PROVIDER)) {
			Utils.showToast(this, "Aborting Tracker: " + NO_PROVIDER
					+ "\nYou must have GPS enabled. ");
			return false;
		}
		Log.i(TAG, "TrackerActivity,  hasNecessaryServices() = true");
		return true;
	}
	
	
	/**
	 * This method is called by the Tracker Service.  It must run in the UI thread. 
	 */
	public void updateUI(final TrackerState state) {
		mTrack = state;
		// make sure this runs in the UI thread... since it's messing with views...
		this.runOnUiThread(new Runnable() {
            public void run() {
        		restoreExpeditionState(); 
        		updateViewTrackingMode();
        		if (state.mLocation != null) {
        			myLocationOverlay.onLocationChanged(state.mLocation);
        			Log.i(TAG,"TrackerActivity, updatingUI  <" + state.mLocation.getLatitude() + "," + state.mLocation.getLongitude() + ">");
        		} else 
        			Log.w(TAG,"TrackerActivity, updatingUI  unable to get location from TrackerState");

              }
            });
	}
	
	/**
	 * A helper method to restore the Activity's state and its UI when it is restarted.  The
	 * Service runs in the background independently of the foreground Activity (the UI).  We
	 * get some of the data from the Service -- namely the TrackerState object, which contains
	 * an array of all the points collected and the current lat,long,alt. 
	 */
	private void restoreExpeditionState() {
		mBackgroundService = TrackerBackgroundService.getInstance();
		if (mBackgroundService != null) {
			mTrack = mBackgroundService.getTrackerState();
			if (mapView.getOverlays().contains(mTrackerOverlay)) {
				mapView.getOverlays().remove(mTrackerOverlay);
			}
			mTrackerOverlay = new TrackerOverlay(mTrack);
			mOverlays.add(mTrackerOverlay);
		}
	}

	/**
	 * This static method lets the Activity acquire a reference to the Service.  The
	 * reference is used to get the Tracker's state.
	 * @param service
	 */
	public static void setTrackerService(TrackerBackgroundService service) {
		mBackgroundService = service;
	}
	
	/**
	 * A helper method to update the UI. Most of the data displayed in the View
	 * are taken from the TrackerState object,  mTrack.
	 */
	private void updateViewTrackingMode() {	
		if (mExecutionState == TrackerSettings.VIEWING_MODE || 
				mExecutionState == TrackerSettings.SYNCING_POINTS) {
			return;
		}
		
		String s = " Idle ";
		
		// Manage the control buttons
		if (mExecutionState == TrackerSettings.RUNNING) {
			s = " Running ";
			mTrackerButton.setText("Stop");
			mTrackerButton.setCompoundDrawablesWithIntrinsicBounds(
					getResources().getDrawable(R.drawable.stop_icon),null,null,null); 
		} else {
			mTrackerButton.setText("Start");
			mTrackerButton.setCompoundDrawablesWithIntrinsicBounds(
					getResources().getDrawable(R.drawable.play_icon),null,null,null);  
		}

		// Display the current state of the GPS and Network services.
		String netStr = "none"; // Assume no network
		mConnectivityMgr = 
			(ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = mConnectivityMgr.getActiveNetworkInfo();
		if (info != null) {
			mNetworkType = info.getType();
			netStr = (mNetworkType == ConnectivityManager.TYPE_WIFI) ? " WIFI" : " MOBILE";
		}
		
		mStatusTextView.setText(s + " (GPS = " + LocationManager.GPS_PROVIDER 
				+ ", Ntwk = " + netStr + ")");

		// Display the expedition's (i.e., mTrack's) current state.
		if (mTrack != null) {
			mPointsTextView.setText("" + mTrack.mPoints + " (" + mTrack.mSynced + ")");
			mExpeditionTextView.setText(""+ mTrack.mExpeditionNumber);
			mSwathTextView.setText("" + mTrack.mSwath);
			mMinDistTextView.setText("" + mTrack.mMinDistance);

			if (mTrack.mLocation != null) {
				// Center on my location
				Double geoLat = mTrack.mLocation.getLatitude()*1E6;
				Double geoLng = mTrack.mLocation.getLongitude()*1E6;
				GeoPoint point = new GeoPoint(geoLat.intValue(), geoLng.intValue());
				mMapController.animateTo(point);
				String lat = mTrack.mLocation.getLatitude() + "";
				String lon = mTrack.mLocation.getLongitude() + "";		
				mLocationTextView.setText("(" + lat.substring(0,10) + "...," 
						+ lon.substring(0,10) + "...," + mTrack.mLocation.getAltitude() + ")");	
			} else
				Log.w(TAG, "TrackerActivity, updateView unable to get Location from TrackerState");
		}
	}

	/**
	 * Part of the View.OnClickListener interface. Called when any button in 
	 * the Tracker View is clicked.  This also handles other bookkeeping tasks, 
	 * such as posting and canceling a Notification, displaying a Toast to the UI, and
	 * saving the changed state in the SharedPreferences. 
	 */ 
	public void onClick(View v) {
		switch (v.getId()) {
		
		case R.id.idTrackerButton:   // The play/stop button

		if (mExecutionState == TrackerSettings.IDLE) {  // Start the tracker
			startTrackerService();
			mListButton.setClickable(false);
		    mListButton.setEnabled(false);

		} else  { /* RUNNING */            // Stop the tracker
			stopTrackerService();
			mListButton.setClickable(true);
		    mListButton.setEnabled(true);
		}
		updateViewTrackingMode();
		break;
		
		// This button starts the TrackerSettings activity which lets the user
		//  change the tracker parameters on the fly. Changes are commnicated
		//  through shared preferences.
		case R.id.idTrackerSettingsButton:   
			String text = (String) mSettingsButton.getText();
			Log.d(TAG, "Text = " + text);
			if (text.equals("Settings")) {
				// Try to get the background service
				mBackgroundService = TrackerBackgroundService.getInstance();
				startActivity(new Intent(this, TrackerSettings.class));	
				
			} else if (text.equals("Sync")) {  
				// This is only reached by clicking on the Sync button in List View	
				mSettingsButton.setEnabled(false);
				mSettingsButton.setClickable(false);
				mListButton.setEnabled(false);
				mListButton.setClickable(false);
				syncUnsyncedPoints();
			} else if (text.equals("Register")) {
				Utils.showToast(this, "Attempting to register with server. Please wait.");
				registerUnregisteredExpedition();
			}
			break;
			
		// The "list" button is used for listing and deleting tracks. The button's 
		//	text and icon are changed depending on the current state of the Activity.
		case R.id.idTrackerListButton:
			text = (String) mListButton.getText();
			if (text.equals("List")) {				
				Intent intent = new Intent(this, TrackerListActivity.class); // List tracks
				startActivityForResult(intent, GET_EXPEDITION_ROW_ID);
			} else { 						// Delete this track
				deleteExpedition();
				finish();
			}
			break;
		}
	}
	
	
	/**
	 * Returns the result of selecting an Expedition in TrackerListActivity
	 * @param requestCode The code of the subactivity
	 * @param resultCode Success or canceled
	 * @param data An Intent containing the rowId of the selected expedition
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.d(TrackerActivity.TAG, "onActivityResult reqcode = " + requestCode + " resultCode = " + resultCode);

		switch (requestCode) {

		case GET_EXPEDITION_ROW_ID:
			int expRowId = 0;
			if (resultCode == Activity.RESULT_OK) {
				expRowId = (int) data.getLongExtra(PositDbHelper.EXPEDITION_ROW_ID, -1);
				Log.d(TrackerActivity.TAG, "onActivityResult expedition rowId = " + expRowId);

				mExecutionState = updateExecutionState(TrackerSettings.VIEWING_MODE,true);
				mRowIdExpeditionBeingSynced = expRowId;
				displayExistingExpedition(expRowId, !RESUMING_SYNC);  // i.e., starting a new sync
 			}
			break;
		}
	}

	/**
	 *  Registers an expedition that was recorded while network was out.
	 */
	private void registerUnregisteredExpedition() {
		mCommunicator = new Communicator(this);
		
		if (registerExpedition() != true) {
			Log.d(TrackerActivity.TAG, "TrackerActivity.syncUnsyncedPoints Stopping b/c FAILED TO REGISTER expedition");
			return;			
		} else {
			Log.d(TrackerActivity.TAG, "TrackerActivity.syncUnsyncedPoints Success! New Expedition Id = " + mExpId);
		}
		
		Utils.showToast(this, "Expedition now registered as " +mExpId+ " with the server.");
		
		// Now let the user sync the unsynced points with the Server
		mTrack.mExpeditionNumber = mExpId;
		mExpeditionTextView.setText(""+ mExpId);
		mSettingsButton.setText("Sync");  
	}
	
	/**
	 * Helper method retrieves unsycned points from Db and sends them to the server in a
	 * background thread. 
	 */
	private void syncUnsyncedPoints() {

		// Get the unsynced points
		ArrayList<ContentValues> points = mDbHelper.fetchExpeditionPointsUnsynced(mExpId);
		
		if (points.size() == 0) { // There were probably a few lost points 
			Log.d(TrackerActivity.TAG, "TrackerActivity, Stopping sync " + points.size() + " to sync");
			mSettingsButton.setVisibility(View.GONE);
			mListButton.setEnabled(true);
			mListButton.setClickable(true);
		}
		
		Log.d(TrackerActivity.TAG, "TrackerActivity, Syncing " + points.size() + " unsynced points");
		Utils.showToast(this, "Please wait. Syncing " + points.size() + " unsynced points");
		
		// Send all unsynced points to the server using a background Async Thread
		mCommunicator = new Communicator(this);
		
		ContentValues[] valuesArray = new ContentValues[points.size()];
		points.toArray(valuesArray);

		mExecutionState = updateExecutionState(TrackerSettings.SYNCING_POINTS,true);
		mExecutionState = mPreferences.getInt(TrackerSettings.TRACKER_STATE_PREFERENCE, TrackerSettings.IDLE);

		new SendExpeditionPointTask().execute(valuesArray);	
	}
	
	/*
	 * Helper method to delete the current track. Returns to TrackerListActivity
	 */
	private void deleteExpedition() {
		PositDbHelper dbHelper = new PositDbHelper(this);
		int mExpedNum = Integer.parseInt((String) mExpeditionTextView.getText().toString().trim());
		boolean success = dbHelper.deleteExpedition(mExpedNum);
		if (success) {
			if (mPoints > 0) {
				success = dbHelper.deleteExpeditionPoints(mExpedNum);
				if (success) {
					Log.i(TAG, "TrackerActivity, Deleted expedition " + mExpedNum);
					Utils.showToast(this, "Deleted expedition " + mExpedNum);
				} else {
					Log.i(TAG, "TrackerActivity, Oops, something wrong when deleting expedition " + mExpedNum);
					Utils.showToast(this, "Oops, something went wrong when deleting " + mExpedNum);
				}
			} else {
				Log.i(TAG, "TrackerActivity, Deleted expedition " + mExpedNum);
				Utils.showToast(this, "Deleted expedition " + mExpedNum);					
			}
		} else {
			Log.i(TAG, "TrackerActivity, Deleted expedition " + mExpedNum);
			Utils.showToast(this, "Deleted expedition " + mExpedNum);			
		}
	}
   
	
	/**
	 * Post a notification in the status bar while this service is running.
	 */
	private void postNotification() {  
		// The text that shows when the notification is posted.
		CharSequence text = getText(R.string.local_service_started);

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.radar, text,
				System.currentTimeMillis());

		// Launch TrackerActivity when the user clicks on this notification.
		// PRoblem with this is when it is used, you can never restart POSIT
		// at the main screen.  Try: Start Posit, start tracking, stop posit,
		// Click on the "tracker" notification.  Stop the tracker. Hit the back
		// key. You will exit to Android.  Now try restarting Posit.  It 
		// always starts directly in Tracker, rather than PositMain.  
		PendingIntent contentIntent = PendingIntent.getActivity(this.getBaseContext(), 0,
				new Intent(this, TrackerActivity.class), 0);
		 

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this,
				getText(R.string.local_service_label), text, contentIntent);

	}
	
	/*
	 * Helper method to start the background tracker service.
	 */
	private void startTrackerService() {
		Log.d(TAG, "TrackerActivity, Starting Tracker Service");

		mTrack = new TrackerState(this);
		
		Intent intent = new Intent(this, TrackerBackgroundService.class);
		intent.putExtra(TrackerState.BUNDLE_NAME, mTrack.bundle());
		startService(intent);
	
		mExecutionState = updateExecutionState(TrackerSettings.RUNNING, true);
		Utils.showToast(this, "Starting background tracking.");
		postNotification(); 
		
		mTrackerButton.setText("Stop");
		mTrackerButton.setCompoundDrawablesWithIntrinsicBounds(
				getResources().getDrawable(R.drawable.stop_icon),null,null,null); 
		updateViewTrackingMode();
	}

	/*
	 * Helper method to stop background tracker service.
	 */
	private void stopTrackerService () {
		Log.d(TAG, "TrackerActivity, Stopping Tracker Service");

		if (mBackgroundService != null)
			mBackgroundService.stopListening();
		
		stopService(new Intent(this, TrackerBackgroundService.class));
		mNotificationMgr.cancel(R.string.local_service_label);  // Cancel the notification
		
		mExecutionState = updateExecutionState(TrackerSettings.IDLE, true);		
		myLocationOverlay.disableMyLocation();

		Utils.showToast(this, "Tracking is stopped.");
		mTrackerButton.setText("Start");
		mTrackerButton.setCompoundDrawablesWithIntrinsicBounds(
				getResources().getDrawable(R.drawable.play_icon),null,null,null);  		
	}
	
	@Override
	public void onPause() {
		super.onPause();
		myLocationOverlay.disableMyLocation();
		myLocationOverlay.disableCompass();
		spEditor.putInt(TrackerSettings.TRACKER_STATE_PREFERENCE, mExecutionState);
		spEditor.commit();
		Log.i(TAG,"TrackerActivity, Paused in state: " + mExecutionState);
		
		mExecutionState = mPreferences.getInt(TrackerSettings.TRACKER_STATE_PREFERENCE, TrackerSettings.IDLE);

	}
		
	// The following methods don't change the default behavior, but they show (in the Log) the
	// life cycle of the Activity.

	@Override protected void onDestroy() {
		super.onDestroy();
		
		// If stopped in Viewing mode, reset the state to IDLE;. 
		// otherwise leave it in VIEWING_MODE
		
		mExecutionState = mPreferences.getInt(TrackerSettings.TRACKER_STATE_PREFERENCE, TrackerSettings.IDLE);
		if (mExecutionState == TrackerSettings.VIEWING_MODE)
			mExecutionState = updateExecutionState(TrackerSettings.IDLE, true);
		else if (mExecutionState == TrackerSettings.SYNCING_POINTS) {
			if (mSynced == mPoints) {
				mExecutionState = updateExecutionState(TrackerSettings.IDLE, true);
			} else {
				spEditor.putInt(TrackerSettings.ROW_ID_EXPEDITION_BEING_SYNCED, mRowIdExpeditionBeingSynced);
				spEditor.commit();
			}
		} 
		
		Log.i(TAG,"TrackerActivity, Destroyed in state " + mExecutionState);
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.i(TAG,"TrackerActivity, Stopped");
		mExecutionState = mPreferences.getInt(TrackerSettings.TRACKER_STATE_PREFERENCE, TrackerSettings.IDLE);

	}

	/**
	 * Listener for changes to the Tracker Preferences.  Since the Tracker
	 * service cannot listen for changes, this method will pass changes to
	 * the tracker. 
	 */
	public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
		Log.d(TAG, "TrackerActivity, Shared Preference Changed, key = " 
				+ key);
		
		// This is a hack.  TrackerState is not a shared preference but 
		// this is called repeatedly throughout the run with this key.
		// Don't understand why???
		
		if (key.equals("TrackerState")) 
			return;
		
		if (key != null && mBackgroundService != null) {
			try {
				mBackgroundService.changePreference(sp, key);
			} catch (Exception e) {
		        Log.w(TAG, "TrackerActivity, Failed to inform Tracker of shared preference change", e);
			}
		}
		mTrack.updatePreference(sp, key);
		updateViewTrackingMode();
	}
	
	/**
	 * Required for MapActivity
	 * @see com.google.android.maps.MapActivity#isRouteDisplayed()
	 */
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}


	/**
	 * Helper method to update a point in the Db.  Needs to be synchronized 
	 * because it's called by the Async thread. 
	 * @param rowId Row Id of this expedition
	 * @param vals  The Columns to retrieve in the Db
	 * @param isSynced Value to set in the updated expedition
	 */
	private synchronized void updatePointAndExpedition(long rowId, ContentValues vals, int isSynced, int nSynced) {
		// Update the point in the database
		boolean success = mDbHelper.updateGPSPoint(rowId, vals);
		if (success) {
			Log.i(TAG, "TrackerService.Async, Updated point# " + rowId + " synced = " + isSynced);
			
			ContentValues expVals = new ContentValues();
			expVals.put(PositDbHelper.EXPEDITION_SYNCED, mSynced);
			mDbHelper.updateExpedition(mExpId, expVals);
			
			this.runOnUiThread(new Runnable() {
	            public void run() {
    				Log.d(TrackerActivity.TAG, "TrackerActivity, Updating UI from Async");
    				mSwathTextView.setText(""+mSynced);
	        		((TextView)findViewById(R.id.trackerSwath)).setText("" + mSynced);		
	    			if (mSynced == mPoints) { // Yay. Success!
	    				Log.d(TrackerActivity.TAG, "TrackerActivity, Syncing --> SUCCESS!");
	    				mSettingsButton.setVisibility(View.GONE);
	    				mListButton.setEnabled(true);
	    				mListButton.setClickable(true);
	    			}
	            }
	            });
		}
		else
			Log.i(TAG, "TrackerService.Async, Oops. Failed to update point# " 
					+ rowId + " synced = " + isSynced);
	}
	
	
	// From http://teneo.wordpress.com/2008/12/23/java-ip-address-to-integer-and-back/
	private static Long ipToInt(String addr) {
        String[] addrArray = addr.split("\\.");

        long num = 0;
        for (int i=0;i<addrArray.length;i++) {
            int power = 3-i;

            num += ((Integer.parseInt(addrArray[i])%256 * Math.pow(256,power)));
        }
        return num;
    }
	
	/**
	 * Helper method to check that we have network connectivity and that the server
	 * is reachable. 
	 * @return
	 */
	private boolean hasNetworkAndServerIsReachable () {
		NetworkInfo info = mConnectivityMgr.getActiveNetworkInfo();
		if (info == null) {
			Log.d(TrackerActivity.TAG, "registedExpedition: Error,  No network connectivity.");
			Utils.showToast(this, "Error: Can't register expedition. " +
			"\nMake sure you have a network connection.");

			return false; 
		} 
		return true;

	}
	
	/*
	 * Registers the expedition with the POSIT server, which returns the expedition's id. 
	 * Then updates all points associated with the old expedition number, assigning them
	 * the new number.
	 */ 
	private boolean registerExpedition () {
		
		// Make sure this is doable
		if (!hasNetworkAndServerIsReachable())
			return false;
		
		int newExpId = 0;
		
		// Register a new expedition
		// TODO: Embed this in an error check or try/catch block
		newExpId = mCommunicator.registerExpeditionId(mTrack.mProjId);

		// A return value of -1 represents some kind of error on the server side. 
		if (newExpId != -1) {
			mTrack.isRegistered = true;
			mTrack.isInLocalMode = false;
			Log.i(TAG, "TrackerActivity.Async, Registered expedition id = " + newExpId);
		} else {
			Utils.showToast(this, "Error: Can't register expedition. " +
					"\nPlease check that your server is reachable.");
			Log.d(TrackerActivity.TAG, "registedExpedition: Error,  Uable to register. Check server.");
			return false;
		}

		// Update the expedition table
		ContentValues values = new ContentValues();
		values.put(PositDbHelper.EXPEDITION_NUM, newExpId);
		values.put(PositDbHelper.EXPEDITION_REGISTERED, PositDbHelper.EXPEDITION_IS_REGISTERED);

		// Now update the points table.
		boolean success = false;
		try {
			success = mDbHelper.updateExpedition(mExpId, values);

			// If the expedition is successfully registered, update the 
			// the associated points to reflect the new expedition id
			if (success) {
				values = new ContentValues();
				values.put(PositDbHelper.EXPEDITION, newExpId);
				success = mDbHelper.updateGPSPointsNewExpedition(mExpId, values);
				if (success) {
					Log.d(TrackerActivity.TAG, "TrackerActivity.RegisterGPSPoints SUCCESS for new ID = " + newExpId);
				} else {
					Log.d(TrackerActivity.TAG, "TrackerActivity.RegisterGPSPoints FAILURE for new ID = " + newExpId);
					return false;
				}
			} else {
				Log.d(TrackerActivity.TAG, "registerExpedition FAILURE for new ID = " + newExpId);
				return false;
			}

		} catch (Exception e) {
			Log.e(TrackerActivity.TAG, "registerExpedition, registeringExpedition " + e.getMessage());
			e.printStackTrace();
			return false;
		} 
		mExpId = newExpId;
		Log.e(TrackerActivity.TAG, "registerExpedition, returning with SUCCESS");
		return true;
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
	
		for (int k = 0; k < values.length; k++) {
			ContentValues vals = values[k]; 
			
			// Wait for connectivity 
			try {			
				// Wait until we have WIFI or MOBILE (MOBILE works best of course)	
				NetworkInfo info = mConnectivityMgr.getActiveNetworkInfo();
				if (info == null) {
					Log.d(TAG, "TrackerActivity.Async now waiting for network");
				}
				while (info == null) {
					try {
						Thread.sleep(2000);  // Wait for 2 seconds
						info = mConnectivityMgr.getActiveNetworkInfo();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				Log.d(TAG, "TrackerActivity.Async now sending point " + k + " to server");
				
				// Send the point to the Server
				result = mCommunicator.registerExpeditionPoint(
						vals.getAsDouble(PositDbHelper.GPS_POINT_LATITUDE),
						vals.getAsDouble(PositDbHelper.GPS_POINT_LONGITUDE), 
						vals.getAsDouble(PositDbHelper.GPS_POINT_ALTITUDE), 
						vals.getAsInteger(PositDbHelper.GPS_POINT_SWATH), 
						vals.getAsInteger(PositDbHelper.EXPEDITION),  
						vals.getAsLong(PositDbHelper.GPS_TIME));
				
				// Successful result has the form mmm,nnnn where mmm = expediton_id
				String s = result.substring(0, result.indexOf(","));
				 
				// Get this point's row id
				long rowId = vals.getAsLong(PositDbHelper.EXPEDITION_GPS_POINT_ROW_ID);
				int expId = vals.getAsInteger(PositDbHelper.EXPEDITION);
				
				++mTrack.mSent;	
				Log.i(TAG, "TrackerActivity.Async, Sent  point " +  k + " rowId=" + rowId + " to server, result = |" + result + "|");

				vals = new ContentValues();
				int isSynced = 0;
				if (s.equals("" + expId)) {
					++mSynced;
					isSynced = PositDbHelper.FIND_IS_SYNCED;
					vals.put(PositDbHelper.GPS_SYNCED, isSynced);
				} else {
					isSynced = PositDbHelper.FIND_NOT_SYNCED;
					vals.put(PositDbHelper.GPS_SYNCED, isSynced);
				}
				// Mark the point as synced and update the expedition
				updatePointAndExpedition(rowId, vals, isSynced, mSynced);
			} catch (Exception e) {
				Log.i(TAG, "TrackerService.Async, Exception on point " + k + " message=" + e.getMessage());
				e.printStackTrace();
			}
		}
		// When we finish processing all the points, reset the Tracker's state
		spEditor.putInt(TrackerSettings.TRACKER_STATE_PREFERENCE, TrackerSettings.IDLE);
		spEditor.commit();
		return null;
	}
}

}
