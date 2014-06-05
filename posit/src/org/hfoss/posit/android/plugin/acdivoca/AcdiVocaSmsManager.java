package org.hfoss.posit.android.plugin.acdivoca;
///*
// * File: AcdiVocaSmsManager.java
// * 
// * Copyright (C) 2011 The Humanitarian FOSS Project (http://www.hfoss.org)
// * 
// * This file is part of the ACDI/VOCA plugin for POSIT, Portable Open Search 
// * and Identification Tool.
// *
// * This plugin is free software; you can redistribute it and/or modify
// * it under the terms of the GNU Lesser General Public License (LGPL) as published 
// * by the Free Software Foundation; either version 3.0 of the License, or (at
// * your option) any later version.
// * 
// * This program is distributed in the hope that it will be useful, 
// * but WITHOUT ANY WARRANTY; without even the implied warranty of 
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// * General Public License for more details.
// * 
// * You should have received a copy of the GNU LGPL along with this program; 
// * if not visit http://www.gnu.org/licenses/lgpl.html.
// * 
// */
//
//package org.hfoss.posit.android.plugin.acdivoca;
//
//import java.io.IOException;
//import java.io.StreamTokenizer;
//import java.io.StringReader;
//import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.Iterator;
//
//import org.hfoss.posit.android.R;
//import org.hfoss.posit.android.api.DbManager;
//import org.hfoss.posit.android.api.SmsService;
//
//import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
//import com.j256.ormlite.android.apptools.OrmLiteBaseListActivity;
//import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
//import com.j256.ormlite.dao.Dao;
//
//import android.app.Activity;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Message;
//import android.preference.PreferenceManager;
//import android.telephony.SmsMessage;
//import android.util.Log;
//
//public class AcdiVocaSmsManager extends BroadcastReceiver {
//	
//	public static final String TAG = "AcdiVocaSmsManager";
//	public static final String SENT = "SMS_SENT";
//	public static final String DELIVERED = "SMS_DELIVERED";
//	
//	public static final String INCOMING_PREFIX = 
//		AcdiVocaMessage.ACDI_VOCA_PREFIX 
//		+ AttributeManager.ATTR_VAL_SEPARATOR;
//
//	
//	public static final int MAX_MESSAGE_LENGTH = 140;
//	public static final int MAX_PHONE_NUMBER_LENGTH = 13;
//	public static final int MIN_PHONE_NUMBER_LENGTH = 5;
//	public static final int DONE = 0;
//	
//	private AcdiVocaDbManager dbManager;
//	
//	public int msgId = 0;
//	private static Context mContext = null;
//	private static AcdiVocaSmsManager mInstance = null; 
//	
//	private static String mAcdiVocaPhone = null;
//	private static Activity mActivity;
//	
//	private String mErrorMsg = ""; // Set to last error by BroadcastReceiver
//	
//	public AcdiVocaSmsManager()  {
//		
//	}
//	
//	public static AcdiVocaSmsManager getInstance(Activity activity){
//		mActivity = activity;
//		mInstance = new AcdiVocaSmsManager();
//		AcdiVocaSmsManager.initInstance(activity);
//		return mInstance;
//	}
//	
//	public static void initInstance(Context context) {
//		mContext = context;
//		mInstance = new AcdiVocaSmsManager();
//		
//        mAcdiVocaPhone = 
//			PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.smsPhoneKey), ""); 
//	}
//
//
//	/**
//	 * Invoked automatically when a message is received.  This will start the app
//	 * if it is not currently running.  Used for handling ACKs from AcdiVoca Modem.
//	 * 
//	 * Requires Manifest:
//	 * <uses-permission android:name="android.permission.SEND_SMS"></uses-permission>
//     * <uses-permission android:name="android.permission.RECEIVE_SMS"></uses-permission>
// 	 */
//	@Override
//	public void onReceive(Context context, Intent intent) {
//		Log.i(TAG, "Intent action = " + intent.getAction());
//		
//		Bundle bundle = intent.getExtras();
//
//		ArrayList<SmsMessage> messages = new ArrayList<SmsMessage>();
//
//		if (bundle != null) {
//			Object[] pdus = (Object[]) bundle.get("pdus");
//
//			for (Object pdu : pdus) {
//				SmsMessage message = SmsMessage.createFromPdu((byte[]) pdu);
//				messages.add(message);
//
//				String incomingMsg = message.getMessageBody();
//				String originatingNumber = message.getOriginatingAddress();
//
//				Log.i(TAG, "FROM: " + originatingNumber);
//				Log.i(TAG, "MESSAGE: " + incomingMsg);
//				int[] msgLen = SmsMessage.calculateLength(message.getMessageBody(), true);
//				Log.i(TAG, "" + msgLen[0]  + " " + msgLen[1] + " " + msgLen[2] + " " + msgLen[3]);
//				msgLen = SmsMessage.calculateLength(message.getMessageBody(), false);
//				Log.i(TAG, "" + msgLen[0]  + " " + msgLen[1] + " " + msgLen[2] + " " + msgLen[3]);
//
//				//Log.i(TAG, "Protocol = " + message.getProtocolIdentifier());
//				Log.i(TAG, "LENGTH: " + incomingMsg.length());				 
//
//				if (incomingMsg.startsWith(INCOMING_PREFIX)) {
//					handleAcdiVocaIncoming(context, incomingMsg);
//				}
//			}
//		}
//	}
//
//	/**
//	 * Handles an incoming Sms from AcdiVoca Modem. We are interested in AcdiVoca ACK messages, 
//	 * which are:
//	 * 
//	 * AV=ACK,IDS=id1&id2&id3&...&idN,..., 
//	 * 
//	 * The list of ids represent either beneficiary ids (i.e., row_ids, which were sent in 
//	 * the original message) for regular messages or they represent message ids for bulk
//	 * beneficiary update messages, in which case the id numbers are negative.  These 
//	 * messages should be marked acknowledged.
//	 * @param msg
//	 */
//	private void handleAcdiVocaIncoming(Context context, String msg) {
//		Log.i(TAG, "Processing incoming SMS: " + msg);
//		boolean isAck  = false;
//		String attrvalPairs[] = msg.split(AttributeManager.PAIRS_SEPARATOR);
//
//		// The message has the format AV=ACK,IDS=1/2/3/.../,  so just two pairs
//		for (int k = 0; k < attrvalPairs.length; k++) {
//			String attrval[] = attrvalPairs[k].split(AttributeManager.ATTR_VAL_SEPARATOR);
//			String attr = "", val = "";
//			if (attrval.length == 2) {
//				attr = attrval[0];
//				val = attrval[1];
//			} else if (attrval.length == 1) {
//				attr = attrval[0];
//			}
//
//			// If this is an ACK message,  set on the isAck flag
//			if (attr.equals(AcdiVocaMessage.ACDI_VOCA_PREFIX)
//					&& val.equals(AcdiVocaMessage.ACK)) {
//				isAck = true;
//			}
//			// If this is the list of IDs,  parse the ID numbers and update the Db
//			if (attr.equals(AcdiVocaMessage.IDS) && isAck) {
//				Log.i(TAG, attr + "=" + val);
//				processAckList(context, attr, val);
//			}
//		}
//	}
//	
//	/**
//	 * Helper method to process of list of IDs as tokens.
//	 * @param val
//	 */
//	private void processAckList(Context context, String attr, String val) {
//
//		// We use a tokenizer with a number parser so we can handle non-numeric 
//		//  data without crashing.  It skips all non-numerics as it reads the stream.
//		StreamTokenizer t = new StreamTokenizer(new StringReader(val));
//		t.resetSyntax( );
//		t.parseNumbers( );
//		try {
//
//			//  While not end-of-file, get the next token and extract number
//			int token =  t.nextToken();
//			while (token != StreamTokenizer.TT_EOF) {
//				if (token != StreamTokenizer.TT_NUMBER )  {
//					//Log.i(TAG, "Integer parser skipping token = " + token); // Skip nonnumerics
//				}
//				else {
//
//					// Construct an SmsMessage and update the Db
//					int ackId = (int)t.nval;
//					Log.i(TAG, "ACKing, ackId: " + ackId);
//					AcdiVocaMessage avMsg = null;
//					
//					// Message for bulk messages, where IDs < 0 and represent message Ids
//					if (ackId < 0)  {   // Check for bulk 
//						avMsg = new AcdiVocaMessage(
//								ackId * -1,  // For Bulks, the ackId is MsgId
//								AcdiVocaMessage.UNKNOWN_ID,  // Beneficiary Id
//								AcdiVocaMessage.MESSAGE_STATUS_ACK,
//								attr + AttributeManager.ATTR_VAL_SEPARATOR + val, // Raw message
//								"",   // SmsMessage N/A
//								"",    // Header  N/A
//								!AcdiVocaMessage.EXISTING
//						);
//					} else {
//						// Message for normal messages, where IDs > 0 and represent beneficiary IDs
//						avMsg = new AcdiVocaMessage(
//								AcdiVocaMessage.UNKNOWN_ID,  // Message Id is unknown -- Modem sends back Beneficiary Id
//								ackId,  // For non-bulks, ackId is Beneficiary Id
//								AcdiVocaMessage.MESSAGE_STATUS_ACK,
//								attr + AttributeManager.ATTR_VAL_SEPARATOR + val, // Raw message
//								"",   // SmsMessage N/A
//								"",    // Header  N/A
//								!AcdiVocaMessage.EXISTING
//						);
//					}
//					recordAckInDb(context, avMsg);   // Save the ACK in the FINDS and MESSAGES tables
//				}
//				token = t.nextToken();
//			}
//		}
//		catch ( IOException e ) {
//			Log.i(TAG,"Number format exception");
//			e.printStackTrace();
//		}
//	}
//	
//	/**
//	 * Helper method to record a received ACK for an SMS message (avMsg) in the Db.
//	 */
//	private void recordAckInDb (Context context, AcdiVocaMessage avMsg) {
//		AcdiVocaDbManager db = new AcdiVocaDbManager(context);
//		//AcdiVocaDbManager dbManager = (AcdiVocaDbManager)this.getHelper();
//		boolean success = false;
//		try {
//			int beneId = avMsg.getBeneficiaryId();
//			int msgId = avMsg.getMessageId();
//			Dao<AcdiVocaFind, Integer> daoFind = db.getAcdiVocaFindDao();
//			Dao<AcdiVocaMessage, Integer> daoMsg = db.getAcdiVocaMessageDao();
//			
//			if (beneId < 0) {  // This was a bulk message containing dossiers numbers of many beneficiaries
//				success = AcdiVocaMessage.updateStatus(daoMsg, beneId, msgId, AcdiVocaMessage.MESSAGE_STATUS_ACK);
//				if (success) 
//					Log.d(TAG, "Updated ACK, for msg_id = " +  msgId);
//				else 
//					Log.e(TAG, "Unable to process ACK, for msg_id = " +  msgId);	
//				
//				AcdiVocaMessage bulkMsg = AcdiVocaMessage.fetchById(daoMsg, msgId);
//				if (bulkMsg != null) {
//					int rows = AcdiVocaFind.updateMessageStatusForBulkMsg(daoFind, bulkMsg, msgId, AcdiVocaMessage.MESSAGE_STATUS_ACK);
//					Log.i(TAG, "Updated bulk for " + rows + " beneficiaries");
//				} else {
//					Log.e(TAG, "Unable to retrieve message with id = " + msgId);
//				}
//				
//
//			} else {
//				// First retrieve the message id from the Find and update the Find
//				AcdiVocaFind avFind = AcdiVocaFind.fetchById(daoFind, beneId);
//				if (avFind != null) {
//					msgId = avFind.message_id;
//					avFind.message_status = AcdiVocaMessage.MESSAGE_STATUS_ACK;
//					int rows = daoFind.update(avFind);
//					if (rows == 1) {
//						Log.d(TAG, "Updated ACK, for beneficiary_id = " +  beneId);
//					} else {
//						Log.e(TAG, "Unable to process ACK, for beneficiary_id = " +  beneId);						
//					}
//				} else {
//					Log.e(TAG, "Unable to process ACK, for beneficiary_id = " +  beneId);		
//				}
//				
//				// Next update the message
//				success = AcdiVocaMessage.updateStatus(daoMsg, beneId, msgId, AcdiVocaMessage.MESSAGE_STATUS_ACK);
//				if (success) 
//					Log.d(TAG, "Updated ACK, for msg_id = " +  msgId);
//				else 
//					Log.e(TAG, "Unable to process ACK, for msg_id = " +  msgId);
//			}
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
////		db.recordAcknowledgedMessage(avMsg);
//		
//	}
//	
//	/**
//	 * Checks for a validly-formatted phone number, which 
//	 * takes the form: [+]1234567890
//	 * @param number
//	 * @return
//	 */
//	private static boolean isValidPhoneString(String number) {
//		if (number.length() < MIN_PHONE_NUMBER_LENGTH
//				|| number.length() > MAX_PHONE_NUMBER_LENGTH)
//			return false;
//		
//		// Check for valid digits
//		for(int i = 0; i < number.length(); i++) {
//			if(number.charAt(i)<'0'|| number.charAt(i)>'9')
//				if(!(i==0&&number.charAt(i)=='+'))
//					return false;
//		}
//		return true;
//	}
//	
//	/**
//	 * Publicly exposed method for processing Sms messages.  It starts a thread to
//	 * handle the details. 
//	 * @param context
//	 * @param acdiVocaMsgs
//	 */
//	public void sendMessages(Context context, ArrayList<AcdiVocaMessage> acdiVocaMsgs) {
//		mContext = context;
//		
//		Log.i(TAG, "sendMessages,  n =" + acdiVocaMsgs.size());
//
//		// Build a list of messages (with updates to the Db) to pass
//		// to the Service.  Do it in a separate thread.
//
//		BuildMessagesThread thread = new BuildMessagesThread(context, 
//				new BuildMessagesHandler(), acdiVocaMsgs);
//		thread.start();				
//	}
//	
//	
//	/**
//	 * Utility method to test that the phone number preference is set before
//	 * trying to send messages. 
//	 * @param context
//	 * @return
//	 */
//	public boolean isPhoneNumberSet(Context context) {
//		String phoneNumber = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.smsPhoneKey), "");
//		Log.i(TAG, "Phone number = " + phoneNumber);
//		if (!isValidPhoneString(phoneNumber)) {
//			Log.e(TAG, "Invalid phone number " + phoneNumber);
//			return false;			
//		}
//		return true;
//	}
//	
//	public static String getPhoneNumber(Context context) {
//		String phone = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.smsPhoneKey), "");
//		return phone;
//	}
//	
//	/**
//	 * Helper method to convert the message objects into a flat list of strings.
//	 * @param context
//	 * @param acdiVocaMsgs
//	 * @return
//	 */
//	private ArrayList<String> getMessagesAsArray (Context context, ArrayList<AcdiVocaMessage> acdiVocaMsgs) {
//		ArrayList<String> messages = new ArrayList<String>();
//		AcdiVocaMessage acdiVocaMsg = null;
//		Iterator<AcdiVocaMessage> it = acdiVocaMsgs.iterator();
//		int count = 1;
//		int size = acdiVocaMsgs.size();
//		///OrmLiteBaseActivity ormLiteBaseActivity = 
//		dbManager = (AcdiVocaDbManager)((OrmLiteBaseActivity)context).getHelper(); 
//		while (it.hasNext()) {
//			acdiVocaMsg = it.next();
//			acdiVocaMsg.setNumberSlashBatchSize(count + AttributeManager.NUMBER_SLASH_SIZE_SEPARATOR + size);
//			int beneficiary_id = acdiVocaMsg.getBeneficiaryId();
////			Log.i(TAG, "To Send: " + acdiVocaMsg.getSmsMessage());
//			
//			if (!acdiVocaMsg.isExisting()) {
//				Log.i(TAG,"This is a NEW message");
//				Dao<AcdiVocaMessage, Integer> daoMsg = null;
//				Dao<AcdiVocaFind, Integer> daoFind = null;
//
//				try {
//					if (mActivity instanceof OrmLiteBaseActivity<?>) {
//						daoMsg = dbManager.getAcdiVocaMessageDao();
//						daoFind = dbManager.getAcdiVocaFindDao();
//					} else if (mActivity instanceof OrmLiteBaseListActivity<?>) {
//						daoMsg = dbManager.getAcdiVocaMessageDao();
//						daoFind = dbManager.getAcdiVocaFindDao();
//					}
//				} catch (SQLException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
////				AcdiVocaDbHelper db = new AcdiVocaDbHelper(context);
////				int msgId = (int)db.createNewMessageTableEntry(acdiVocaMsg,beneficiary_id,AcdiVocaMessage.MESSAGE_STATUS_UNSENT);
//				int msgId = AcdiVocaMessage.createMessage(daoMsg,daoFind,acdiVocaMsg,beneficiary_id,AcdiVocaMessage.MESSAGE_STATUS_UNSENT);
//				acdiVocaMsg.setMessageId(msgId);
//				acdiVocaMsg.setExisting(true);
//			}
//			messages.add(acdiVocaMsg.toString());
//			++count;
//		}
//		return messages;
//	}
//	
//	class BuildMessagesHandler extends Handler {
//		@Override
//		public void handleMessage(Message msg) {
//			if (msg.what == DONE) {
//				Log.i(TAG, "BuildMessages thread finished");
//			}
//		}
//	}
//	
//	/**
//	 * Thread to build text messages. 
//	 *
//	 */
//	class BuildMessagesThread extends Thread {
//		private Context mContext;
//		private Handler mHandler;
//		private  ArrayList<AcdiVocaMessage> mAcdiVocaMsgs;
//		
//		public BuildMessagesThread(Context context, 
//				Handler handler, ArrayList<AcdiVocaMessage> acdiVocaMsgs) {
//			mHandler = handler;
//			mContext = context;
//			mAcdiVocaMsgs = acdiVocaMsgs;
//		}
//	
//		@Override
//		public void run() {
//			// We pass the service a list of messages. It handles the rest.
//			Intent smsService = new Intent(mContext, SmsService.class);
//			ArrayList<String> messagesToSend = getMessagesAsArray(mContext, mAcdiVocaMsgs);
//
//			Log.i(TAG, "Starting background service");
//			smsService.putExtra("messages", messagesToSend);  // These messages know their msgIds
//			smsService.putExtra("phonenumber", mAcdiVocaPhone);
//			mContext.startService(smsService);
//			mHandler.sendEmptyMessage(AcdiVocaAdminActivity.DONE);
//		}
//	}
//	
//	
//}
