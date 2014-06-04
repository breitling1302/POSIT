/*
 * File: RegisterActivity.java
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

import java.util.List;

import org.apache.commons.validator.EmailValidator;
import org.hfoss.posit.android.utilities.Utils;
import org.hfoss.posit.android.web.Communicator;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * Handles both user registration and phone registration. In order to use POSIT 
 * effectively a user should have a valid account on a POSIT server. They can create a new 
 * account from their phone or by visiting the server (Default = posit.hfoss.org).
 * If they have an existing account, then the phone must be registered with the
 * server.  A properly registered phone receives an authKey.
 * 
 * Various preference settings are stored in SharedPreferences and carry over from
 * one use of POSIT to another. These include SERVER_ADDRESS, EMAIL, AUTHKEY, PROJECT.
 *
 */
public class RegisterActivity extends Activity implements OnClickListener {

	private static final String TAG = "RegisterActivity";
	private static final int LOGIN_BY_BARCODE_READER = 0;
	private static final int PROMPT_REGISTRATION = 0;
	private static final int CONFIRM_EXIT = 1;

	public static final String REGISTER_USER = "RegisterUser";
	public static final String REGISTER_PHONE = "RegisterPhone";


	private SharedPreferences mSharedPrefs;
	private ProgressDialog mProgressDialog;
	private String mAction; // Save current action for restartability

	private Button mRegisterUsingBarcodeButton;
	private Button mRegisterUsingDeviceButton;
	private Button mRegisterButton;

