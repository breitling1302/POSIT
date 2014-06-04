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
package org.hfoss.posit.android.web;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.hfoss.posit.android.Constants;
import org.hfoss.posit.android.Find;
import org.hfoss.posit.android.Log;
import org.hfoss.posit.android.R;
import org.hfoss.posit.android.TrackerActivity;
import org.hfoss.posit.android.provider.PositDbHelper;
import org.hfoss.posit.android.utilities.Utils;
import org.hfoss.third.Base64Coder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

/**
 * The communication module for POSIT. Handles most calls to the server to get
 * information regarding projects and finds.
 * 
 *  
 */
public class Communicator {
	private static final String MESSAGE = "message";
	private static final String MESSAGE_CODE = "messageCode";
	private static final String ERROR_MESSAGE = "errorMessage";
	private static final String ERROR_CODE = "errorCode";
	private static final String COLUMN_IMEI = "imei";
	public static final int CONNECTION_TIMEOUT = 6000; // millisecs
	public static final int SOCKET_TIMEOUT = 10000;

	/*
	 * You should be careful with putting names for server. DO NOT always trust
	 * DNS.
	 */

	public static final String RESULT_FAIL = "false";
	private static String server;
	private static String authKey;
	private static String imei;

	private static int projectId;

	private static String TAG = "Communicator";
	private String responseString;
	private Context mContext;
	private SharedPreferences applicationPreferences;
	private HttpParams mHttpParams;
	private HttpClient mHttpClient;
	private ThreadSafeClientConnManager mConnectionManager;
	public static long mTotalTime = 0;
	private long mStart = 0;

	public Communicator(Context _context) {
		mContext = _context;
		mTotalTime = 0;
		mStart = 0;

		mHttpParams = new BasicHttpParams();

		// Set the timeout in milliseconds until a connection is established.
		HttpConnectionParams.setConnectionTimeout(mHttpParams, CONNECTION_TIMEOUT);
		
		// Set the default socket timeout (SO_TIMEOUT) 
		// in milliseconds which is the timeout for waiting for data.
		HttpConnectionParams.setSoTimeout(mHttpParams, SOCKET_TIMEOUT);

		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", new PlainSocketFactory(), 80));
		mConnectionManager = new ThreadSafeClientConnManager(mHttpParams,
				registry);
		mHttpClient = new DefaultHttpClient(mConnectionManager, mHttpParams);

		PreferenceManager.setDefaultValues(mContext, R.xml.posit_preferences,
				false);
		applicationPreferences = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		setApplicationAttributes(applicationPreferences
				.getString("AUTHKEY", ""), applicationPreferences.getString(
				"SERVER_ADDRESS", server), applicationPreferences.getInt(
				"PROJECT_ID", projectId));
		TelephonyManager manager = (TelephonyManager) mContext
				.getSystemService(Context.TELEPHONY_SERVICE);
		imei = manager.getDeviceId();

	}

