package org.hfoss.posit.android.sync;

/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.http.ParseException;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.background.BackgroundListener;
import org.hfoss.posit.android.background.BackgroundManager;
import org.hfoss.posit.android.background.SyncCallable;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

/**
 * SyncAdapter implementation for syncing sample SyncAdapter contacts to the
 * platform ContactOperations provider.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

	private static final String TAG = "SyncAdapter";

	private final AccountManager mAccountManager;

	private final Context mContext;

	private Communicator communicator;

	private Date mLastUpdated;

	/**
	 * Account type string.
	 */
	public static final String ACCOUNT_TYPE = "org.hfoss.posit.account";

	/**
	 * Authtoken type string.
	 */
	public static final String AUTHTOKEN_TYPE = "org.hfoss.posit.account";

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		mContext = context;
		mAccountManager = AccountManager.get(context);
	}
	
	

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
	        ContentProviderClient provider, SyncResult syncResult) {

		List<Find> finds;
		Log.i(TAG, "In onPerformSync()");
		String authToken = null;
		boolean success;
		
		try {
		    BackgroundManager.runTask(
		            new SyncCallable(mContext, account, mAccountManager),
		            new BackgroundListener<Void>() {
                public void onBackgroundResult(Void response) {}
		    });
			
/*			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
			int projectId = prefs.getInt(mContext.getString(R.string.projectPref), 0);
			
			// Get finds changed/created on phone
			finds = DbHelper.getDbManager(mContext).getChangedFinds(projectId);
			
			// Get finds changed/created on server
			String serverFindsIds = Communicator.getServerFindsNeedingSync(mContext, authToken);
			
			// Get each find from the server and store in the phone's database
			if (!serverFindsIds.equals("")) {
				success = Communicator.getFindsFromServer(mContext, authToken, serverFindsIds);
				Log.i(TAG, "server find id's: " + serverFindsIds);
			}

			// Send each find to the server 
			if (finds != null) {
				Communicator.sendFindsToServer(finds, mContext, authToken);
				// Record sync on the phone
				DbHelper.getDbManager(mContext).recordSync(new SyncHistory("idkwhatthisissupposedtobe"));
			}
			
			// Record the sync on the server
			success = Communicator.recordSync(mContext, authToken);
			
			Log.i(TAG, "Sync recorded: " + success);
			
			DbHelper.releaseDbManager();
			/**/
//			
//			boolean success = false;
//			//mdbh = new PositDbHelper(mContext);
//
//			//Log.i(TAG, "server=" + server + " key=" + authKey + " pid="
//			//		+ mProjectId + " imei=" + imei);
//
//			// Wait here to make sure there is a WIFI connection
//			//waitHere();
//			
//			// Check that project exists
//			if(!comm.projectExists(""+mProjectId, server))
//				mHandler.sendEmptyMessage(PROJECTERROR);
//			
//			// Get finds from the server since last sync with this device
//			// (NEEDED: should be made project specific)
//
			//String serverFindGuIds = Communicator.getServerFindsNeedingSync(mContext, authToken);
//
//			// Get finds from the client
//
//			String phoneFindGuIds = mdbh.getDeltaFindsIds(mProjectId);
//			Log.i(TAG, "phoneFindsNeedingSync = " + phoneFindGuIds);
//
//			// Send finds to the server
//
//			success = sendFindsToServer(phoneFindGuIds);
//
//			// Get finds from the server and store in the DB
//
//			success = getFindsFromServer(serverFindGuIds);
//
//			// Record the synchronization in the client's sync_history table
//
//			ContentValues values = new ContentValues();
//			values.put(PositDbHelper.SYNC_COLUMN_SERVER, server);
//
//			success = mdbh.recordSync(values);
//			if (!success) {
//				Log.i(TAG, "Error recording sync stamp");
//				mHandler.sendEmptyMessage(SYNCERROR);
//			}
//
//			// Record the synchronization in the server's sync_history table
//
//			String url = server + "/api/recordSync?authKey=" + authKey + "&imei="
//					+ imei;
//			Log.i(TAG, "recordSyncDone URL=" + url);
//			String responseString = "";
//
//			try {
//				responseString = comm.doHTTPGET(url);
//			} catch (Exception e) {
//				Log.i(TAG, e.getMessage());
//				e.printStackTrace();
//				mHandler.sendEmptyMessage(NETWORKERROR);
//			}
//			Log.i(TAG, "HTTPGet recordSync response = " + responseString);
//
//			mHandler.sendEmptyMessage(DONE);
//			return;
		
			
		} catch (final ParseException e) {
			syncResult.stats.numParseExceptions++;
			Log.e(TAG, "ParseException", e);
		}
	}
}