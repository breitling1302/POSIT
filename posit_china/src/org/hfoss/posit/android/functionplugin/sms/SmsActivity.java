/*
 * File: SmsActivity.java
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

package org.hfoss.posit.android.functionplugin.sms;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.plugin.FindPluginManager;
import org.hfoss.posit.android.sync.SyncSms;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


/**
 * Retrieve phone number and SMS message and transmit the contents of
 * the Find. 
 * 
 */
public class SmsActivity extends Activity {

	private static final String TAG = "SmsActivity";

	protected EditText mEditPhoneNum;
	protected TextView mContents;
	protected Button mSendButton;
	protected Bundle mDbEntries;
	protected TextView mNumMessagesView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.send_sms);
		mEditPhoneNum = (EditText) findViewById(R.id.phoneEditText);
		mSendButton = (Button) findViewById(R.id.sendButton);
		mContents = (TextView) findViewById(R.id.contentsTextView);
		mNumMessagesView = (TextView) findViewById(R.id.smsNumMessagesView);
		// Get default phone # and default message prefix
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		String defaultNum = preferences.getString(
				getString(R.string.defaultPhoneNumKey), "");
		mEditPhoneNum.setText(defaultNum);
		// Retrieve DbEntries of Find from intent
		mDbEntries = getIntent().getParcelableExtra("DbEntries");
		// Get message text and set listener
		try {
			SyncSms syncSms = new SyncSms( this );
			String contents = SyncSms.convertBundleToRaw( mDbEntries );
			syncSms.addFind( mDbEntries, mEditPhoneNum.getText().toString() );
			mContents.setText(contents);
			// Get # of messages that need to be sent and display
			int[] length = SmsMessage.calculateLength(contents, false);
			mNumMessagesView.setText(String.valueOf(length[0]));
			mSendButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					SyncSms sync = new SyncSms( v.getContext() );
					sync.addFind( mDbEntries, mEditPhoneNum.getText().toString() );
					sync.sendFinds();
					finish();
				}
			});
		} catch (IllegalArgumentException e) {
			Toast.makeText(
					this,
					"Plugin \"" + FindPluginManager.mFindPlugin.getName()
							+ "\" not supported.", Toast.LENGTH_LONG).show();
			finish();
		}

	}
}
