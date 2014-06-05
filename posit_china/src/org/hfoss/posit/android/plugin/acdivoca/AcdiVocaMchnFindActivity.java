/* File: AcdiVocaFindActivity.java
 * 
 * Copyright (C) 2011 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of the ACDI/VOCA plugin for POSIT, Portable Open Search 
 * and Identification Tool.
 *
 * This plugin is free software; you can redistribute it and/or modify
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
package org.hfoss.posit.android.plugin.acdivoca;

import java.sql.SQLException;
import java.util.Calendar;

import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.LocaleManager;
import org.hfoss.posit.android.api.activity.FindActivity;
import org.hfoss.posit.android.api.activity.SettingsActivity;
import org.hfoss.posit.android.R;

import com.j256.ormlite.dao.Dao;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.DatePicker.OnDateChangedListener;

/**
 * Handles Finds for AcdiVoca Mobile App.
 * 
 */
public class AcdiVocaMchnFindActivity extends FindActivity implements OnDateChangedListener, 
	TextWatcher, OnItemSelectedListener { //, OnKeyListener {
	public static final String TAG = "AcdiVocaAddActivity";

	private static final int CONFIRM_EXIT = 0;

	private AcdiVocaDbManager dbManager;
	
	private boolean isProbablyEdited = false;   // Set to true if user edits a datum
	private String mAction = "";
	private int mFindId = 0;
	private Button mSaveButton;
	ContentValues mSavedStateValues = null;
	int mCurrentViewId;  // Used to distinguish edit from no-edit mode. 
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
		dbManager = (AcdiVocaDbManager)getHelper();
		isProbablyEdited = false;
	}

	/**
	 * Inflates the Apps menus from a resource file.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(TAG, "onCreateOptionsMenu");
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.acdivoca_menu_add, menu);
		return true;
	}
	
	
	/**
	 * Localizes already created menu items.
	 */
	@Override	
	public boolean onPrepareOptionsMenu(Menu menu) {
		
		// Re-inflate to force localization.
		Log.i(TAG, "onPrepareOptionsMenu");
		menu.clear();
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.acdivoca_menu_add, menu);
		
		return super.onPrepareOptionsMenu(menu);
	}

	/**
	 * Implements the requested action when user selects a menu item.
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Log.i(TAG, "onMenuItemSelected");
		switch (item.getItemId()) {
		case R.id.settings_menu_item:
			startActivity(new Intent(this, SettingsActivity.class));
			break;
		}
		return true;
	}


	@Override
	protected void onPause() {
		Log.i(TAG, "onPause");
		super.onPause();
	}

	/**
	 * Methods to saved and restore state if the user hits home or times out or
	 * hits the power button. 
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
	Log.i(TAG, "onRestoreInstanceState");
		mSavedStateValues = (ContentValues) savedInstanceState.get("savedstate");
		isProbablyEdited = savedInstanceState.getBoolean("isprobablyEdited");
		this.displayContentInView(mSavedStateValues);
		mSavedStateValues = null;
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.i(TAG, "onSaveInstanceState");
		
		// Don't bother saving state if we're in no-edit mode.
		if (mCurrentViewId == R.layout.acdivoca_health_beneficiary_noedit)
			return;
		//mSavedStateValues = this.retrieveContentFromView();
		outState.putParcelable("savedstate", mSavedStateValues);
		outState.putBoolean("isprobablyEdited",this.isProbablyEdited);
		super.onSaveInstanceState(outState);
	}
	
	/**
	 * 
	 */
	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume, isProbablyEdited= " + isProbablyEdited);

		LocaleManager.setDefaultLocale(this);  // Locale Manager should be in API

		if (this.mSavedStateValues != null) {
			Log.i(TAG, "onResume, restoring instance state ");
			this.displayContentInView(mSavedStateValues);
			mSavedStateValues = null;
			initializeListeners();
		} else {

			setContentView(R.layout.acdivoca_registration);  // Should be done after locale configuration
			mCurrentViewId = R.layout.acdivoca_registration;
			
			// Listen for clicks on radio buttons, edit texts, spinners, etc.
			initializeListeners();

			// for EDIT mode
			final Intent intent = getIntent();
			mAction = intent.getAction();
			if (mAction != null && mAction.equals(Intent.ACTION_EDIT)) {
				displayAsUneditable();
				isProbablyEdited = false; // In EDIT mode, initialize after filling in data
				mSaveButton.setEnabled(false);
			}

			// for INSERT mode, do nothing
			if (mAction != null && mAction.equals(Intent.ACTION_INSERT)){
				Log.i(TAG,"############################################");
				Log.i(TAG,"you are now in insert");
				if(intent.getExtras() != null){
				}
			}
		}
	}
	
	/**
	 * Helper method to create listener for radio buttons, etc.
	 */
	protected void initializeListeners() {
		mSaveButton = ((Button)findViewById(R.id.saveToDbButton));
		mSaveButton.setOnClickListener(this);

		((RadioButton)findViewById(R.id.femaleRadio)).setOnClickListener(this);
		((RadioButton)findViewById(R.id.maleRadio)).setOnClickListener(this);
		((RadioButton)findViewById(R.id.malnourishedRadio)).setOnClickListener(this);
		((RadioButton)findViewById(R.id.inpreventionRadio)).setOnClickListener(this);
		((RadioButton)findViewById(R.id.expectingRadio)).setOnClickListener(this);
		((RadioButton)findViewById(R.id.nursingRadio)).setOnClickListener(this);
		((RadioButton)findViewById(R.id.radio_motherleader_yes)).setOnClickListener(this);
		((RadioButton)findViewById(R.id.radio_motherleader_no)).setOnClickListener(this);
		((RadioButton)findViewById(R.id.radio_visit_yes)).setOnClickListener(this);
		((RadioButton)findViewById(R.id.radio_visit_no)).setOnClickListener(this);
		((RadioButton)findViewById(R.id.radio_yes_participating_agri)).setOnClickListener(this);
		((RadioButton)findViewById(R.id.radio_no_participating_agri)).setOnClickListener(this);

		// Listen for text changes in edit texts and set the isEdited flag
		((EditText)findViewById(R.id.firstnameEdit)).addTextChangedListener(this);
		((EditText)findViewById(R.id.lastnameEdit)).addTextChangedListener(this);
		((EditText)findViewById(R.id.addressEdit)).addTextChangedListener(this);
		((EditText)findViewById(R.id.inhomeEdit)).addTextChangedListener(this);
		((EditText)findViewById(R.id.responsibleIfChildEdit)).addTextChangedListener(this);
		((EditText)findViewById(R.id.responsibleIfMotherEdit)).addTextChangedListener(this);
		((EditText)findViewById(R.id.give_name)).addTextChangedListener(this);

		// Initialize the DatePicker and listen for changes
		Calendar calendar = Calendar.getInstance();

		((DatePicker)findViewById(R.id.datepicker)).init(
				calendar.get(Calendar.YEAR),
				calendar.get(Calendar.MONTH), 
				calendar.get(Calendar.DAY_OF_MONTH), this);

		((Spinner)findViewById(R.id.healthcenterSpinner)).setOnItemSelectedListener(this);
		((Spinner)findViewById(R.id.distributionSpinner)).setOnItemSelectedListener(this);


	}
	
	/**
	 * Allows editing of editable data for existing finds.  For existing finds, 
	 * we retrieve the Find's data from the DB and display it in a TextView. The
	 * Find's location and time stamp are not updated.
	 */
	private void displayAsUneditable() {
		Log.i(TAG, "doEditAction");
		mFindId = (int) getIntent().getLongExtra(AcdiVocaFind.ORM_ID, 0); 
		Log.i(TAG,"Find id = " + mFindId);

		AcdiVocaFind avFind = null; 
		try {
			avFind = this.dbManager.getAcdiVocaFindDao().queryForId(mFindId);
			if (avFind != null) {
				ContentValues values = avFind.toContentValues();
				displayContentUneditable(values);
			} else {
				Log.e(TAG, "Error: Unable to retrieve Find, id = " + mFindId);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * saveText method
	 * Retrieves value from the given id in view
	 * save the content
	 * @param c is ContentValues datatype to store the content
	 * @param id is id in view
	 * @param tag is tag name to be stored in ContentValues c 
	 * store the corresponding values into c
	 */
	private void putTextResult(ContentValues c,int id, String tag){
		EditText eText = (EditText)findViewById(id);
		String value = "";
		if (eText != null && eText.getText()!=null){
			value = eText.getText().toString();
		}
		c.put(tag, value);
	}

	/**
	 * saveRadio method to handlle radio group with two options to chooose
	 * @param c is ContentValues
	 * @param id1 is first choice id
	 * @param id2 is second choice id 
	 * @param tag is the tag name to be stored in param c
	 * @param val1 is to check the looked up value from c
	 * @param val2 is to check the looked up value from c
	 * store the corresponding values into the c
	 */
	private void putRadioResult(ContentValues c,int id1, int id2, String tag, String val1, String val2){
		RadioButton rb1 = (RadioButton)findViewById(id1);
		RadioButton rb2 = (RadioButton)findViewById(id2);
		String value = "";
		if (rb1 != null && rb1.isChecked()){
			value = val1;
		}
		if (rb2 != null && rb2.isChecked()){
			value = val2;
		}
		c.put(tag, value);
	}
	/**
	 * Retrieves values from the View fields and stores them as <key,value> pairs in a ContentValues.
	 * This method is invoked from the Save menu item.  
	 * @return The ContentValues hash table.
	 */
	protected Find retrieveContentFromView() {
		Log.i(TAG, "retrieveContentFromView");
		ContentValues result = new ContentValues();
		String value = "";

		// Retrieving TYPE
		final Intent intent = getIntent();
		int x = intent.getIntExtra(AcdiVocaFind.TYPE, 0);
		if (x == AcdiVocaFind.TYPE_MCHN)
			result.put(AcdiVocaFind.TYPE, AcdiVocaFind.TYPE_MCHN);
		
		// Retrieving First name, last name, locality
		putTextResult(result,R.id.firstnameEdit,AcdiVocaFind.FIRSTNAME);
		putTextResult(result,R.id.lastnameEdit,AcdiVocaFind.LASTNAME);
		putTextResult(result,R.id.addressEdit,AcdiVocaFind.ADDRESS);

		// Retrieving DOB 
		
		//value = mMonth + "/" + mDay + "/" + mYear;
		DatePicker picker = ((DatePicker)findViewById(R.id.datepicker));
		value = picker.getYear() + "/" + picker.getMonth() + "/" + picker.getDayOfMonth();
		Log.i(TAG, "Date = " + value);
		result.put(AcdiVocaFind.DOB, value);
		
		// Retrieving sex, number of people in home
		putRadioResult(result,R.id.femaleRadio,R.id.maleRadio,AcdiVocaFind.SEX, AcdiVocaFind.FEMALE, AcdiVocaFind.MALE);				
		putTextResult(result,R.id.inhomeEdit,AcdiVocaFind.HOUSEHOLD_SIZE);
		
		// Retrieving DISTRIBUTION POST from Spinner      
		String spinnerStr = "";
		Spinner spinner = (Spinner)findViewById(R.id.distributionSpinner);
		if (spinner != null) {
			spinnerStr = (String) spinner.getSelectedItem();
			result.put(AcdiVocaFind.DISTRIBUTION_POST, AttributeManager.getMapping(spinnerStr));
		}
		
		// Retrieving Alternate Collector (relative_1)
		RadioButton rb = (RadioButton)findViewById(R.id.malnourishedRadio);
		RadioButton rb2 = (RadioButton)findViewById(R.id.inpreventionRadio);
		if (rb.isChecked() || rb2.isChecked()){
			putTextResult(result,R.id.responsibleIfChildEdit,AcdiVocaFind.RELATIVE_1);
		}
		rb = (RadioButton)findViewById(R.id.expectingRadio);
		rb2 = (RadioButton)findViewById(R.id.nursingRadio);
		if (rb.isChecked() || rb2.isChecked()){
			putTextResult(result,R.id.responsibleIfMotherEdit,AcdiVocaFind.RELATIVE_1);
		}
		
		// Retrieve the Beneficiary's category (4 exclusive radio buttons)
		String category = "";
		rb = (RadioButton)findViewById(R.id.malnourishedRadio);
		if (rb.isChecked()) {
			category = AcdiVocaFind.MALNOURISHED;
		}
		rb = (RadioButton)findViewById(R.id.inpreventionRadio);
		if (rb.isChecked()){
			category = AcdiVocaFind.PREVENTION;
		}
		rb = (RadioButton)findViewById(R.id.expectingRadio);
		if (rb.isChecked()) {
			category = AcdiVocaFind.EXPECTING;
		}
		rb = (RadioButton)findViewById(R.id.nursingRadio);
		if (rb.isChecked()){
			category = AcdiVocaFind.NURSING;
		}
        result.put(AcdiVocaFind.BENEFICIARY_CATEGORY, category);
        	
        // Retrieving Mother leader questions
        putRadioResult(result,R.id.radio_motherleader_yes,R.id.radio_motherleader_no,AcdiVocaFind.Q_MOTHER_LEADER,AcdiVocaFind.TRUE,AcdiVocaFind.FALSE);
		putRadioResult(result,R.id.radio_visit_yes, R.id.radio_visit_no,AcdiVocaFind.Q_VISIT_MOTHER_LEADER,AcdiVocaFind.TRUE,AcdiVocaFind.FALSE);
		putRadioResult(result,R.id.radio_yes_participating_agri,R.id.radio_no_participating_agri,AcdiVocaFind.Q_PARTICIPATING_AGRI,AcdiVocaFind.TRUE,AcdiVocaFind.FALSE);

		// Retrieving Agri Participant (relative_2)
		RadioButton acdiAgriRB = (RadioButton)findViewById(R.id.radio_yes_participating_agri);
		if (acdiAgriRB.isChecked()) {
			putTextResult(result,R.id.give_name,AcdiVocaFind.RELATIVE_2);
		}
		if (!acdiAgriRB.isChecked())	{
			String none="";
			result.put(AcdiVocaFind.RELATIVE_2, none);
		}
		
		//return result;
		return new AcdiVocaFind();
	}
	
	/**
	 * setTextView method
	 * @param c is contentValues
	 * @param id is id of the field
	 * @param label is label to be added to
	 * @param key is the text to be looked up
	 * set the text on the view
	 */
	private void setTextView(ContentValues c, int id, int label,String key){
		TextView tv = ((TextView) findViewById(id));
		String val = c.getAsString(key);
		if (val != null) 
			tv.setText(getString(label) + ": " +  val);
		else
			tv.setText(getString(label)+": "+"");
	}
	/**
	 * setDistoTextView method
	 * @param c is contentValues
	 * @param id is id of the field
	 * @param label is label to be added to
	 * @param key is the text to be looked up
	 * set the text on the view
	 */
	private void setDistroTextView(ContentValues c, int id, int label,String key){
		TextView tv = ((TextView) findViewById(id));
		String val = AttributeManager.getMapping(c.getAsString(key));
		if(val!=null && label != R.string.participating_acdivoca && label != R.string.give_name)
			tv.setText(getString(label) + ": " +  val);
		else if (val == null)
			tv.setText(getString(label) + ": " + "");	
		else
			tv.setText(": "+"");
	}
    /**
     * Displays the content as uneditable labels -- default view.
     * @param values
     */
	private void displayContentUneditable(ContentValues values) {

		if (values != null){
			this.setContentView(R.layout.acdivoca_health_beneficiary_noedit);
			mCurrentViewId = R.layout.acdivoca_health_beneficiary_noedit;
			
			((Button)findViewById(R.id.editFind)).setOnClickListener(this);

			findViewById(R.id.unedit).setVisibility(View.VISIBLE);

			setTextView(values, R.id.first_label, R.string.firstname, AcdiVocaFind.FIRSTNAME);
			setTextView(values, R.id.last_label, R.string.lastname, AcdiVocaFind.LASTNAME);	
			setTextView(values, R.id.address_label, R.string.address, AcdiVocaFind.ADDRESS);
			
			String date = values.getAsString(AcdiVocaFind.DOB);
			Log.i(TAG,"display DOB = " + date);
			int yr=0, mon=0, day=0;
			day = Integer.parseInt(date.substring(date.lastIndexOf("/")+1));
			yr = Integer.parseInt(date.substring(0,date.indexOf("/")));
			mon = Integer.parseInt(date.substring(date.indexOf("/")+1,date.lastIndexOf("/")));
			mon += 1;
			String dateAdj = yr + "/" + mon + "/" + day;
			Log.i(TAG, dateAdj);
			
			((TextView) findViewById(R.id.dob_label)).setText(getString(R.string.dob) +": " + dateAdj);
			
			setTextView(values, R.id.sex_label, R.string.sex, AcdiVocaFind.SEX);
			setTextView(values, R.id.num_ppl_label, R.string.Number_of_people_in_home, AcdiVocaFind.HOUSEHOLD_SIZE);
			
			// MCHN PART    	

			setDistroTextView(values, R.id.distro_label, R.string.distribution_post, AcdiVocaFind.DISTRIBUTION_POST);
			setTextView(values, R.id.bene_category_label, R.string.Beneficiary_Category, AcdiVocaFind.BENEFICIARY_CATEGORY);

			if (values.getAsString(AcdiVocaFind.BENEFICIARY_CATEGORY).equals(AcdiVocaFind.MALNOURISHED) || values.getAsString(AcdiVocaFind.BENEFICIARY_CATEGORY).equals(AcdiVocaFind.PREVENTION)){
				setTextView(values, R.id.alternate_collector, R.string.alternate, AcdiVocaFind.RELATIVE_1);
			}

			setTextView(values, R.id.mleader_label, R.string.mother_leader, AcdiVocaFind.Q_MOTHER_LEADER);
			setTextView(values, R.id.visit_label, R.string.visit_mother_leader, AcdiVocaFind.Q_VISIT_MOTHER_LEADER);			
			setTextView(values, R.id.participating_agri, R.string.participating_agri, AcdiVocaFind.Q_PARTICIPATING_AGRI);	
			setTextView(values, R.id.participating_relative_name, R.string.participating_beneficiary_agri, AcdiVocaFind.RELATIVE_2);

		}
	}
	
	/**
	 * displayText method
	 * @param c is contentValues
	 * @param id is id of the field
	 * @param key is to be looked up from c
	 */
	 
	private void displayText(ContentValues c, int id, String key){
		EditText e = (EditText)findViewById(id);
		String val = c.getAsString(key);
		if(val!=null){
			e.setText(val);
			((EditText) findViewById(id)).setVisibility(View.VISIBLE);
		}
	}
	
	/**
	 * displayRadio method
	 * @param c is contentValues
	 * @param id is id of the field
	 * @param key is to be looked up
	 * @param value is the String to be checked
	 * set the radio buttons
	 */
	private void displayRadio(ContentValues c, int id, String key, String value){
		RadioButton rb = (RadioButton)findViewById(id);
		String val = c.getAsString(key);
		if (val!=null && val.equals(value))
			rb.setChecked(true);
	}
	
	private void displayRadio(ContentValues c, int id, String key, boolean bVal){
		RadioButton rb = (RadioButton)findViewById(id);
		Boolean B = c.getAsBoolean(key);
		if (B!=null && B.equals(bVal))
			rb.setChecked(true);
	}
	
	
	
	/**
	 * Displays values from a ContentValues in the View.
	 * @param contentValues stores <key, value> pairs
	 */
	private void displayContentInView(ContentValues contentValues) {
		Log.i(TAG, "displayContentInView");

		if (contentValues != null) {
			setContentView(R.layout.acdivoca_registration);
			mCurrentViewId = R.layout.acdivoca_registration;
			
			initializeListeners();
			
			displayText(contentValues, R.id.lastnameEdit, AcdiVocaFind.LASTNAME);

			displayText(contentValues, R.id.firstnameEdit, AcdiVocaFind.FIRSTNAME);

			displayText(contentValues, R.id.addressEdit, AcdiVocaFind.ADDRESS);

			DatePicker dp = (DatePicker) findViewById(R.id.datepicker);
			String date = contentValues.getAsString(AcdiVocaFind.DOB);
			Log.i(TAG,"display DOB = " + date);
			int yr=0, mon=0, day=0;
			day = Integer.parseInt(date.substring(date.lastIndexOf("/")+1));
			yr = Integer.parseInt(date.substring(0,date.indexOf("/")));
			mon = Integer.parseInt(date.substring(date.indexOf("/")+1,date.lastIndexOf("/")));
			Log.i(TAG, yr + "/" + mon + "/" + day);
			try {
				if (date != null) {
					Log.i(TAG,"display DOB = " + date);
					dp.init(yr, mon, day, this);
				}
			} catch (IllegalArgumentException e) {
				Log.e(TAG, "Illegal Argument, probably month == 12 in " + date);
				e.printStackTrace();
			}

			displayRadio(contentValues,R.id.femaleRadio,AcdiVocaFind.SEX,AcdiVocaFind.FEMALE);
			displayRadio(contentValues,R.id.maleRadio,AcdiVocaFind.SEX,AcdiVocaFind.MALE);

			// SPINNERS FOR MCHN
			Spinner spinner = (Spinner)findViewById(R.id.distributionSpinner);
			setDistroSpinner(spinner, contentValues, AcdiVocaFind.DISTRIBUTION_POST);

			// NUMBNER OF PEOPLE IN HOME
			displayText(contentValues, R.id.inhomeEdit, AcdiVocaFind.HOUSEHOLD_SIZE);

			// MCHN CATEGORY

			displayRadio(contentValues,R.id.malnourishedRadio,AcdiVocaFind.BENEFICIARY_CATEGORY,AcdiVocaFind.MALNOURISHED.toString());
			displayRadio(contentValues,R.id.inpreventionRadio,AcdiVocaFind.BENEFICIARY_CATEGORY,AcdiVocaFind.PREVENTION.toString());

			RadioButton beneRB1 = (RadioButton)findViewById(R.id.malnourishedRadio);
			RadioButton beneRB2 = (RadioButton)findViewById(R.id.inpreventionRadio);
			if(beneRB1.isChecked() || beneRB2.isChecked()){
				findViewById(R.id.relatives).setVisibility(View.VISIBLE);		
				findViewById(R.id.responsibleIfChildEdit).setVisibility(View.VISIBLE);
				findViewById(R.id.responsibleIfMotherEdit).setVisibility(View.INVISIBLE);
				displayText(contentValues, R.id.responsibleIfChildEdit, AcdiVocaFind.RELATIVE_1);
			}

			displayRadio(contentValues,R.id.expectingRadio,AcdiVocaFind.BENEFICIARY_CATEGORY,AcdiVocaFind.EXPECTING.toString());
			displayRadio(contentValues,R.id.nursingRadio,AcdiVocaFind.BENEFICIARY_CATEGORY,AcdiVocaFind.NURSING.toString());

			RadioButton beneRB3 = (RadioButton)findViewById(R.id.expectingRadio);
			RadioButton beneRB4 = (RadioButton)findViewById(R.id.nursingRadio);
			if(beneRB3.isChecked() || beneRB4.isChecked()){
				findViewById(R.id.relatives).setVisibility(View.VISIBLE);		
				findViewById(R.id.responsibleIfChildEdit).setVisibility(View.INVISIBLE);
				findViewById(R.id.responsibleIfMotherEdit).setVisibility(View.VISIBLE);
				displayText(contentValues, R.id.responsibleIfMotherEdit, AcdiVocaFind.RELATIVE_1);
			}

			// MCHN QUESTIONS
	
			// Are you a mother leader?
			displayRadio(contentValues,R.id.radio_motherleader_yes,AcdiVocaFind.Q_MOTHER_LEADER,true);
			displayRadio(contentValues,R.id.radio_motherleader_no,AcdiVocaFind.Q_MOTHER_LEADER,false);
			
			// Have you received a visit from a mother leader?
			displayRadio(contentValues,R.id.radio_visit_yes,AcdiVocaFind.Q_VISIT_MOTHER_LEADER,true);
			displayRadio(contentValues,R.id.radio_visit_no,AcdiVocaFind.Q_VISIT_MOTHER_LEADER,false);

			// Q: Are you participating in Agri program?
			displayRadio(contentValues,R.id.radio_yes_participating_agri,AcdiVocaFind.Q_PARTICIPATING_AGRI,true);
			displayRadio(contentValues,R.id.radio_no_participating_agri,AcdiVocaFind.Q_PARTICIPATING_AGRI,false);
			// Get self or relative's name
			if(((RadioButton)findViewById(R.id.radio_yes_participating_agri)).isChecked()==true)
				displayText(contentValues,R.id.give_name,AcdiVocaFind.RELATIVE_2);
			
			// Disable Save button until form is edited
			isProbablyEdited = false;
			mSaveButton.setEnabled(false);	
		}
	}


	/**
	 * Required as part of OnClickListener interface. Handles button clicks.
	 */
	public void onClick(View v) {
		Log.i(TAG, "onClick");
		
		// If a RadioButton was clicked, mark the form as edited.
		try {
			if (v.getClass().equals(Class.forName("android.widget.RadioButton"))) {
					//Toast.makeText(this, "RadioClicked", Toast.LENGTH_SHORT).show();
					Log.i(TAG, "Radio clicked");
					isProbablyEdited = true;
					mSaveButton.setEnabled(true);	
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		// The Edit Button
		int id = v.getId();
		if (id == R.id.editFind) {
			displayExistingFind();
		}
		
		if (id == R.id.datepicker) {
			isProbablyEdited = true;
			mSaveButton.setEnabled(true);	
		}
		
		// Are you or a relative participating in Agri?
		// If no, do nothing
		
		if (id == R.id.radio_no_participating_agri){
			Log.i(TAG, "Clicked no on you or relative participating relative");
			findViewById(R.id.give_name).setVisibility(View.GONE);
			findViewById(R.id.give_name).setEnabled(false);		}
		
//		// If yes, get name.
		
		if (id == R.id.radio_yes_participating_agri){
			Log.i(TAG, "Clicked yes_relative_participating_agri");
			findViewById(R.id.give_name).setVisibility(View.VISIBLE);
			findViewById(R.id.give_name).setEnabled(true);
		}
		
		if (id == R.id.expectingRadio || id == R.id.nursingRadio) {
			findViewById(R.id.relatives).setVisibility(View.VISIBLE);
			findViewById(R.id.responsibleIfMotherEdit).setVisibility(View.VISIBLE);
			findViewById(R.id.responsibleIfChildEdit).setVisibility(View.GONE);
		} 
		if (id == R.id.malnourishedRadio || id == R.id.inpreventionRadio) {
			findViewById(R.id.relatives).setVisibility(View.VISIBLE);		
			findViewById(R.id.responsibleIfChildEdit).setVisibility(View.VISIBLE);
			findViewById(R.id.responsibleIfMotherEdit).setVisibility(View.GONE);
		}
		
		//  Save Find Button
		
//		if(v.getId()==R.id.saveToDbButton) {
//			boolean success = false;
//			ContentValues data = this.retrieveContentFromView(); 
//						
//			if (mAction.equals(Intent.ACTION_EDIT)) { // Editing an existing beneficiary
//				success = updateExistingFind(data);
//			} else { // New beneficiary
//				data.put(AcdiVocaFind.STATUS, AcdiVocaFind.STATUS_NEW);
//				data.put(AcdiVocaFind.DOSSIER, AttributeManager.FINDS_BENE_DOSSIER);
//				success = createNewFind(data);
//			}
//			if (success){
//				Log.i(TAG, "Save to Db returned success");
//				Toast.makeText(this, getString(R.string.toast_saved_db), Toast.LENGTH_SHORT).show();  
//			}
//			else {
//				Log.i(TAG, "Save to Db returned failure");
//				Toast.makeText(this, getString(R.string.toast_error_db), Toast.LENGTH_SHORT).show();
//			}
//			finish();
//		}
	}

	/**
	 * Helper method to display an existing Find after querying it from Db.
	 */
	private void displayExistingFind() {
    	mFindId = (int) getIntent().getLongExtra(AcdiVocaFind.ORM_ID, 0); 
		
		AcdiVocaFind avFind = null; // = new AcdiVocaFind(this, mFindId);
		try {
			avFind = this.dbManager.getAcdiVocaFindDao().queryForId(mFindId);
			if (avFind != null) {
		    	ContentValues values = avFind.toContentValues();
				displayContentInView(values);	
			} else {
				Log.e(TAG, "Error: Unable to retrieve Find, id = " + mFindId);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/** 
	 * Helper method to create a new Find from the form's data values.
	 * @param data
	 * @return
	 */
	private boolean createNewFind(ContentValues data) {
		AcdiVocaFind avFind = null; 
		boolean success = false;
		int rows = 0;
		try {
			Dao<AcdiVocaFind, Integer> dao = this.dbManager.getAcdiVocaFindDao();
			avFind = new AcdiVocaFind(data);
			avFind.updateFromContentValues(data);
			rows = dao.create(avFind);
			success = rows == 1;
			if (success) {
				Log.i(TAG, "Created Db row for Find, id = " + mFindId);
			} else {
				Log.e(TAG, "Db Error for Find, id = " + mFindId);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}	
		return success;
	}
	
	/**
	 * Helper method to update an existing Find from data extracted from the form.
	 * @param data
	 * @return
	 */
	private boolean updateExistingFind(ContentValues data) {
		AcdiVocaFind avFind = null; 
		boolean success = false;
		int rows = 0;
		
		try {
			Dao<AcdiVocaFind, Integer> dao = this.dbManager.getAcdiVocaFindDao();
			avFind = dao.queryForId(mFindId);
			if (avFind != null) {
				avFind.updateFromContentValues(data);
				rows = dao.update(avFind);
				success = rows == 1;
				if (success) {
					Log.i(TAG, "Updated Db for Find, id = " + mFindId);
				} else {
					Log.e(TAG, "Db Error for Find, id = " + mFindId);
				}
			} else {
				Log.e(TAG, "Error: Unable to retrieve Find, id = " + mFindId);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}	
		return success;
	}

	
	/**
	 * Intercepts the back key (KEYCODE_BACK) and displays a confirmation dialog
	 * when the user tries to exit POSIT.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.i(TAG, "onKeyDown keyCode = " + keyCode);
		if(keyCode==KeyEvent.KEYCODE_BACK && isProbablyEdited){
			//Toast.makeText(this, "Backkey isEdited=" +  isProbablyEdited, Toast.LENGTH_SHORT).show();
			showDialog(CONFIRM_EXIT);
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
		Log.i(TAG, "onCreateDialog");
		switch (id) {
		case CONFIRM_EXIT:
			return new AlertDialog.Builder(this).setIcon(
					R.drawable.alert_dialog_icon).setTitle(R.string.acdivoca_exit_findactivity)
					.setPositiveButton(R.string.Yes,
							new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// User clicked OK so do some stuff
							finish();
						}
					}).setNegativeButton(R.string.acdivoca_cancel,
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
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		AlertDialog d = (AlertDialog) dialog;
		Button needsabutton;
		switch (id) {
		case CONFIRM_EXIT:
					d.setTitle(R.string.acdivoca_exit_findactivity);

					needsabutton = d.getButton(DialogInterface.BUTTON_POSITIVE);
					needsabutton.setText(R.string.Yes);
					needsabutton.invalidate();
					
					needsabutton = d.getButton(DialogInterface.BUTTON_NEGATIVE);
					needsabutton.setText(R.string.alert_dialog_cancel);
					needsabutton.invalidate();
					
					break;
		}
	}
	

	public void onDateChanged(DatePicker view, int year, int monthOfYear,
			int dayOfMonth) {
		Log.i(TAG, "onDateChanged");
		isProbablyEdited = true;
		mSaveButton.setEnabled(true);	
	}

	//  The remaining methods are part of unused interfaces inherited from the super class.
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) { }
	public void onLocationChanged(Location location) {	}
	public void onProviderDisabled(String provider) {	}
	public void onProviderEnabled(String provider) {	}
	public void onStatusChanged(String provider, int status, Bundle extras) {	}


	/**
	 * Sets the 'edited' flag if text has been changed in an EditText
	 */
	public void afterTextChanged(Editable arg0) {
		//Log.i(TAG, "afterTextChanged " + arg0.toString());
		isProbablyEdited = true;
		mSaveButton.setEnabled(true);			
	}

	// Unused
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
	public void onNothingSelected(AdapterView<?> arg0) {	}

	
	
	/**
	 * Called when a spinner selection is made.
	 */
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		Log.i(TAG, "onItemSelected = " + arg2);
		//isProbablyEdited = true;
		//mSaveButton.setEnabled(true);	
	}


	//spinner function	
	public static void setSpinner(Spinner spinner, ContentValues contentValues, String attribute){
		String selected = contentValues.getAsString(attribute);
		int k = 0;
		if(selected != null){
			String item = (String) spinner.getItemAtPosition(k);
			while (k < spinner.getCount()-1 && !selected.equals(item)) {
				++k;
				item = (String) spinner.getItemAtPosition(k);				
			}
			if (k < spinner.getCount())
				spinner.setSelection(k);
			else
				spinner.setSelection(0);
		}
		else{
			spinner.setSelection(0);
		}
	}
	
	public static void setDistroSpinner(Spinner spinner, ContentValues contentValues, String attribute){
		String selected = AttributeManager.getMapping(contentValues.getAsString(attribute));
		int k = 0;
		if(selected != null){
			String item = (String) spinner.getItemAtPosition(k);
			while (k < spinner.getCount()-1 && !selected.equals(item)) {
				++k;
				item = (String) spinner.getItemAtPosition(k);				
			}
			if (k < spinner.getCount())
				spinner.setSelection(k);
			else
				spinner.setSelection(0);
		}
		else{
			spinner.setSelection(0);
		}
	}
}				
