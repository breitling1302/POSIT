/*
 * File: AcdiVocaFindActivity.java
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
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Handles Finds for AcdiVoca Mobile App.
 * 
 */
public class AcdiVocaAgriFindActivity extends FindActivity implements OnDateChangedListener, 
TextWatcher, OnItemSelectedListener { //, OnKeyListener {
	public static final String TAG = "AcdiVocaAddAgriActivity";

	private static final int CONFIRM_EXIT = 0;

	private AcdiVocaDbManager dbManager;
	
	private boolean isProbablyEdited = false;   // Set to true if user edits a datum
	private String mAction = "";
	private int mFindId = 0;
	private Button mSaveButton;
	ContentValues mSavedStateValues = null;
	int mCurrentViewId;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");

		// Create DB helper
		//		mDbHelper = new AcdiVocaDbHelper(this);
		dbManager = (AcdiVocaDbManager)dbManager;
		isProbablyEdited = false;
	}

	/**
	 * Inflates the App's menus from a resource file.
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
		isProbablyEdited = (boolean) savedInstanceState.getBoolean("isprobablyEdited");
		this.displayContentInView(mSavedStateValues);
		mSavedStateValues = null;
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.i(TAG, "onSaveInstanceState");
		
		// Don't bother saving state if we're in no-edit mode.
		if (mCurrentViewId == R.layout.acdivoca_agri_beneficiary_noedit)
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
		Log.i(TAG, "onResume");

		LocaleManager.setDefaultLocale(this);  // Locale Manager should be in API


		if (this.mSavedStateValues != null) {
			Log.i(TAG, "onResume, restoring instance state ");
			this.displayContentInView(mSavedStateValues);
			mSavedStateValues = null;
			initializeListeners();
		} else {

			Log.i(TAG, "Before edited = " + isProbablyEdited);
			setContentView(R.layout.acdivoca_agri_registration);  // Should be done after locale configuration
			mCurrentViewId = R.layout.acdivoca_agri_registration;
			
			initializeListeners();

			final Intent intent = getIntent();
			mAction = intent.getAction();

			if (mAction.equals(Intent.ACTION_EDIT)) {
				displayAsUneditable();
				isProbablyEdited = false; // In EDIT mode, initialize after filling in data
				mSaveButton.setEnabled(false);
			}
			if (mAction.equals(Intent.ACTION_INSERT)){
				Log.i(TAG,"############################################");
				Log.i(TAG,"you are now in insert");
				if (intent.getExtras() != null){
				}
			}
		}
	}

	/**
	 * Helper to initialize radio buttons, text edits, etc. 
	 */
	protected void initializeListeners() {
		mSaveButton = ((Button)findViewById(R.id.saveToDbButton));
		mSaveButton.setOnClickListener(this);
		((Button)findViewById(R.id.sendSmsButton)).setOnClickListener(this);

		// Listen for clicks on radio buttons
		((RadioButton)findViewById(R.id.femaleRadio)).setOnClickListener(this);
		((RadioButton)findViewById(R.id.maleRadio)).setOnClickListener(this);


		((RadioButton)findViewById(R.id.radio_yes_participating_mchn)).setOnClickListener(this);
		((RadioButton)findViewById(R.id.radio_no_participating_mchn)).setOnClickListener(this);
		//		 ((RadioButton)findViewById(R.id.radio_yes_relative_participating_mchn)).setOnClickListener(this);
		//		 ((RadioButton)findViewById(R.id.radio_no_relative_participating_mchn)).setOnClickListener(this);

		// Listen for clicks on check boxes
		((CheckBox)findViewById(R.id.farmerCheckBox)).setOnClickListener(this);
		((CheckBox)findViewById(R.id.musoCheckBox)).setOnClickListener(this);
		((CheckBox)findViewById(R.id.rancherCheckBox)).setOnClickListener(this);
		((CheckBox)findViewById(R.id.storeOwnerCheckBox)).setOnClickListener(this);
		((CheckBox)findViewById(R.id.fisherCheckBox)).setOnClickListener(this);
		((CheckBox)findViewById(R.id.otherCheckBox)).setOnClickListener(this);

		((CheckBox)findViewById(R.id.vegeCheckBox)).setOnClickListener(this);
		((CheckBox)findViewById(R.id.cerealCheckBox)).setOnClickListener(this);
		((CheckBox)findViewById(R.id.tuberCheckBox)).setOnClickListener(this);
		((CheckBox)findViewById(R.id.treeCheckBox)).setOnClickListener(this);
		((CheckBox)findViewById(R.id.graftingCheckBox)).setOnClickListener(this);
		((CheckBox)findViewById(R.id.coffeeCheckBox)).setOnClickListener(this);

		((CheckBox)findViewById(R.id.houeCheckBox)).setOnClickListener(this);
		((CheckBox)findViewById(R.id.piocheCheckBox)).setOnClickListener(this);
		((CheckBox)findViewById(R.id.brouetteCheckBox)).setOnClickListener(this);
		((CheckBox)findViewById(R.id.machetteCheckBox)).setOnClickListener(this);
		((CheckBox)findViewById(R.id.serpetteCheckBox)).setOnClickListener(this);
		((CheckBox)findViewById(R.id.pelleCheckBox)).setOnClickListener(this);
		((CheckBox)findViewById(R.id.barreAMinesCheckBox)).setOnClickListener(this);


		((CheckBox)findViewById(R.id.faoCheckBox)).setOnClickListener(this);
		((CheckBox)findViewById(R.id.saveCheckBox)).setOnClickListener(this);
		((CheckBox)findViewById(R.id.croseCheckBox)).setOnClickListener(this);
		((CheckBox)findViewById(R.id.mardnrCheckBox)).setOnClickListener(this);
		((CheckBox)findViewById(R.id.planCheckBox)).setOnClickListener(this);
		((CheckBox)findViewById(R.id.otherPartnerCheckBox)).setOnClickListener(this);	 


		// Listen for text changes in edit texts and set the isEdited flag
		((EditText)findViewById(R.id.firstnameEdit)).addTextChangedListener(this);
		((EditText)findViewById(R.id.lastnameEdit)).addTextChangedListener(this);
		((EditText)findViewById(R.id.addressEdit)).addTextChangedListener(this);
		((EditText)findViewById(R.id.inhomeEdit)).addTextChangedListener(this);
		((EditText)findViewById(R.id.amount_of_land)).addTextChangedListener(this);
		((EditText)findViewById(R.id.quantityEdit)).addTextChangedListener(this);

		// Initialize the DatePicker and listen for changes
		Calendar calendar = Calendar.getInstance();

		((DatePicker)findViewById(R.id.datepicker)).init(
				calendar.get(Calendar.YEAR),
				calendar.get(Calendar.MONTH), 
				calendar.get(Calendar.DAY_OF_MONTH), this);

		//Spinner listeners
//		((Spinner)findViewById(R.id.healthcenterSpinner)).setOnItemSelectedListener(this);
		((Spinner)findViewById(R.id.distributionSpinner)).setOnItemSelectedListener(this);
//		((Spinner)findViewById(R.id.unitSpinner)).setOnItemSelectedListener(this);		
	}

	/**
	 * Allows editing of editable data for existing finds.  For existing finds, 
	 * we retrieve the Find's data from the DB and display it in a TextView. The
	 * Find's location and time stamp are not updated.
	 */
	private void displayAsUneditable() {
		Log.i(TAG, "doEditAction");
		mFindId = (int) getIntent().getLongExtra(Find.ORM_ID, 0); 
		Log.i(TAG,"Find id = " + mFindId);
		
		AcdiVocaFind avFind = null; // = new AcdiVocaFind(this, mFindId);
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
	 * setTextView method
	 * @param c is contentValues
	 * @param id is id of the field
	 * @param label is label to be added to
	 * @param key is the text to be added to
	 * set the text on the view
	 */
	private void setTextView(ContentValues c, int id, int label,String key){
		TextView tv = ((TextView) findViewById(id));
		String val = c.getAsString(key);
//		if(val!=null && label != R.string.participating_bene_same)

		if(val!=null)
			tv.setText(getString(label) + ": " +  val);
		else
//			tv.setText(": "+val);
			tv.setText(getString(label)+": ");
	}
	/**
	 * setArrayView method
	 * @param c is ContentValue
	 * @param id is id  of the field
	 * @param label id the label to be added to
	 * @param keys is the String array of the items
	 * set the text on the view
	 */
	private void setCheckBoxView(ContentValues c, int id, int label,String[] keys){
		TextView tv = (TextView)findViewById(id);
		String cat = "";
		for (int i=0; i<keys.length; i++){
			Boolean val = c.getAsBoolean(keys[i]);
//			Integer val = c.getAsInteger(keys[i]);
			if(val!=null && val && keys[i].substring(0, 2).equals("is"))
				cat += keys[i].subSequence(3, keys[i].length())+", ";
			if(val!=null && val && keys[i].substring(0, 4).equals("have"))
				cat += keys[i].subSequence(5, keys[i].length())+", ";
			if(val!=null && val && keys[i].substring(0, 7).equals("partner"))
				cat += keys[i].subSequence(8, keys[i].length())+", ";
		}
		if (cat.length()!=0)
			tv.setText(getString(label)+": "+cat.substring(0,cat.length()-2));
		else
			tv.setText(getString(label)+": "+cat);
	}
	/**
	 * Displays the content as uneditable labels -- default view.
	 * @param values
	 */
	private void displayContentUneditable(ContentValues values) {
		Log.i(TAG, "Displaying content in review mode");
		if (values != null){
			this.setContentView(R.layout.acdivoca_agri_beneficiary_noedit);
			mCurrentViewId = R.layout.acdivoca_agri_beneficiary_noedit;

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
//			setTextView(values, R.id.dob_label, R.string.dob, AcdiVocaFind.DOB);

			setTextView(values, R.id.sex_label, R.string.sex, AcdiVocaFind.SEX);
			setTextView(values, R.id.num_ppl_label, R.string.Number_of_people_in_home, AcdiVocaFind.HOUSEHOLD_SIZE);
			setTextView(values, R.id.commune_label, R.string.commune, AcdiVocaFind.COMMUNE_SECTION);

			String[] arr = {AcdiVocaFind.IS_FARMER,AcdiVocaFind.IS_MUSO,AcdiVocaFind.IS_RANCHER,
					AcdiVocaFind.IS_STOREOWN,AcdiVocaFind.IS_FISHER,
					AcdiVocaFind.IS_ARTISAN,AcdiVocaFind.IS_OTHER};
			setCheckBoxView(values,R.id.agri_category_label, R.string.Beneficiary_Category,arr);

			setTextView(values, R.id.land_label, R.string.amount_of_land, AcdiVocaFind.LAND_AMOUNT);

			String[] seedArr = {AcdiVocaFind.HAVE_VEGE, AcdiVocaFind.HAVE_CEREAL,
					AcdiVocaFind.HAVE_TUBER, AcdiVocaFind.HAVE_TREE,
					AcdiVocaFind.HAVE_GRAFTING,AcdiVocaFind.HAVE_COFFEE};

			setCheckBoxView(values,R.id.seed_label, R.string.seed_group,seedArr);

			String[] toolArr = {AcdiVocaFind.HAVE_HOUE, AcdiVocaFind.HAVE_PIOCHE,
					AcdiVocaFind.HAVE_BROUETTE, AcdiVocaFind.HAVE_MACHETTE,
					AcdiVocaFind.HAVE_SERPETTE,AcdiVocaFind.HAVE_PELLE,
					AcdiVocaFind.HAVE_BARREAMINES};
			setCheckBoxView(values,R.id.tool_label, R.string.tools,toolArr);

			String[] partArr = {AcdiVocaFind.PARTNER_FAO, AcdiVocaFind.PARTNER_SAVE,
					AcdiVocaFind.PARTNER_CROSE,AcdiVocaFind.PARTNER_PLAN,
					AcdiVocaFind.PARTNER_MARDNR,AcdiVocaFind.PARTNER_OTHER};
			setCheckBoxView(values,R.id.partner_label, R.string.partners,partArr);

			setTextView(values, R.id.participating_mchn, R.string.participating_mchn,AcdiVocaFind.Q_PARTICIPATING_BENE);
			setTextView(values, R.id.participating_relative_name, R.string.participating_beneficiary_mchn,AcdiVocaFind.RELATIVE_2);
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
	 * saveCheckBox method
	 * @param c is contentValues
	 * @param ids is int array of ids
	 * @param keys is string array of tags to be saved
	 * store the corresponding values into the c
	 */
	private void putCheckBoxResult(ContentValues c, int[] ids, String[] keys){
		for(int i = 0; i<keys.length; i++){
			CheckBox cb = (CheckBox)findViewById(ids[i]);
			if(cb.isChecked()){
				c.put(keys[i], AcdiVocaFind.TRUE);
//				c.put(keys[i], AcdiVocaFind.ONE);
			}
			else
//				c.put(keys[i],AcdiVocaFind.ZERO);
				c.put(keys[i],AcdiVocaFind.FALSE);
		}
	}

	/**
	 * Retrieves values from the View fields and stores them as <key,value> pairs in a ContentValues.
	 * This method is invoked from the Save menu item.  It also marks the find 'unsynced'
	 * so it will be updated to the server.
	 * multiple checkBox results will be stored as the addition of the binary summation.
	 * @return The ContentValues hash table.
	 */
	protected Find retrieveContentFromView() {
		Log.i(TAG, "retrieveContentFromView");
		ContentValues result = new ContentValues();
		String value = "";

		result.put(AcdiVocaFind.TYPE, AcdiVocaFind.TYPE_AGRI);

		EditText eText = (EditText) findViewById(R.id.lastnameEdit);
		
		// Retrieving  NAME, and Locality
		putTextResult(result,R.id.firstnameEdit,AcdiVocaFind.FIRSTNAME);
		putTextResult(result,R.id.lastnameEdit,AcdiVocaFind.LASTNAME);
		putTextResult(result,R.id.addressEdit,AcdiVocaFind.ADDRESS);

		// Retrieving DOB

		//value = mMonth + "/" + mDay + "/" + mYear;
		DatePicker picker = ((DatePicker)findViewById(R.id.datepicker));
		value = picker.getYear() + "/" + picker.getMonth() + "/" + picker.getDayOfMonth();
		Log.i(TAG, "Date = " + value);
		result.put(AcdiVocaFind.DOB, value);

		// Retrieving SEX
		putRadioResult(result,R.id.femaleRadio,R.id.maleRadio,AcdiVocaFind.SEX, AcdiVocaFind.FEMALE, AcdiVocaFind.MALE);

		//Retrieving NUMER OF PEOPLE AT HOME
		putTextResult(result,R.id.inhomeEdit,AcdiVocaFind.HOUSEHOLD_SIZE);

		//Retrieving Amount OF LAND 
		eText = (EditText)findViewById(R.id.amount_of_land);
		value = eText.getText().toString();
		result.put(AcdiVocaFind.LAND_AMOUNT, value);

		//PARTICIPATING IN MCHN PROGRAM
		putRadioResult(result,R.id.radio_yes_participating_mchn,R.id.radio_no_participating_mchn,AcdiVocaFind.Q_PARTICIPATING_BENE,AcdiVocaFind.TRUE,AcdiVocaFind.FALSE);

		//NAME OF PERSON PARTICIPATING IN MCHN PROGRAM
		RadioButton beneRB = (RadioButton)findViewById(R.id.radio_yes_participating_mchn);
		if (beneRB.isChecked()) {
			putTextResult(result,R.id.give_name,AcdiVocaFind.RELATIVE_2);
		}
		if (!beneRB.isChecked())	{
			String none="";
			result.put(AcdiVocaFind.RELATIVE_2, none);
		}


		//Add beneficiary checkbox values
		// The following two arrays must be in parallel with beneIds giving
		// the resource id for the checkboxes and the beneData giving
		// the values for the checkboxes
		int[] beneIds = {R.id.farmerCheckBox,R.id.musoCheckBox,R.id.rancherCheckBox,
				R.id.storeOwnerCheckBox,R.id.fisherCheckBox,
				R.id.artisanCheckBox,R.id.otherCheckBox};
		String[] beneArr = {AcdiVocaFind.IS_FARMER,AcdiVocaFind.IS_MUSO,
				AcdiVocaFind.IS_RANCHER,AcdiVocaFind.IS_STOREOWN,
				AcdiVocaFind.IS_FISHER,AcdiVocaFind.IS_ARTISAN,
				AcdiVocaFind.IS_OTHER};
		putCheckBoxResult(result,beneIds,beneArr);


		// Add Seed types -- also parallel arrays (see above)
		int[] seedIds = {R.id.vegeCheckBox,R.id.cerealCheckBox,R.id.tuberCheckBox,
				R.id.treeCheckBox,R.id.graftingCheckBox,R.id.coffeeCheckBox};
		String[] seedArr = {AcdiVocaFind.HAVE_VEGE,AcdiVocaFind.HAVE_CEREAL,
				AcdiVocaFind.HAVE_TUBER,AcdiVocaFind.HAVE_TREE,
				AcdiVocaFind.HAVE_GRAFTING,AcdiVocaFind.HAVE_COFFEE};
		putCheckBoxResult(result,seedIds, seedArr);

		// Add Partner GROUPS -- also parallel arrays (see above)

		int[] partnerIds = {R.id.faoCheckBox,R.id.saveCheckBox, R.id.croseCheckBox,
				R.id.planCheckBox,R.id.mardnrCheckBox,R.id.otherPartnerCheckBox};
		String[] partnerArr = {AcdiVocaFind.PARTNER_FAO,AcdiVocaFind.PARTNER_SAVE,
				AcdiVocaFind.PARTNER_CROSE,AcdiVocaFind.PARTNER_PLAN,
				AcdiVocaFind.PARTNER_MARDNR,AcdiVocaFind.PARTNER_OTHER};
		putCheckBoxResult(result, partnerIds, partnerArr);

		// ADD TOOLS -- also parallel arrays (see above)
		int[] toolIds = {R.id.houeCheckBox,R.id.piocheCheckBox,R.id.brouetteCheckBox,
				R.id.machetteCheckBox, R.id.serpetteCheckBox, R.id.pelleCheckBox,
				R.id.barreAMinesCheckBox};
		String[] toolArr = {AcdiVocaFind.HAVE_HOUE,AcdiVocaFind.HAVE_PIOCHE,
				AcdiVocaFind.HAVE_BROUETTE,AcdiVocaFind.HAVE_MACHETTE,
				AcdiVocaFind.HAVE_SERPETTE,AcdiVocaFind.HAVE_PELLE,
				AcdiVocaFind.HAVE_BARREAMINES};
		putCheckBoxResult(result, toolIds, toolArr);

		// COummune section
		Spinner spinner = null;
		spinner = (Spinner)findViewById(R.id.commune_sectionSpinner);
		String communeSection = (String) spinner.getSelectedItem();
		result.put(AcdiVocaFind.COMMUNE_SECTION, communeSection);		

		//return result;
		return new AcdiVocaFind();
	}


	/**
	 * DISPLAY EDIT TEXT
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
	 * @param c is the contentValues
	 * @param id is the id of the field
	 * @param key is the key to be looked up
	 * @param value is the value to be checked
	 * set the RadioButton 
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
	 * displayCheckBox
	 * @param c is contentValue
	 * @param ids is array of the ids of the fields
	 * @param keys are the tags to be lookedup
	 * set the checkBoxes
	 */
	private void displayCheckBox(ContentValues c, int[] ids, String[] keys){
		for(int i = 0; i<keys.length; i++){
			CheckBox cb = (CheckBox)findViewById(ids[i]);
			Boolean val = c.getAsBoolean(keys[i]);
//			String val = c.getAsString(keys[i]);
//			if(val != null && val.equals(AcdiVocaFind.ONE)){
			if(val != null && val){
				cb.setChecked(true);
			}
			else
				cb.setChecked(false);
		}
	}
	/**
	 * Displays values from a ContentValues in the View.
	 * @param contentValues stores <key, value> pairs
	 */
	private void displayContentInView(ContentValues contentValues) {
		Log.i(TAG, "displayContentInView");
		if (contentValues != null){
			setContentView(R.layout.acdivoca_agri_registration);
			mCurrentViewId = R.layout.acdivoca_agri_registration;

			initializeListeners();
			// DISPLAY LAST NAME
			displayText(contentValues, R.id.lastnameEdit, AcdiVocaFind.LASTNAME);
			// DISPLAY FIRSTNAME
			displayText(contentValues, R.id.firstnameEdit, AcdiVocaFind.FIRSTNAME);
			// COMMUNE SECTION SPINNER
			Spinner spinner = (Spinner)findViewById(R.id.commune_sectionSpinner);
			AcdiVocaMchnFindActivity.setSpinner(spinner, contentValues, AcdiVocaFind.COMMUNE_SECTION);
			// ADDRESS
			displayText(contentValues, R.id.addressEdit, AcdiVocaFind.ADDRESS);
			// DOB
			DatePicker dp = (DatePicker) findViewById(R.id.datepicker);
			String date = contentValues.getAsString(AcdiVocaFind.DOB);
			Log.i(TAG,"display DOB = " + date);
			int yr=0, mon=0, day=0;
			day = Integer.parseInt(date.substring(date.lastIndexOf("/")+1));
			yr = Integer.parseInt(date.substring(0,date.indexOf("/")));
			mon = Integer.parseInt(date.substring(date.indexOf("/")+1,date.lastIndexOf("/")));
			Log.i(TAG, yr + "/" + mon + "/" + day);
			//		mon = mon + 1;  // Months are number 0..11
			//		day = day - 1;
			try {
				if (date != null) {
					Log.i(TAG,"display DOB = " + date);
					dp.init(yr, mon, day, this);
				}
			} catch (IllegalArgumentException e) {
				Log.e(TAG, "Illegal Argument, probably month == 12 in " + date);
				e.printStackTrace();
			}
			// AMOUNT OF LAND
			displayText(contentValues, R.id.amount_of_land, AcdiVocaFind.LAND_AMOUNT);
			// NUMBER OF PEOPLE IN HOME
			displayText(contentValues, R.id.inhomeEdit, AcdiVocaFind.HOUSEHOLD_SIZE);
			// SEX
			displayRadio(contentValues,R.id.femaleRadio,AcdiVocaFind.SEX,AcdiVocaFind.FEMALE);
			displayRadio(contentValues,R.id.maleRadio,AcdiVocaFind.SEX,AcdiVocaFind.MALE);
			
			// PARTICIPATING in MCHN
			displayRadio(contentValues,R.id.radio_yes_participating_mchn,AcdiVocaFind.Q_PARTICIPATING_BENE, true);
			displayRadio(contentValues,R.id.radio_no_participating_mchn,AcdiVocaFind.Q_PARTICIPATING_BENE, false);
			// Get self or relative's name
			if(((RadioButton)findViewById(R.id.radio_yes_participating_mchn)).isChecked()==true)
				displayText(contentValues,R.id.give_name,AcdiVocaFind.RELATIVE_2);

			// The following two arrays must be in parallel with beneIds giving
			// the resource id for the checkboxes and the beneData giving
			// the values for the checkboxes
			int[] beneIds = {R.id.farmerCheckBox,R.id.musoCheckBox,R.id.rancherCheckBox,
					R.id.storeOwnerCheckBox,R.id.fisherCheckBox,
					R.id.artisanCheckBox,R.id.otherCheckBox};
			String[] beneData = {AcdiVocaFind.IS_FARMER,AcdiVocaFind.IS_MUSO,
					AcdiVocaFind.IS_RANCHER,AcdiVocaFind.IS_STOREOWN,
					AcdiVocaFind.IS_FISHER,AcdiVocaFind.IS_ARTISAN,
					AcdiVocaFind.IS_OTHER};
			displayCheckBox(contentValues,beneIds,beneData);


			// Add Seed types -- also parallel arrays (see above)
			int[] seedIds = {R.id.vegeCheckBox,R.id.cerealCheckBox,R.id.tuberCheckBox,
					R.id.treeCheckBox,R.id.graftingCheckBox,R.id.coffeeCheckBox};
			String[] seedData = {AcdiVocaFind.HAVE_VEGE,AcdiVocaFind.HAVE_CEREAL,
					AcdiVocaFind.HAVE_TUBER,AcdiVocaFind.HAVE_TREE,
					AcdiVocaFind.HAVE_GRAFTING,AcdiVocaFind.HAVE_COFFEE};
			displayCheckBox(contentValues,seedIds, seedData);

			// Add Partner GROUPS -- also parallel arrays (see above)

			int[] partnerIds = {R.id.faoCheckBox,R.id.saveCheckBox, R.id.croseCheckBox,
					R.id.planCheckBox,R.id.mardnrCheckBox,R.id.otherCheckBox};
			String[] partnerArr = {AcdiVocaFind.PARTNER_FAO,AcdiVocaFind.PARTNER_SAVE,
					AcdiVocaFind.PARTNER_CROSE,AcdiVocaFind.PARTNER_PLAN,
					AcdiVocaFind.PARTNER_MARDNR,AcdiVocaFind.PARTNER_OTHER};
			displayCheckBox(contentValues, partnerIds, partnerArr);

			// ADD TOOLS -- also parallel arrays (see above)
			int[] toolIds = {R.id.houeCheckBox,R.id.piocheCheckBox,R.id.brouetteCheckBox,
					R.id.machetteCheckBox, R.id.serpetteCheckBox, R.id.pelleCheckBox,
					R.id.barreAMinesCheckBox};
			String[] toolArr = {AcdiVocaFind.HAVE_HOUE,AcdiVocaFind.HAVE_PIOCHE,
					AcdiVocaFind.HAVE_BROUETTE,AcdiVocaFind.HAVE_MACHETTE,
					AcdiVocaFind.HAVE_SERPETTE,AcdiVocaFind.HAVE_PELLE,
					AcdiVocaFind.HAVE_BARREAMINES};
			displayCheckBox(contentValues, toolIds, toolArr);
			Log.i(TAG, "display Beneficiary Catagory=" + contentValues.getAsString(AcdiVocaFind.BENEFICIARY_CATEGORY));

		}
	}

	/**
	 * Required as part of OnClickListener interface. Handles button clicks.
	 */
	public void onClick(View v) {
		Log.i(TAG, "onClick");

		// If a checkbox clicked, mark the form as 'edited'
		try {
			if (v.getClass().equals(Class.forName("android.widget.CheckBox"))) {
				isProbablyEdited = true;
				mSaveButton.setEnabled(true);
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		// If Edit button was clicked, display the Find in an editable form
		int id = v.getId();
		if (id == R.id.editFind) {
			displayExistingFind();
		}

		//  If DatePicker was touched, mark the form as edited
		if (id == R.id.datepicker) {
			isProbablyEdited = true;
			mSaveButton.setEnabled(true);	
		}

		// Are you or a family member participating in Mchn?
		// If no, do nothing

		if (id == R.id.radio_no_participating_mchn){
			Log.i(TAG, "Clicked no on MCHN or relative");
			findViewById(R.id.give_name).setVisibility(View.GONE);
			findViewById(R.id.give_name).setEnabled(false);			}

		// If yes, get the name	
		
		if (id == R.id.radio_yes_participating_mchn){
			Log.i(TAG, "Clicked yes on MCHN or relative");
			findViewById(R.id.give_name).setVisibility(View.VISIBLE);
			findViewById(R.id.give_name).setEnabled(true);	
		}

		//  Save Find Button: either create a new Find or update an existing Find
		
		if(v.getId()==R.id.saveToDbButton) {
			boolean success = false;
			//ContentValues data = this.retrieveContentFromView(); 
			int rows = 0;

//			Log.i(TAG,"View Content: " + data.toString());
//			if (mAction.equals(Intent.ACTION_EDIT)) {
//				success = updateExistingFind(data);
//			} else {
//				data.put(AcdiVocaFind.DOSSIER, AttributeManager.FINDS_AGRI_DOSSIER);
//				data.put(AcdiVocaFind.STATUS, AcdiVocaFind.STATUS_NEW);
//				success = createNewFind(data);
//			}
			if (success){
				Toast.makeText(this, getString(R.string.toast_saved_db), Toast.LENGTH_LONG).show();
			}
			else 
				Toast.makeText(this, getString(R.string.toast_error_db), Toast.LENGTH_SHORT).show();
			finish();
		}
	}
	
	/**
	 * Helper method to display an existing Find after querying it from Db.
	 */
	private void displayExistingFind() {
		mFindId = (int) getIntent().getLongExtra(AcdiVocaFind.ORM_ID, 0); 
		
		AcdiVocaFind avFind = null; 
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
				Log.i(TAG, "Created Db row for a new Find");
			} else {
				Log.e(TAG, "Db Error attempting to create a new Find");
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
	 * Creates a dialog to confirm that the user wants to exit FindActivity.
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
	
	/**
	 * Invoked automatically before the Dialog is shown.
	 */
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

	/**
	 * Invoked when the DatePicker is touched.
	 */
	public void onDateChanged(DatePicker view, int year, int monthOfYear,
			int dayOfMonth) {
		Log.i(TAG, "onDateChanged");
		isProbablyEdited = true;
		mSaveButton.setEnabled(true);	
	}

	//  The remaining methods are part of unused interfaces inherited from the super class.
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) { }
	public void onLocationChanged(Location location) { }
	public void onProviderDisabled(String provider) { }
	public void onProviderEnabled(String provider) { }
	public void onStatusChanged(String provider, int status, Bundle extras) {	}

	/**
	 * Sets the 'edited' flag if text has been changed in an EditText
	 */
	public void afterTextChanged(Editable arg0) {
		//	Log.i(TAG, "afterTextChanged " + arg0.toString());
		isProbablyEdited = true;
		mSaveButton.setEnabled(true);	
	}

	// Unused
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {}
	public void onNothingSelected(AdapterView<?> arg0) {}
}