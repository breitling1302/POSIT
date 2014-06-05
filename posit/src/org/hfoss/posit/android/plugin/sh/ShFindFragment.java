/*
 * File: ShFindFragment.java
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
package org.hfoss.posit.android.plugin.sh;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.fragment.FindFragment;

import android.os.Bundle;
import android.util.Log;
import android.widget.RadioButton;

public class ShFindFragment extends FindFragment {

	private static final String TAG = "ShFindFragment";
	
	/**
	 * Creates the UI elements that extend the Basic Find's UI.
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.i(TAG, "onActivityCreated()");
		super.onActivityCreated(savedInstanceState);
		
		((RadioButton)getView().findViewById(R.id.pickupRadio)).setOnClickListener(this);
		((RadioButton)getView().findViewById(R.id.dropoffRadio)).setOnClickListener(this);
	}
	
	/**
	 * Retrieves values from the ShFind fields and stores them in 
	 * a Find instance. Note that it also calls superclass method.
	 * 
	 * This method is invoked from the Save menu item. 
	 * Finds are marked 'unsynced' after this method.
	 * 
	 * @return a new Find object with data from the view.
	 */
	@Override
	protected Find retrieveContentFromView() {
		ShFind find =  (ShFind)super.retrieveContentFromView();
		
		RadioButton rb1 = (RadioButton)getView().findViewById(R.id.pickupRadio);
		RadioButton rb2 = (RadioButton)getView().findViewById(R.id.dropoffRadio);
		if (rb1 != null && rb1.isChecked()){
			find.setStopType(ShFind.PICKUP);
		}
		if (rb2 != null && rb2.isChecked()){
			find.setStopType(ShFind.DROPOFF);
		}
		return find;
	}

	/**
	 * Adds UI elements for the ShFind to the element
	 * displayed by the superclass method.
	 */
	@Override
	protected void displayContentInView(Find find) {
		super.displayContentInView(find);
		
		RadioButton rb1 = (RadioButton)getView().findViewById(R.id.pickupRadio);
		int val = ((ShFind)find).getStopType();
		if (val == ShFind.PICKUP)
			rb1.setChecked(true);
		RadioButton rb2 = (RadioButton)getView().findViewById(R.id.dropoffRadio);
		if (val == ShFind.DROPOFF)
			rb2.setChecked(true);		
	}
}
