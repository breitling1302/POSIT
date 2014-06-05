/*
 * File: Communicator.java
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
package org.hfoss.posit.android.sync;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.hfoss.posit.android.Constants;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.FindHistory;
import org.hfoss.posit.android.api.activity.ListProjectsActivity;
import org.hfoss.posit.android.api.authentication.AuthenticatorActivity;
import org.hfoss.posit.android.api.database.DbHelper;
import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.api.plugin.FindPluginManager;
import org.hfoss.posit.android.functionplugin.camera.Camera;
import org.hfoss.posit.android.R;
//import org.hfoss.posit.android.functionplugin.tracker.TrackerActivity;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

/**
 * The communication module for POSIT. Handles most calls to the server to get
 * information regarding projects and finds.
 * 
 * 
 */

//public class Communicator extends OrmLiteBaseActivity<TrackerDbManager> {
public class Communicator {
	private static final String MESSAGE = "message";
	private static final String MESSAGE_CODE = "messageCode";
	private static final String ERROR_MESSAGE = "errorMessage";
	private static final String ERROR_CODE = "errorCode";
	private static final String COLUMN_IMEI = "imei";
	//Start of addition columns for photo table
	private static final String COLUMN_GUID = "guid"; 				//guid of the find
	private static final String COLUMN_IDENTIFIER = "identifier"; 	//does not seem to be useful
	private static final String COLUMN_PROJECT_ID = "project_id"; 	//project id of the find
	private static final String COLUMN_TIMESTAMP = "timestamp"; 	//if this is not set, it uses the current timestamp
	private static final String COLUMN_MIME_TYPE = "mime_type"; 	//data type, in this case, "image/jpeg"
	private static final String COLUMN_DATA_FULL = "data_full"; 	//data for the image, takes Base64 string of image
	private static final String COLUMN_DATA_THUMBNAIL = "data_thumbnail"; //data for the image, take Base 64 string of image
	//End of addition columns for photo table
	public static final int THUMBNAIL_TARGET_SIZE = 320; //width and height of thumbnail data
	public static final int CONNECTION_TIMEOUT = 3000; // millisecs
	public static final int SOCKET_TIMEOUT = 5000;
	public static final String RESULT_FAIL = "false";
	private static String server;
	private static String authKey;
	private static String imei;
	
	private static int projectId;
	
	private static final String SERVER_PREF = "serverKey";
	private static final String PROJECT_PREF = "projectKey";

	private static String TAG = "Communicator";
	private static String responseString;
	private Context mContext;
	private SharedPreferences applicationPreferences;
	private HttpParams mHttpParams;
	private static HttpClient mHttpClient;
	private ThreadSafeClientConnManager mConnectionManager;
	public static long mTotalTime = 0;
	private static long mStart = 0;

	
//	public void setContext(Context _context) {
//		mContext = _context;
//		mTotalTime = 0;
//		mStart = 0;
//
//		mHttpParams = new BasicHttpParams();
//
//		// Set the timeout in milliseconds until a connection is established.
//		HttpConnectionParams.setConnectionTimeout(mHttpParams, CONNECTION_TIMEOUT);
//		
//		// Set the default socket timeout (SO_TIMEOUT) 
//		// in milliseconds which is the timeout for waiting for data.
//		HttpConnectionParams.setSoTimeout(mHttpParams, SOCKET_TIMEOUT);
//
//		SchemeRegistry registry = new SchemeRegistry();
//		registry.register(new Scheme("http", new PlainSocketFactory(), 80));
//		mConnectionManager = new ThreadSafeClientConnManager(mHttpParams,
//				registry);
//		mHttpClient = new DefaultHttpClient(mConnectionManager, mHttpParams);
//
//		PreferenceManager.setDefaultValues(mContext, R.xml.posit_preferences,
//				false);
//		applicationPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
//		setApplicationAttributes(
//				getAuthKey(mContext),
//				//applicationPreferences.getString("AUTHKEY", ""), 
//				applicationPreferences.getString(SERVER_PREF, server), 
//				applicationPreferences.getInt(PROJECT_PREF, projectId));
//		TelephonyManager manager = (TelephonyManager) mContext
//				.getSystemService(Context.TELEPHONY_SERVICE);
//		imei = manager.getDeviceId();
//
//	}
//	
//	private void setApplicationAttributes(String aKey, String serverAddress,
//			int projId) {
//		authKey = aKey;
//		server = serverAddress;
//		projectId = projId;
//	}

	// /**
	// * Attempts to get the auth token. Apparently this might have to perform a
	// * network request, so you're supposed to use a thread.
	// */
	// public static Thread getAuthToken(final Context context) {
	//
	// final Runnable runnable = new Runnable() {
	// public void run() {
	// AccountManager mAccountManager = AccountManager.get(context);
	//
	// // TODO: again just picking the first account here.. how are you
	// // supposed to handle this?
	// Account[] accounts =
	// mAccountManager.getAccountsByType(SyncAdapter.ACCOUNT_TYPE);
	//
	// try {
	// String authKey = mAccountManager
	// .blockingGetAuthToken(accounts[0],
	// SyncAdapter.AUTHTOKEN_TYPE, true /* notifyAuthFailure */);
	// Log.i(TAG, "AUTH TOKEN: " + authKey);
	// } catch (OperationCanceledException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (AuthenticatorException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }
	// };
	// // run on background thread.
	// return performOnBackgroundThread(runnable);
	// }

	
	public static boolean isServerReachable(Context context) {
		SharedPreferences applicationPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		String server = applicationPreferences.getString(SERVER_PREF, "");
		String url = server + "/api/isreachable?authKey=" + getAuthKey(context);

		HashMap<String, Object> responseMap = null;
		Log.i(TAG, "is reachable URL=" + url);

		String responseString = null;
		String responseCode = null;
		try {
			responseString = doHTTPGET(url);
			Log.i(TAG, "isreachable response = " + responseString);
			if (responseString.contains("[Error] ")) {
				Log.e(TAG, responseString);
				return false;
			} else {
				ResponseParser parser = new ResponseParser(responseString);
				responseMap = parser.parseObject();
				//responseCode = (String) responseMap.get(MESSAGE_CODE);
			}
		} catch (Exception e) {
			Log.i(TAG, "longinUser catch clause response = " + responseString);
			Toast.makeText(context, e.getMessage() + "", Toast.LENGTH_LONG).show();
			//sendAuthenticationResult(authKey, false, handler, context);
			return false;
		}
		try {
			if (responseMap.containsKey(ERROR_CODE)) {
				return false;
			} else if (responseMap.containsKey(MESSAGE_CODE)) {
				if (responseMap.get(MESSAGE_CODE).equals(Constants.AUTHN_OK)) {
					return true;
				}
			} else {
				return false;
			}
		} catch (Exception e) {
			Log.e(TAG, "loginUser " + e.getMessage() + " ");
			return false;
		}
		return false;
	}
	
