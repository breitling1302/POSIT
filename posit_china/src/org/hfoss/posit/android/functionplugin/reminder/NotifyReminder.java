/*
 * File: NotifyReminder.java
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

package org.hfoss.posit.android.functionplugin.reminder;

import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;

/**
 * This class is used when the user press on the reminder notification
 * in the notification center.
 * 
 * It displays detailed reminder information, including its name and
 * description, and gives the user the options to either keep or
 * discard the associated find.
 **/
public class NotifyReminder extends OrmLiteBaseActivity<DbManager> {
	
	private static final String TAG = "NotifyReminder";
	
	private NotificationManager mNotificationManager;
	
	// Notification Pop-up Dialog ID
	private static final int NOTIFICATION_ID = 0;
	private static final int NOTIFICATION_CANCEL = 1;

	// Find associated with the reminder
	private Find find;
	private int findId;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Set the transparent blank screen as a background for dialogs
		setContentView(R.layout.blank_screen);
		
		// Set up Notification Manager
		String ns = Context.NOTIFICATION_SERVICE;
		mNotificationManager = (NotificationManager) getSystemService(ns);
		
		// Initialize the find through find ID passed in by the intent
		final Bundle bundle = getIntent().getExtras();
		findId = bundle.getInt(Find.ORM_ID);
		find = getHelper().getFindById(findId);
		
		// Check that the find still exists
		if (find == null)  {
			showDialog(NOTIFICATION_CANCEL);
		} else {  // Show the Notification Pop-up Dialog
			showDialog(NOTIFICATION_ID);
		}
	}
	
	// Create and show Notification Pop-up Dialog
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		
		case NOTIFICATION_CANCEL:
			mNotificationManager.cancel(findId);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Reminder: Find # " + findId);
			builder.setMessage("This Find no longer exists in Db");
			// Do nothing when the user press "Dismiss"
			builder.setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
				}
			});
			
			AlertDialog cancel = builder.create();
			return cancel;
	        
		case NOTIFICATION_ID:
			// Create Notification Dialog
			builder = new AlertDialog.Builder(this);
			builder.setTitle("Reminder: " + find.getName());
			builder.setMessage(find.getDescription());
			
			// Do nothing when the user press "Keep the find"
			builder.setPositiveButton("Keep the find", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					find.setIs_adhoc(SetReminder.REMINDER_UNSET);
					getHelper().update(find);
				}
			});
			
			// Delete the find from the database when the user press "Discard the find"
			builder.setNegativeButton("Discard the find", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					getHelper().delete(find);
//					getApplicationContext().startService(new Intent(getApplicationContext(), ToDoReminderService.class));
				}
			});
			
			// Clear the notification and show the dialog
			AlertDialog notification = builder.create();
			notification.setOnDismissListener(new DialogInterface.OnDismissListener() {
				public void onDismiss(DialogInterface dialog) {
					mNotificationManager.cancel(find.getId());
					finish();
				}
			});
			
	        return notification;
	    
		default:
			return null;
		
		}
	}
	
}
