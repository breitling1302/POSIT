/*
 * File: LocationService.java
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

package org.hfoss.posit.android.functionplugin.reminder;

import java.util.ArrayList;
import java.util.Date;

import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.database.DbHelper;
import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * This service class is used to notify the user when he/she is in the 
 * proximity (about 300 m's) of the desired reminder location.
 * 
 * When turned on, the service keeps track of the GPS location of the 
 * user, and when it detects that the user is near any of the reminder
 * locations, it sends a notification to the notification center to alert
 * the user of the associated reminder.
 * 
 **/
public class ToDoReminderService extends Service  implements LocationListener  {

	private static final String TAG = "LocationService";
	// Time interval used to receive location
	// updates from the Location Manager
	private static final int ONE_MINUTE = 60 * 1000;

	// Project currently associated 
	private int projectID;
	// A list of all finds with projectID that have reminders set
	private ArrayList<Find> reminderFinds = null;
	// A list of all finds whose reminders have already been sent
	private ArrayList<Integer> reminderIDs = null;

	// Location variables
	private LocationManager mLocationManager = null;
	private Location mCurrentLocation = null;

	private DbManager dbManager = null;

	private NotificationManager mNotificationManager = null;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		Log.i(TAG, "onCreate()");

		// Set the current project
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		projectID = prefs.getInt(getString(R.string.projectPref), 0);

		// Initialize variables

		reminderFinds = new ArrayList<Find>();
		reminderIDs = new ArrayList<Integer>();

		// Initialize Managers
		mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

