/*
 * File: CsvListFindActivity.java
 * 
 * Copyright (C) 2011 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Source Information Tool. 
 *
 * This code is free software; you can redistribute it and/or modify
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
package org.hfoss.posit.android.plugin.csv;

import java.io.File;

import org.hfoss.posit.android.api.activity.ListFindsActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;

/**
 * A CsvFind is a Find that is created from a CSV record -- i.e.,
 * a comma-delimited spreadsheet row.  The columns of the record
 * cannot contain embedded commas.  
 * 
 * This activity reads the necessary information from the plugin spec
 * and then reads in and displays a Csv file.  Typically the file
 * will contain location data (GPS).  
 * 
 */
public class CsvListFindsActivity extends ListFindsActivity {
	
	/**
	 * This activity can be started from PositMain without filename
	 * in the intent or from FileViewActivity with a filename. 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		finds = new CsvListFindsFragment();
		super.onCreate(savedInstanceState);
	}
}
