/*
 * File: Find.java
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hfoss.posit.android.provider.PositDbHelper;
import org.hfoss.posit.android.utilities.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * Represents a specific find for a project, with a unique identifier
 * 
 */
public class Find {
	private static final String TAG = "Find";

	private PositDbHelper mDbHelper;  // Handles all the DB actions
	private long mId = -1;  	         // The Find's rowID (should be changed to DB ID)
	private String mGuid = "";       // BARCODE -- globally unique ID
	public Cursor images = null;

	/**
	 * This constructor is used for a new Find
	 * @param context is the Activity
	 */
	public Find (Context context) {
		mDbHelper = new PositDbHelper(context);
		mId = -1;
	}
	
	/**
	 * This constructor is used for an existing Find.
	 * @param context is the Activity
	 * @param id is the Find's _id in the Sqlite DB
	 */
	public Find (Context context, long id) {
		this(context);
		mId = id;
		mGuid = "";
		images = getImages();
	}
	
	/**
	 * This constructor is used for an existing Find.
	 * @param context is the Activity
	 * @param guid is a globally unique identifier, used by the server
	 *   and other devices
	 */
	public Find (Context context, String guid) {
		this(context);
		mId = -1;
		mGuid = guid;
	}

	public void setGuid(String guid) {
		mGuid = guid;
	}
	/**
	 * 	getContent() returns the Find's <attr:value> pairs in a ContentValues array. 
	 *  This method assumes the Find object has been instantiated with a context
	 *  and an id.
	 *  @return A ContentValues with <key, value> pairs
	 */
	public ContentValues getContent() {
		ContentValues values = mDbHelper.fetchFindDataById(mId,null);
		return values;
	}
	
	private void getMediaUrisFromDb(ContentValues values) {
	}
	
	public Uri getImageUriByPosition(long findId, int position) {
		return mDbHelper.getPhotoUriByPosition(findId, position);
	}
	
	
	/**
	 * Deprecated
	 * Returns a hashmap of string key and value
	 * this is used primarily for sending and receiving over Http, which is why it 
	 * somewhat works as everything is sent as string eventually. 
	 * @return
	 */
	public HashMap<String,String> getContentMap(){
		Log.i(TAG, "getContentMap " + mId);
		return mDbHelper.fetchFindMapById(mId);
	}
	
	/**
	 * Returns a hashmap of key/value pairs represented as Strings.
	 * This is used primarily for sending and receiving over Http.
	 * @return
	 */
	public HashMap<String,String> getContentMapGuid(){
		Log.i(TAG, "getContentMapGuid " + mGuid);
		return mDbHelper.fetchFindMapByGuid(mGuid);
	}

	/** 
	 * Creates an entry for a Find in the DB. 
	 * Assumes that the context has been passed through a constructor.
	 * @param content contains the Find's attributes and values.  
	 * @param images list of contentvalues containing the image references to add
	 * @return whether the DB operation succeeds
	 */
	public boolean insertToDB(ContentValues content, List<ContentValues> images) {
		if (content != null) {
			mId = mDbHelper.addNewFind(content);
		}
		
		if (mId != -1)
			return insertImagesToDB(images);
		else 
			return false;
	}
	
	/**
	 * Inserts images for this find
	 * @param images
	 * @return
	 */
	public boolean insertImagesToDB(List<ContentValues> images) {
		if (Utils.debug) Log.i(TAG, "insertImagesToDB, mId=" + mId + " guId=" + mGuid);
		if (images == null || images.size() == 0)
			return true; // Nothing to do
		if (mId != -1 && !mGuid.equals(""))
			return mDbHelper.addPhotos(mId, mGuid, images);
		else 
			return false;
	}

	/** 
	 * updateDB() assumes that the context and rowID has be passed through 
	 * a constructor.
	 * @param content contains the Find's attributes and values.  
	 * @return whether the DB operation succeeds
	 */
	// TODO: Confirm that this works with GUIDs
	public boolean updateToDB(ContentValues content) {//, ContentValues images) {
		if (isSynced()) { //TODO think of a better way to do this
			//set it as unsynced
			content.put(PositDbHelper.FINDS_SYNCED, PositDbHelper.FIND_NOT_SYNCED);
			//add revision only once. Don't count revisions for in-phone updates.
		}
		return mDbHelper.updateFind(mId, content);
	}
	