	private int currentView;
	private static final int VIEW_MAIN = 0;
	private static final int VIEW_LOGIN = 1;
	private static final int VIEW_REGISTER = 2;


	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG,"Registration Activity");
		
		// If the authKey is set, the user is already register, but what if
		// they want to create a new account??
		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		setContentView(R.layout.main_register);
		currentView = VIEW_MAIN;
		
		final TextView version = (TextView) findViewById(R.id.version);
		try {
			version.setText(getPackageManager().getPackageInfo("org.hfoss.posit.android", 0).versionName);
		} catch(NameNotFoundException nnfe) {
			//shouldn't happen
			Log.w(TAG, nnfe.toString(), nnfe);
			version.setVisibility(View.INVISIBLE);
		}
		
		
		// Register existing user button
		Button register = (Button) findViewById(R.id.register);
		if (register != null) {
			register.setOnClickListener(this);
		}
		
		// Create new user button
		Button login = (Button) findViewById(R.id.login);
		if (login != null)
			login.setOnClickListener(this);
		
		// If RegisterActivity is started from PositMain, then no action is set
		//  until the user selects one. We need to store that selection 
		//  (see onRetainNonConfigurationInstance()) so that the user's choice
		//  is not lost if he rotates the phone. This if-statment gets that
		//  value, if any.
		String action = (String) getLastNonConfigurationInstance();
		if (action != null) {
			mAction = action;
			if (action.equals(REGISTER_USER))
				createNewUserAccount();
			else if (action.equals(REGISTER_PHONE))
				registerExistingAccount();
		}
		// If we got here via SettingsActivity, then it has pre-set the action
		//  (in a way that is preserved on RegisterActivity restart).
		else if ((action = getIntent().getAction()) != null) {
			if (action.equals(REGISTER_USER))
				createNewUserAccount();
			else if (action.equals(REGISTER_PHONE))
				registerExistingAccount();
		}
		else
			showDialog(PROMPT_REGISTRATION);
	}
	
	
	// Retain a record of what we were doing so that we can resume there if
	//  RegisterActivity has to restart because user rotated phone.
	@Override
	public Object onRetainNonConfigurationInstance() {
		return mAction;
	}
	
	
	public void onResume(){
		super.onResume();
	}
	
	
	/**
	 * Handle all button clicks. There are two main buttons that appear on the View
	 * when the Activity is started.  When one of those buttons is clicked, a new 
	 * View is displayed with one or more additional buttons.
	 */
	public void onClick(View v) {

		if (!Utils.isNetworkAvailable(this)) {
			Utils.showToast(this,"There's a problem. To register you must be on a network.");
			return;
		}
		
		Intent intent;
		
		switch (v.getId()) {
		
		// Register phone for an existing account
		case R.id.register:
			mAction = RegisterActivity.REGISTER_USER;
			createNewUserAccount();
			break;
		
		// Create a new user account
		case R.id.login:
			mAction = RegisterActivity.REGISTER_PHONE;
			registerExistingAccount();
			break;
		
		// Register the phone from the phone by providing valid email and password
		case R.id.registerDeviceButton:
			String password = (((TextView) findViewById(R.id.password)).getText()).toString();
			String email = (((TextView) findViewById(R.id.email)).getText()).toString();
			if (password.equals("") || email.equals("")) {
				Utils.showToast(this, "Please fill in all the fields");
				break;
			}
			EmailValidator emailValidator = EmailValidator.getInstance();
			if (emailValidator.isValid(email) != true) {
				Utils.showToast(this, "Please enter a valid address");
				break;
			}
			loginUser(email, password);
			break;
		
		// Register the phone by reading a barcode on the server's website (Settings > Register)
		case R.id.registerUsingBarcodeButton:
			if (!isIntentAvailable(this, "com.google.zxing.client.android.SCAN")) {
				Utils.showToast(this,  "Please install the Zxing Barcode Scanner from the Market");
				break;
			}
			intent = new Intent("com.google.zxing.client.android.SCAN");
			try {
				startActivityForResult(intent, LOGIN_BY_BARCODE_READER);
			} catch (ActivityNotFoundException e) {
				if (Utils.debug)
					Log.i(TAG, e.toString());
			}
			break;
			
		// User clicks the "Login" button in the Create User View	
		case (R.id.submitInfo):
			password = (((TextView) findViewById(R.id.password)).getText()).toString();
			String check = (((TextView) findViewById(R.id.passCheck)).getText()).toString();
			email = (((TextView) findViewById(R.id.email)).getText()).toString();
			String lastname = (((TextView) findViewById(R.id.lastName)).getText()).toString();
			String firstname = (((TextView) findViewById(R.id.firstName)).getText()).toString();
			
			if (password.equals("") || check.equals("") || lastname.equals("")
					|| firstname.equals("") || email.equals("")) {
				Utils.showToast(this,"Please fill in all the fields");
				break;
			}
			
			EmailValidator emV = EmailValidator.getInstance();
			if (emV.isValid(email) != true) {
				Utils.showToast(this, "Please enter a valid email address");
				break;
			}
			if (!check.equals(password)) {
				Utils.showToast(this,"Your passwords do not match");
				break;
			}
			
			TelephonyManager manager = (TelephonyManager) this
					.getSystemService(Context.TELEPHONY_SERVICE);
			String imei = manager.getDeviceId();
			
			Communicator com = new Communicator(this);
			
			String server = mSharedPrefs.getString("SERVER_ADDRESS", getString(R.string.defaultServer));
			
			String result = com.registerUser(server, firstname, lastname,
					email, password, check, imei);
			Log.i(TAG, "RegisterUser result = " + result);
			if (result != null) {
				String[] message = result.split(":");
				if (message.length != 2) {
					Utils.showToast(this, "Error: " + result);
					break;
				}
				// A new account has successfully been created.
				if (message[0].equals("" + Constants.AUTHN_OK)) {
					Editor editor = mSharedPrefs.edit();
					editor.putString("EMAIL", email);
					editor.commit();
					
					// The user logs in to register the device.
					loginUser(email, password);
					
				} else {
					Utils.showToast(this, message[1]);
				}
			break;
			
			}
			mProgressDialog.dismiss();
		}
	}
	
	private void createNewUserAccount() {
		Log.i(TAG,"Creating new user");
		setContentView(R.layout.registeruser);
		currentView = VIEW_LOGIN;
		String server = mSharedPrefs.getString("SERVER_ADDRESS", "");
		String email = mSharedPrefs.getString("EMAIL", "");
		((TextView) findViewById(R.id.serverName)).setText(server);
		((TextView) findViewById(R.id.email)).setText(email);
		mRegisterButton = (Button) findViewById(R.id.submitInfo);
		mRegisterButton.setOnClickListener(this);
	}
	
	
	/**
	 * Handles server registration by decoding the JSON Object that the barcode
	 * reader gets from the server site containing the server address and the
	 * authentication key. These two pieces of information are stored as shared
	 * preferences. The user is then prompted to choose a project from the
	 * server to work on and sync with.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i(TAG, "Requestcode = " + requestCode + "Result code = " + resultCode);
		super.onActivityResult(requestCode, resultCode, data);
		
		switch (requestCode) {
		case LOGIN_BY_BARCODE_READER:
			String value = data.getStringExtra("SCAN_RESULT");
			Log.i(TAG,"Bar code scanner result " + value);
			
			// Hack to remove extra escape characters from JSON text.
			StringBuffer sb = new StringBuffer("");
			for (int k = 0; k < value.length(); k++) {
				char ch = value.charAt(k);
				if (ch != '\\') {
					sb.append(ch);
				} else if (value.charAt(k + 1) == '\\') {
					sb.append(ch);
				}
			}
			value = sb.toString(); // Valid JSON encoded string
			// End of Hack
			
			JSONObject object;
			
			try {
				Log.i(TAG, "JSON=" + value);
				
				object = new JSONObject(value);
				String server = object.getString("server");
				String authKey = object.getString("authKey");
				if (Utils.debug)
					Log.i(TAG, "server= " + server + ", authKey= " + authKey);
				TelephonyManager manager = (TelephonyManager) this
						.getSystemService(Context.TELEPHONY_SERVICE);
				String imei = manager.getDeviceId();
				Communicator communicator = new Communicator(this);
				mProgressDialog = ProgressDialog.show(this, "Registering device",
						"Please wait.", true, true);
				try {
					String registered = communicator.registerDevice(server, authKey, imei);
					Log.d(TAG, "onActivityResult, registered = " + registered);
					if (registered != null) {
						Log.i(TAG, "registered");
						Editor spEditor = mSharedPrefs.edit();
						
						spEditor.putString("SERVER_ADDRESS", server);
						spEditor.putString("AUTHKEY", authKey);
						spEditor.putInt("PROJECT_ID", 0);
						spEditor.putString("PROJECT_NAME", "");
						spEditor.commit();
						
						Intent intent = new Intent(this, ShowProjectsActivity.class);
						startActivity(intent);
					}
				} catch (NullPointerException e) {
					Utils.showToast(this, "Registration Error");
				} finally {
					mProgressDialog.dismiss();
				}
				
				mProgressDialog.dismiss();
				int projectId = mSharedPrefs.getInt("PROJECT_ID", 0);
				if (projectId == 0) {
					Intent intent = new Intent(this, ShowProjectsActivity.class);
					startActivity(intent);
					}
				finish();
				
			} catch (JSONException e) {
				if (Utils.debug)
					Log.e(TAG, e.toString());
			}
			break;
		}
	}
	
	
	/**
	 * For a user with an existing account on the server, this method will register
	 * the phone, resulting in an authKey being sent to the phone by the server and
	 * saved in SharedPreferences.
	 */
	private void registerExistingAccount() {
		Log.i(TAG,"Creating new user");
		
		setContentView(R.layout.registerphone);
		currentView = VIEW_REGISTER;
		
		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		String serverName = mSharedPrefs.getString("SERVER_ADDRESS", "");
		Log.i(TAG,"Server = " + serverName);
		((TextView) findViewById(R.id.serverName)).setText(serverName);
		
		String email = mSharedPrefs.getString("EMAIL", "");
		((TextView) findViewById(R.id.email)).setText(email);
		
		mRegisterUsingBarcodeButton = (Button) findViewById(R.id.registerUsingBarcodeButton);
		mRegisterUsingBarcodeButton.setOnClickListener(this);
		
		mRegisterUsingDeviceButton = (Button) findViewById(R.id.registerDeviceButton);
		mRegisterUsingDeviceButton.setOnClickListener(this);
	}
	
	/**
	 * Logs the user onto the server, give the user's email and password. If
	 * the user can successfully login, the phone receives an AUTHKEY, which is
	 * saved in SharedPreferences.
	 * 
	 * Actually, this method makes two HTTP requests to the server, one to login
	 * the user and a second to register the user's phone.
	 * 
	 * @param email email account user is using to register with a given server
	 * @param password password used to register and sign in to a server
	 */
	private void loginUser(String email, String password) {
		mProgressDialog = ProgressDialog.show(this, "Registering device",
				"Please wait.", true, true);
		String serverName = mSharedPrefs.getString("SERVER_ADDRESS", "");
		Communicator com = new Communicator(this);
		TelephonyManager manager = (TelephonyManager) this
				.getSystemService(Context.TELEPHONY_SERVICE);
		String imei = manager.getDeviceId();
		
		// First login the user.
		String result = null;
		try {
			result = com.loginUser(serverName, email, password, imei);
		} catch (Exception e) {
			Log.i(TAG, "Exception " + e.getMessage());
		}
		
		Log.i(TAG, "loginUser result: " + result);
		String authKey;
		if (null==result){
			Utils.showToast(this, "Failed to get authentication key from server.");
			mProgressDialog.dismiss();
			return;
		}
		//TODO this is still little uglyish
		String[] message = result.split(":");
		if (message.length != 2 || message[1].equals("null")){
			Utils.showToast(this, "Login failed: " + result);
			mProgressDialog.dismiss();
			return;
		}
		// Successfully logged in
		if (message[0].equals(""+Constants.AUTHN_OK)){
			authKey = message[1];
			Log.i(TAG, "AuthKey "+ authKey +" obtained, registering device");
			
			// Here we register the device
			String responseString = com.registerDevice(serverName, authKey, imei);
			
			if (responseString.equals("true")){
				Editor spEditor = mSharedPrefs.edit();
				spEditor.putString("AUTHKEY", authKey); // Remember the AUTHKEY
				spEditor.putString("EMAIL", email); // Remember the userID
				spEditor.commit();
				
				Intent intent = new Intent(this, ShowProjectsActivity.class);
				startActivity(intent);
				
				Utils.showToast(this, "Successfully logged in.");
				setResult(PositMain.LOGIN_SUCCESSFUL);
				finish();
			}
		}else {
			Utils.showToast(this, message[1] + 
					"\nMake sure you have connectivity" +
					" and a working server.");
			mProgressDialog.dismiss();
			return;
		}
		mProgressDialog.dismiss();
	}
	
	/**
	 * Creates the menu options for the PositMain screen. Menu items are
	 * inflated from a resource file.
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.positmain_menu, menu);
		MenuItem item = menu.findItem(R.id.projects_menu_item);
		item.setEnabled(false);
		item = menu.findItem(R.id.track_menu_item);
		item.setEnabled(false);
		return true;
	}
	
	/**
	 * Manages the selection of menu items.
	 * 
	 * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settings_menu_item:
			startActivity(new Intent(this, SettingsActivity.class));
			break;
		case R.id.about_menu_item:
			startActivity(new Intent(this, AboutActivity.class));
			break;
		case R.id.tutorial_menu_item:
			startActivity(new Intent(this, TutorialActivity.class));
			break;
		}
		return true;
	}
	
	
	/**
	 * This method is used to check whether or not the user has an intent
	 * available before an activity is actually started. In this case we
	 * check to see whether the barcode scanner is available. Since the 
	 * barcode scanner requires a downloadable dependency, the user cannot
	 * be allowed to click the "Read Barcode" button unless the phone is able 
	 * to do so.
	 */
	public static boolean isIntentAvailable(Context context, String action) {
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
				PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}
	
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_BACK){
			if (currentView != VIEW_MAIN) {
				setContentView(R.layout.main_register);
				currentView = VIEW_MAIN;
				
				// Register existing user button
				Button register = (Button) findViewById(R.id.register);
				if (register != null) {
					register.setOnClickListener(this);
				}
				
				// Create new user button
				Button login = (Button) findViewById(R.id.login);
				if (login != null)
					login.setOnClickListener(this);
			} else {
				showDialog(CONFIRM_EXIT);
			}
			return true;
		}
		Log.i("code", keyCode+"");
		return super.onKeyDown(keyCode, event);
	}

	
	
	/**
	 * Creates a dialog to confirm that the user wants to exit POSIT.
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case PROMPT_REGISTRATION:
			return new AlertDialog.Builder(this)
			.setIcon(R.drawable.icon)
			.setMessage("To effectively use POSIT " +
					" you should register with a POSIT server. " +
					" Either create an account on " + 
					getString(R.string.defaultServer) + " or create a new account.")
					.setPositiveButton(R.string.alert_dialog_ok,
							new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// User clicked OK so do some stuff
						}
					})
					.create();
		case CONFIRM_EXIT:
			return new AlertDialog.Builder(this).setIcon(
					R.drawable.alert_dialog_icon).setTitle(R.string.exit)
					.setPositiveButton(R.string.alert_dialog_ok,
							new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// User clicked OK so do some stuff
							setResult(PositMain.LOGIN_CANCELED);
							finish();
						}
					}).setNegativeButton(R.string.alert_dialog_cancel,
							new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							/* User clicked Cancel so do nothing */
						}
					}).create();

		default:
			return null;
		}
	}
}