	private void setApplicationAttributes(String aKey, String serverAddress,
			int projId) {
		authKey = aKey;
		server = serverAddress;
		projectId = projId;
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
	public ArrayList<HashMap<String, Object>> getProjects() {
		Log.i(TAG, "authkey=" + authKey);
		if (authKey.equals("")) {
			Log.e(TAG, "getProjects() authKey == ");
			Utils.showToast(mContext,
					"Aborting Communicator:\nPhone does not have a valid authKey."
							+ "\nUse settings menu to register phone.");
			return null;
		}
		String url = server + "/api/listMyProjects?authKey=" + authKey;
		ArrayList<HashMap<String, Object>> list;
		responseString = doHTTPGET(url);
		if (Utils.debug)
			Log.i(TAG, responseString);
		if (responseString.contains("Error")) {
			return null;
		}
		list = new ArrayList<HashMap<String, Object>>();
		try {
			list = (ArrayList<HashMap<String, Object>>) (new ResponseParser(
					responseString).parseList());
		} catch (JSONException e1) {
			Log.i(TAG, "getProjects JSON exception " + e1.getMessage());
			return null;
		}
		return list;
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

		String url = server + "/api/registerDevice?authKey=" + authKey
				+ "&imei=" + imei;
		Log.i(TAG, "registerDevice URL=" + url);

		try {
			responseString = doHTTPGET(url);
		} catch (Exception e) {
			Utils.showToast(mContext, e.getMessage());
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
	 * @param server
	 * @param authKey
	 * @param imei
	 * @return authentication key if successful and null if unsuccessful
	 * @throws JSONException
	 */
	public String loginUser(String server, String email, String password,
			String imei) {
		String url = server + "/api/login";
		
		HashMap<String, Object> responseMap = null;
		Log.i(TAG, "loginUser URL=" + url);
		HashMap<String,String> sendMap = new HashMap<String, String>();
		sendMap.put("email", email);
		sendMap.put("password", password);
		sendMap.put("imei", imei);
		try {
			responseString = doHTTPPost(url,sendMap);
			Log.i(TAG, "longinUser response = " + responseString);
			if (responseString.contains("[Error] ")){
				Utils.showToast(mContext, responseString);
				return Constants.AUTHN_FAILED+":"+ responseString;
			} else {
				ResponseParser parser = new ResponseParser(responseString);
				responseMap = parser.parseObject();
			}
		} catch (Exception e) {
			Log.i(TAG, "longinUser catch clause response = " + responseString);
			Utils.showToast(mContext, e.getMessage()+"");
			return Constants.AUTHN_FAILED+":"+responseString;
		}
		try {
			if (responseMap.containsKey(ERROR_CODE))
				return responseMap.get(ERROR_CODE) + ":"
						+ responseMap.get(ERROR_MESSAGE);
			else if (responseMap.containsKey(MESSAGE_CODE)) {
				if (responseMap.get(MESSAGE_CODE).equals(Constants.AUTHN_OK)) {
					return Constants.AUTHN_OK + ":"
							+ responseMap.get(MESSAGE);

				}
			} else {
				return Constants.AUTHN_FAILED + ":"
						+ "repsonseMap = " + responseMap.toString(); //"Malformed message from server.";
			}
		} catch (Exception e) {
			Log.e(TAG, "loginUser " + e.getMessage()+" ");
			return Constants.AUTHN_FAILED + ": "
					+ e.getMessage();
		}
		return null;
	}
	

	public String createProject(String server, String projectName, String projectDescription, String authKey) {
		String url = server + "/api/newProject?authKey="+authKey;
		HashMap<String,String> sendMap = new HashMap<String,String>();
		sendMap.put("name", projectName);
		sendMap.put("description", projectDescription);
		
		HashMap<String, Object> responseMap = null;
		Log.i(TAG, "Create Project URL=" + url);

		try {
			responseString = doHTTPPost(url, sendMap);
			Log.i(TAG, responseString);
			if (responseString.contains("[ERROR]")){
				Utils.showToast(mContext, responseString);
				return Constants.AUTHN_FAILED+":"+ "Error";
			}
			ResponseParser parser = new ResponseParser(responseString);
			responseMap = parser.parseObject();
		} catch (Exception e) {
			Utils.showToast(mContext, e.getMessage()+"");
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
			Log.e(TAG,  "createProject " + e.getMessage());
			return e.getMessage();
		}
	}

	public String registerUser(String server, String firstname,
			String lastname, String email, String password, String check, String imei) {
		String url = server + "/api/registerUser";
		Log.i(TAG, "registerUser URL=" + url + "&imei=" + imei);
		HashMap<String,String>sendMap = new HashMap<String, String>();
		sendMap.put("email", email);
		sendMap.put("password1", password);
		sendMap.put("password2", check);
		sendMap.put("firstname", firstname);
		sendMap.put("lastname", lastname);
		try {
			responseString = doHTTPPost(url,sendMap);
			Log.i(TAG, "registerUser Httpost responseString = " + responseString);
			if (responseString.contains("[ERROR]")){
				Utils.showToast(mContext, Constants.AUTHN_FAILED+":"+responseString);
				return Constants.AUTHN_FAILED+":"+ responseString;
			}
			ResponseParser parser = new ResponseParser(responseString);
			HashMap<String, Object> responseMap = parser.parseObject();
			if (responseMap.containsKey(ERROR_CODE))
				return responseMap.get(ERROR_CODE)+":"+responseMap.get(ERROR_MESSAGE);
			else if (responseMap.containsKey(MESSAGE_CODE)){
				if (responseMap.get(MESSAGE_CODE).equals(Constants.AUTHN_OK)){
					return Constants.AUTHN_OK+":"+responseMap.get(MESSAGE);
				}
			}else {
				return Constants.AUTHN_FAILED+":"+"Malformed message from the server.";
			}
		}catch (Exception e){
			Log.e(TAG, "registerUser " + e.getMessage()+" ");
			return Constants.AUTHN_FAILED + ":"
			+ e.getMessage();
		}
		return null;
	}

	/*
	 * TODO: This method is a little long and could be split up. Send one find
	 * to the server, including its images.
	 * 
	 * @param find a reference to the Find object
	 * 
	 * @param action -- either 'create' or 'update'
	 */
	public boolean sendFind(Find find, String action) {
		boolean success = false;
		String url;
		HashMap<String, String> sendMap = find.getContentMapGuid();
		// Log.i(TAG, "sendFind map = " + sendMap.toString());
		cleanupOnSend(sendMap);
		sendMap.put("imei", imei);
		String guid = sendMap.get(PositDbHelper.FINDS_GUID);
		long id = find.getId();
		// Create the url

		if (action.equals("create")) {
			url = server + "/api/createFind?authKey=" + authKey;
		} else {
			url = server + "/api/updateFind?authKey=" + authKey;
		}
		if (Utils.debug) {
			Log.i(TAG, "SendFind=" + sendMap.toString());
		}

		// Send the find
		try {
			responseString = doHTTPPost(url, sendMap);
		} catch (Exception e) {
			Log.i(TAG, e.getMessage());
			Utils.showToast(mContext, e.getMessage());
			return false;
		}
		if (Utils.debug)
			Log.i(TAG, "sendFind.ResponseString: " + responseString);

		// If the update failed return false
		if (responseString.indexOf("True") == -1) {
			Log.i(TAG, "sendFind result doesn't contain 'True'");
			return false;
		} else {
			PositDbHelper dbh = new PositDbHelper(mContext);
			success = dbh.markFindSynced(id);
			if (Utils.debug)
				Log.i(TAG, "sendfind synced " + id + " " + success);
		}

		if (success) {
			// Otherwise send the Find's images
	
			//long id = Long.parseLong(sendMap.get(PositDbHelper.FINDS_ID));
			PositDbHelper dbh = new PositDbHelper(mContext);
			ArrayList<ContentValues> photosList = dbh.getImagesListSinceUpdate(id, projectId);
	
			Log.i(TAG, "sendFind, photosList=" + photosList.toString());
	
			Iterator<ContentValues> it = photosList.listIterator();
			while (it.hasNext()) {
				ContentValues imageData = it.next();
				Uri uri = Uri.parse(imageData
						.getAsString(PositDbHelper.PHOTOS_IMAGE_URI));
				String base64Data = convertUriToBase64(uri);
				uri = Uri.parse(imageData
						.getAsString(PositDbHelper.PHOTOS_THUMBNAIL_URI));
				String base64Thumbnail = convertUriToBase64(uri);
				sendMap = new HashMap<String, String>();
				sendMap.put(COLUMN_IMEI, Utils.getIMEI(mContext));
				sendMap.put(PositDbHelper.FINDS_GUID, guid);
	
				sendMap.put(PositDbHelper.PHOTOS_IDENTIFIER, imageData
						.getAsString(PositDbHelper.PHOTOS_IDENTIFIER));
				sendMap.put(PositDbHelper.FINDS_PROJECT_ID, imageData
						.getAsString(PositDbHelper.FINDS_PROJECT_ID));
				sendMap.put(PositDbHelper.FINDS_TIME, imageData
						.getAsString(PositDbHelper.FINDS_TIME));
				sendMap.put(PositDbHelper.PHOTOS_MIME_TYPE, imageData
						.getAsString(PositDbHelper.PHOTOS_MIME_TYPE));
	
				sendMap.put("mime_type", "image/jpeg");
	
				sendMap.put(PositDbHelper.PHOTOS_DATA_FULL, base64Data);
				sendMap.put(PositDbHelper.PHOTOS_DATA_THUMBNAIL, base64Thumbnail);
				sendMedia(sendMap);
			}	
		}
		// Update the Synced attribute.
		return success;
	}

	/**
	 * Sends an image (or sound file or video) to the server.
	 * 
	 * @param identifier
	 * @param findId
	 *            the guid of the associated find
	 * @param data
	 * @param mimeType
	 */
	public void sendMedia(HashMap<String, String> sendMap) {
		Log.i(TAG, "sendMedia, sendMap= " + sendMap);

		String url = server + "/api/attachPicture?authKey=" + authKey;

		responseString = doHTTPPost(url, sendMap);
		if (Utils.debug)
			Log.i(TAG, "sendImage.ResponseString: " + responseString);
	}

	/**
	 * Converts a uri to a base64 encoded String for transmission to server.
	 * 
	 * @param uri
	 * @return
	 */
	private String convertUriToBase64(Uri uri) {
		ByteArrayOutputStream imageByteStream = new ByteArrayOutputStream();
		byte[] imageByteArray = null;
		Bitmap bitmap = null;

		try {
			bitmap = android.provider.MediaStore.Images.Media.getBitmap(
					mContext.getContentResolver(), uri);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (bitmap == null) {
			Log.d(TAG, "No bitmap");
		}
		// Compress bmp to jpg, write to the byte output stream
		bitmap.compress(Bitmap.CompressFormat.JPEG, 80, imageByteStream);
		// Turn the byte stream into a byte array
		imageByteArray = imageByteStream.toByteArray();
		char[] base64 = Base64Coder.encode(imageByteArray);
		String base64String = new String(base64);
		return base64String;
	}

	/**
	 * cleanup the item key,value pairs so that we can send the data.
	 * 
	 * @param sendMap
	 */
	private void cleanupOnSend(HashMap<String, String> sendMap) {
		addRemoteIdentificationInfo(sendMap);
	}

	/**
	 * Add the standard values to our request. We might as well use this as
	 * initializer for our requests.
	 * 
	 * @param sendMap
	 */
	private void addRemoteIdentificationInfo(HashMap<String, String> sendMap) {
		sendMap.put(COLUMN_IMEI, Utils.getIMEI(mContext));
	}

	/**
	 * cleanup the item key,value pairs so that we can receive and save to the
	 * internal database
	 * 
	 * @param rMap
	 */
	public static void cleanupOnReceive(HashMap<String, Object> rMap) {
		rMap.put(PositDbHelper.FINDS_SYNCED, PositDbHelper.FIND_IS_SYNCED);
		rMap.put(PositDbHelper.FINDS_GUID, rMap.get("guid"));
		// rMap.put(PositDbHelper.FINDS_GUID, rMap.get("guid"));

		rMap.put(PositDbHelper.FINDS_PROJECT_ID, projectId);
		if (rMap.containsKey("add_time")) {
			rMap.put(PositDbHelper.FINDS_TIME, rMap.get("add_time"));
			rMap.remove("add_time");
		}
		if (rMap.containsKey("images")) {
			if (Utils.debug)
				Log.d(TAG, "contains image key");
			rMap.put(PositDbHelper.PHOTOS_IMAGE_URI, rMap.get("images"));
			rMap.remove("images");
		}
	}

	/**
	 * Sends a HttpPost request to the given URL. Any JSON
	 * 
	 * @param Uri
	 *            the URL to send to/receive from
	 * @param sendMap
	 *            the hashMap of data to send to the server as POST data
	 * @return the response from the URL
	 */
	private String doHTTPPost(String Uri, HashMap<String, String> sendMap) {
		long startTime = System.currentTimeMillis();
		if (Uri == null)
			throw new NullPointerException("The URL has to be passed");
		String responseString = null;
		HttpPost post = new HttpPost();
		if (Utils.debug)
			Log.i(TAG, "doHTTPPost() URI = " + Uri);
		try {
			post.setURI(new URI(Uri));
		} catch (URISyntaxException e) {
			Log.e(TAG, "URISyntaxException " + e.getMessage());
			e.printStackTrace();
			return "[Error] " + e.getMessage();

		}
		List<NameValuePair> nvp = PositHttpUtils.getNameValuePairs(sendMap);
		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		try {
			post.setEntity(new UrlEncodedFormEntity(nvp, HTTP.UTF_8));
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "UnsupportedEncodingException " + e.getMessage());
			return "[Error] " + e.getMessage();
		}
		mStart = System.currentTimeMillis();

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
		long time = System.currentTimeMillis() - startTime;
		mTotalTime += time;
		Log.i(TAG, "doHTTPpost response = " + responseString + " TIME = " + time + " millisecs");

		return responseString;
	}

	public boolean projectExists(String projectId, String server){
		String url = server+"/api/projectExists?authKey="+authKey+"&projectId="+projectId;
		Log.i(TAG, url);
		String response = doHTTPGET(url);
		Log.i(TAG, "projectExists response = " + response);

		if(response.equals("true"))
			return true;
		if(response.equals("false"))
			return false;
		return false;
	}
	/**
	 * A wrapper(does some cleanup too) for sending HTTP GET requests to the URI
	 * 
	 * @param Uri
	 * @return the request from the remote server
	 */
	public String doHTTPGET(String Uri) {
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
		if (Utils.debug) {
			Log.i(TAG, "doHTTPGet Uri = " + Uri);
		}
		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		mStart = System.currentTimeMillis();

		try {
			responseString = mHttpClient.execute(httpGet, responseHandler);
		} catch (ClientProtocolException e) {
			Log.e(TAG, "ClientProtocolException" + e.getMessage());
			e.printStackTrace();
			return "[Error] " + e.getMessage();
		} catch (SocketTimeoutException e) {
			Log.e(TAG, "[Error: SocketTimeoutException]" + e.getMessage());
			e.printStackTrace();
			return "[Error] " + e.getMessage();			
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
			return "[Error] " + e.getMessage();
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
			return "[Error] " + e.getMessage();
		}

		long time = System.currentTimeMillis() - mStart;
		mTotalTime += time;
		Log.i(TAG, "TIME = " + time + " millisecs");

		if (Utils.debug)
			Log.i(TAG, "doHTTPGet Response: " + responseString);
		return responseString;
	}

	/**
	 * Pull the remote find from the server using the guid provided.
	 * 
	 * @param guid
	 *            , a globally unique identifier
	 * @return an associative list of attribute/value pairs
	 */
	public ContentValues getRemoteFindById(String guid) {
		String url = server + "/api/getFind?guid=" + guid + "&authKey="
				+ authKey;
		HashMap<String, String> sendMap = new HashMap<String, String>();
		addRemoteIdentificationInfo(sendMap);
		sendMap.put("guid", guid + "");
		String responseString = doHTTPPost(url, sendMap);
		ContentValues cv = new ContentValues();

		Log.i(TAG, "getRemoteFindById = " + responseString);
		try {
			JSONObject jobj = new JSONObject(responseString);
			cv.put(PositDbHelper.FINDS_GUID, jobj.getString(PositDbHelper.FINDS_GUID));
			cv.put(PositDbHelper.FINDS_PROJECT_ID, jobj.getInt(PositDbHelper.FINDS_PROJECT_ID));
			cv.put(PositDbHelper.FINDS_NAME, jobj.getString(PositDbHelper.FINDS_NAME));
			cv.put(PositDbHelper.FINDS_DESCRIPTION, jobj
					.getString(PositDbHelper.FINDS_DESCRIPTION));
			//FIXME add add_time and modify_time for this
			cv.put(PositDbHelper.FINDS_TIME, jobj.getString("add_time"));
			cv.put(PositDbHelper.FINDS_TIME, jobj.getString("modify_time"));
			cv.put(PositDbHelper.FINDS_LATITUDE, jobj.getDouble(PositDbHelper.FINDS_LATITUDE));
			cv.put(PositDbHelper.FINDS_LONGITUDE, jobj.getDouble(PositDbHelper.FINDS_LONGITUDE));
			cv.put(PositDbHelper.FINDS_REVISION, jobj.getInt(PositDbHelper.FINDS_REVISION));
			return cv;
		} catch (JSONException e) {
			Log.i(TAG, e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			Log.i(TAG, e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Get an image from the server using the guid as Key.
	 * 
	 * @param guid
	 *            the Find's globally unique Id
	 */
	public ArrayList<HashMap<String, String>> getRemoteFindImages(String guid) {
		ArrayList<HashMap<String, String>> imagesMap = null;
		// ArrayList<HashMap<String, String>> imagesMap = null;
		String imageUrl = server + "/api/getPicturesByFind?findId=" + guid
				+ "&authKey=" + authKey;
		HashMap<String, String> sendMap = new HashMap<String, String>();
		Log.i(TAG, "getRemoteFindImages, sendMap=" + sendMap.toString());
		sendMap.put(PositDbHelper.FINDS_GUID, guid);
		addRemoteIdentificationInfo(sendMap);
		try {
			String imageResponseString = doHTTPPost(imageUrl, sendMap);
			Log.i(TAG, "getRemoteFindImages, response=" + imageResponseString);

			if (!imageResponseString.equals(RESULT_FAIL)) {
				JSONArray jsonArr = new JSONArray(imageResponseString);
				imagesMap = new ArrayList<HashMap<String, String>>();

				for (int i = 0; i < jsonArr.length(); i++) {
					JSONObject jsonObj = jsonArr.getJSONObject(i);
					if (Utils.debug)
						Log.i(TAG, "JSON Image Response String: "
								+ jsonObj.toString());
					Iterator<String> iterKeys = jsonObj.keys();
					HashMap<String, String> map = new HashMap<String, String>();
					while (iterKeys.hasNext()) {
						String key = iterKeys.next();
						map.put(key, jsonObj.getString(key));
					}
					imagesMap.add(map);
				}
			}
		} catch (Exception e) {
			Log.i(TAG, e.getMessage());
			e.printStackTrace();
		}
		if (imagesMap != null && Utils.debug)
			Log
					.i(TAG, "getRemoteFindImages, imagesMap="
							+ imagesMap.toString());
		else
			Log.i(TAG, "getRemoteFindImages, imagesMap= null");
		return imagesMap;
	}

	/**
	 * Checks if a given image already exists on the server. Allows for quicker
	 * syncing to the server, as this allows the application to bypass
	 * converting from a bitmap to base64 to send to the server
	 * 
	 * @param imageId
	 *            the id of the image to query
	 * @return whether the image already exists on the server
	 */
	public boolean imageExistsOnServer(int imageId) {
		HashMap<String, String> sendMap = new HashMap<String, String>();
		addRemoteIdentificationInfo(sendMap);
		String imageUrl = server + "/api/getPicture?id=" + imageId
				+ "&authKey=" + authKey;
		String imageResponseString = doHTTPPost(imageUrl, sendMap);
		if (imageResponseString.equals(RESULT_FAIL))
			return false;
		else
			return true;
	}

	
	/**
	 * Sends a GPS point and associated data to the Posit server. Called from 
	 *  Tracker Activity or TrackerBackgroundService.  
	 */
	public String registerExpeditionPoint(double lat, double lng, double alt,
			int swath, int expedition, long time) {

		HashMap<String, String> sendMap = new HashMap<String, String>();
		addRemoteIdentificationInfo(sendMap);
		String addExpeditionUrl = server + "/api/addExpeditionPoint?authKey="
				+ authKey;
		sendMap.put(PositDbHelper.GPS_POINT_LATITUDE, "" + lat);
		sendMap.put(PositDbHelper.GPS_POINT_LONGITUDE, lng + "");
		sendMap.put(PositDbHelper.GPS_POINT_ALTITUDE, "" + alt);
		sendMap.put(PositDbHelper.GPS_POINT_SWATH, "" + swath);
		sendMap.put(PositDbHelper.EXPEDITION, expedition + "");
		sendMap.put(PositDbHelper.GPS_TIME, time + "");
		String response = doHTTPPost(addExpeditionUrl, sendMap);

		return response;
	}

	/**
	 * Registers a new expedition with the server.
	 * @param projectId  Posit's current project id.
	 * @return Returns the expedition number received from the server or -1 if something
	 * goes wrong.
	 */
	public int registerExpeditionId(int projectId) {
		HashMap<String, String> sendMap = new HashMap<String, String>();
		addRemoteIdentificationInfo(sendMap);
		String addExpeditionUrl = server + "/api/addExpedition?authKey="
				+ authKey;
		sendMap.put("projectId", "" + projectId);
		String response = doHTTPPost(addExpeditionUrl, sendMap);
		Log.d(TAG,"registerExpeditionId response = " + response);

		// The server should return an expedition number if everything goes ok.  If 
		//  an error occurs, it will return an error message that cannot parse to an int
		//  which will cause an exception here.
		try {
			Integer i = Integer.parseInt(response);
			return i;
		} catch (NumberFormatException e) {
			Log.e(TrackerActivity.TAG, "Communicator, registerExpeditionId, Invalid response received");
			return -1;
		}
	}
}
