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
public class AcdiVocaUpdateFindActivity extends FindActivity implements OnDateChangedListener, 
    TextWatcher, OnItemSelectedListener { //, OnKeyListener {
    public static final String TAG = "AcdiVocaUpdateActivity";

    private static final int CONFIRM_EXIT = 0;

    private static final int ACTION_ID = 0;
    
    private AcdiVocaDbManager dbManager;
    
    private String mBeneficiaryId = "unknown";

    private boolean isProbablyEdited = false;   // Set to true if user edits a datum
    private String mAction = "";
    private ContentValues mContentValues;
    private boolean inEditableMode = false;
    
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	Log.i(TAG, "onCreate");
    	dbManager = (AcdiVocaDbManager)dbManager;
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
        return true;
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
    }

    
    /**
     * 
     */
    @Override
    protected void onResume() {
    	super.onResume();
    	Log.i(TAG, "onResume beneficiary id = " + mBeneficiaryId);

    	LocaleManager.setDefaultLocale(this);  // Locale Manager should be in API

    	Log.i(TAG, "Before edited = " + isProbablyEdited);

    	if (mBeneficiaryId.equals("unknown")) {
    		Intent lookupIntent = new Intent();
    		lookupIntent.setClass(this, AcdiVocaLookupActivity.class);
    		this.startActivityForResult(lookupIntent, ACTION_ID);
    	} else {
    		displayExistingFind();
    	}
    }
    
	/**
	 * Helper method to display an existing Find after querying it from Db.
	 */
	private void displayExistingFind() {
		Log.i(TAG, "Display existing Find");
		try {
			Dao<AcdiVocaFind, Integer>  avFindDao = this.dbManager.getAcdiVocaFindDao();
//			AcdiVocaFind avFind = AcdiVocaFind.fetchFindByDossier(avFindDao, mBeneficiaryId);
			AcdiVocaFind avFind = AcdiVocaFind.fetchByAttributeValue(avFindDao, AcdiVocaFind.DOSSIER, mBeneficiaryId);
   			if (avFind != null) {
	    		mContentValues = avFind.toContentValues();
	    		if (mContentValues == null) {
	    			Toast.makeText(this, getString(R.string.toast_no_beneficiary) + mBeneficiaryId, Toast.LENGTH_SHORT).show();
	    		} else {
	    			Log.i(TAG,mContentValues.toString());
	    			setContentView(R.layout.acdivoca_update_noedit);  // Should be done after locale configuration
	    			((Button)findViewById(R.id.update_to_db_button)).setOnClickListener(this);
	    			((Button)findViewById(R.id.update_edit_button)).setOnClickListener(this);

	    			displayContentUneditable(mContentValues);
	    		}    				
			} else {
				Log.e(TAG, "Db Error: Unable to retrieve find, dossier = " + mBeneficiaryId);
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}	
	}

    
    /**
     * Displays the content as uneditable labels -- default view.
     * @param values
     */
    private void displayContentUneditable(ContentValues values) {
    	TextView tv = ((TextView) findViewById(R.id.dossier_label));
    	tv.setText(getString(R.string.beneficiary_dossier) + " " + mBeneficiaryId);
    	tv = ((TextView) findViewById(R.id.firstnameLabel));
    	tv.setText(getString(R.string.firstname) + ": " 
    			+  values.getAsString(AcdiVocaFind.FIRSTNAME));
    	tv = ((TextView) findViewById(R.id.lastnameLabel));
    	tv.setText(getString(R.string.lastname) + ": " 
    			+  values.getAsString(AcdiVocaFind.LASTNAME));  	
    	tv = ((TextView) findViewById(R.id.dobLabel));
    	tv.setText(getString(R.string.dob) + ": " 
    			+  values.getAsString(AcdiVocaFind.DOB));  
    	tv = ((TextView) findViewById(R.id.sexLabel));
    	tv.setText(getString(R.string.sex) + ": " 
    			+  values.getAsString(AcdiVocaFind.SEX));  	
    	tv = ((TextView) findViewById(R.id.categoryLabel));
    	tv.setText(getString(R.string.Beneficiary_Category) + ": " 
    			+  values.getAsString(AcdiVocaFind.BENEFICIARY_CATEGORY)); 
    	
    	displayUpdateQuestions(values);
    }
    
    /**
     * The update questions are displayed regardless of whether the form is
     * in editable or uneditable mode.
     * @param contentValues
     */
    private void displayUpdateQuestions(ContentValues contentValues) {
    	((RadioButton) findViewById(R.id.radio_present_yes)).setOnClickListener(this);
    	((RadioButton) findViewById(R.id.radio_present_no)).setOnClickListener(this);
    	((RadioButton) findViewById(R.id.radio_change_in_status_yes)).setOnClickListener(this);
    	((RadioButton) findViewById(R.id.radio_change_in_status_no)).setOnClickListener(this);

    	RadioButton aRb = (RadioButton) findViewById(R.id.radio_present_yes);
  
    	if (contentValues.getAsBoolean(AcdiVocaFind.Q_PRESENT) != null) {
    		if (contentValues.getAsBoolean(AcdiVocaFind.Q_PRESENT))
    			aRb.setChecked(true);
    		aRb = (RadioButton) findViewById(R.id.radio_present_no);
    		if (!contentValues.getAsBoolean(AcdiVocaFind.Q_PRESENT))
    			aRb.setChecked(true);
    	}

    	//  New button - 6/17/11          

    	Spinner spinner = (Spinner)findViewById(R.id.statuschangeSpinner);
		String selected = contentValues.getAsString(AcdiVocaFind.CHANGE_TYPE); 
		int k = 0;  //I was unable to use the spinner function here.
		if(selected != null){
			while (k < spinner.getCount()-1 && !(k == Integer.parseInt(selected))) {
				++k;			
			}
			if (k < spinner.getCount())
				spinner.setSelection(k);
			else
				spinner.setSelection(0);
		}
		else{
			spinner.setSelection(0);
		}

    	aRb = (RadioButton) findViewById(R.id.radio_change_in_status_yes);
    	
    	if (contentValues.getAsString(AcdiVocaFind.Q_CHANGE) != null){

    		if (contentValues.getAsBoolean(AcdiVocaFind.Q_CHANGE)){
    			findViewById(R.id.statuschange).setVisibility(View.VISIBLE);
    			findViewById(R.id.statuschangeSpinner).setVisibility(View.VISIBLE);
    			aRb.setChecked(true);
    		}
    		
    		aRb = (RadioButton) findViewById(R.id.radio_change_in_status_no);
    		
    		if (!contentValues.getAsBoolean(AcdiVocaFind.Q_CHANGE)){
    			findViewById(R.id.statuschange).setVisibility(View.GONE);
    			findViewById(R.id.statuschangeSpinner).setVisibility(View.GONE);
    			aRb.setChecked(true);
    		}
    	}
    }
    
    /**
     * Called when the "edit" button is clicked to change the view to 
     * an editable view. 
     */
    private void setUpEditableView() {
    	setContentView(R.layout.acdivoca_update);  // Should be done after locale configuration
    	displayContentInView(mContentValues);    	
    	((Button)findViewById(R.id.update_to_db_button)).setOnClickListener(this);
    	((Button)findViewById(R.id.update_to_db_button)).setOnClickListener(this);

    	TextView tv = ((TextView) findViewById(R.id.dossier_label));
    	tv.setText(getString(R.string.beneficiary_dossier) + mBeneficiaryId);

    	// Listen for text changes in edit texts and set the isEdited flag
    	((EditText)findViewById(R.id.firstnameEdit)).addTextChangedListener(this);
    	((EditText)findViewById(R.id.lastnameEdit)).addTextChangedListener(this);
//    	((EditText)findViewById(R.id.monthsInProgramEdit)).addTextChangedListener(this);

    	// Initialize the DatePicker and listen for changes
    	Calendar calendar = Calendar.getInstance();

    	((DatePicker)findViewById(R.id.datepicker)).init(
    			calendar.get(Calendar.YEAR),
    			calendar.get(Calendar.MONTH), 
    			calendar.get(Calendar.DAY_OF_MONTH), this);

    	// Listen for clicks on radio buttons
    	((RadioButton)findViewById(R.id.femaleRadio)).setOnClickListener(this);
    	((RadioButton)findViewById(R.id.maleRadio)).setOnClickListener(this);
    	((RadioButton)findViewById(R.id.malnourishedRadio)).setOnClickListener(this);
    	((RadioButton)findViewById(R.id.inpreventionRadio)).setOnClickListener(this);
    	((RadioButton)findViewById(R.id.expectingRadio)).setOnClickListener(this);
    	((RadioButton)findViewById(R.id.nursingRadio)).setOnClickListener(this);

    	////New Listeners - 6/17/11
    	//      
    	((RadioButton)findViewById(R.id.radio_present_yes)).setOnClickListener(this);
    	((RadioButton)findViewById(R.id.radio_present_no)).setOnClickListener(this);
    	((RadioButton)findViewById(R.id.radio_change_in_status_yes)).setOnClickListener(this);
    	((RadioButton)findViewById(R.id.radio_change_in_status_no)).setOnClickListener(this);
    	((Spinner)findViewById(R.id.statuschangeSpinner)).setOnItemSelectedListener(this);
    	
    	final Intent intent = getIntent();
    	mAction = intent.getAction();
    	Log.i(TAG, "mAction = " + mAction);
    	if (mAction.equals(Intent.ACTION_EDIT)) {
    		doEditAction();
    		isProbablyEdited = false; // In EDIT mode, initialize after filling in data
    	}

    }


    /**
     * Returns the result of the Lookup Activity, which gets the beneficiary Id.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult");
        switch(requestCode) {
        case ACTION_ID:
            if (resultCode == RESULT_OK) {
                mBeneficiaryId = data.getStringExtra("Id");
               // Toast.makeText(this, "Beneficiary Id = " + " " + beneficiaryId, Toast.LENGTH_LONG).show();
                break;
            } else {
                finish();
            }
        }   
    }
    
    
    /**
     * Allows editing of editable data for existing finds.  For existing finds, 
     * we retrieve the Find's data from the DB and display it in a TextView. The
     * Find's location and time stamp are not updated.
     */    
    private void doEditAction() {
        Log.i(TAG, "doEditAction");
        
		try {
			Dao<AcdiVocaFind, Integer>  avFindDao = this.dbManager.getAcdiVocaFindDao();
			AcdiVocaFind avFind = AcdiVocaFind.fetchByAttributeValue(avFindDao, AcdiVocaFind.DOSSIER, mBeneficiaryId);
   			if (avFind != null) {
   				ContentValues values = avFind.toContentValues();
	    		if (values == null) {
	    			Toast.makeText(this, getString(R.string.toast_no_beneficiary) + mBeneficiaryId, Toast.LENGTH_SHORT).show();
	    		} else {
	    			Log.i(TAG, values.toString());
	    	        displayContentInView(values);                        
	    		}    				
			} else {
				Log.e(TAG, "Db Error: Unable to retrieve find, dossier = " + mBeneficiaryId);
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}	
	}

    /**
     * Retrieves values from the View fields and stores them as <key,value> pairs in a ContentValues.
     * This method is invoked from the Save menu item.  It also marks the find 'unsynced'
     * so it will be updated to the server.
     * @return The ContentValues hash table.
     */
    protected Find retrieveContentFromView() {
    	Log.i(TAG, "retrieveContentFromView");
    	ContentValues result = new ContentValues();

    	if (inEditableMode) {
    		EditText eText = (EditText) findViewById(R.id.lastnameEdit);


    		String value = eText.getText().toString();
    		result.put(AcdiVocaFind.LASTNAME, value);
    		Log.i(TAG, "retrieve LAST NAME = " + value);

    		eText = (EditText)findViewById(R.id.firstnameEdit);
    		value = eText.getText().toString();
    		result.put(AcdiVocaFind.FIRSTNAME, value);

//    		eText = (EditText)findViewById(R.id.monthsInProgramEdit);
//    		value = eText.getText().toString();
//    		result.put(AcdiVocaFind.MONTHS_REMAINING, value);

    		//value = mMonth + "/" + mDay + "/" + mYear;
    		DatePicker picker = ((DatePicker)findViewById(R.id.datepicker));
    		value = picker.getYear() + "/" + picker.getMonth() + "/" + picker.getDayOfMonth();
    		Log.i(TAG, "Date = " + value);
    		result.put(AcdiVocaFind.DOB, value);

    		RadioButton sexRB = (RadioButton)findViewById(R.id.femaleRadio);
    		String sex = "";
    		if (sexRB.isChecked()) 
    			sex = AcdiVocaFind.FEMALE;
    		sexRB = (RadioButton)findViewById(R.id.maleRadio);
    		if (sexRB.isChecked()) 
    			sex = AcdiVocaFind.MALE;
    		result.put(AcdiVocaFind.SEX, sex);     

    		// Set the Beneficiary's category (4 exclusive radio buttons)
    		String category = "";
    		RadioButton rb = (RadioButton)findViewById(R.id.malnourishedRadio);
    		if (rb.isChecked()) 
    			category = AcdiVocaFind.MALNOURISHED;
    		rb = (RadioButton)findViewById(R.id.inpreventionRadio);
    		if (rb.isChecked())
    			category = AcdiVocaFind.PREVENTION;

    		rb = (RadioButton)findViewById(R.id.expectingRadio);
    		if (rb.isChecked()) 
    			category = AcdiVocaFind.EXPECTING;
    		rb = (RadioButton)findViewById(R.id.nursingRadio);
    		if (rb.isChecked())
    			category = AcdiVocaFind.NURSING;
    		result.put(AcdiVocaFind.BENEFICIARY_CATEGORY, category);   

    	}

    	RadioButton presentRB = (RadioButton)findViewById(R.id.radio_present_yes);
    	String present = "";
    	if (presentRB.isChecked()) 
    		present = AcdiVocaFind.TRUE;
    	presentRB = (RadioButton)findViewById(R.id.radio_present_no);
    	if (presentRB.isChecked()) 
    		present = AcdiVocaFind.FALSE;
    	result.put(AcdiVocaFind.Q_PRESENT, present); 

    	// New button - 6/17/11        

    	RadioButton changeRB = (RadioButton)findViewById(R.id.radio_change_in_status_yes);
    	String change = "";
    	if (changeRB.isChecked()) 
    		change = AcdiVocaFind.TRUE;
    	changeRB = (RadioButton)findViewById(R.id.radio_change_in_status_no);
    	if (changeRB.isChecked()) 
    		change = AcdiVocaFind.FALSE;
    	result.put(AcdiVocaFind.Q_CHANGE, change); 

    	int spinnerInt;
    	Spinner spinner = (Spinner)findViewById(R.id.statuschangeSpinner);
    	if (spinner != null) {
//    		spinnerStr = (String) spinner.getSelectedItem();
    		//Note: I changed how the spinner gets data. It is now based on position
    		spinnerInt = spinner.getSelectedItemPosition();
//    		spinnerStr = strArrSpin[spinnerInt];
    		result.put(AcdiVocaFind.CHANGE_TYPE, String.valueOf(spinnerInt));
    	}

    	//return result;
    	return new AcdiVocaFind();
    }

    /**
     * Displays values from a ContentValues in the View.
     * @param contentValues stores <key, value> pairs
     */
    private void displayContentInView(ContentValues contentValues) {
        Log.i(TAG, "displayContentInView");
        EditText eText = (EditText) findViewById(R.id.lastnameEdit);
        eText.setText(contentValues.getAsString(AcdiVocaFind.LASTNAME));

        eText = (EditText) findViewById(R.id.firstnameEdit);
        eText.setText(contentValues.getAsString(AcdiVocaFind.FIRSTNAME));
        Log.i(TAG,"display First Name = " + contentValues.getAsString(AcdiVocaFind.FIRSTNAME));
        
        DatePicker dp = (DatePicker) findViewById(R.id.datepicker);
        String date = contentValues.getAsString(AcdiVocaFind.DOB);
        int yr=0, mon=0, day=0;
		day = Integer.parseInt(date.substring(date.lastIndexOf("/")+1));
		yr = Integer.parseInt(date.substring(0,date.indexOf("/")));
		mon = Integer.parseInt(date.substring(date.indexOf("/")+1,date.lastIndexOf("/")));

		try {
        if (date != null) {
            Log.i(TAG,"display DOB = " + date);
            dp.init(yr, mon, day, this);
        }
        } catch (IllegalArgumentException e) {
        	Log.e(TAG, "Illegal Argument, probably month == 12 in " + date);
        	e.printStackTrace();
        }

//        eText = (EditText)findViewById(R.id.monthsInProgramEdit);
//        eText.setText(contentValues.getAsString(AcdiVocaFind.MONTHS_REMAINING));

        
        // Chris - 6/9/11 - Filling the form            
        
        RadioButton sexRB = (RadioButton)findViewById(R.id.maleRadio);
        Log.i(TAG, "sex=" + contentValues.getAsString(AcdiVocaFind.SEX));
        if (contentValues.getAsString(AcdiVocaFind.SEX).equals(AcdiVocaFind.MALE.toString()))
            sexRB.setChecked(true);
        sexRB = (RadioButton)findViewById(R.id.femaleRadio);
        if (contentValues.getAsString(AcdiVocaFind.SEX).equals(AcdiVocaFind.FEMALE.toString())){
            sexRB.setChecked(true);
        }
        
        RadioButton motherRB = (RadioButton) findViewById(R.id.expectingRadio);
        String cat = contentValues.getAsString(AcdiVocaFind.BENEFICIARY_CATEGORY);
        if (cat != null){
        	if (cat.equals(AcdiVocaFind.EXPECTING.toString()))
        		motherRB.setChecked(true);
        	else
        		motherRB.setChecked(false);
        	motherRB = (RadioButton)findViewById(R.id.nursingRadio);
        	if (cat.equals(AcdiVocaFind.NURSING.toString()))
        		motherRB.setChecked(true);
        	else
        		motherRB.setChecked(false);
        	RadioButton infantRB = (RadioButton) findViewById(R.id.malnourishedRadio);
        	if (cat.equals(AcdiVocaFind.MALNOURISHED.toString()))
        		infantRB.setChecked(true);
        	else
        		motherRB.setChecked(false);
        	infantRB = (RadioButton)findViewById(R.id.inpreventionRadio);
        	if (cat.equals(AcdiVocaFind.PREVENTION.toString()))
        		infantRB.setChecked(true);
        	else
        		motherRB.setChecked(false);
        }
        else{
        	motherRB.setChecked(false);
        	motherRB = (RadioButton)findViewById(R.id.nursingRadio);
        	motherRB.setChecked(false);
        	motherRB = (RadioButton)findViewById(R.id.malnourishedRadio);
        	motherRB.setChecked(false);
        	motherRB = (RadioButton)findViewById(R.id.inpreventionRadio);
        	motherRB.setChecked(false);
        }
        
        displayUpdateQuestions(contentValues);
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
    			isProbablyEdited = true;
    		}
    	} catch (ClassNotFoundException e) {
    		e.printStackTrace();
    	}

    	// If the datePicker was clicked, mark the form as edited
    	int id = v.getId();
    	if (id == R.id.datepicker) 
    		isProbablyEdited = true;

    	//New code for the spinner - 9/17/11        

    	if (id == R.id.radio_change_in_status_yes){
    		findViewById(R.id.statuschange).setVisibility(View.VISIBLE);
    		findViewById(R.id.statuschangeSpinner).setVisibility(View.VISIBLE);	
    	}

    	if (id == R.id.radio_change_in_status_no){
    		findViewById(R.id.statuschange).setVisibility(View.GONE);
    		findViewById(R.id.statuschangeSpinner).setVisibility(View.GONE);	
    	}

    	if (v.getId() == R.id.update_edit_button) {
    		inEditableMode = true;
    		setUpEditableView();
    	}

    	//  Save Button
    	if(v.getId()==R.id.update_to_db_button) {
    		
    		boolean success = false;
//    		ContentValues data = this.retrieveContentFromView(); 
////    		Log.i(TAG, "Retrieved = " + data.toString());
//    		success = updateExistingFind(data);
    	}
    }
    
	/**
	 * Helper method to update an existing Find from data extracted from the form.
	 * @param data
	 * @return
	 */
	private boolean updateExistingFind(ContentValues data) {
		boolean success = false;

		try {
			Dao<AcdiVocaFind, Integer> dao = this.dbManager.getAcdiVocaFindDao();
			AcdiVocaFind avFind = AcdiVocaFind.fetchByAttributeValue(dao, AcdiVocaFind.DOSSIER, mBeneficiaryId);
			avFind.updateFromContentValues(data);
			int rows = dao.update(avFind);
			success = rows == 1;
    		if (success){
        		Log.i(TAG, "Updated to Db for Find with dossier = " + mBeneficiaryId);
    		}
    		else {
        		Log.e(TAG, "Db Error in attempt to update Find with dossier =  " + mBeneficiaryId);
    			Toast.makeText(this, getString(R.string.toast_db_error), Toast.LENGTH_SHORT).show();
    		}
    		finish();
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
        if(keyCode==KeyEvent.KEYCODE_BACK && isProbablyEdited){  // 
            Toast.makeText(this, getString(R.string.toast_Backkey_isEdited) +  isProbablyEdited, Toast.LENGTH_SHORT).show();
            

            
            
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
					needsabutton.setText(R.string.acdivoca_cancel);
					needsabutton.invalidate();
					
					break;
		}
	}
    
    public void onDateChanged(DatePicker view, int year, int monthOfYear,
            int dayOfMonth) {
        Log.i(TAG, "onDateChanged");
        isProbablyEdited = true;
    }

    //  The remaining methods are part of unused interfaces inherited from the super class.
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) { }
    public void onLocationChanged(Location location) {    }
    public void onProviderDisabled(String provider) {    }
    public void onProviderEnabled(String provider) {    }
    public void onStatusChanged(String provider, int status, Bundle extras) {    }


    /**
     * Sets the 'edited' flag if text has been changed in an EditText
     */
    public void afterTextChanged(Editable arg0) {
        Log.i(TAG, "afterTextChanged " + arg0.toString());
        isProbablyEdited = true;        
    }

    // Unused
    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) { }
    public void onNothingSelected(AdapterView<?> arg0) { }
}
