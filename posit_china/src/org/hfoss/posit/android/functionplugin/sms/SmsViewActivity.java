/*
 * File: SmsViewActivity.java
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

import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.api.plugin.FindPlugin;
import org.hfoss.posit.android.api.plugin.FindPluginManager;
import org.hfoss.posit.android.R;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;

/**
 * Plugin to view and edit a Find received from a SMS message
 *
 */
public class SmsViewActivity extends OrmLiteBaseActivity<DbManager> {

	private static final String TAG = "SmsViewActivity";

	protected TextView mFindView;
	protected TextView mSenderView;
	protected Button mSaveButton;
	protected Button mDismissButton;
	protected int mNotificationId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sms_view);
		mFindView = (TextView) findViewById(R.id.smsFindView);
		mSaveButton = (Button) findViewById(R.id.smsSaveButton);
		mDismissButton = (Button) findViewById(R.id.smsDismissButton);
		mSenderView = (TextView) findViewById(R.id.smsSenderView);

		// Get extras and display information in views
		final Bundle bundle = getIntent().getBundleExtra("findbundle");
		String sender = getIntent().getStringExtra("sender");
		mNotificationId = getIntent().getIntExtra("notificationid", 0);
		if (mNotificationId == 0) {
			Log.e(TAG, "Could not retrieve notification ID.");
			Toast.makeText(this, "A fatal error has occurred in SMS viewer.",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		refreshViews(bundle, sender);

		// Listener for Save button click
		mSaveButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Find find = new Find();
				find.updateObject(bundle);
				// Change project ID to reflect current project on this end
				SharedPreferences prefs = PreferenceManager
						.getDefaultSharedPreferences(getApplicationContext());
				int projectId = prefs
						.getInt(getString(R.string.projectPref), 0);
				find.setProject_id(projectId);
				// Start the appropriate FindActivity so we can actually save
				// the find
				FindPlugin plugin = FindPluginManager.mFindPlugin;
				if (plugin == null) {
					Log.e(TAG, "Could not retrieve Find Plugin.");
					Toast
							.makeText(
									v.getContext(),
									"A fatal error occurred while trying to start FindActivity",
									Toast.LENGTH_LONG).show();
					finish();
					return;
				}
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_INSERT_OR_EDIT);
				intent.putExtra("findbundle", bundle);
				intent.setClass(v.getContext(), plugin.getmFindActivityClass());
				startActivity(intent);
				// Cancel notification
				String ns = Context.NOTIFICATION_SERVICE;
				NotificationManager notificationMgr = (NotificationManager) getSystemService(ns);
				notificationMgr.cancel(mNotificationId);
				finish();
			}
		});
		// Listener for Dismiss button click
		mDismissButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Cancel notification
				String ns = Context.NOTIFICATION_SERVICE;
				NotificationManager notificationMgr = (NotificationManager) getSystemService(ns);
				notificationMgr.cancel(mNotificationId);
				finish();
			}
		});
	}

	/**
	 * Helper function that refreshes the text views
	 */
	private void refreshViews(Bundle bundle, String sender) {
		StringBuilder builder = new StringBuilder();
		for (String key : bundle.keySet()) {
			Object val = bundle.get(key);
			if (val == null) {
				builder.append(key + ": null\n");
			} else {
				builder.append(key + ": " + val + "\n");
			}
		}
		mFindView.setText(builder.toString());
		mSenderView.setText(getString(R.string.smsSenderLabel) + sender);
	}
}
