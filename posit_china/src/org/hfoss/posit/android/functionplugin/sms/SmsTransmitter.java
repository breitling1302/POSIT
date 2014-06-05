/*
 * File: SmsTransmitter.java
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.plugin.FindPluginManager;
import org.hfoss.posit.android.api.service.SmsService;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

/**
 * This class is for the easy sending of SMS messages. Currently, there are two
 * types of SMS messages that can be sent: An entire Find, or a simple String.
 * This class encapsulates the SMS sending protocol. If someone were to want to
 * make the protocol more robust (for instance, to allow the sending of partial
 * finds), they would likely want to do it here. For the purposes of my plugin,
 * I have kept it fairly simple.
 * 
 * @author Ryan McLeod
 * 
 */
public class SmsTransmitter {
	public static final String TAG = "SmsTransmitter";

	public static final String FIND_PREFIX = "~_";

	private ArrayList<String> mMessages;
	private ArrayList<String> mPhoneNumbers;
	private Context mContext;

	public SmsTransmitter(Context context) {
		mMessages = new ArrayList<String>();
		mPhoneNumbers = new ArrayList<String>();
		mContext = context;
	}

	/**
	 * Adds the contents of an ENTIRE Find to be transmitted later. Also returns
	 * the raw message. The format of the message is as follows: <br>
	 * <br>
	 * 
	 * (prefix)value1,value2,value3,... <br>
	 * <br>
	 * 
	 * The values are, in lexicographical order of the attribute names, strings
	 * encoding the attributes' values. These encodings are handled by
	 * ObjectCoder.
	 * 
	 * @param dbEntries
	 *            A Bundle object containing all of a Find's database fields.
	 * @param phoneNumber
	 *            The phone number that the Find should be transmitted to
	 * @return A String containing the text message.
	 * @throws IllegalArgumentException
	 *             if one of the values could not be encoded.
	 * 
	 */
	public String addFind(Bundle dbEntries, String phoneNumber)
			throws IllegalArgumentException {

		List<String> keys = new ArrayList<String>(dbEntries.keySet());
		// Next sort the list of keys. It is important that attribute values be
		// transmitted and interpreted in the same order.
		Collections.sort(keys);

		// Now attempt to put values with attributes
		StringBuilder builder = new StringBuilder();
		for (String key : keys) {
			if (builder.length() > 0)
				builder.append(",");
			String code = ObjectCoder.encode(dbEntries.get(key));
			if (code != null) {
				// Object was encoded successfully
				builder.append(code);
			} else {
				// Object could not be encoded
				Log.e(TAG, "Tried to encode object of unsupported type.");
				throw new IllegalArgumentException();
			}
		}
		// Put prefix
		builder.insert(0, FIND_PREFIX);
		// String building done
		String text = builder.toString();
		addMessage(text, phoneNumber);
		return text;
	}

	/**
	 * Add a Find to be transmitted via SMS later.
	 * 
	 * @param find
	 *            The Find object to be transmitted.
	 * @param phoneNumber
	 *            The phone number that the Find should be transmitted to
	 * @throws IllegalArgumentException
	 */
	public void addFind(Find find, String phoneNumber)
			throws IllegalArgumentException {
		addFind(find.getDbEntries(), phoneNumber);
	}

	/**
	 * Add a text message to be sent later.
	 * 
	 * @param text
	 *            The string contents of the SMS message
	 * @param phoneNumber
	 *            The phone number to which the message should be sent
	 */
	public void addMessage(String text, String phoneNumber) {
		mMessages.add(text);
		mPhoneNumbers.add(phoneNumber);
	}

	/**
	 * Sends all previously added messages to their respective destinations.
	 * 
	 */
	public void sendAll() {
		Intent smsService = new Intent(mContext, SmsService.class);
		smsService.putExtra("messages", mMessages);
		smsService.putExtra("phonenumbers", mPhoneNumbers);
		smsService.setAction(Intent.ACTION_SEND);
		mContext.startService(smsService);
	}
}
