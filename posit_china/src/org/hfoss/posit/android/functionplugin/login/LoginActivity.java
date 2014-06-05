/*
 * File: LoginActivity.java
 * 
 * Copyright (C) 2011 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Source Information Tool.
 *
 * This is free software; you can redistribute it and/or modify
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
package org.hfoss.posit.android.functionplugin.login;


import org.hfoss.posit.android.api.AppControlManager;
import org.hfoss.posit.android.api.LocaleManager;
import org.hfoss.posit.android.api.User;
import org.hfoss.posit.android.api.User.UserType;
import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.R;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

/**
 * Handles Login for applications where users share a single phone.
 * 
 */
public class LoginActivity extends OrmLiteBaseActivity<DbManager> implements OnClickListener {
	public static final String TAG = "LoginActivity";
	public static final int ACTION_LOGIN = 0;
	public static final int INVALID_LOGIN = 1;
	public static final int VALID_LOGIN = 2;
	
	private static final int CONFIRM_EXIT = 0;
		
	private UserType mUserType;
	private int mUserTypeOrdinal;

	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = this.getIntent();
		Bundle extras = intent.getExtras();
		if (extras == null) {
			return;
		}

		mUserTypeOrdinal = extras.getInt(User.USER_TYPE_STRING);
		if (mUserTypeOrdinal == UserType.USER.ordinal()) {
			mUserType = UserType.USER;
		} else if (mUserTypeOrdinal == UserType.ADMIN.ordinal()) {
			mUserType = UserType.ADMIN;
		} else {
			Log.e(TAG, "Invalid user type passed to LoginActivity");
		}
 	}


	@Override
	protected void onPause() {
		Log.i(TAG, "onPause");
		super.onPause();
	}

	/**
	 * 
	 */
	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume");
		
		LocaleManager.setDefaultLocale(this);  // Locale Manager should be in API

		setContentView(R.layout.login);  // Should be done after locale configuration
		
		((Button)findViewById(R.id.login_button)).setOnClickListener(this);
		((Button)findViewById(R.id.cancel_login_button)).setOnClickListener(this);
	}

	/**
	 * Required as part of OnClickListener interface. Handles button clicks.
	 */
	public void onClick(View v) {
		Log.i(TAG, "onClick");
	    Intent returnIntent = new Intent();
	
		if (v.getId() == R.id.login_button) {
			EditText etext = ((EditText)findViewById(R.id.usernameEdit));
			String username = etext.getText().toString();
			etext = ((EditText)findViewById(R.id.passwordEdit));
			String password = etext.getText().toString();
			int userTypeOrdinal = authenticateUser(username, password, mUserType);
			if (userTypeOrdinal != -1) {
				setResult(Activity.RESULT_OK,returnIntent);
				
				// Remember what type of user this is -- for controlling menus, etc.			
				AppControlManager.setUserType(userTypeOrdinal);
//				Log.i(TAG, "UserType = " + AppControlManager.getUserType()); 
				
				finish();
			} else {
				showDialog(INVALID_LOGIN);
			}
		} else {
			setResult(Activity.RESULT_CANCELED, returnIntent);
			finish();
		}
	    //finish();
	}
	
	/**
	 * Creates a dialog to confirm that the user wants to exit POSIT.
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case INVALID_LOGIN:
			return new AlertDialog.Builder(this).setIcon(
					R.drawable.alerts_and_states_warning).setTitle(R.string.password_alert_message)
					.setPositiveButton(R.string.alert_dialog_ok,
							new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// User clicked OK so do nothing

						}
					}
					).create();

		default:
			return null;
		}
	}
	
	/**
	 * Local authentication method just calls User class to authenticate.
	 * Override if you want to use your own custom defined User table.
	 * @param username
	 * @param password
	 * @param userType
	 * @return
	 */
	private int authenticateUser(String username, String password, UserType userType) {
		return User.authenticateUser(this, username, password, userType, this.getHelper().getUserDao());
	}

}