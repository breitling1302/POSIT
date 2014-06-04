/*
 * File: SyncMedium.java
 * 
 * Copyright (C) 2011 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Source Information Tool. 
 *
 * This code is free software; you can redistribute it and/or modify
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
package org.hfoss.posit.android.sync;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.database.DbHelper;
import org.hfoss.posit.android.api.plugin.FindPlugin;
import org.hfoss.posit.android.api.plugin.FindPluginManager;
import org.hfoss.posit.android.functionplugin.sms.ObjectCoder;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

/**
 * Abstract class that implements generic synchronization functions that are to be
 * used by various synchronization methods. Each new type of synchronization method
 * should inherit from this class and override methods as necessary.
 * 
 * Some of its functionality has been made public static so that other developers may
 * have access to the methods created in this class.
 * 
 * @author Andrew Matsusaka
 *
 */
public abstract class SyncMedium {
	private static final String TAG = "SyncMedium";
	
	protected Context m_context;
	protected int m_projectId;
	protected String m_authKey;
	
	public void sync( String authToken ) {
		m_authKey = authToken;
		getFinds();
		sendFinds();
	}
	
	/**
	 * Generic method for getting all unsynced finds from another device.
	 */
	public void getFinds(){
		List<String> findGuids = getFindsNeedingSync();
		
		for( String guid : findGuids ){
			String rawFind = retrieveRawFind(guid );
			Find newFind = convertRawToFind( rawFind );
			storeFind( newFind );
		}
	}
	
	/**
	 * Generic method for sending all newly changed finds to another device
	 */
	public void sendFinds(){
		List<Find> changedFinds = getChangedFinds();

		for( Find find : changedFinds ){
		    if (find != null)
		        sendFind( find );
		}
		
		postSendTasks();
	}
	
	/**
	 * Returns a list of find guids that are not synced to the current device
	 * @return list of guids
	 */
	public abstract List<String> getFindsNeedingSync();
	
	/**
	 * Retrieves raw find data in string from based on the passed guid
	 * @param guid of the find to be retrieved
	 * @return raw form of find data
	 */
	public abstract String retrieveRawFind( String guid );
	
	/**
	 * Sends a find to another device
	 * @param find to be sent
	 * @return whether or not sending was successful
	 */
	public abstract boolean sendFind( Find find );
	
	/**
	 * Any tasks that need to be performed after sending finds
	 * @return whether or not tasks were successful
	 */
	public abstract boolean postSendTasks();
	
	/**
	 * Accepts the raw form of a find and returns a Find object
	 * @param rawFind - raw form of find
	 * @return Find object filled in with raw find data
	 */
	public Find convertRawToFind( String rawFind ){
		return convertRawToFindWithBundle( rawFind );
	}
	
	/**
	 * Accepts the raw form of a find, extracts values and stores in a bundle
	 * which is then used to fill in a Find object
	 * @param rawFind - raw form of find
	 * @return Find object filled in with raw find data
	 */
	public static Find convertRawToFindWithBundle( String rawFind ){
		Find newFind 		= createTypedFind();
		Bundle bundle 		= newFind.getDbEntries();
		List<String> keys 	= getBundleKeys( bundle );
		List<String> values = parseValuesFromRaw( rawFind );
		boolean success		= false;
		
		if( validateNewDataSize( keys, values ) )
			success = fillBundleValues( bundle, keys, values, newFind );
		
		if( success )
			newFind.updateObject(bundle);
		else
			newFind = null;
		
		return newFind;
	}

	/**
	 * Accepts a Find object to be converted to its raw string form
	 * @param find - find to be converted into string form
	 * @return string containing all the find object's data
	 */
	public static String convertFindToRaw( Find find ){
		return convertBundleToRaw( find.getDbEntries() );
	}

	/**
	 * Adds the contents of an ENTIRE Find to be transmitted later. Also returns
	 * the raw message. The format of the message is as follows: <br>
	 * <br>
	 * 
	 * (prefix)value1,value2,value3,... <br>
	 * <br>
	 * 
	 * The values are, in lexicographical order of the attribute names, strings
	 * encoding the attributes' values. These encodings are handled by
	 * ObjectCoder.
	 * 
	 * @param dbEntries
	 *            A Bundle object containing all of a Find's database fields.
	 * @param phoneNumber
	 *            The phone number that the Find should be transmitted to
	 * @return A String containing the text message.
	 * @throws IllegalArgumentException
	 *             if one of the values could not be encoded.
	 * 
	 */
	public static String convertBundleToRaw( Bundle dbEntries )
			throws IllegalArgumentException {
		List<String> keys = new ArrayList<String>(dbEntries.keySet());
		StringBuilder builder = new StringBuilder();
		
		Collections.sort(keys);
		
		for (String key : keys) {
			if (builder.length() > 0)
				builder.append(",");
			String code = ObjectCoder.encode(dbEntries.get(key));
			if (code != null) {
				builder.append(code);
			} else {
				Log.e(TAG, "Tried to encode object of unsupported type.");
				throw new IllegalArgumentException();
			}
		}
		String text = builder.toString();
		return text;
	}
	
	/**
	 * Accepts raw find data and extracts each of its string values
	 * separated by commas
	 * @param rawFind - raw form of find
	 * @return list of strings extracted from the raw find data
	 */
	private static List<String> parseValuesFromRaw( String rawFind ){
		List<String> values = new ArrayList<String>();
		StringBuilder current = new StringBuilder();
		
		for (int i = 0; i < rawFind.length(); i++) {
			char c = rawFind.charAt(i);
			if (c == ObjectCoder.ESCAPE_CHAR) {
				current.append(c);
				if (i + 1 < rawFind.length())
					c = rawFind.charAt(++i);
				current.append(c);
			} else if (c == ',') {
				values.add(current.toString());
				current = new StringBuilder();
			} else {
				current.append(c);
			}
		}
		values.add(current.toString());
		
		return values;
	}
	
