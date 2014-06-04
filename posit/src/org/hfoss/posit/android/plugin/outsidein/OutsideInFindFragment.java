/*
 * File: OutsideInFindFragment.java
 * 
 * Copyright (C) 2012 The Humanitarian FOSS Project (http://www.hfoss.org)
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
package org.hfoss.posit.android.plugin.outsidein;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.fragment.FindFragment;

import android.util.Log;
import android.widget.CheckBox;
import android.widget.EditText;

public class OutsideInFindFragment extends FindFragment {

	private static final String TAG = "OutsideInFindFragment";

	/**
	 * Retrieves values from the OutsideInFind fields and stores them in a Find instance. 
	 * This method is invoked from the Save menu item. It also marks the find
	 * 'unsynced' so it will be updated to the server.
	 * 
	 * @return a new Find object with data from the view.
	 */
	@Override
	protected Find retrieveContentFromView() {
		OutsideInFind find =  (OutsideInFind)super.retrieveContentFromView();
		String value; //used to get the string from the textbox

		EditText eText = (EditText) getView().findViewById(R.id.syringesInEditText);
		//If no value is supplied, set it to 0.
		if(eText.getText().toString().equals("")){
			value = "0";
		}
		else{
			value = eText.getText().toString();
		}
		find.setSyringesIn(Integer.parseInt(value));

		eText = (EditText) getView().findViewById(R.id.syringesOutEditText);
		if(eText.getText().toString().equals("")){
			value = "0";
		}
		else{
			value = eText.getText().toString();
		}
		find.setSyringesOut(Integer.parseInt(value));

		CheckBox checkBox = (CheckBox) getView().findViewById(R.id.isNewCheckBox);
		find.setIsNew(checkBox.isChecked());

		// Retrieve unique ID created by with:
		// first 2 letters of the first name, first 2 letters of their mom's name, month of birth, and year of birth
		find.setGuid(getGuidEditText());

		return find;
	}

	@Override
	protected void displayContentInView(Find find) {
		OutsideInFind oiFind = (OutsideInFind)find;
		
		EditText et = (EditText)getView().findViewById(R.id.syringesInEditText);
		et.setText(Integer.toString(oiFind.getSyringesIn()));
		
		et = (EditText)getView().findViewById(R.id.syringesOutEditText);
		et.setText(Integer.toString(oiFind.getSyringesOut()));
		
		CheckBox cb = (CheckBox)getView().findViewById(R.id.isNewCheckBox);
		cb.setChecked(oiFind.getIsNew());
		
		String guid = oiFind.getGuid();
		if(guid.length() == 8) {
			et = (EditText)getView().findViewById(R.id.idEditText_fname);
			et.setText(guid.substring(0, 2));
			et = (EditText)getView().findViewById(R.id.idEditText_mname);
			et.setText(guid.substring(2, 4));
			et = (EditText)getView().findViewById(R.id.idEditText_mbirth);
			et.setText(guid.substring(4, 6));
			et = (EditText)getView().findViewById(R.id.idEditText_ybirth);
			et.setText(guid.substring(6, 8));
		}	
	}

	/**
	 * A valid Guid must have the form AABB0011. This overrides
	 * the default validity test.
	 */
	@Override
	protected boolean isValidGuid(String guid) {
		if (guid.length() != 8) 
			return false;
		int month = Integer.parseInt(guid.substring(4,6));
		Log.i(TAG, "Month = " + month);
		if (month < 1 || month > 12)
			return false;
		int year = Integer.parseInt(guid.substring(6));
		Log.i(TAG, "Year = " + year);
		if (year < 20)  // valid birth years 1920 - 1999 (so 12-91 years old??)
			return false; 
		return true;
	}

	/**
	 * Retrieve unique ID created with:
	 * first 2 letters of the first name, first 2 letters of their mom's name, month of birth, and year of birth
	 * @return String with all 4 components combined.
	 */
	private String getGuidEditText() {
		EditText eText = (EditText) getView().findViewById(R.id.idEditText_fname);
		StringBuilder guid = new StringBuilder();
		guid.append(eText.getText().toString());

		eText = (EditText) getView().findViewById(R.id.idEditText_mname);
		guid.append(eText.getText().toString());

		eText = (EditText) getView().findViewById(R.id.idEditText_mbirth);
		if (eText.getText().toString().length() == 1) {
			guid.append("0").append(eText.getText().toString());
		} else {
			guid.append(eText.getText().toString());
		}		

		eText = (EditText) getView().findViewById(R.id.idEditText_ybirth);
		if (eText.getText().toString().length() == 1) {
			guid.append("0").append(eText.getText().toString());
		} else {
			guid.append(eText.getText().toString());
		}		

		return guid.toString();
	}
	
	@Override
	protected void prepareForSave(Find find) {
		((OutsideInFind)find).isLogged = false;
		
		if (this.mCurrentLocation != null) {
			find.setLatitude(mCurrentLocation.getLatitude());
			find.setLongitude(mCurrentLocation.getLongitude());
		}
		super.prepareForSave(find);
	}
}