	/**
	 * Updates a find given its guid.
	 * @param guid
	 * @param content
	 * @return
	 */
	public boolean updateToDB(String guid, ContentValues content) {//, ContentValues images) {
		if (isSynced()) { //TODO think of a better way to do this
			//set it as unsynced
			content.put(PositDbHelper.FINDS_SYNCED, PositDbHelper.FIND_NOT_SYNCED);
			//add revision only once. Don't count revisions for in-phone updates.
		}
		return mDbHelper.updateFind(guid, content, null);
	}
	
	/**
	 * deletes the Find object form the DB, not including its photos
	 * Call deleteFindPhotos() to delete its photos.
	 * @return whether the DB operation was successful
	 */
	public boolean delete() {
		Log.i(TAG,"deleteing find #"+mId);
		return mDbHelper.deleteFind(mId);
	}
	
	/**
	 * deletes the photos associated with this find.
	 * @return
	 */
	public boolean deleteFindPhotos() {
		Log.i(TAG,"deleteing find #"+mId);
		return mDbHelper.deletePhotosById(mId);
	}

	/**
	 * @return the mId
	 */
	public long getId() {
		if (mId != -1) 
			return mId;
		else {
			mId = mDbHelper.getRowIdFromGuId(mGuid);
			return mId;
		}
	}
	
	/**
	 * @return the guId
	 */
	public String getguId() {
		return mGuid;
	}
	
	
	/**
	 * NOTE: This may cause a leak because the Cursor is not closed
	 * Get all images attached to this find
	 * @return the cursor that points to the images
	 */
	public Cursor getImages() {
		Log.i(TAG,"GetImages find id = "+mId);
		return mDbHelper.getImagesCursor(mId);
	}
	
	public ArrayList<ContentValues> getImagesContentValuesList() {
		return mDbHelper.getImagesList(mId);
	}
	
	/**
	 * @return whether or not there are images attached to this find
	 */
	public boolean hasImages(){
		return getImages().getCount() > 0;
	}

	public boolean deleteImageByPosition(int position) {
		return false;
	}
	

	/**
	 * Directly sets the Find as either Synced or not.
	 * @param status
	 */
	public void setSyncStatus(boolean status){
		ContentValues content = new ContentValues();
		content.put(PositDbHelper.FINDS_SYNCED, status);
		if (mId != -1)
			mDbHelper.updateFind(mId, content);
		else 
			mDbHelper.updateFind(mGuid, content,null); //photos=null
	}
	
	// TODO: Test that this method works with GUIDs
	public int getRevision() {
		ContentValues value = mDbHelper.fetchFindDataById(mId, new String[] { PositDbHelper.FINDS_REVISION});
		return value.getAsInteger(PositDbHelper.FINDS_REVISION);
	}
	
	/**
	 * Used for adhoc finds.  Tests whether find already exists
	 * @param guid
	 * @return
	 */
	public boolean exists(String guid) {
		return mDbHelper.containsFind(guid);
	}
	
	/**
	 * Tests whether the Find is synced.  This method should work with
	 * either GUIDs (Find from a server) or row iDs (Finds from the phone).
	 * If neither is set, it returns false.
	 * @return
	 */
	public boolean isSynced() {
		ContentValues value=null;
		Log.i(TAG, "isSynced mId = " + mId + " guId = " + mGuid);
		if (mId != -1) {
			value = mDbHelper.fetchFindDataById(mId, new String[] { PositDbHelper.FINDS_SYNCED});
			return value.getAsInteger(PositDbHelper.FINDS_SYNCED)==PositDbHelper.FIND_IS_SYNCED;
		} else if (!mGuid.equals("")){
			value = mDbHelper.fetchFindDataByGuId(mGuid, new String[] { PositDbHelper.FINDS_SYNCED});
			return value.getAsInteger(PositDbHelper.FINDS_SYNCED)==PositDbHelper.FIND_IS_SYNCED;
		} else 
			return false;
	}	
}