	/**
	 * Removes an account. This should be called when, e.g., the user changes
	 * to a new server.
	 * @param context
	 * @param accountType
	 */
	public static void removeAccount(Context context, String accountType) {
		AccountManager am = AccountManager.get(context);
		am.invalidateAuthToken(accountType, SyncAdapter.AUTHTOKEN_TYPE);
		Account[] accounts = am.getAccountsByType(accountType);
		if (accounts.length != 0)
			am.removeAccount(accounts[0], null, null);
		//String authkey = getAuthKey(context);
		//return authkey == null;
	}
	
	public static String getAuthKey(Context context) {
		AccountManager accountManager = AccountManager.get(context);

		// TODO: again just picking the first account here.. how are you
		// supposed to handle this?
		Account[] accounts = accountManager.getAccountsByType(SyncAdapter.ACCOUNT_TYPE);

		if (accounts.length == 0)
			return null;

		String authKey = null;
		try {
			authKey = accountManager
					.blockingGetAuthToken(accounts[0], SyncAdapter.AUTHTOKEN_TYPE, true /* notifyAuthFailure */);
		} catch (OperationCanceledException e) {
			Log.e(TAG, "getAuthKey(), cancelled during request: " + e.getMessage());
			e.printStackTrace();
		} catch (AuthenticatorException e) {
			Log.e(TAG, "getAuthKey(), authentication exception: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, "getAuthKey() IOException" + e.getMessage());
			e.printStackTrace();
		} catch (IllegalStateException e) {
			Log.e(TAG, "getAuthKey() IllegalStateException" + e.getMessage());
			e.printStackTrace();			
		}
		return authKey;
	}

	/**
	 * NOTE: Calls doHTTPGet
	 * 
	 * Get all open projects from the server. Eventually, the goal is to be able
	 * to get different types of projects depending on the privileges of the
	 * user.
	 * 
	 * @return a list of all the projects and their information, encoded as maps
	 * @throws JSONException
	 */
	public static ArrayList<HashMap<String, Object>> getProjects(Handler handler, Context context) {

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String server = prefs.getString(SERVER_PREF, "");

		String authKey = getAuthKey(context);

		if (authKey != null) {

			String url = server + "/api/listMyProjects?authKey=" + authKey;

			ArrayList<HashMap<String, Object>> list;
			String responseString = doHTTPGET(url);
			Log.i(TAG, responseString);

			if (responseString.contains("Error")) {
				return null;
			}
			list = new ArrayList<HashMap<String, Object>>();
			try {
				list = (ArrayList<HashMap<String, Object>>) (new ResponseParser(responseString).parseList());
			} catch (JSONException e) {
				Log.i(TAG, "getProjects JSON exception " + e.getMessage());
				return null;
			}
			sendProjectsResult(list, true, handler, context);
			return list;
		} else {
			Log.e(TAG, "authKey is null.");
			return null;
		}
	}

	/**
	 * Registers the phone being used with the given server address, the
	 * authentication key, and the phone's imei
	 * 
	 * @param server
	 * @param authKey
	 * @param imei
	 * @return whether the registration was successful
	 */
	public String registerDevice(String server, String authKey, String imei) {
		String url = server + "/api/registerDevice?authKey=" + authKey + "&imei=" + imei;
		Log.i(TAG, "registerDevice URL=" + url);

		String responseString = null;
		try {
			responseString = doHTTPGET(url);
		} catch (Exception e) {
			// Toast.makeText(mContext, e.getMessage(),
			// Toast.LENGTH_LONG).show();
		}
		Log.i(TAG, responseString);
		if (responseString.equals(RESULT_FAIL))
			return null;
		else {
			return responseString;
		}
	}

	/**
	 * Registers the phone being used with the given server address, email,
	 * password and imei.
	 * 
	 * @param email
	 *            email/username
	 * @param password
	 *            the password
	 * @param imei
	 * @param handler
	 *            the handler instance from the UI thread
	 * @param context
	 *            the context of the calling activity
	 * @return the result
	 */
	public static String loginUser(String email, String password, String imei, Handler handler, Context context) {

		SharedPreferences applicationPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		String server = applicationPreferences.getString(SERVER_PREF, "");

		String url = server + "/api/login";

		HashMap<String, Object> responseMap = null;
		Log.i(TAG, "loginUser URL=" + url);

		List<NameValuePair> sendList = new ArrayList<NameValuePair>();
		sendList.add(new BasicNameValuePair("email", email));
		sendList.add(new BasicNameValuePair("password", password));
		sendList.add(new BasicNameValuePair("imei", imei));

		String responseString = null;
		String authKey = null;
		try {
			responseString = doHTTPPost(url, sendList);
			Log.i(TAG, "longinUser response = " + responseString);
			if (responseString.contains("[Error] ")) {
				Log.e(TAG, responseString);
				return null;
			} else {
				ResponseParser parser = new ResponseParser(responseString);
				responseMap = parser.parseObject();
				authKey = (String) responseMap.get(MESSAGE);
			}
		} catch (Exception e) {
			Log.i(TAG, "longinUser catch clause response = " + responseString);
			Toast.makeText(context, e.getMessage() + "", Toast.LENGTH_LONG).show();
			sendAuthenticationResult(authKey, false, handler, context);
			return null;
		}
		try {
			if (responseMap.containsKey(ERROR_CODE)) {
				sendAuthenticationResult(authKey, false, handler, context);
				return null;
			} else if (responseMap.containsKey(MESSAGE_CODE)) {
				if (responseMap.get(MESSAGE_CODE).equals(Constants.AUTHN_OK)) {
					sendAuthenticationResult(authKey, true, handler, context);
					return authKey;
				}
			} else {
				sendAuthenticationResult(authKey, false, handler, context);
				return null;
			}
		} catch (Exception e) {
			Log.e(TAG, "loginUser " + e.getMessage() + " ");
			sendAuthenticationResult(authKey, false, handler, context);
			return null;
		}
		sendAuthenticationResult(authKey, false, handler, context);
		return null;
	}

	/**
	 * Attempts to authenticate the user credentials on the server.
	 * 
	 * @param email
	 *            The user's username
	 * @param password
	 *            The user's password to be authenticated
	 * @param imei
	 *            the phone's IMEI
	 * @param handler
	 *            The main UI thread's handler instance.
	 * @param context
	 *            The caller Activity's context
	 * @return Thread The thread on which the network mOperations are executed.
	 */
	public static Thread attemptAuth(final String email, final String password, final String imei,
			final Handler handler, final Context context) {

		final Runnable runnable = new Runnable() {
			public void run() {
				loginUser(email, password, imei, handler, context);
			}
		};
		// run on background thread.
		return performOnBackgroundThread(runnable);
	}

	/**
	 * Attempts to get the user's projects from the server.
	 * 
	 * @param handler
	 *            The main UI thread's handler instance.
	 * @param context
	 *            The caller Activity's context
	 * @return Thread The thread on which the network mOperations are executed.
	 */
	public static Thread attemptGetProjects(final Handler handler, final Context context) {

		final Runnable runnable = new Runnable() {
			ArrayList<HashMap<String, Object>> projectList;

			public void run() {
				projectList = getProjects(handler, context);
			}
		};
		// run on background thread.
		return performOnBackgroundThread(runnable);
	}

	// /**
	// * Attempts to get changed finds from the server.
	// *
	// * @param handler
	// * The main UI thread's handler instance.
	// * @param context
	// * The caller Activity's context
	// * @return Thread The thread on which the network mOperations are
	// executed.
	// */
	// public static Thread attemptGetChangedFinds(final Handler handler, final
	// Context context) {
	//
	// final Runnable runnable = new Runnable() {
	// //ArrayList<Integer> finds;
	// String finds;
	// public void run() {
	// finds = getServerFindsNeedingSync(handler, context);
	// }
	// };
	// // run on background thread.
	// return performOnBackgroundThread(runnable);
	// }
	/**
	 * Executes the network requests on a separate thread.
	 * 
	 * @param runnable
	 *            The runnable instance containing network mOperations to be
	 *            executed.
	 */
	public static Thread performOnBackgroundThread(final Runnable runnable) {
		final Thread t = new Thread() {
			@Override
			public void run() {
				try {
					runnable.run();
				} finally {
				}
			}
		};
		t.start();
		return t;
	}

	// /**
	// * Sends the result of a getChangedFinds request from server back to the
	// caller
	// * main UI thread through its handler.
	// *
	// * @param projects
	// * the list of projects gotten from server
	// * @param result
	// * The boolean holding authentication result
	// * @param authToken
	// * The auth token returned from the server for this account.
	// * @param handler
	// * The main UI thread's handler instance.
	// * @param context
	// * The caller Activity's context.
	// */
	// private static void sendFindsResult(final String finds, final Boolean
	// result,
	// final Handler handler, final Context context) {
	// if (handler == null || context == null) {
	// return;
	// }
	// handler.post(new Runnable() {
	// public void run() {
	// ((ListFindsActivity) context).onGetChangedFindsResult(finds);
	// }
	// });
	// }

	/**
	 * Sends the result of a getProjects request from server back to the caller
	 * main UI thread through its handler.
	 * 
	 * @param projects
	 *            the list of projects gotten from server
	 * @param result
	 *            The boolean holding authentication result
	 * @param authToken
	 *            The auth token returned from the server for this account.
	 * @param handler
	 *            The main UI thread's handler instance.
	 * @param context
	 *            The caller Activity's context.
	 */
	private static void sendProjectsResult(final ArrayList<HashMap<String, Object>> projects, final Boolean result,
			final Handler handler, final Context context) {
		if (handler == null || context == null) {
			return;
		}
		handler.post(new Runnable() {
			public void run() {
				((ListProjectsActivity) context).onShowProjectsResult(projects, result);
			}
		});
	}

	/**
	 * Sends the authentication response from server back to the caller main UI
	 * thread through its handler.
	 * 
	 * @param authKey
	 *            the auth key obtained from the server
	 * @param result
	 *            The boolean holding authentication result
	 * @param authToken
	 *            The auth token returned from the server for this account.
	 * @param handler
	 *            The main UI thread's handler instance.
	 * @param context
	 *            The caller Activity's context.
	 */
	private static void sendAuthenticationResult(final String authKey, final Boolean result, final Handler handler,
			final Context context) {
		if (handler == null || context == null) {
			return;
		}
		handler.post(new Runnable() {
			public void run() {
				((AuthenticatorActivity) context).onAuthenticationResult(result, authKey);
			}
		});
	}

	
	public String createProject(Context context, String server, String projectName,
			String projectDescription, String authKey) {
		String url = server + "/api/newProject?authKey=" + authKey;
		
		List<NameValuePair> nvp = new ArrayList<NameValuePair>();
		
		nvp.add(new BasicNameValuePair("name", projectName));
		nvp.add(new BasicNameValuePair("description", projectDescription));
		
		HashMap<String, Object> responseMap = null;
		Log.i(TAG, "Create Project URL=" + url);

		String responseString = null;
		
		try {
			responseString = doHTTPPost(url, nvp);
			Log.i(TAG, responseString);
			if (responseString.contains("[ERROR]")) {
				Toast.makeText(context, responseString, Toast.LENGTH_LONG).show();
				return Constants.AUTHN_FAILED + ":" + "Error";
			}
			ResponseParser parser = new ResponseParser(responseString);
			responseMap = parser.parseObject();
		} catch (Exception e) {
			Toast.makeText(context, e.getMessage() + "", Toast.LENGTH_LONG).show();
		}
		try {
			if (responseMap.containsKey(ERROR_CODE))
				return responseMap.get(ERROR_CODE) + ":"
				+ responseMap.get(ERROR_MESSAGE);
			else if (responseMap.containsKey(MESSAGE_CODE)) {
				return (String) responseMap.get(MESSAGE);

			} else {
				return "Malformed message from server.";
			}
		} catch (Exception e) {
			Log.e(TAG, "createProject " + e.getMessage());
			return e.getMessage();
		}
	}
	
	
	//
	// public String registerUser(String server, String firstname,
	// String lastname, String email, String password, String check,
	// String imei) {
	// String url = server + "/api/registerUser";
	// Log.i(TAG, "registerUser URL=" + url + "&imei=" + imei);
	// HashMap<String, String> sendMap = new HashMap<String, String>();
	// sendMap.put("email", email);
	// sendMap.put("password1", password);
	// sendMap.put("password2", check);
	// sendMap.put("firstname", firstname);
	// sendMap.put("lastname", lastname);
	// try {
	// responseString = doHTTPPost(url, sendMap);
	// Log.i(TAG, "registerUser Httpost responseString = "
	// + responseString);
	// if (responseString.contains("[ERROR]")) {
	// Toast.makeText(mContext,
	// Constants.AUTHN_FAILED + ":" + responseString,
	// Toast.LENGTH_LONG).show();
	// return Constants.AUTHN_FAILED + ":" + responseString;
	// }
	// ResponseParser parser = new ResponseParser(responseString);
	// HashMap<String, Object> responseMap = parser.parseObject();
	// if (responseMap.containsKey(ERROR_CODE))
	// return responseMap.get(ERROR_CODE) + ":"
	// + responseMap.get(ERROR_MESSAGE);
	// else if (responseMap.containsKey(MESSAGE_CODE)) {
	// if (responseMap.get(MESSAGE_CODE).equals(Constants.AUTHN_OK)) {
	// return Constants.AUTHN_OK + ":" + responseMap.get(MESSAGE);
	// }
	// } else {
	// return Constants.AUTHN_FAILED + ":"
	// + "Malformed message from the server.";
	// }
	// } catch (Exception e) {
	// Log.e(TAG, "registerUser " + e.getMessage() + " ");
	// return Constants.AUTHN_FAILED + ":" + e.getMessage();
	// }
	// return null;
	// }

	/**
	 * Returns a list of guIds for server finds that need syncing.
	 * 
	 * @return
	 */
	public static String getServerFindsNeedingSync(Context context, String authKey) {
		String response = "";
		String url = "";

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String server = prefs.getString(SERVER_PREF, "");
		int projectId = prefs.getInt(PROJECT_PREF, 0);

		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String imei = telephonyManager.getDeviceId();

		url = server + "/api/getDeltaFindsIds?authKey=" + authKey + "&imei=" + imei + "&projectId=" + projectId;
		Log.i(TAG, "getDeltaFindsIds URL=" + url);

		try {
			response = doHTTPGET(url);
		} catch (Exception e) {
			Log.i(TAG, e.getMessage());
		}
		Log.i(TAG, "serverFindsNeedingSync = " + response);

		return response;
	}
	
	

	/**
	 * 
	 * Retrieve finds from the server using a Communicator.
	 * 
	 * @param serverGuids
	 * @return
	 */
	public static boolean getFindsFromServer(Context context, String authKey, String serverGuids) {
		String guid;
		int rows = 0;
		StringTokenizer st = new StringTokenizer(serverGuids, ",");
		
		while (st.hasMoreElements()) {
			guid = st.nextElement().toString();
			ContentValues cv = getRemoteFindById(context, authKey, guid);

			if (cv == null) {
				return false; // Shouldn't be null--we know its ID
			} else {
				Log.i(TAG, cv.toString());
				try {
					
					// Find out what Find class POSIT is configured for
					Class<? extends Find> findClass = FindPluginManager.mFindPlugin.getmFindClass();
					
					Log.i(TAG, "Find class = " + findClass.getSimpleName());
					
					// Update the DB
					Find find = DbHelper.getDbManager(context).getFindByGuid(guid);
					if (find != null) {
						Log.i(TAG, "Updating existing find: " + find.getId());
						Find updatedFind = findClass.newInstance();
						updatedFind.updateObject(cv);
						
//						Find updatedFind = (OutsideInFind)find;
//						((OutsideInFind) updatedFind).updateObject(cv);
						updatedFind.setId(find.getId());
						rows = DbHelper.getDbManager(context).updateWithoutHistory(updatedFind);				
					} else {
					//	find = new OutsideInFind();
						find = findClass.newInstance();
						Log.i(TAG, "Inserting new find: " + find.getId());
						find.updateObject(cv);
//						((OutsideInFind) find).updateObject(cv);
						Log.i(TAG, "Adding a new find " + find);
						rows = DbHelper.getDbManager(context).insertWithoutHistory(find);
					}
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		DbHelper.releaseDbManager();
		return rows > 0;
	}

	public static boolean recordSync(Context context, String authKey) {
		// Record the synchronization in the server's sync_history table

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String server = prefs.getString(SERVER_PREF, "");
		int projectId = prefs.getInt(context.getString(R.string.projectPref), 0);

		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String imei = telephonyManager.getDeviceId();

		String url = server + "/api/recordSync?authKey=" + authKey + "&imei=" + imei + "&projectId=" + projectId;
		Log.i(TAG, "recordSync URL=" + url);
		String responseString = "";

		try {
			responseString = doHTTPGET(url);
		} catch (Exception e) {
			Log.i(TAG, e.getMessage());
			e.printStackTrace();
			return false;
		}
		Log.i(TAG, "HTTPGet recordSync response = " + responseString);
		return true;
	}

	/*
	 * TODO: This method is a little long and could be split up. Send one find
	 * to the server, including its images.
	 * 
	 * @param find a reference to the Find object
	 * 
	 * @param action -- either 'create' or 'update'
	 */
	public static boolean sendFind(Find find, Context context, String authToken) {
		String url = "";
		boolean success = false;

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String server = prefs.getString(SERVER_PREF, "");

		if (find.getAction().equals(FindHistory.ACTION_CREATE))
			url = server + "/api/createFind?authKey=" + authToken;
		else if (find.getAction().equals(FindHistory.ACTION_UPDATE))
			url = server + "/api/updateFind?authKey=" + authToken;
		else {
			Log.e(TAG, "Find object does not contain an appropriate action: " + find);
			return false;
		}

		Log.i(TAG, "SendFind=" + find);

		List<NameValuePair> pairs = getNameValuePairs(find);

		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String imei = telephonyManager.getDeviceId();

		BasicNameValuePair pair = new BasicNameValuePair("imei", imei);
		pairs.add(pair);
		Log.i(TAG, "pairs: " + pairs);
		String responseString = null;

		// Send the find
		try {
			responseString = doHTTPPost(url, pairs);
			DbHelper.getDbManager(context).updateStatus(find, Constants.TRANSACTING);
			DbHelper.getDbManager(context).updateSyncOperation(find, Constants.POSTING);
		} catch (Exception e) {
			Log.i(TAG, e.getMessage());
			Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
			DbHelper.getDbManager(context).updateStatus(find, Constants.FAILED);
			success = false;
		}

		Log.i(TAG, "sendFind.ResponseString: " + responseString);

		// If the update failed return false
		if (responseString.indexOf("True") == -1) {
			Log.i(TAG, "sendFind result doesn't contain 'True'");
			DbHelper.getDbManager(context).updateStatus(find, Constants.FAILED);
			success = false;
		} else {
			DbHelper.getDbManager(context).updateStatus(find, Constants.SUCCEEDED);
			Log.i(TAG, "sendFind() synced find id: " + find.getId());
			success = true;
		}

		if (!success){
			//don't bother sending the images if we can't save the find
			return false;
		}

		//Check if the image is out of sync and needs to be sent
		if(Camera.isPhotoSynced(find, context) == false){
			//We have an image to send!
			//Get the image string
			String fullPicStr = Camera.getPhotoAsString(find.getGuid(), context);
			//Get the thumbnail version of it too
			String thumbPicStr = Camera.getPhotoThumbAsString(find.getGuid(), context);
			//fill in the data needed to send to the photo table
			HashMap<String, String> sendMap = new HashMap<String, String>();
			sendMap.put(COLUMN_IMEI, imei);
			sendMap.put(COLUMN_GUID, find.getGuid());
			sendMap.put(COLUMN_IDENTIFIER,Integer.toString(find.getId()));
			sendMap.put(COLUMN_PROJECT_ID,Integer.toString(find.getProject_id()));
//			 sendMap.put("COLUMN_TIMESTAMP",find.getTime()); //uses current timestamp if not set
//			 sendMap.put("mine_type",imageData.getAsString(PositDbHelper.PHOTOS_MIME_TYPE));		
			sendMap.put(COLUMN_MIME_TYPE, "image/jpeg");
			sendMap.put(COLUMN_DATA_FULL, fullPicStr);
			sendMap.put(COLUMN_DATA_THUMBNAIL, thumbPicStr);

			//ready to send the image to the server
			sendMedia(sendMap, context);
	 	}

		DbHelper.releaseDbManager();
		return success;
	}

	/**
	 * Sends finds to the server. Uses a Communicator.
	 * 
	 * @param phoneGuids
	 * @return
	 */
	public static boolean sendFindsToServer(List<Find> finds, Context context, String authToken) {
		boolean success = false;
		// StringTokenizer st = new StringTokenizer(phoneGuids, ",");
		// String str, guid, action;
		// while (st.hasMoreElements()) {
		// str = st.nextElement().toString();
		// int indx = str.indexOf(':');
		// guid = str.substring(0, indx);
		// action = str.substring(indx + 1);

		// Find find = new Find(mContext, guid); // Create a Find object
		for (Find find : finds) {
			try {
				if (find.getAction().equals("delete"))
					Log.i(TAG, "Ignoring deletions");
				else {
					Log.i(TAG, "sending Find=" + find);
					success = sendFind(find, context, authToken);
				}
			} catch (Exception e) {
				Log.i(TAG, e.toString());
				e.printStackTrace();
				success = false;
				// mHandler.sendEmptyMessage(NETWORKERROR);
			}
			if (!success) {
				// mHandler.sendEmptyMessage(SYNCERROR);
			}
		}

		return success;
	}

	// /**
	// * Sends an image (or sound file or video) to the server.
	// *
	// * @param identifier
	// * @param findId
	// * the guid of the associated find
	// * @param data
	// * @param mimeType
	// */
	 public static void sendMedia(HashMap<String, String> sendMap, Context context) {
		Log.i(TAG, "sendMedia, sendMap= " + sendMap);
		
		SharedPreferences applicationPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		String server = applicationPreferences.getString(SERVER_PREF, "");
			 
		String url = server + "/api/attachPicture?authKey=" + getAuthKey(context);
		
		responseString = doHTTPPost(url, sendMap);
//		if (Utils.debug)
//			Log.i(TAG, "sendImage.ResponseString: " + responseString);
	 }
	//
	// /**
	// * Converts a uri to a base64 encoded String for transmission to server.
	// *
	// * @param uri
	// * @return
	// */
	// private String convertUriToBase64(Uri uri) {
	// ByteArrayOutputStream imageByteStream = new ByteArrayOutputStream();
	// byte[] imageByteArray = null;
	// Bitmap bitmap = null;
	//
	// try {
	// bitmap = android.provider.MediaStore.Images.Media.getBitmap(
	// mContext.getContentResolver(), uri);
	// } catch (FileNotFoundException e) {
	// e.printStackTrace();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	//
	// if (bitmap == null) {
	// Log.d(TAG, "No bitmap");
	// }
	// // Compress bmp to jpg, write to the byte output stream
	// bitmap.compress(Bitmap.CompressFormat.JPEG, 80, imageByteStream);
	// // Turn the byte stream into a byte array
	// imageByteArray = imageByteStream.toByteArray();
	// char[] base64 = Base64Coder.encode(imageByteArray);
	// String base64String = new String(base64);
	// return base64String;
	// }
	//
	// /**
	// * cleanup the item key,value pairs so that we can send the data.
	// *
	// * @param sendMap
	// */
	// private void cleanupOnSend(HashMap<String, String> sendMap) {
	// addRemoteIdentificationInfo(sendMap);
	// }
	//
	// /**
	// * Add the standard values to our request. We might as well use this as
	// * initializer for our requests.
	// *
	// * @param sendMap
	// */
	// private void addRemoteIdentificationInfo(HashMap<String, String> sendMap)
	// {
	// // sendMap.put(COLUMN_APP_KEY, appKey);
	// sendMap.put(COLUMN_IMEI, Utils.getIMEI(mContext));
	// }
	//
	// /**
	// * cleanup the item key,value pairs so that we can receive and save to the
	// * internal database
	// *
	// * @param rMap
	// */
	// public static void cleanupOnReceive(HashMap<String, Object> rMap) {
	// rMap.put(PositDbHelper.FINDS_SYNCED, PositDbHelper.FIND_IS_SYNCED);
	// rMap.put(PositDbHelper.FINDS_GUID, rMap.get("guid"));
	// // rMap.put(PositDbHelper.FINDS_GUID, rMap.get("guid"));
	//
	// rMap.put(PositDbHelper.FINDS_PROJECT_ID, projectId);
	// if (rMap.containsKey("add_time")) {
	// rMap.put(PositDbHelper.FINDS_TIME, rMap.get("add_time"));
	// rMap.remove("add_time");
	// }
	// if (rMap.containsKey("images")) {
	// if (Utils.debug)
	// Log.d(TAG, "contains image key");
	// rMap.put(PositDbHelper.PHOTOS_IMAGE_URI, rMap.get("images"));
	// rMap.remove("images");
	// }
	// }

	/**
	 * Sends a HttpPost request to the given URL. Any JSON
	 * 
	 * @param Uri, the URL to send to/receive from
	 * @param pairs, a list of attribute/value pairs
	 * @return the response from the URL
	 */
	public static String doHTTPPost(String Uri, List<NameValuePair> pairs) {
		BasicHttpParams mHttpParams = new BasicHttpParams();

		// Set the timeout in milliseconds until a connection is established.
		HttpConnectionParams.setConnectionTimeout(mHttpParams, CONNECTION_TIMEOUT);

		// Set the default socket timeout (SO_TIMEOUT)
		// in milliseconds which is the timeout for waiting for data.
		HttpConnectionParams.setSoTimeout(mHttpParams, SOCKET_TIMEOUT);

		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", new PlainSocketFactory(), 80));
		ThreadSafeClientConnManager mConnectionManager = new ThreadSafeClientConnManager(mHttpParams, registry);
		DefaultHttpClient mHttpClient = new DefaultHttpClient(mConnectionManager, mHttpParams);

		if (Uri == null)
			throw new NullPointerException("The URL has to be passed");
		String responseString = null;
		HttpPost post = new HttpPost();

		Log.i(TAG, "doHTTPPost() URI = " + Uri);
		try {
			post.setURI(new URI(Uri));
		} catch (URISyntaxException e) {
			Log.e(TAG, "URISyntaxException " + e.getMessage());
			e.printStackTrace();
			return "[Error] " + e.getMessage();
		}

		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		try {
			post.setEntity(new UrlEncodedFormEntity(pairs, HTTP.UTF_8));
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "UnsupportedEncodingException " + e.getMessage());
			return "[Error] " + e.getMessage();
		}

		try {
			responseString = mHttpClient.execute(post, responseHandler);
			Log.d(TAG, "doHTTPpost responseString = " + responseString);
		} catch (ClientProtocolException e) {
			Log.e(TAG, "ClientProtocolException" + e.getMessage());
			e.printStackTrace();
			return "[Error] " + e.getMessage();
		} catch (IOException e) {
			Log.e(TAG, "IOException " + e.getMessage());
			e.printStackTrace();
			return "[Error] " + e.getMessage();
		} catch (IllegalStateException e) {
			Log.e(TAG, "IllegalStateException: " + e.getMessage());
			e.printStackTrace();
			return "[Error] " + e.getMessage();
		} catch (Exception e) {
			Log.e(TAG, "Exception on HttpPost " + e.getMessage());
			e.printStackTrace();
			return "[Error] " + e.getMessage();
		}

		Log.i(TAG, "doHTTPpost response = " + responseString);

		return responseString;
	}
	
	
	/**
	 * Sends a HttpPost request to the given URL. Any JSON
	 * 
	 * @param Uri, the URL to send to/receive from
	 * @param sendMap, the hashMap of data to send to the server as POST data
	 * @return the response from the URL
	 */	
	public static String doHTTPPost(String Uri, HashMap<String, String> sendMap) {
		return doHTTPPost(Uri, getNameValuePairs(sendMap));
	}

	// public boolean projectExists(String projectId, String server){
	// String url =
	// server+"/api/projectExists?authKey="+authKey+"&projectId="+projectId;
	// Log.i(TAG, url);
	// String response = doHTTPGET(url);
	// Log.i(TAG, "projectExists response = " + response);
	//
	// if(response.equals("true"))
	// return true;
	// if(response.equals("false"))
	// return false;
	// return false;
	// }
	/**
	 * A wrapper(does some cleanup too) for sending HTTP GET requests to the URI
	 * 
	 * @param Uri
	 * @return the request from the remote server
	 */
	public static String doHTTPGET(String Uri) {
		BasicHttpParams mHttpParams = new BasicHttpParams();

		// Set the timeout in milliseconds until a connection is established.
		HttpConnectionParams.setConnectionTimeout(mHttpParams, CONNECTION_TIMEOUT);

		// Set the default socket timeout (SO_TIMEOUT)
		// in milliseconds which is the timeout for waiting for data.
		HttpConnectionParams.setSoTimeout(mHttpParams, SOCKET_TIMEOUT);

		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", new PlainSocketFactory(), 80));
		ThreadSafeClientConnManager mConnectionManager = new ThreadSafeClientConnManager(mHttpParams, registry);
		DefaultHttpClient mHttpClient = new DefaultHttpClient(mConnectionManager, mHttpParams);

		if (Uri == null)
			throw new NullPointerException("The URL has to be passed");
		String responseString = null;
		HttpGet httpGet = new HttpGet();

		try {
			httpGet.setURI(new URI(Uri));
		} catch (URISyntaxException e) {
			Log.e(TAG, "doHTTPGet " + e.getMessage());
			e.printStackTrace();
			return "[Error] " + e.getMessage();
		}

		Log.i(TAG, "doHTTPGet Uri = " + Uri);

		ResponseHandler<String> responseHandler = new BasicResponseHandler();

		try {
			responseString = mHttpClient.execute(httpGet, responseHandler);
		} catch (ClientProtocolException e) {
			Log.e(TAG, "ClientProtocolException" + e.getMessage());
			return "[Error] " + e.getMessage();
		} catch (SocketTimeoutException e) {
			Log.e(TAG, "[Error: SocketTimeoutException]" + e.getMessage());
			return "[Error] " + e.getMessage();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
			return "[Error] " + e.getMessage();
		} catch (Exception e) {
			Log.e(TAG, e.getMessage() + "what");
			return "[Error] " + e.getMessage();
		}

		Log.i(TAG, "doHTTPGet Response: " + responseString);
		return responseString;
	}

	public static List<NameValuePair> getNameValuePairs(Find find) {
		// Get fields from both class and superclass
		List<NameValuePair> pairs = null;
		if (find.getClass().getName().equals(Find.class.getName())) { // For basic POSIT
			pairs = getNameValuePairs(find, find.getClass());
		} else { // For find extensions
			String extendedDataPairs = getNameValuePairs(find, find.getClass()).toString();
			pairs = getNameValuePairs(find, find.getClass().getSuperclass());
			pairs.add(new BasicNameValuePair("data", extendedDataPairs));
		}
		return pairs;
	}

	/**
	 * Returns a list on name/value pairs for the Find.  Should work for Plugin Finds as
	 * well as Basic Finds.  
	 * @param find
	 * @param clazz
	 * @return
	 */
	private static List<NameValuePair> getNameValuePairs(Find find, Class clazz) {
		Field[] fields = clazz.getDeclaredFields();

		List<NameValuePair> nvp = new ArrayList<NameValuePair>();
		String methodName = "";
		String value = "";

		for (Field field : fields) {
//			Log.i(TAG, "class= " + clazz + " field = " + field);
			if (!Modifier.isFinal(field.getModifiers())) {
				String key = field.getName();
				methodName = "get" + key.substring(0, 1).toUpperCase() + key.substring(1);
				value = "";

				try {
					Class returnType = clazz.getDeclaredMethod(methodName, null).getReturnType();
					if (returnType.equals(String.class))
						value = (String) clazz.getDeclaredMethod(methodName, null).invoke(find, (Object[]) null);
					else if (returnType.equals(int.class))
						value = String.valueOf((Integer) clazz.getDeclaredMethod(methodName, null).invoke(find,
								(Object[]) null));
					else if (returnType.equals(double.class))
						value = String.valueOf((Double) clazz.getDeclaredMethod(methodName, null).invoke(find,
								(Object[]) null));
					else if (returnType.equals(boolean.class))
						value = String.valueOf((Boolean) clazz.getDeclaredMethod(methodName, null).invoke(find,
								(Object[]) null));

				} catch (IllegalArgumentException e) {
					Log.e(TAG, e + ": " + e.getMessage());
				} catch (SecurityException e) {
					Log.e(TAG, e + ": " + e.getMessage());
				} catch (IllegalAccessException e) {
					Log.e(TAG, e + ": " + e.getMessage());
				} catch (InvocationTargetException e) {
					Log.e(TAG, e + ": " + e.getMessage());
				} catch (NoSuchMethodException e) {
					Log.e(TAG, e + ": " + e.getMessage());
				}
				nvp.add(new BasicNameValuePair(key, value));
			}
		}
		return nvp;

	}

	/**
	 * Pull the remote find from the server using the guid provided.
	 * 
	 * @param guid
	 *            , a globally unique identifier
	 * @return an associative list of attribute/value pairs
	 */
	public static ContentValues getRemoteFindById(Context context, String authKey, String guid) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String server = prefs.getString(SERVER_PREF, "");

		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String imei = telephonyManager.getDeviceId();

		String url = server + "/api/getFind?guid=" + guid + "&authKey=" + authKey;
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		pairs.add(new BasicNameValuePair("guid", guid));
		pairs.add(new BasicNameValuePair("imei", imei));

		String responseString = doHTTPPost(url, pairs);
		ContentValues cv = new ContentValues();

		Log.i(TAG, "getRemoteFindById = " + responseString);
		try {
			JSONObject jobj = new JSONObject(responseString);
			String findJson = jobj.getString("find");
			JSONObject find = new JSONObject(findJson);			
			cv.put(Find.GUID, find.getString(Find.GUID));
			cv.put(Find.PROJECT_ID, find.getInt(Find.PROJECT_ID));
			cv.put(Find.NAME, find.getString(Find.NAME));			
			cv.put(Find.DESCRIPTION, find.getString(Find.DESCRIPTION));
			// FIXME add add_time and modify_time for this
			cv.put(Find.TIME, find.getString("add_time"));
			cv.put(Find.TIME, find.getString("modify_time"));
			cv.put(Find.LATITUDE, find.getDouble(Find.LATITUDE));
			cv.put(Find.LONGITUDE, find.getDouble(Find.LONGITUDE));
			cv.put(Find.REVISION, find.getInt(Find.REVISION));
			
			// Does this find have an image?
			if (jobj.has("images")) {
				String imageIds = jobj.getString("images");		
				Log.i(TAG, "imageIds = " + imageIds);
				
				//check to see if we actually have an image to fetch
				String imageId = parseImageIds(imageIds); //this returns at most one image id
				
				//we have an image to fetch!
				if(imageId != null){
					if(getImageOnServer(imageId, context)){
						//success
						Log.i(TAG, "Successfully retrieve image for " + find.getString(Find.GUID));
					}
					else{
						//failed
						Log.i(TAG, "Failed to retrieve image for " + find.getString(Find.GUID));
					}
				}
			}
			
			// Is this a extended find?
			if (jobj.has(Find.EXTENSION)) {
				String extradata = jobj.getString(Find.EXTENSION);		
				Log.i(TAG, "extradata = " + extradata);
				if ( !extradata.equals("null") )
					addExtraDataToContentValues(cv, extradata);
			}
			
			return cv;
		} catch (JSONException e) {
			Log.i(TAG, "JSONException " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			Log.i(TAG, "Exception " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * The data has the form: ["1","2", ...] or '[]'
	 * @param data
	 * the list of image ids
	 * @return the last image id in the list or null
	 */
	private static String parseImageIds(String data) {
		Log.i(TAG, "imageIdData = " + data  + " " + data.length());
		if (data.equals("[]")){ 
			return null;
		}
		data = data.trim();
		data = data.substring(1,data.length()-1); //removes brackets
		StringTokenizer st = new StringTokenizer(data,","); //in the form "123"
		String imgId = null; //only care about one image for this version of posit
		while (st.hasMoreElements()) {
			imgId = (String) st.nextElement();
			Log.i(TAG, "Is this with quotes: " + imgId);
			imgId = imgId.substring(1, imgId.indexOf('"',1)); // removes quotes. find the second quote in the string
			Log.i(TAG, "Is this without quotes: " + imgId);
		}
		Log.i(TAG, "Planning to fetch imageId " + imgId + " for a find");
		return imgId;
	}
	
	 /**
	 * Retrieve the specified image id from the server and save it to the phone
	 * @param imageId
	 * the id of the image to query
	 * @param context
	 * the application context
	 * @return true if successful, false otherwise
	 */
	 static boolean getImageOnServer(String imageId, Context context) throws FileNotFoundException, IOException {
		SharedPreferences applicationPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		String server = applicationPreferences.getString(SERVER_PREF, "");
		 
		String imageUrl = server + "/api/getPicture?id=" + imageId + "&authKey=" + getAuthKey(context);
		
		HashMap<String, String> sendMap = new HashMap<String, String>();
		sendMap.put(COLUMN_IMEI, getIMEI(context));
		
		String imageResponseString = doHTTPPost(imageUrl, sendMap);
		if (imageResponseString.equals(RESULT_FAIL)){
			return false;
		}
		else{
			//we got the image data!
			Log.i(TAG, "imageResponseString = " + imageResponseString);
			//parse to get the guid and base64 string of the image
			//then just save it to the phone's internal memory
			try {
				JSONObject jobj = new JSONObject(imageResponseString);
				String guid = jobj.getString(Find.GUID);
				String imgData = jobj.getString("data_full");
				
				//save the Base64 string to internal memory
				Camera.savePhoto(guid, imgData, context);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true; //success!
		}
	 }
	
	/**
	 * The data has the form: [attr=value, ...] or 'null'
	 * @param cv
	 * @param data
	 */
	static private void addExtraDataToContentValues(ContentValues cv, String data) {
		Log.i(TAG, "data = " + data  + " " + data.length());
		if (data.equals("null")) 
			return;
		data = data.trim();
		data = data.substring(1,data.length()-1);
		StringTokenizer st = new StringTokenizer(data,",");
		while (st.hasMoreElements()) {
			String attrvalpair = (String) st.nextElement();
			String attr = attrvalpair.substring(0,attrvalpair.indexOf("="));
			attr = attr.trim();
			String val = attrvalpair.substring(attrvalpair.indexOf("=")+1);
			val = val.trim();
			Log.i(TAG, "Putting " + attr + "=" + val + " into CV");
			if (Integer.getInteger(val) != null)
				cv.put(attr, Integer.parseInt(val));
//			else if (Boolean.getBoolean(val) != null)
//				cv.put(attr, Boolean.parseBoolean(val))
//			else if (Double.getDouble(val) != null))
//				cv.put(attr, Double.parseDouble(val))
			else
				cv.put(attr, val);
		}
		
	}
	
	/**
	 * Registers a new expedition with the server.
	 * @param projectId  Posit's current project id.
	 * @return Returns the expedition number received from the server or -1 if something
	 * goes wrong.
	 */
	public int registerExpeditionId(Context context, int projectId) {
		mContext = context;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String server = prefs.getString(SERVER_PREF, "");

		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String imei = telephonyManager.getDeviceId();
		
		HashMap<String, String> sendMap = new HashMap<String, String>();
		addRemoteIdentificationInfo(sendMap);
		String addExpeditionUrl = server + "/api/addExpedition?authKey="   + getAuthKey(context);
		sendMap.put("projectId", "" + projectId);
		Log.i(TAG, "URL=" + addExpeditionUrl + " projectId = " + projectId);
		String response = doHTTPPost(addExpeditionUrl, sendMap);
		Log.d(TAG,"registerExpeditionId response = " + response);

		// The server should return an expedition number if everything goes ok.  If 
		//  an error occurs, it will return an error message that cannot parse to an int
		//  which will cause an exception here.
		try {
			Integer i = Integer.parseInt(response);
			return i;
		} catch (NumberFormatException e) {
			Log.e(TAG, "Communicator, registerExpeditionId, Invalid response received");
			return -1;
		}
	}
	
	private void addRemoteIdentificationInfo(HashMap<String, String> sendMap) {
		// sendMap.put(COLUMN_APP_KEY, appKey);
		sendMap.put(COLUMN_IMEI, getIMEI(mContext));
	}
	
	/**
	 * Gets the unique IMEI code for the phone used for identification
	 * The phone should have proper permissions (READ_PHONE_STATE) to be able to get this data.
	 */
	public static String getIMEI(Context mContext) {
		TelephonyManager tm = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
		return tm.getDeviceId();
	}
	
	public static List<NameValuePair> getNameValuePairs (HashMap<String,String> nameValuesMap) {
		Iterator<String> iter = nameValuesMap.keySet().iterator();
		List<NameValuePair> nvp = new ArrayList<NameValuePair>();
		while (iter.hasNext()) {
			String key = iter.next();
			String value = nameValuesMap.get(key);
			nvp.add(new BasicNameValuePair(key,value));
		}
		return nvp;
	}

	/**
	 * Sends a GPS point and associated data to the Posit server. Called from 
	 *  Tracker Activity or TrackerBackgroundService.  
	 */
	public String registerExpeditionPoint(Context context, double lat, double lng, double alt,
			int swath, int expedition, long time) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String server = prefs.getString(SERVER_PREF, "");

			//long swath, int expedition) {
		Log.i(TAG, "Communicator, registerExpeditionPoint " + lat + " " + lng + " " + time);
		HashMap<String, String> sendMap = new HashMap<String, String>();
		addRemoteIdentificationInfo(sendMap);
		Log.i(TAG, "Sendmap= " + sendMap.toString());
		String addExpeditionUrl = server + "/api/addExpeditionPoint?authKey="  + this.getAuthKey(context);
		sendMap.put(DbManager.GPS_POINT_LATITUDE, "" + lat);
		sendMap.put(DbManager.GPS_POINT_LONGITUDE, lng + "");
		sendMap.put(DbManager.GPS_POINT_ALTITUDE, "" + alt);
		sendMap.put(DbManager.GPS_POINT_SWATH, "" + swath);
		sendMap.put(DbManager.EXPEDITION, expedition + "");
		sendMap.put(DbManager.GPS_TIME, time + "");
		Log.i(TAG, "Sendmap= " + sendMap.toString());
		
		String response = doHTTPPost(addExpeditionUrl, sendMap);
		Log.i(TAG, "Communicator, registerExpeditionPoint, response: " + response);
		return response;
	}
	
	// /**
	// * Get an image from the server using the guid as Key.
	// *
	// * @param guid
	// * the Find's globally unique Id
	// */
	// public ArrayList<HashMap<String, String>> getRemoteFindImages(String
	// guid) {
	// ArrayList<HashMap<String, String>> imagesMap = null;
	// // ArrayList<HashMap<String, String>> imagesMap = null;
	// String imageUrl = server + "/api/getPicturesByFind?findId=" + guid
	// + "&authKey=" + authKey;
	// HashMap<String, String> sendMap = new HashMap<String, String>();
	// Log.i(TAG, "getRemoteFindImages, sendMap=" + sendMap.toString());
	// sendMap.put(PositDbHelper.FINDS_GUID, guid);
	// addRemoteIdentificationInfo(sendMap);
	// try {
	// String imageResponseString = doHTTPPost(imageUrl, sendMap);
	// Log.i(TAG, "getRemoteFindImages, response=" + imageResponseString);
	//
	// if (!imageResponseString.equals(RESULT_FAIL)) {
	// JSONArray jsonArr = new JSONArray(imageResponseString);
	// imagesMap = new ArrayList<HashMap<String, String>>();
	// // imagesMap = new ArrayList<HashMap<String, String>>();
	//
	// for (int i = 0; i < jsonArr.length(); i++) {
	// JSONObject jsonObj = jsonArr.getJSONObject(i);
	// if (Utils.debug)
	// Log.i(TAG, "JSON Image Response String: "
	// + jsonObj.toString());
	// // imagesMap.add((HashMap<String, String>) jsonArr.get(i));
	// Iterator<String> iterKeys = jsonObj.keys();
	// HashMap<String, String> map = new HashMap<String, String>();
	// while (iterKeys.hasNext()) {
	// String key = iterKeys.next();
	// map.put(key, jsonObj.getString(key));
	// }
	// imagesMap.add(map);
	// }
	// }
	// } catch (Exception e) {
	// Log.i(TAG, e.getMessage());
	// e.printStackTrace();
	// }
	// if (imagesMap != null && Utils.debug)
	// Log
	// .i(TAG, "getRemoteFindImages, imagesMap="
	// + imagesMap.toString());
	// else
	// Log.i(TAG, "getRemoteFindImages, imagesMap= null");
	// return imagesMap;
	// }
	//
	// /**
	// * Checks if a given image already exists on the server. Allows for
	// quicker
	// * syncing to the server, as this allows the application to bypass
	// * converting from a bitmap to base64 to send to the server
	// *
	// * @param imageId
	// * the id of the image to query
	// * @return whether the image already exists on the server
	// */
	// public boolean imageExistsOnServer(int imageId) {
	// HashMap<String, String> sendMap = new HashMap<String, String>();
	// addRemoteIdentificationInfo(sendMap);
	// String imageUrl = server + "/api/getPicture?id=" + imageId
	// + "&authKey=" + authKey;
	// String imageResponseString = doHTTPPost(imageUrl, sendMap);
	// if (imageResponseString.equals(RESULT_FAIL))
	// return false;
	// else
	// return true;
	// }
	//
	// // public String registerExpeditionPoint(double lat, double lng, int
	// expedition) {
	// // String result = doHTTPGET(server + "/api/addExpeditionPoint?authKey="
	// // + authKey + "&lat=" + lat + "&lng=" + lng + "&expedition="
	// // + expedition);
	// // return result;
	// // }
	//
	//
	// /**
	// * Sends a GPS point and associated data to the Posit server. Called from
	// * Tracker Activity or TrackerBackgroundService.
	// */
	// public String registerExpeditionPoint(double lat, double lng, double alt,
	// int swath, int expedition, long time) {
	// //long swath, int expedition) {
	// // if (Utils.debug)
	// // Log.i(TrackerActivity.TAG, "Communicator, registerExpeditionPoint " +
	// lat + " " + lng + " " + time);
	// HashMap<String, String> sendMap = new HashMap<String, String>();
	// addRemoteIdentificationInfo(sendMap);
	// String addExpeditionUrl = server + "/api/addExpeditionPoint?authKey="
	// + authKey;
	// sendMap.put(PositDbHelper.GPS_POINT_LATITUDE, "" + lat);
	// sendMap.put(PositDbHelper.GPS_POINT_LONGITUDE, lng + "");
	// sendMap.put(PositDbHelper.GPS_POINT_ALTITUDE, "" + alt);
	// sendMap.put(PositDbHelper.GPS_POINT_SWATH, "" + swath);
	// sendMap.put(PositDbHelper.EXPEDITION, expedition + "");
	// sendMap.put(PositDbHelper.GPS_TIME, time + "");
	// String response = doHTTPPost(addExpeditionUrl, sendMap);
	// // if (Utils.debug) {
	// // Log.i(TrackerActivity.TAG,
	// "Communicator, registerExpeditionPoint, response: " +
	// addExpeditionResponseString);
	// // }
	// return response;
	// }
	//
	// /**
	// * Registers a new expedition with the server.
	// * @param projectId Posit's current project id.
	// * @return Returns the expedition number received from the server or -1 if
	// something
	// * goes wrong.
	// */
	// public int registerExpeditionId(int projectId) {
	// HashMap<String, String> sendMap = new HashMap<String, String>();
	// addRemoteIdentificationInfo(sendMap);
	// String addExpeditionUrl = server + "/api/addExpedition?authKey="
	// + authKey;
	// sendMap.put("projectId", "" + projectId);
	// String response = doHTTPPost(addExpeditionUrl, sendMap);
	// Log.d(TAG,"registerExpeditionId response = " + response);
	// // if (Utils.debug) {
	// // Log.i(TrackerActivity.TAG,
	// "Communicator, registerExpeditionId, response: "
	// // + addExpeditionResponseString);
	// // }
	// // The server should return an expedition number if everything goes ok.
	// If
	// // an error occurs, it will return an error message that cannot parse to
	// an int
	// // which will cause an exception here.
	// try {
	// Integer i = Integer.parseInt(response);
	// return i;
	// } catch (NumberFormatException e) {
	// Log.e(TrackerActivity.TAG,
	// "Communicator, registerExpeditionId, Invalid response received");
	// return -1;
	// }
	// }
}