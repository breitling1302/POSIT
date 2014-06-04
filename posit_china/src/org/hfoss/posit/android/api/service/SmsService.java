/*
 * File: SmsService.java
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

package org.hfoss.posit.android.api.service;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

/**
 * Background service for sending SMS messages and handling
 * the callback from the phone's Radio regarding the status
 * of the attempt to send the message.
 *
 */
public class SmsService extends Service {
	public static final String TAG = "SmsManager";

	private static final String SENT = "SMS_SENT";

	private int nMsgsSent = 0;
	private int nMsgsPending = 0;
	private int mBroadcastsOutstanding = 0;
	private String mErrorMsg;

	private SendMessagesTask sendMessagesTask;

	private List<String> mMessages;
	private List<String> mPhoneNumbers;

	/**
	 * This service is not accepting bindings.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

	}

	/**
	 * Starts the service. Creates a AsyncTask to perform the actual sending of
	 * messages in a background task.
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			mMessages = intent.getStringArrayListExtra("messages");
			mPhoneNumbers = intent.getStringArrayListExtra("phonenumbers");
			Log.i(TAG, "Started background service, " + " nMessages = "
					+ mMessages.size());

			sendMessagesTask = new SendMessagesTask();
			sendMessagesTask.execute(this);
		}

		return super.onStartCommand(intent, flags, startId);
	}

	/**
	 * Callback method to handle the result of attempting to send a message. 
	 * Each message is assigned a Broadcast receiver that is notified by 
	 * the phone's radio regarding the status of the sent message. The 
	 * receivers call this method.  (See transmitMessage() method below.)
	 * 
	 * @param context
	 *            The context in which the calling BroadcastReceiver is running.
	 * @param receiver
	 *            Currently unused. Intended as a special BroadcastReceiver to
	 *            send results to. (For instance, if another plugin wanted to do
	 *            its own handling.)
	 * @param resultCode, the code sent back by the phone's Radio
	 * @param seq, the message's sequence number
	 * @param smsMsg, the message being processed
	 */
	private synchronized void handleSentMessage(Context context,
			BroadcastReceiver receiver, int resultCode, String seq,
			String smsMsg) {
		switch (resultCode) {
		case Activity.RESULT_OK:
			Log.i(TAG, "Received OK, seq = " + seq + " msg:" + smsMsg);
			++nMsgsSent;
			break;
		case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
			Log.e(TAG, "Received generic failure, seq =  " + seq + " msg:"
					+ smsMsg);
			++nMsgsPending;
			break;
		case SmsManager.RESULT_ERROR_NO_SERVICE:
			Log.e(TAG, "Received no service error, seq =  " + seq + " msg:"
					+ smsMsg);
			++nMsgsPending;
			Toast.makeText(context, "No service available.", Toast.LENGTH_SHORT).show();
			break;
		case SmsManager.RESULT_ERROR_NULL_PDU:
			Log.e(TAG, "Received null PDU error, seq =  " + seq + " msg:"
					+ smsMsg);
			++nMsgsPending;
			break;
		case SmsManager.RESULT_ERROR_RADIO_OFF:
			Log.e(TAG, "Received radio off error, seq =  " + seq + " msg:"
					+ smsMsg);
			Toast.makeText(context, "Could not send SMS message: radio off.", Toast.LENGTH_LONG).show();
			++nMsgsPending;
			break;
		}
		--mBroadcastsOutstanding;
		// Notify the user if all messages have been attempted
		if (mBroadcastsOutstanding == 0) {
			if (nMsgsPending == 0) {
				Toast.makeText(context, "All messages sent successfully.", Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(context, "One or more messages failed to be sent.", Toast.LENGTH_LONG).show();
			}
		}
	}

	/**
	 * Separately threaded task to send messages.
	 * 
	 */
	class SendMessagesTask extends AsyncTask<Context, Integer, String> {
		public static final String TAG = "AsyncSmsSendMessagesTask";

		private Context context;

		@Override
		protected String doInBackground(Context... contexts) {
			Log.i(TAG, "doInBackground");
			this.context = contexts[0];
			// logMessages(mMessages);
			transmitMessages(context);
			return null;
		}

		/**
		 * Transmits the messages stored in mMessages list. For each
		 * message a BroadcastReceiver is created to receive the
		 * status report on the message from the phone's radio.
		 * @param context
		 */
		protected void transmitMessages(final Context context) {

			mBroadcastsOutstanding = mMessages.size();

			for (int i = 0; i < mMessages.size(); i++) {
				final String message = mMessages.get(i);
				String phoneNum = mPhoneNumbers.get(i);
				final String seq = Integer.toString(i);

				PendingIntent sentPI = PendingIntent.getBroadcast(context, 0,
						new Intent(SENT + seq), 0);

				// Receiver for when the SMS is sent
				BroadcastReceiver sendReceiver = new BroadcastReceiver() {
					@Override
					public synchronized void onReceive(Context arg0, Intent arg1) {
						try {
							handleSentMessage(arg0, null, getResultCode(), seq,
									message);
							context.unregisterReceiver(this);
							Log.i(TAG, "Broadcasts outstanding  = "
									+ mBroadcastsOutstanding);
						} catch (Exception e) {
							Log.e("BroadcastReceiver",
									"Error in onReceive for msgId "
											+ arg1.getAction());
							Log.e("BroadcastReceiver", e.getMessage());
							e.printStackTrace();
						}

					}
				};
				context.registerReceiver(sendReceiver, new IntentFilter(SENT
						+ seq));

				// We need to determine how many message we need to send this
				// as.
				// The length array contains 4 result:
				// length[0] the number of Sms messages required
				// length[1] the number of 7-bit code units used
				// length[2] the number of 7-bit code units remaining
				// length[3] an indicator of the encoding code unit size
				int[] length = null;
				length = SmsMessage.calculateLength(message, true);
				Log.i(TAG, "Length - 7 bit encoding = " + length[0] + " "
						+ length[1] + " " + length[2] + " " + length[3]);
				length = SmsMessage.calculateLength(message, false);
				Log.i(TAG, "Length - 16 bit encoding = " + length[0] + " "
						+ length[1] + " " + length[2] + " " + length[3]);

				// This is where the message is actually sent. The sentPI
				// argument links the message to its receiver. 
				SmsManager smsMgr = SmsManager.getDefault();
				if (length[0] == 1) {
					// Single part message
					try {
						smsMgr.sendTextMessage(phoneNum, null, message, sentPI,
								null);
						Log.i(TAG, "SMS Sent. seq = " + seq + " msg :"
								+ message + " phone= " + phoneNum);
					} catch (IllegalArgumentException e) {
						Log.e(TAG,
								"IllegalArgumentException, probably phone number = "
										+ phoneNum);
						Toast.makeText(context, "Could not send SMS " + seq + ". Check phone number.", Toast.LENGTH_LONG).show();
						mErrorMsg = e.getMessage();
						e.printStackTrace();
						return;
					} catch (Exception e) {
						Log.e(TAG, "Exception " + e.getMessage());
						e.printStackTrace();
					}
				} else {
					// Multi-part message
					int nMessagesNeeded = length[0];
					ArrayList<String> msgList = smsMgr.divideMessage(message);
					ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>();
					for (int k = 0; k < msgList.size(); k++) {
						sentIntents.add(sentPI);
					}

					smsMgr.sendMultipartTextMessage(phoneNum, null, msgList,
							sentIntents, null);
					Log.i(TAG, "SMS Sent multipart message. seq = " + seq
							+ " msg :" + msgList.toString() + " phone= "
							+ phoneNum);
				}
			}
		}

		@Override
		protected void onCancelled() {
			Log.i(TAG, "onCancelled");
			super.onCancelled();
		}

		@Override
		protected void onPostExecute(String result) {
			Log.i(TAG, "onPostExecute, broadcasts outstanding = "
					+ mBroadcastsOutstanding);
			super.onPostExecute(result);
		}

		@Override
		protected void onPreExecute() {
			Log.i(TAG, "onPreExecute");
			super.onPreExecute();
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			Log.i(TAG, "onProgressUpdate");
			super.onProgressUpdate(values);
		}
	}
}
