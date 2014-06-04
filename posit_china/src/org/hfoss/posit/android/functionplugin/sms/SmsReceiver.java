/*
 * File: SmsReceiver.java
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

import java.util.List;
import java.util.Map;

import org.hfoss.posit.android.api.plugin.FindPluginManager;
import org.hfoss.posit.android.api.plugin.FunctionPlugin;
import org.hfoss.posit.android.sync.SyncSms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SmsReceiver extends BroadcastReceiver {
	private static final String TAG = "SmsReceiver";
	private static int mNextNotificationId = 1;

	/**
	 * Helper function that checks if the SMS Plugin is turned on.
	 * 
	 * @return true if the SMS Plugin is on, false otherwise.
	 */
	private Boolean smsPluginOn() {
		List<FunctionPlugin> plugins = FindPluginManager
				.getFunctionPlugins(FindPluginManager.ADD_FIND_MENU_EXTENSION);
		for (FunctionPlugin plugin : plugins)
			if (plugin.getName().equals("sendsms"))
				return true;
		return false;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (!smsPluginOn()) {
			Log.i(TAG, "Received text message, but SMS plugin is disabled.");
			
			return;
		}
		
		SyncSms syncSms = new SyncSms( context );
		
		Map<String, String> msgTexts = syncSms.getMessages( intent );
		syncSms.processMessages( msgTexts, mNextNotificationId );
	}
}
