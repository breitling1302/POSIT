/*
 * File: TrackerState.java
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.baidu.platform.comapi.basestruct.GeoPoint;

/**
 * A class to encapsulate Tracker state data
 * @author rmorelli
 *
 */
public class TrackerState {

	public static final String TAG = "PositTracker";
	
	public static final String BUNDLE_NAME = "TrackerState";
	public static final String BUNDLE_PROJECT = "ProjectId";
	public static final String BUNDLE_SWATH = "Swath";
	public static final String BUNDLE_MIN_DISTANCE = "MinDistance";
	public static final String BUNDLE_EXPEDITION = "Expedition";
		
	public int mProjId = -1;			// No project id assigned 
	public int mExpeditionNumber = -1;  // No expedition number assigned yet
	public int mPoints = 0;
	public int mSynced = 0;
	public int mSent = 0;
	public int mUpdates = 0;

	public int mSwath; 
	public int mMinDistance; 

	// Has the expedition been saved by TrackerActivity?
	private boolean mSaved = false;
	
	// Control variables
	public boolean isRunning = true;
	public boolean isRegistered = false;
	public boolean isInLocalMode = false;  // Set to true by TrackerService if no network access

	private List<PointAndTime> pointsAndTimes;
	public Location mLocation;

	
	/**
	 * Default constructor
	 */
	public TrackerState() {}

	
	
	public TrackerState(Context c) {
		pointsAndTimes = new ArrayList<PointAndTime>();
		mSaved = false;
		SharedPreferences sp =  PreferenceManager.getDefaultSharedPreferences(c);
		try {
			mMinDistance = Integer.parseInt(
					sp.getString(
					TrackerSettings.MINIMUM_DISTANCE_PREFERENCE,
					""+TrackerSettings.DEFAULT_MIN_RECORDING_DISTANCE));
			mSwath = Integer.parseInt(
					sp.getString(
					TrackerSettings.SWATH_PREFERENCE, 
					""+TrackerSettings.DEFAULT_SWATH_WIDTH));
			} catch (Exception e) {
				Log.e(TrackerActivity.TAG, "TrackerState, Oops. something wrong probably Integer parse error " + e);
			}
			
			mProjId = sp.getInt(
					TrackerSettings.POSIT_PROJECT_PREFERENCE, 
					-1);
	}  
	
	/**
	 * Construct from a Bundle.  The Bundle contains only some of the elements
	 * of the track's state -- e.g., no points.  This is used to construct a
	 * TrackerState for displaying existing expeditions.
	 * 
	 * @param b A bundle storing data provided by an existing expedition.
	 */
	public TrackerState(Bundle b) {
		mProjId = b.getInt(BUNDLE_PROJECT);
		mExpeditionNumber = b.getInt(BUNDLE_EXPEDITION);
		mSwath = b.getInt(BUNDLE_SWATH);
		mMinDistance = b.getInt(BUNDLE_MIN_DISTANCE);
		pointsAndTimes = new ArrayList<PointAndTime>();
		mSaved = true;
		Log.i(TAG, "TrackerState() setting mSaved to true" );
	}
	
	/**
	 * Returns a Bundle of some of the elements of the tracker's state. Useful for
	 * making the Intent used to display an existing expedition.
	 * 
	 * @return
	 */
	public Bundle bundle() {
		Bundle b = new Bundle();
		b.putInt(BUNDLE_PROJECT, mProjId);
		b.putInt(BUNDLE_MIN_DISTANCE, mMinDistance);
		b.putInt(BUNDLE_EXPEDITION, mExpeditionNumber);
		b.putInt(BUNDLE_SWATH, mSwath);
		return b;
	}
	
	/**
	 * Updates attributes that are set/changed by the user.  
	 * 
	 * @param preferenceKey The attribute that has been changed
	 */
	public void updatePreference (SharedPreferences sp, String preferenceKey) {
		if (preferenceKey.equals(TrackerSettings.MINIMUM_DISTANCE_PREFERENCE))
			mMinDistance = Integer.parseInt(
					sp.getString(preferenceKey,
							""+TrackerSettings.DEFAULT_MIN_RECORDING_DISTANCE));
		if (preferenceKey.equals(TrackerSettings.SWATH_PREFERENCE))
			mSwath = Integer.parseInt(
					sp.getString(preferenceKey, 
							""+TrackerSettings.DEFAULT_SWATH_WIDTH));
	}
	
 	/**
 	 * Inserts a new point and the time it was gathered into the pointsAndTimes 
 	 * list. 
 	 * @param geoPoint A latitude and longitude.
 	 * @param time The time when the point was marked.
 	 */
	public synchronized void addGeoPointAndTime(GeoPoint geoPoint, long time) {
		pointsAndTimes.add(new PointAndTime(geoPoint, time));
	}
	
	/**
	 * Inserts a new point into the pointsAndTimes list. 
	 * @param geoPoint A latitude and longitude.
	 */
	public synchronized void addGeoPoint(GeoPoint geoPoint) {
		pointsAndTimes.add(new PointAndTime(geoPoint, System
				.currentTimeMillis()));
	}
	
	/**
	 *  Returns the list of geopoints associated with this expedition.
	 */
	public List<PointAndTime> getPoints() {
		return pointsAndTimes;
	}
	
	/**
	 * Passes a list of geopoints to this expeditions.
	 * @param points
	 */
	public void setPoints(List<PointAndTime> points) {
		pointsAndTimes = points;
	}
	
	/**
	 * Passes a list of geopoints to this expeditions.
	 * @param points
	 */
	public void setPointsFromDbValues(List<Points> points) {
		Iterator iterator = points.iterator();
		List<PointAndTime> ptList = new ArrayList<PointAndTime>();
		while (iterator.hasNext()) {
			Points p = (Points)iterator.next();
			GeoPoint gp = new GeoPoint(
					(int)(Double.parseDouble(p.latitude) * 1E6), (int)(Double.parseDouble(p.longitude) * 1E6));
			ptList.add(new PointAndTime(gp, p.time));
		}
		pointsAndTimes = ptList;
	}
	
	
	/**
	 * Returns the size of the ArrayList storing the geopoints. This is
	 * useful for displaying an existing expedition.
	 * 
	 * @return
	 */
	public int getSize() {
		return pointsAndTimes.size();
	}
	
	// Getters and setters
	
	public boolean isSaved() {
		return mSaved;
	}

	public void setSaved(boolean saved) {
		mSaved = saved;
	}
	
	/**
	 * Helper class to store a geopoint and its time stamp.
	 * Code adapted from on online tutorial.
	 * @see http://www.calvin.edu/~jpr5/android/tracker.html
	 * @author rmorelli
	 *
	 */
	public class PointAndTime {
		private GeoPoint geoPoint;
		private long time;

		public PointAndTime(GeoPoint geoPoint, long time) {
			this.geoPoint = geoPoint;
			this.time = time;
		}

		public GeoPoint getGeoPoint() {
			return geoPoint;
		}

		public long getTime() {
			return time;
		}

	}
}
