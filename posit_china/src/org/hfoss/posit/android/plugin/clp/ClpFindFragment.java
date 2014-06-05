/*
 * File: ClpFindFragment.java
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
package org.hfoss.posit.android.plugin.clp;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.fragment.FindFragment;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class ClpFindFragment extends FindFragment {

	private static final String TAG = "ClpFindFragment";

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate()");
		super.onActivityCreated(savedInstanceState);
		
		// Change prompt
		TextView tv = (TextView)getView().findViewById(R.id.nameTextView);
		tv.setText(this.getString(R.string.namePrompt));
	}
}
