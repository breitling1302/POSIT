/*
 * File: FindInterface.java
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
package org.hfoss.posit.android.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.j256.ormlite.dao.Dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/**
 * Currently unused interface for Finds.
 * 
 * TODO: Decide whether its necessary and, if so,
 * build out its implementation.
 *
 */
public interface FindInterface {

	/**
	 * @return the mId
	 */
	public int getId();
	
	/**
	 * @return the guId
	 */
	public String getGuid();
	
//	/**
//	 * Inserts this Find object into the DB, including its data.
//	 * Call delete() to delete this Find.
//	 * @return the row id or -1 if it was unsuccessful
//	 */
//	public int insert(Dao<Find, Integer> dao);
//	
//	/**
//	 * UPdates this Find object in the DB.
//	 * Call delete() to delete this Find.
//	 * @return the row id or -1 if it was unsuccessful
//	 */
//	public int update(Dao<Find, Integer> dao);
//
//	/**
//	 * Deletes this Find object form the DB, including its data
//	 * @return whether the DB operation was successful
//	 */
//	public int delete(Dao<Find, Integer> dao);
//	
//	/**
//	 * Syncs this Find object using the designated protocol.
//	 * @param protocol is a String, either SMS, HTTP
//	 * @return whether the DB operation was successful
//	 */
//	public void sync(String protocol);

	

//	public void setGuid(String guid);
//	
//	/**
//	 * 	getContent() returns the Find's <attr:value> pairs in a ContentValues array. 
//	 *  This method assumes the Find object has been instantiated with a context
//	 *  and an id.
//	 *  @return A ContentValues with <key, value> pairs
//	 */
//	public ContentValues getContent();
//		
//	public Uri getFindDataUriByPosition(long findId, int position);
//	
//	
//	/**
//	 * Inserts find data entries for this find
//	 * @param find_data_entries
//	 * @return
//	 */
//	public boolean insertFindDataEntriesToDB(List<ContentValues> find_data_entries);
//	
//	/**
//	 * deletes the data associated with this find.
//	 * @return
//	 */
//	public boolean deleteFindDataEntry();

//	/**
//	 * NOTE: This may cause a leak because the Cursor is not closed
//	 * Get all find data entries attached to this find
//	 * @return the cursor that points to the find data
//	 */
//	public Cursor getFindDataEntriesCursor();
//	
//	public ArrayList<ContentValues> getFindDataEntriesList();

	
//	/**
//	 * @return whether or not there is find data attached to this find
//	 */
//	public boolean hasDataEntries();
//
//	public boolean deleteFindDataEntriesByPosition(int position);
//	
//
//	/**
//	 * Directly sets the Find as either Synced or not.
//	 * @param status
//	 */
//	public void setSyncStatus(boolean status);
//	
//	// TODO: Test that this method works with GUIDs
//	public int getRevision();
//	
//	/**
//	 * Used for adhoc finds.  Tests whether find already exists
//	 * @param guid
//	 * @return
//	 */
//	public boolean exists(String guid);
//	
//	/**
//	 * Tests whether the Find is synced.  This method should work with
//	 * either GUIDs (Find from a server) or row iDs (Finds from the phone).
//	 * If neither is set, it returns false.
//	 * @return
//	 */
//	public boolean isSynced();

}
