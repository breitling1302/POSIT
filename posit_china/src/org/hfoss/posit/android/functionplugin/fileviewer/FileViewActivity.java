/*
 * File: FileViewActivity.java
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

package org.hfoss.posit.android.functionplugin.fileviewer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;


import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.plugin.csv.CsvListFindsActivity;
import org.hfoss.posit.android.plugin.csv.CsvListFindsFragment;
import org.hfoss.posit.android.R;

import com.j256.ormlite.android.apptools.OrmLiteBaseListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Function plugin for opening a file on the phone's SdCard.
 *
 */
public class FileViewActivity extends OrmLiteBaseListActivity<DbManager> {

	public static final String TAG = "FileViewActivity";
	private static final String HOME_DIRECTORY = "csv";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_files);

		// Create /sdcard/HOME_DIRECTORY if it doesn't exist already
		File dir = new File(Environment.getExternalStorageDirectory() + "/"
				+ HOME_DIRECTORY);
		if (!dir.exists()) {
			if (dir.mkdir()) {
				Log.i(TAG, "Created directory " + dir);
			}
		}
		// Start file picker activity using /sdcard/HOME_DIRECTORY as the home
		// directory
		Intent intent = new Intent();
		intent.putExtra("home", Environment.getExternalStorageDirectory() + "/"
				+ HOME_DIRECTORY);
		intent.setClass(this, FilePickerActivity.class);
		this.startActivityForResult(intent, FilePickerActivity.ACTION_CHOOSER);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case FilePickerActivity.ACTION_CHOOSER:
			if (resultCode == FilePickerActivity.RESULT_OK) {
				
				// A filename (absolute path) was returned.  Try to display it.
				String filename = data.getStringExtra(Intent.ACTION_CHOOSER);
				File file = new File(filename);		
				
				// If this is a Csv file, start the CsvList activity to parse it.
				if (filename.endsWith(".csv")) {
					Intent intent = new Intent();
					intent.setClass(this, CsvListFindsActivity.class);
					intent.putExtra(CsvListFindsFragment.FILENAME_TAG, filename);
					this.startActivity(intent);
					
				// If this is a txt file, just display it with line breaks.	
				} else if (filename.endsWith(".txt")) {
					setContentView(R.layout.list_finds);
					String text = readFileAsText(file, "\n-----\n");
					TextView tv = (TextView) findViewById(R.id.emptyText);
					tv.setTextSize(11);
					tv.setText(text);	
				// If this is an XML file, display it as is.
				} else if (filename.endsWith(".xml")) {
						setContentView(R.layout.list_finds);
						String text = readFileAsText(file, "\n");
						TextView tv = (TextView) findViewById(R.id.emptyText);
						tv.setTextSize(11);
						tv.setText(text);						
				// Punt on all other types of files	
				} else {
					Log.i(TAG, "Don't know what to do with " + filename);
					Toast.makeText(this, "Sorry, don't know what to do with " + filename,
							Toast.LENGTH_SHORT).show();
				}

			} else {
				Log.i(TAG, "Exiting with result code = " + resultCode);
				// Result not good, do something about it
//				Toast.makeText(this, "Error occurred in File Picker",
//						Toast.LENGTH_LONG).show();
				finish();
			}
			break;
		default:
			// Shouldn't happen
			Log.e(TAG, "Request code on activity result not recognized");
			finish();
		}
	}
	
	/**
	 * Reads a text file and return it as a String
	 * @param file, should be a text file
	 * @return
	 */
	protected String readFileAsText(File file, String lineseparator) {
		StringBuilder text = new StringBuilder();

		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;

			while ((line = br.readLine()) != null) {
				text.append(line);
				text.append(lineseparator);
			}
		} catch (IOException e) {
			Log.e(TAG, "IO Exception reading from file "
					+ e.getMessage());
			e.printStackTrace();
			Toast.makeText(this, "Error occurred reading from file",
					Toast.LENGTH_LONG).show();
			finish();
		}
		return text.toString();

	}
}
