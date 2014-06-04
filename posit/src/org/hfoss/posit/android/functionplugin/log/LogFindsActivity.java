/*
 * File: LogFindsActivity.java
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

package org.hfoss.posit.android.functionplugin.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.database.DbManager;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;

/**
 * Plugin to log a Find to a log file.
 *
 */
public class LogFindsActivity extends OrmLiteBaseActivity<DbManager> {

	public static final String TAG = "LogFindsActivity";
	public static final int IS_LOGGED = 1;
	private static final String DEFAULT_LOG_DIRECTORY = "log";
	private static final String DEFAULT_LOG_FILE = "log.txt";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		int count = 0;
		super.onResume();
		List<? extends Find> finds = this.getHelper().getAllFinds();
//		Toast.makeText(this, "Saving Finds to Log File", Toast.LENGTH_LONG).show();
		count = logFinds(finds);
		if (count  >= 0) {
			finish();
			Toast.makeText(
					this, count + 
					" Finds saved to SD Card: " + DEFAULT_LOG_DIRECTORY + "/"
							+ DEFAULT_LOG_FILE, Toast.LENGTH_LONG).show();
		} else {
			finish();
			Toast.makeText(
					this,
					"Error while writing to file: " + DEFAULT_LOG_DIRECTORY + "/"
							+ DEFAULT_LOG_FILE, Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Appends Finds (as strings) to a text file on the SD card.
	 * 
	 * @param finds, a list of Find records	 * 
	 * @return True if Finds were written successfully, False otherwise.
	 */
	protected int logFinds(List<? extends Find> finds) {
		int count = 0;
		try {
			File dir = new File(Environment.getExternalStorageDirectory()
					+ "/" + DEFAULT_LOG_DIRECTORY);
			if (!dir.exists()) {
				if (dir.mkdir()) {
					Log.i(TAG, "Created directory " + dir);
				}
			}
			if (dir.canWrite()) {
				Log.i(TAG, dir + " is writeable");
			}
            File file = new File(Environment.getExternalStorageDirectory()
                    + "/" + DEFAULT_LOG_DIRECTORY 
                    + "/"
                    + DEFAULT_LOG_FILE);
            if (!file.exists()) {
            	if (file.createNewFile()) 
            		Log.i(TAG, "Created file " + file);
            }
            
			PrintWriter writer = new PrintWriter(new BufferedWriter(
					new FileWriter(file, true)));
			
			// NOTE:  For now we use the Find's isAdhoc field (which is unused) for
			// recording whether the Find is logged.

			Iterator<? extends Find> it = finds.iterator();
			while (it.hasNext()) {
				Find find = it.next();
				Log.i(TAG, "Find = " + find);
				
				if (find.getDeleted() != IS_LOGGED) {
					find.setDeleted(IS_LOGGED);
//				if (find.getIs_adhoc() != IS_LOGGED) {
//					find.setIs_adhoc(IS_LOGGED);

					
					getHelper().update(find);
					writer.println(new Date() + ": " + find);
					Log.i(TAG, "Wrote to file: " + find);
					++count;
				}
			}
			writer.flush();
			writer.close();
			return count;
		} catch (IOException e) {
			Log.e(TAG, "IO Exception writing to Log " + e.getMessage());
			e.printStackTrace();
			return -1;
		}
	}

}
