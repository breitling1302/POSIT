/*
 * File: FilePickerActivity.java
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.hfoss.posit.android.R;

/**
 * Simple file chooser allows the user to browse the file system.  Returns 
 * onActivityResult the name of the selected file. No browsing restrictions are
 * enforced.
 * 
 * @see http://www.dreamincode.net/forums/topic/190013-creating-simple-file-chooser/
 */
public class FilePickerActivity extends ListActivity {

	public static final String TAG = "FilePicker";
	public static final int ACTION_CHOOSER = 1;
	public static final int RESULT_OK = 1;
	public static final int RESULT_ERROR = 0;

	private File currentDir;
	private FileArrayAdapter adapter;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = this.getIntent();
		 Bundle extras = intent.getExtras();
			if (extras == null) {
				return;
			}
		String home = extras.getString("home");
		currentDir = new File(home);
		Log.i(TAG, "Current directory =" + currentDir);
		
		displayFiles(currentDir);

	}
	
	/**
	 * Helper to display files given a directory name.  Files
	 * and hidden directories are ignored. 
	 * @param dir, a File which can be either a file or a directory.
	 */
	private void displayFiles(File dir) {
		Log.i(TAG, "displayFiles, dir=" + dir);
		if (!dir.isDirectory() || dir.isHidden())
			return;
		currentDir = dir;
		List<String> datafiles = getFileNames(dir);
		if (datafiles.size() == 0) 
			setContentView(R.layout.acdivoca_list_files);
        adapter = new FileArrayAdapter(this, R.layout.acdivoca_list_files, datafiles );
        this.setListAdapter(adapter);		
	}
	
	/**
	 * Helper method returns a list of files and directories in the
	 * current directory.
	 * @param dir
	 * @return
	 */
	private List<String> getFileNames(File dir) {
		Log.i(TAG, "dir=" + dir);
		File files[] = dir.listFiles();
		List<String> datafiles = new ArrayList<String>();
		try {
			datafiles.add("..");
			for (File ff: files) {
				if ((ff.isFile() && !ff.getName().startsWith(".")) || 
						(ff.isDirectory() && !ff.getName().startsWith("."))) {
					datafiles.add(ff.getName());
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "IO Exception");
			e.printStackTrace();
			Intent returnIntent = new Intent();
			setResult(RESULT_ERROR, returnIntent);
			finish();
		}
		return datafiles;
	}
	
	
	/**
	 * Inherited method for handling clicks on list items.  If a filename
	 * is clicked, return it.  If a directory is picked, make it the current
	 * directory and continue.
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		String filename = adapter.getItem(position);
		Log.i(TAG, "filename clicked =" + filename);

		File f = null;

		try {
			if (filename.equals("..") ) { // Can't go above sdcard
				f = new File(currentDir.getParentFile().getAbsolutePath());
			} else {
				f = new File(currentDir + File.separator + filename);
			}
			if (f.isDirectory()) {
				displayFiles(f);
			} else {
				Intent returnIntent = new Intent();
				returnIntent.putExtra(Intent.ACTION_CHOOSER, f.getAbsolutePath());
				setResult(RESULT_OK, returnIntent);
				finish();
			}
		} catch (Exception e) {
			Log.e(TAG, "IO Exception");
			e.printStackTrace();
			Intent returnIntent = new Intent();
			setResult(RESULT_ERROR, returnIntent);
			finish();
		}
	}

	/**
	 * Adapter for displaying file names.
	 *
	 */
	class FileArrayAdapter extends ArrayAdapter<String>{

		private Context c;
		private int id;
		private List<String>items;

		public FileArrayAdapter(Context context, int textViewResourceId,
				List<String> filenames) {
			super(context, textViewResourceId, filenames);
			c = context;
			id = textViewResourceId;
			items = filenames;
		}
		public String getItem(int i){
			return items.get(i);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.acdivoca_list_files_row, null);
			}
			final String filename = items.get(position);
			if (filename != null) {
				TextView t1 = (TextView) v.findViewById(R.id.filename);
				t1.setText(filename);
			}
			return v;
		}
	}
}