	/**
	 * Creates a find of the correct type based on the plugin
	 * @return Find object of the correct type
	 */
	private static Find createTypedFind(){
		Find find;
		
		try {
			FindPlugin plugin = FindPluginManager.mFindPlugin;
			if (plugin == null) {
				Log.e(TAG, "Could not retrieve Find Plugin.");
				return null;
			}
			find = plugin.getmFindClass().newInstance();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		} catch (InstantiationException e) {
			e.printStackTrace();
			return null;
		}
		
		return find;
	}
	
	/**
	 * Given a bundle, a list of strings containing the bundle's keys is
	 * returned
	 * @param bundle - Bundle to extract key list from
	 * @return list of keys in string form
	 */
	private static List<String> getBundleKeys( Bundle bundle ){
		List<String> keys = new ArrayList<String>(bundle.keySet());
		Collections.sort(keys);
		return keys;
	}
	
	/**
	 * Compares to keys and values to make sure they match in size
	 * @param keys - list of keys to be compared
	 * @param values - list of values to be compared
	 * @return whether or not keys and values match sizes
	 */
	private static boolean validateNewDataSize( List<String> keys, List<String> values ){
		boolean valid = true;
		if (values.size() != keys.size()) {
			Log.e(TAG,
					"Received value set does not have expected size. values = "
							+ values.size() + ", keys = " + keys.size());
			valid = false;
		}
		
		return valid;
	}
	
	/**
	 * Fills in the passed in Bundle with the keys and values.
	 * @param bundle - Bundle to be filled in with new data
	 * @param keys - keys for the new data being added to Bundle
	 * @param values - values being added to Bundle
	 * @param newFind - used to extract the correct entry type based on key
	 * @return whether or not filling the Bundle was successful
	 */
	private static boolean fillBundleValues( Bundle bundle, List<String> keys, List<String> values, Find newFind ){
		boolean success = true;
		
		for (int i = 0; i < values.size(); i++) {
			String key = keys.get(i);
			String value = values.get(i);
			Class<Object> type = getEntryType( newFind, key );
			
			if( type != null ){
				Serializable obj = null;
				
				try {
					obj = (Serializable) ObjectCoder.decode( value, type );
				} catch (IllegalArgumentException e) {
					Log.e(TAG, "Failed to decode value for attribute \"" + key
							+ "\", string was \"" + value + "\"");
					success = false;
				}
				
				if( success )
					bundle.putSerializable(key, obj);
			}
			else{
				success = false;
			}
		}
		
		return success;
	}

	/**
	 * Determines the correct object type for the corresponding key using
	 * the find passed in
	 * @param newFind - find used to determine type
	 * @param key - key used to determine type
	 * @return Object type corresponding to the key
	 */
	private static Class<Object> getEntryType( Find newFind, String key ){
		Class<Object> type = null;
		try {
			type = newFind.getType(key);
		} catch (NoSuchFieldException e) {
			Log.e(TAG, "Encountered no such field exception on field: "
					+ key);
			e.printStackTrace();
		}
		
		return type;
	}

	/**
	 * Stores the find on the current device.
	 * If the find exists, it is updated.
	 * If the find does not exist, a new find is inserted.
	 * @param newFind - find to be inserted into device's storage
	 */
	public void storeFind( Find newFind ){
		Find find = DbHelper.getDbManager(m_context).getFindByGuid(newFind.getGuid());
		if (find != null) {
			Log.i("SyncMedium", "Updating existing find: " + find.getId());
			DbHelper.getDbManager(m_context).updateWithoutHistory(newFind);				
		} else {
			Log.i("SyncMedium", "Inserting new find: " + newFind.getId());
			Log.i("SyncMedium", "Adding a new find " + newFind);
			
			DbHelper.getDbManager(m_context).insertWithoutHistory(newFind);
		}
	}
	
	/**
	 * Accepts a comma separated list of strings and returns a List
	 * containing the strings
	 * @param guids - string of guids to be tokenized and stored in List
	 * @return List containing guid strings
	 */
	public List<String> convertGuidStringToList( String guids ){
		StringTokenizer tokenizer = new StringTokenizer( guids, "," );
		List<String> guidList = new ArrayList<String>();
		
		while( tokenizer.hasMoreTokens() ){
			guidList.add( tokenizer.nextToken() );
		}
		
		return guidList;
	}
	
	/**
	 * Gets a list of changed find guids in the form of a single string
	 * @return
	 */
	public String getChangedFindGuidsString(){
		List<String> changedGuids = getChangedFindGuids();
		StringBuilder builder = new StringBuilder();
		
		for( String guid : changedGuids ){
			builder.append( guid );
			builder.append(",");
		}
		
		builder.deleteCharAt(builder.length()-1);
		
		return builder.toString();
	}
	
	/**
	 * Gets the list of changed find guids
	 * @return List of strings containing changed find guids
	 */
	public List<String> getChangedFindGuids(){
		List<Find> changedFinds = getChangedFinds();
		List<String> changedGuids = new ArrayList<String>();
		
		for( Find find : changedFinds ){
			changedGuids.add( find.getGuid() );
		}
		
		return changedGuids;
	}
	
	/**
	 * Calls database to get a list of all changed finds on current device
	 * @return List containing all changed finds
	 */
	public List<Find> getChangedFinds(){
		return DbHelper.getDbManager(m_context).getChangedFinds(m_projectId);
	}
}
