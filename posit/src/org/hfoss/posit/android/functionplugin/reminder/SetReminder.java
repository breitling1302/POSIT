/*
 * File: SetReminder.java
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.api.plugin.AddFindPluginCallback;
import org.hfoss.posit.android.api.plugin.ListFindPluginCallback;
import org.hfoss.posit.android.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;

/**
 * This class is used for "Location-Aware Reminder" Function Plug-in.
 * 
 * It displays a series of pop-up dialogs that ask user to input
 * information required for a reminder, including date and location.
 * This reminder is then attached to the find.
 * 
 * Protocol:  When a reminder is attached to a Find, the Find's
 * is_adhoc field is set to REMINDER_SET.  When the reminder
 * has been set by the ToDoReminderService, the field is set to
 * REMINDER_NOTIFIED.  When the notification is processed by
 * the user, the field is set to REMINDER_UNSET.
 * 
 **/
public class SetReminder extends OrmLiteBaseActivity<DbManager> 
	implements ListFindPluginCallback, AddFindPluginCallback {

	private static final String TAG = "SetReminder";

	// Pop-up Dialog IDs
	private static final int DATE_PICKER_DIALOG_ID = 0;
	private static final int ADDRESS_PICKER_DIALOG_ID = 1;
	private static final int ADDRESS_ENTER_DIALOG_ID = 2;
	private static final int ADDRESS_CONFIRM_DIALOG_ID = 3;

	// Reminder Status
	public static final int REMINDER_UNSET = 0;
	public static final int REMINDER_SET = 1;
	public static final int REMINDER_NOTIFIED = 2;

	// Pop-up Dialogs
	private DatePickerDialog datePickerDialog;
	private AlertDialog addrPickerDialog;
	private AlertDialog addrEnterDialog;
	private AlertDialog addrConfirmDialog;
	private ProgressDialog progressDialog;

	// Set which dialog we are currently at
	// Used to see which dialog the Back Key should point to
	private int currentDialog;
	// Back Key counter
	private int backKeyCounter = 0;

	// Variables passed between intents 
	private String date, year, month, day;
	private Double currentLongitude, currentLatitude;
	private Double findsLongitude, findsLatitude;

	// Determine if the Set button on Date Picker Dialog is pressed
	private boolean dateSetPressed;

	// EditText filed in Address Enter Dialog
	private EditText addressET;
	// addressURL because the text from addressET can be only retrieved once
	private String addressURL;

	// A list of addressed received from Google
	private JSONArray addressArray;

	// Bundle passed in the Intent
	protected Bundle mDbEntries;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Set the transparent blank screen as a background for dialogs
		setContentView(R.layout.blank_screen);

		// Exit the Activity if the proper settings are not enabled
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean allowReminder = prefs.getBoolean("allowReminderKey",true);
		boolean allowGeoTag = prefs.getBoolean("geotagKey", true);
		// If the user allows, set the "Set Reminder" menu option
		if (!allowReminder || !allowGeoTag) {
			Toast.makeText(this, "Sorry. Geotagging and Allow Reminder settings must "
					+ " be enabled to run the Set Reminder Activity.", Toast.LENGTH_LONG).show();
			finish();
		}

		mDbEntries = getIntent().getParcelableExtra("DbEntries");

		dateSetPressed = false;

		// Get intent passed in from FindActivity
		Bundle bundle = getIntent().getExtras();
		date = bundle.getString("Date");

		// Initialize variables for Date Picker Dialog
		year = date.substring(0, 4);
		month = date.substring(5, 7);
		day = date.substring(8, 10);


		// Initialize variables for longitude and latitude		
		findsLongitude = bundle.getDouble("FindsLongitude");
		findsLatitude = bundle.getDouble("FindsLatitude");

		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		Location netLocation = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		Location gpsLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		Location loc = null;

		if (gpsLocation != null) {
			loc = gpsLocation;
		} else {
			loc = netLocation;
		}
		if (loc != null) {
			currentLongitude = loc.getLongitude();
			currentLatitude = loc.getLatitude();
		}

		showDatePickerDialog();	
	}

	/* Create and show Date Picker Dialog */
	private void showDatePickerDialog() {
		dateSetPressed = false;
		// Set the current dialog
		currentDialog = DATE_PICKER_DIALOG_ID;

		// Parse year, month, and day for Date Picker Dialog
		int mYear = Integer.parseInt(year);
		int mMonth = Integer.parseInt(month) - 1;
		int mDay = Integer.parseInt(day);

		// Build Date Picker Dialog
		datePickerDialog = new myDatePickerDialog(this, mDateSetListener,
				mYear, mMonth, mDay);
		datePickerDialog.setTitle("Step 1: Choose a date");

		// Set Listeners
		datePickerDialog.setOnDismissListener(mDateDismissListener);
		datePickerDialog.setOnKeyListener(mBackKeyListener);

		// Show Date Picker Dialog
		datePickerDialog.show();
	}

	/* Create and show Address Picker Dialog */
	private void showAddrPickerDialog() {
		// Set the current dialog
		currentDialog = ADDRESS_PICKER_DIALOG_ID;

		// Set the options in Address Picker Dialog
		final CharSequence[] items = {"Use Current Location", "Use Find's Location", "Enter Location Name / Landmark Address "};

		// Build Address Picker Dialog
		AlertDialog.Builder addrPickerBuilder = new AlertDialog.Builder(this);
		addrPickerBuilder.setTitle("Step 2: Choose an address");
		addrPickerBuilder.setItems(items, mAddrPickerOnClickListener);

		// Set Listeners
		addrPickerBuilder.setOnKeyListener(mBackKeyListener);
		// Finish the activity when the user presses cancel
		addrPickerBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				setResult(RESULT_OK);
				finish();
			}
		});

		// Show Address Picker Dialog
		addrPickerDialog = addrPickerBuilder.create();
		addrPickerDialog.show();
	}

	/* Create and show Address Enter Dialog */
	private void showAddrEnterDialog() {
		// Set the current dialog
		currentDialog = ADDRESS_ENTER_DIALOG_ID;

		// Build Address Enter Dialog
		AlertDialog.Builder addrEnterBuilder = new AlertDialog.Builder(this);
		addrEnterBuilder.setTitle("Step 3: Enter Location Name / Address");

		// Initialize EditText for user to type the desired address
		addressET = new EditText(this);
		addressET.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
		addrEnterBuilder.setView(addressET);

		// Set Listeners
		addrEnterBuilder.setPositiveButton("Search", mAddrEnterOnClickListner);
		// Finish the activity when the user presses cancel
		addrEnterBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				setResult(RESULT_OK);
				finish();
			}
		});
		addrEnterBuilder.setOnKeyListener(mBackKeyListener);

		// Show Address Enter Dialog
		addrEnterDialog = addrEnterBuilder.create();
		addrEnterDialog.show();
	}

	/* Create and show Address Confirm Dialog */
	private void showAddrConfirmDialog() {
		// Set the current dialog
		currentDialog = ADDRESS_CONFIRM_DIALOG_ID;

		// Build Address Confirm Dialog
		AlertDialog.Builder addrConfirmBuilder = new AlertDialog.Builder(this);
		addrConfirmBuilder.setTitle("Step 4: Did you mean...");

		// Build the possible addresses list from results returned by Google URL request
		ArrayList<String> possibleAddr = new ArrayList<String>();

		for (int i = 0; i < addressArray.length(); i++) {
			try {
				String receivedAddr = addressArray.getJSONObject(i).getString("formatted_address").replace(", ", ",\n");
				possibleAddr.add(receivedAddr);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		final CharSequence[] possibleAddrChar = possibleAddr.toArray(new CharSequence[possibleAddr.size()]);
		addrConfirmBuilder.setItems(possibleAddrChar, mAddrConfirmOnClickListener);

		// Set Listeners
		addrConfirmBuilder.setOnKeyListener(mBackKeyListener);
		// Re-display Address Enter Dialog when the user presses retry
		addrConfirmBuilder.setPositiveButton("Retry", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				showAddrEnterDialog();
				Toast.makeText(getApplicationContext(), "To get better results, please type a more specific name or address "
						+ "with CITY NAME included.", Toast.LENGTH_LONG).show();
			}
		});
		// Finish the activity when the user presses cancel
		addrConfirmBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				setResult(RESULT_OK);
				finish();
			}
		});

		// Show Address Confirm Dialog
		addrConfirmDialog = addrConfirmBuilder.create();
		addrConfirmDialog.show();
	}

	/* 
	 * Listen to when the Back key is pressed
	 * and return to the correct previous dialog
	 */
	private DialogInterface.OnKeyListener mBackKeyListener =
		new DialogInterface.OnKeyListener() {
		public boolean onKey (DialogInterface dialog, int keyCode, KeyEvent event) {
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				backKeyCounter++;
				if (backKeyCounter % 2 == 0) {
					if (currentDialog == DATE_PICKER_DIALOG_ID) {
						datePickerDialog.dismiss();
						setResult(RESULT_OK);
						finish();
					} else if (currentDialog == ADDRESS_PICKER_DIALOG_ID) {
						addrPickerDialog.dismiss();
						showDatePickerDialog();
					} else if (currentDialog == ADDRESS_ENTER_DIALOG_ID) {
						addrEnterDialog.dismiss();
						showAddrPickerDialog();
					}  else if (currentDialog == ADDRESS_CONFIRM_DIALOG_ID) {
						addrConfirmDialog.dismiss();
						Toast.makeText(getApplicationContext(), "To get better results, please type a more specific name or address "
								+ "with CITY NAME included.", Toast.LENGTH_LONG).show();
						showAddrEnterDialog();
					}
				}
				return true;
			} else {
				return false;
			}
		}
	};

	/* Customized Date Picker Dialog with static title */
	private class myDatePickerDialog extends DatePickerDialog {

		private myDatePickerDialog(Context context,	OnDateSetListener callBack,
				int year, int monthOfYear, int dayOfMonth) {
			super(context, callBack, year, monthOfYear, dayOfMonth);
		}

		/* Set the title to be static and not changed into the date picked */
		public void onDateChanged(DatePicker view, int year,
				int month, int day) {
			setTitle("Step 1: Choose a date");
		}

	}

	/* 
	 * Listen to when the Set button on Date Picker Dialog is  
	 * pressed and set the variable date to the date picked
	 */
	private DatePickerDialog.OnDateSetListener mDateSetListener =
		new DatePickerDialog.OnDateSetListener() {

		public void onDateSet(DatePicker view, int year, 
				int monthOfYear, int dayOfMonth) {
			// For month and day less than 10, add 0 in front for display purposes
			String monthStr = (monthOfYear + 1 < 10) ?
					"0" + Integer.toString(monthOfYear + 1) : Integer.toString(monthOfYear + 1);
					String dayStr = (dayOfMonth < 10) ?
							"0" + Integer.toString(dayOfMonth) : Integer.toString(dayOfMonth);
							date = Integer.toString(year) + "/" + monthStr + "/" + dayStr;

							dateSetPressed = true;
		}
	};

	/*
	 * Listen to when Date Picker Dialog is dismissed
	 * and show the Address Picker Dialog or finish
	 * the activity based on user input
	 */
	private DatePickerDialog.OnDismissListener mDateDismissListener =
		new DatePickerDialog.OnDismissListener() {
		public void onDismiss(DialogInterface dialog) {
			if (dateSetPressed) {
				showAddrPickerDialog();
			}
			else {
				setResult(RESULT_OK);
				finish();
			}	
		}
	};

	/*
	 * Listen to when an item on Address Picker Dialog is pressed, finish
	 * the activity or show Address Enter Dialog based on user input
	 */
	private DialogInterface.OnClickListener mAddrPickerOnClickListener =
		new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int item) {
			Bundle bundle = new Bundle();
			Intent newIntent = new Intent();
			if (item == 0) {
				// Use Current Address
				// Set the intent back to FindActivity
				bundle.putString(Find.TIME, date);
				bundle.putDouble(Find.LONGITUDE, currentLongitude);
				bundle.putDouble(Find.LATITUDE, currentLatitude);
				bundle.putInt(Find.IS_ADHOC, REMINDER_SET);
				newIntent.putExtras(bundle);
				setResult(RESULT_OK, newIntent);
				finish();
			} else if (item == 1) {
				// Use Current Address
				// Set the intent back to FindActivity
				bundle.putString(Find.TIME, date);
				bundle.putDouble(Find.LONGITUDE, findsLongitude);
				bundle.putDouble(Find.LATITUDE, findsLatitude);
				bundle.putInt(Find.IS_ADHOC, REMINDER_SET);
				newIntent.putExtras(bundle);
				setResult(RESULT_OK, newIntent);
				finish();
			} else if (item == 2) {
				// Enter Address
				// Show Address Enter Dialog
				showAddrEnterDialog();
			}
		}
	};

	/* 
	 * Listen to when the Search button on Address Enter Dialog is pressed
	 * and send request to search for the address entered in Google
	 */
	private DialogInterface.OnClickListener mAddrEnterOnClickListner =
		new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			// Get the address from the textEdit filed
			addressURL = addressET.getText().toString().replaceAll(" ", "%20");

			if (addressURL.length() == 0) {
				// If nothing is typed, ask the user to type again
				Toast.makeText(getApplicationContext(), "Please type a location name / address.",
						Toast.LENGTH_LONG).show();
				showAddrEnterDialog();
			} else {
				// Display Progress Dialog and search for entered address using a thread
				progressDialog = ProgressDialog.show(SetReminder.this, "", 
						"Retrieving Location Data...", true, false);
				new Thread (new Runnable() {
					public void run() {
						retrieveLocation();
						Message msg = new Message();
						msg.obj = "DISMISS PROGRESS DIALOG";
						handler.sendMessage(msg);
					}
				}).start();
			}
		}

	};

	/*
	 * Listen to when an item on Confirm Address Dialog is pressed
	 * and set the selected location as find reminder's location
	 */
	private DialogInterface.OnClickListener mAddrConfirmOnClickListener =
		new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int item) {
			// Set the intent back to FindActivity
			Bundle bundle = new Bundle();
			Intent newIntent = new Intent();
			bundle.putString("Date", date);
			Double lng = new Double(0);
			Double lat = new Double(0);
			// Get the user selected location coordinates
			try {
				lng = addressArray.getJSONObject(item)
				.getJSONObject("geometry").getJSONObject("location")
				.getDouble("lng");
				lat = addressArray.getJSONObject(item)
				.getJSONObject("geometry").getJSONObject("location")
				.getDouble("lat");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			bundle.putDouble(Find.LONGITUDE, lng);
			bundle.putDouble(Find.LATITUDE, lat);
			bundle.putInt(Find.IS_ADHOC, REMINDER_SET);
			newIntent.putExtras(bundle);
			setResult(RESULT_OK, newIntent);
			finish();
		}
	};

	/*
	 * Handler to dismiss the progress dialog
	 * and inform the user of possible errors
	 */
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.obj.equals("DISMISS PROGRESS DIALOG")) {
				progressDialog.dismiss();
			} else if (msg.obj.equals("SHOW ADDRESS ENTER DIALOG - FAILED")) {
				Toast.makeText(getApplicationContext(), "Location retrieval " +
						"failed. Please try again", Toast.LENGTH_LONG).show();
				showAddrEnterDialog();
			} else if (msg.obj.equals("SHOW ADDRESS ENTER DIALOG - NO RESULTS")) {
				Toast.makeText(getApplicationContext(), "No results returned. " +
						"Please type a more specific name or address with"
						+ " CITY NAME included.", Toast.LENGTH_LONG).show();
				showAddrEnterDialog();
			} else if (msg.obj.equals("SHOW ADDRESS CONFIRM DIALOG")) {
				showAddrConfirmDialog();
			}
		}
	};

	/*
	 * Translate entered address into longitude and latitude
	 * through Google URL request and parsing JSONArray
	 */
	private void retrieveLocation() {
		// Build HTTP request
		StringBuilder builder = new StringBuilder();
		HttpClient client = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet("http://maps.google."
				+ "com/maps/api/geocode/json?address=" + addressURL
				+ "&sensor=false");

		// Send HTTP request
		try {
			HttpResponse response = client.execute(httpGet);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();

			if (statusCode == 200) {
				// Read the returned content into a string
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(content));
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
			} else {
				// Something is wrong, inform user of the error
				Log.e(TAG, "Failed to download file");
				Toast.makeText(getApplicationContext(), "Location retrieval failed. Please try again",
						Toast.LENGTH_LONG).show();
				Message msg = new Message();
				msg.obj = "SHOW ADDRESS ENTER DIALOG";
				handler.sendMessage(msg);
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Create the JSONObject for parsing the string returned
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject = new JSONObject(builder.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}

		// Parse the string returned into JSONObject
		try {
			addressArray = (JSONArray) jsonObject.get("results");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (addressArray == null) {
			// Nothing returned, inform the user of the error
			Message msg = new Message();
			msg.obj = "SHOW ADDRESS ENTER DIALOG - FAILED";
			handler.sendMessage(msg);
		} else if (addressArray.length() == 0) {
			// No match found, inform the user of the error
			Message msg = new Message();
			msg.obj = "SHOW ADDRESS ENTER DIALOG - NO RESULTS";
			handler.sendMessage(msg);
		} else if (addressArray.length() == 1) {
			// Exact one result returned
			// Set the intent back to FindActivity
			Bundle bundle = new Bundle();
			Intent newIntent = new Intent();
			bundle.putString(Find.TIME, date);
			Double lng = new Double(0);
			Double lat = new Double(0);
			// Parse the JSONObject to get the longitude and latitude
			try {
				lng = addressArray.getJSONObject(0)
				.getJSONObject("geometry").getJSONObject("location")
				.getDouble("lng");
				lat = addressArray.getJSONObject(0)
				.getJSONObject("geometry").getJSONObject("location")
				.getDouble("lat");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			bundle.putDouble(Find.LONGITUDE, lng);
			bundle.putDouble(Find.LATITUDE, lat);
			bundle.putInt(Find.IS_ADHOC, REMINDER_SET);
			newIntent.putExtras(bundle);
			setResult(RESULT_OK, newIntent);
			finish();
		} else {
			// More than one results returned
			// Show Address Confirm Dialog for user confirmation
			Message msg = new Message();
			msg.obj = "SHOW ADDRESS CONFIRM DIALOG";
			handler.sendMessage(msg);
		}
	}

	/**
	 * Attaches the alarmIcon to all Finds in ListView that currently have an
	 * associated Reminder, which is indicated by the Find's
	 * is_adhoc field being set to REMINDER_SET or REMINDER_NOTIFIED.
	 * 
	 * @param context -- the context from which this method is called
	 * @param find -- the current Find
	 * @param view -- the calling activity's contentView, which is needed
	 * to access the UI's sub-views.
	 * 
	 */
	public void listFindCallback(Context context, Find find, View view) {
		Log.i(TAG, "listFindCallback");	

		if (find.getIs_adhoc() == REMINDER_SET || find.getIs_adhoc() == this.REMINDER_NOTIFIED) {
			ImageView alarmIcon = new ImageView(context);
			alarmIcon.setImageResource(R.drawable.reminder_alarm);
			RelativeLayout rl = (RelativeLayout) view.findViewById(R.id.list_row_rl);
			RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(25, 25);
			lp.addRule(RelativeLayout.BELOW, R.id.status);
			lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			lp.setMargins(0, 6, 0, 0);
			rl.addView(alarmIcon, lp);
			alarmIcon.setVisibility(ImageView.VISIBLE);		
		}
	}

	/**
	 * Invoked when the Reminder plugin is selected from the FindActivity menu. Used
	 * here to retrieve the Find's lat, long, and date from the FindActivity view.
	 * 
	 * @param context, the calling Activity
	 * @param find, the current Find
	 * @param view, the FindActivity's content view
	 * @param intent, the Intent that is passed to the menu activity
	 */
	public void menuItemSelectedCallback(Context context, Find find, View view, Intent intent) {
		Bundle bundle = new Bundle();
		TextView tv = (TextView) view.findViewById(R.id.timeValueTextView);
		bundle.putString("Date", tv.getText().toString());

		tv = (TextView) view.findViewById(R.id.longitudeValueTextView);
		bundle.putDouble("FindsLongitude", Double.parseDouble(tv.getText().toString()));
		tv = (TextView) view.findViewById(R.id.latitudeValueTextView);
		bundle.putDouble("FindsLatitude", Double.parseDouble(tv.getText().toString()));
		intent.putExtras(bundle);
	}

	/**
	 * Called from FindActivity.onActivityResult(). Used to 
	 * update the Find's View, specifically the date, lat, long,
	 * and alarm icon fields.
	 * 
	 * @param context, the calling Activity
	 * @param find, the current Find
	 * @param view, the FindActivity's content view
	 * @param intent, the Intent that is passed to the menu activity
	 */
	public void onActivityResultCallback(Context context, Find find, View view,Intent intent) {
		Log.i(TAG, "onActivityResultCallbac");
		// Intent is NOT null, meaning it includes reminder
		// date and location information set by the user
		if (intent != null) {
			Bundle bundle = intent.getExtras();

			// Get date, longitude, and latitude
			String date = bundle.getString(Find.TIME);
			Double longitude = bundle.getDouble(Find.LONGITUDE);
			Double latitude = bundle.getDouble(Find.LATITUDE);

			TextView tv = (TextView) view.findViewById(R.id.isAdhocTextView);

			Integer is_adhoc = bundle.getInt(Find.IS_ADHOC);
			Log.i(TAG, "is_adhoc = " + is_adhoc);
			if (tv != null) {
				Log.i(TAG, "Setting isAdhocTextView to " + is_adhoc);
				tv.setText("" + is_adhoc);
			}

			// Display user specified longitude and latitude
			tv = (TextView) view.findViewById(R.id.longitudeValueTextView);
			tv.setText(String.valueOf(longitude));
			tv = (TextView) view.findViewById(R.id.latitudeValueTextView);
			tv.setText(String.valueOf(latitude));

			// Remove the old row that displays time and replace it
			// with a new row that include an alarm clock icon to
			// visually indicate this find has a reminder attached
			ViewGroup parent = (ViewGroup) view.findViewById(
					R.id.timeValueTextView).getParent();
			parent.removeAllViews();
			ImageView alarmIcon = new ImageView(context);
			alarmIcon.setImageResource(R.drawable.reminder_alarm);
			TableRow.LayoutParams lp1 = new TableRow.LayoutParams(
					30, 30);
			lp1.setMargins(0, 6, 80, 0);
			parent.addView(alarmIcon, lp1);
			TextView mCloneTimeTV = new TextView(context);
			mCloneTimeTV.setId(R.id.timeValueTextView);
			mCloneTimeTV.setText(date);
			mCloneTimeTV.setTextSize(12);
			TextView mTimeTV = (TextView)view.findViewById(R.id.timeValueTextView);
			mTimeTV = mCloneTimeTV;
			TableRow.LayoutParams lp2 = new TableRow.LayoutParams();
			lp2.setMargins(6, 6, 0, 0);
			parent.addView(mTimeTV, lp2);
		}
	}

	public void displayFindInViewCallback(Context context, Find find, View view) {
		// TODO Auto-generated method stub
	}

	public void afterSaveCallback(Context context, Find find, View view,
			boolean isSaved) {
		// TODO Auto-generated method stub
		
	}
	
	public void finishCallback(Context context, Find find, View view) {
		// TODO Auto-generated method stub
	}
}