		String ns = Context.NOTIFICATION_SERVICE;
		mNotificationManager = (NotificationManager) getSystemService(ns);

	}

	@Override
	/*
	 * This is called right after onCreate and is also called
	 * every time when startService() is called from other classes 
	 */
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "onStartCommand()");

		// Get the preferences from the Preference Manager
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean allowReminder = prefs.getBoolean("allowReminderKey", true);
		boolean allowGeoTag = prefs.getBoolean("geotagKey", true);
		Log.i(TAG, "rmdrPref=" + allowReminder + " geoPref=" + allowGeoTag);

		if (allowReminder && allowGeoTag) {

			// The user allows reminders to be set
			// Request Location updates every minute
			mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, ONE_MINUTE, 0, this);
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, ONE_MINUTE, 0, this);

			// Get last known location
			Location netLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			Location gpsLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

			// Set current location, with priority given to GPS location
			if (gpsLocation != null) {
				mCurrentLocation = gpsLocation;
			} else {
				mCurrentLocation = netLocation;
			}
		} else {
			Log.i(TAG,"Stopping service, not allowed by user");
			stopService();
			return START_STICKY;
		}

		// The user allows reminders to be set
		// Request Location updates every minute
		mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, ONE_MINUTE, 0, this);
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, ONE_MINUTE, 0, this);

		// Get last known location
		Location netLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		Location gpsLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

		// Set current location, with priority given to GPS location
		if (gpsLocation != null) {
			mCurrentLocation = gpsLocation;
		} else {
			mCurrentLocation = netLocation;
			Log.i(TAG, mCurrentLocation.toString());
		}

		dbManager = DbHelper.getDbManager(this);

		reminderFinds = refreshReminderList(reminderFinds);


		return START_STICKY;
	}

	private ArrayList<Find> refreshReminderList(ArrayList<Find> reminderFinds) {
		reminderFinds.clear();
		reminderIDs.clear();

		dbManager = DbHelper.getDbManager(this);

		// Find all finds that have reminders attached
		for (Find find : dbManager.getFindsByProjectId(projectID)) {
			int is_adhoc = find.getIs_adhoc();

			if (is_adhoc == SetReminder.REMINDER_SET) {
				reminderFinds.add(find);
				reminderIDs.add(find.getId());
			}
		}

		return reminderFinds;
	}

	/**
	 * Helper method to stop the service and clean up.
	 */
	private void stopService() {
		// Remove the location updates to conserve battery
		if (mLocationManager != null) {
			mLocationManager.removeUpdates(this);
			Log.i(TAG, "Location Updates Cancelled!");
		}

		// Release DbManager resources
		if (dbManager != null) {
			DbHelper.releaseDbManager();
			dbManager = null;
			Log.i(TAG, "Releasing Db manager!");
		}

		//  Cancel all notifications
		if (mNotificationManager != null) {
			mNotificationManager.cancelAll();
			Log.i(TAG, "Notifications Cancelled!");
		}	
		Log.i(TAG, "Stopping self");
		this.stopSelf();
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy()");
	}

	/*
	 * This is called every time the location is changed,
	 * or at the end of the indicated time interval (one
	 * minute) in this class
	 * 
	 * @param location  the new location received from Location Manager
	 */
	public void onLocationChanged(Location location) {

		Log.i(TAG, "Location might have changed.");

		// If the new location is better, replace the old one with the new one
		if (isBetterLocation(location, mCurrentLocation)) {
			mCurrentLocation = location;
			Log.i(TAG, "Got a new location: " + mCurrentLocation.getLatitude() + "," + mCurrentLocation.getLongitude());
		}

		// Update the reminderList
		reminderFinds = refreshReminderList(reminderFinds);

		// Get the longitude and latitude
		double currLong = mCurrentLocation.getLongitude();
		double currLat = mCurrentLocation.getLatitude();

		// Go through all the finds with reminders
		// and check if the user is close to any of
		// the set locations
		for (Find find : reminderFinds) {

			// The reminder time should be for today
			Date findTime = find.getTime();
			Date currTime = new Date();
			if ((findTime.getYear() == currTime.getYear())
					&& (findTime.getMonth() == currTime.getMonth())
					&& (findTime.getDate() == currTime.getDate())) {

				// The current location should be within a radius of
				// around 500 meters from reminder's location
				double findLong = find.getLongitude();
				double findLat = find.getLatitude();
				double longDiff = Math.abs(currLong - findLong);
				double latDiff = Math.abs(currLat - findLat);

				//				if (longDiff <= 0.003 && latDiff <= 0.003) {
				if (longDiff <= 0.03 && latDiff <= 0.03) {
					Log.i(TAG, "Trigger a notification");

					// Send the find's notification if it has not yet
					// been sent, this is to ensure that the notification
					// will only be sent once
					//if (!reminderIDs.contains(find.getId())) {
					if (reminderIDs.contains(find.getId())) {
						Log.i(TAG, "Reminder IDs contains find");

						// Set notification icon, name, and time
						int icon = android.R.drawable.stat_sys_warning;
						CharSequence tickerText = "Reminder: " + find.getName();
						long when = System.currentTimeMillis();
						Notification notification = new Notification(icon, tickerText, when);
						notification.defaults |= Notification.DEFAULT_SOUND;

						// Set intent and bundle with find's ID
						Intent intent = new Intent(this, NotifyReminder.class);
						Bundle bundle = new Bundle();
						bundle.putInt(Find.ORM_ID, find.getId());
						intent.putExtras(bundle);

						// Update the Find's status
						find.setIs_adhoc(SetReminder.REMINDER_NOTIFIED);
						dbManager.update(find);

						// Set pending intent and send the notification
						PendingIntent contentIntent = PendingIntent.getActivity(this, find.getId(), intent, 0);
						notification.setLatestEventInfo(this, "Reminder: " + find.getName(), find.getDescription(),
								contentIntent);
						mNotificationManager.notify(find.getId(), notification);

					} else {
						Log.i(TAG, "Reminder IDs does NOT contain find");

					}
				}
			}
		}
	}

	/*
	 * Determines whether one Location reading is better than the current Location fix
	 * 
	 * @param location  The new Location that you want to evaluate
	 * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	 */
	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > ONE_MINUTE * 2;
		boolean isSignificantlyOlder = timeDelta < - ONE_MINUTE * 2;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
			return true;
		}
		return false;
	}

	/* Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	public void onProviderDisabled(String provider) {}

	public void onProviderEnabled(String provider) {}

	public void onStatusChanged(String provider, int status, Bundle extras) {}
}
