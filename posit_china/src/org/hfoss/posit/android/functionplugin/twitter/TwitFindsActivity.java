/*
 * File: TwitFindsActivity.java
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

package org.hfoss.posit.android.functionplugin.twitter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.api.plugin.AddFindPluginCallback;
import org.hfoss.posit.android.api.plugin.FindActivityProvider;
import org.hfoss.posit.android.api.plugin.ListFindPluginCallback;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;

public class TwitFindsActivity extends OrmLiteBaseActivity<DbManager> {
//	implements AddFindPluginCallback, ListFindPluginCallback {

	public static final String TAG = "TwitFindsActivity";

	/** Name to store the users access token 
	 * This is returned after the user provides login information to twitter.*/
	private static String PREF_ACCESS_TOKEN = "";
	/** Name to store the users access token secret.
	 * This is returned after the user provides login information to twitter.*/
	private static String PREF_ACCESS_TOKEN_SECRET = "";
	/** Consumer Key generated when you registered your app at https://dev.twitter.com/apps/ 
	 * It is linked to an application created by a dev account. */
	private static final String CONSUMER_KEY = "s47vJySEVLHBnuhXy3yw";
	/** Consumer Secret generated when you registered your app at https://dev.twitter.com/apps/  
	 * It is linked to an application created by a dev account. */
	private static final String CONSUMER_SECRET = "75MMxh4QYKmJnnm1TtR7o1BiJuloDN1DqVmNF3838"; // XXX Encode in your app
	/** The url that Twitter will redirect to after a user log's in - this will be picked up by your app manifest and redirected into this activity 
	 * This is also defined in the <data android:scheme="twitFinds" /> of the intent filter for this activity. */
	private static final String CALLBACK_URL = "twitFinds:///";
	
	
	/** Twitter4j object */
	private Twitter mTwitter;
	/** The request token signifies the unique ID of the request you are sending to twitter  */
	private RequestToken mReqToken;
	
	private String tweetString = ""; 
	private List<Entry<String, Object>> mEntries;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {	
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		
		// Ensure user is logged in.
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);		
		PREF_ACCESS_TOKEN = prefs.getString("prefaccesstoken", "");
		PREF_ACCESS_TOKEN_SECRET = prefs.getString("prefaccesstokensecret", "");
		if(PREF_ACCESS_TOKEN == "" || PREF_ACCESS_TOKEN_SECRET == "") {
			//TODO: Allow user to login here as well. Challenge, redirecting back to the find. 
			Toast.makeText(this, "Please Login to Twitter in the Preferences", Toast.LENGTH_LONG).show();
			finish();
		} else {
			// Load the twitter4j helper
			mTwitter = new TwitterFactory().getInstance();
			
			// Tell twitter4j that we want to use it with our app
			mTwitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
			
			// Retrieve find string.
			// Retrieve DbEntries of Find from intent
			ContentValues cv = getIntent().getParcelableExtra("DbEntries");
			mEntries = new ArrayList<Entry<String, Object>>(
					cv.valueSet());
			
			// Access point for find plug-ins to insert their own tweet find string.
			// Find plug-in would need to store the tweet find string as a field instead of generating it dynamically.
			for (Entry<String, Object> entry : mEntries) {
				if(entry.getKey().matches("tweetFindString")){
					tweetString = entry.getValue().toString();
				}
			}
			// If the find object does not have an custom find string, create one. 
			if(tweetString == null || tweetString == ""){
				StringBuilder builder = new StringBuilder();
				for (Entry<String, Object> entry : mEntries) {
					builder.append(entry.getKey());
					builder.append("= ");
					builder.append(entry.getValue().toString());
					builder.append(" ");
				}
				tweetString = builder.toString();
			}
			
			AccessToken at = new AccessToken(PREF_ACCESS_TOKEN, PREF_ACCESS_TOKEN_SECRET);
			mTwitter.setOAuthAccessToken(at);
			tweetMessage();
			finish();
		}
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume");
		super.onResume();
	}
	
	/**
	 * Send a tweet based on tweetString. 
	 */
	private void tweetMessage() {
		try {
			for ( String message : formMessages(tweetString)) {
				if(message.length() == 0){
					throw new TwitterException("Tweet Message Empty");
				}
				mTwitter.updateStatus(message);
			}
			
			Toast.makeText(this, "Tweet Successful!", Toast.LENGTH_SHORT).show();
		} catch (TwitterException e) {
			Toast.makeText(this, "Tweet error, try again later", Toast.LENGTH_SHORT).show();
		}
	}
	
	/**
	 * Given a string message, the function breaks it up into messages. It looks for spaces to divide up the message. 
	 * The function returns an string array where each index does not contain more then 140 characters.
	 * 
	 * @param data string array where each index does not contain more then 140 characters.
	 * @return
	 */
	private String[] formMessages(String data) throws TwitterException{
		StringTokenizer tokenize = new StringTokenizer(data, " ", true);
		ArrayList<String> results = new ArrayList<String>();
		StringBuffer result = new StringBuffer();
		String next = "";
		while(tokenize.hasMoreTokens()){
			next = tokenize.nextToken();
			// If the current message + the next token is too long, then save the current message in its own individual message.
			if (result.length() + next.length() > 140) {
				results.add(result.toString());
				result = new StringBuffer();
			}
			
			// If the current message is already saved, and the token is still too long, then we must subdivide the token.
			if(result.length() == 0 && next.length() > 140) {
				// If the token is just way too long, then we must stop. Else we might end up with thousands of messages. 
				if(next.length() > 5*140){
					throw new TwitterException("Twitter message Too Long");
				}
				
				// Assert: the token is not too long. Subdivide the token into pieces of 140 characters.
				for(int i=0; i < next.length(); i=i+140){
					if(next.length() > i+140){
						results.add(next.substring(i, i+140));
					} else {
						results.add(next.substring(i));
					}	
				}
			} else {
				result.append(next);
			}
		}
		
		results.add(result.toString());
		
		return results.toArray(new String[results.size()]);
	}

//	/**
//	 * Required for function plugins. Unused here.
//	 */
//	public void listFindCallback(Context context, Find find, View view) {
//		// TODO Auto-generated method stub
//		
//	}
//	/**
//	 * Required for function plugins. Unused here.
//	 */
//	public void menuItemSelectedCallback(Context context, Find find, View view,
//			Intent intent) {
//		// TODO Auto-generated method stub
//		
//	}
//	/**
//	 * Required for function plugins. Unused here.
//	 */
//	public void onActivityResultCallback(Context context, Find find, View view,
//			Intent intent) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	public void displayFindInViewCallback(Context context, Find find, View view) {
//		// TODO Auto-generated method stub
//		
//	}
//
//	public void afterSaveCallback(Context context, Find find, View view,
//			boolean isSaved) {
//		// TODO Auto-generated method stub
//		
//	}
}
