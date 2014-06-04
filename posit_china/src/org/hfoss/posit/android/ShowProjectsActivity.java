/*
 * File: ShowProjectsActivity.java
 * 
 * Copyright (C) 2009 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Search and Identification Tool.
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
package org.hfoss.posit.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.hfoss.posit.android.utilities.Utils;
import org.hfoss.posit.android.web.Communicator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

/**
 * This activity shows a list of all the projects on the server that the phone is registered with,
 * and allows the user to pick one from the list.  When the user picks one, the phone automatically
 * syncs with the server to get all the finds from that project
 * 
 *
 */
public class ShowProjectsActivity extends ListActivity implements OnClickListener{

	private static final String TAG = "ShowProjectsActivity";
	private static final int CONFIRM_PROJECT_CHANGE = 0;
	static final int NEW_PROJECT = 1;
	private int mClickedPosition = 0;

	private ArrayList<HashMap<String, Object>> projectList;

	/**
	 * Called when the activity is first started.  Shows a list of 
	 * radio buttons, each representing
	 * a different project on the server.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_proj);
		Button addProjectButton = (Button)findViewById(R.id.idAddProjButton);
		addProjectButton.setOnClickListener(this);

		showProjects();
		
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
	}

	private void showProjects() {
		if (!Utils.isConnected(this)) {
			reportNetworkError("No Network connection ... exiting");
			return;
		} 
		Communicator comm = new Communicator(this);
		try{
			projectList = comm.getProjects();
		} catch(Exception e){
			Log.i(TAG, "Communicator error " + e.getMessage());
			e.printStackTrace();
			this.reportNetworkError(e.getMessage());
			finish();
		}
		if (projectList != null) {
			Iterator<HashMap<String, Object>> it = projectList.iterator();
			ArrayList<String> projList = new ArrayList<String>();
			for(int i = 0; it.hasNext(); i++) {
				HashMap<String,Object> next = it.next();
				projList.add((String)(next.get("name")));
			}
			setListAdapter(new ArrayAdapter<String>(this,
			          android.R.layout.simple_list_item_1, projList));
			
		} else {
			this.reportNetworkError("Null project list returned.\nMake sure your server is reachable.");
		}
	} 
	

	
	/**
	 * Reports as much information as it can about the error.
	 * @param str
	 */
	private void reportNetworkError(String str) {
		Log.i(TAG, "Registration Failed: " + str);
		Utils.showToast(this, "Registration Failed: " + str);
		finish();
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == NEW_PROJECT)
			showProjects();
	}

	public void onListItemClick(ListView lv, View v, int position, long idFull){
		mClickedPosition = position;
		String projectId = (String) projectList.get(mClickedPosition).get("id");
		int id  = Integer.parseInt(projectId);
		String projectName = (String) projectList.get(mClickedPosition).get("name");
		
		/* Confirms with the user that they have changed their project and 
		 * automatically syncs with the server
		 * to get all the project finds
		 */
		AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
		alt_bld.setIcon(R.drawable.icon);
		alt_bld.setTitle("Project Selection");
		alt_bld.setMessage("Would you like to select " 
				+ (String) projectList.get(mClickedPosition).get("name") 
				+ " as your current project?");
		// If user confirms selection, start sync of finds 
		alt_bld.setPositiveButton(R.string.alert_dialog_ok,
						new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						Log.i(TAG, "User confirmed project selection to: " 
							+ (String) projectList.get(mClickedPosition)
							.get("name"));
						Context appCont = getApplicationContext();
						
						String projectId = (String) projectList.get(mClickedPosition).get("id");
						int id  = Integer.parseInt(projectId);
						String projectName = (String) projectList.get(mClickedPosition).get("name");
						
						SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(appCont);
						int currentProjectId = sp.getInt("PROJECT_ID",0);
						
						if (id == currentProjectId){
							Utils.showToast(appCont, "'" + projectName + "' is already the current project.");
							finish();
							return;
						}
						
						Editor editor = sp.edit();

						editor.putInt("PROJECT_ID", id);
						editor.putString("PROJECT_NAME", projectName);
						editor.commit();
						
						sp = PreferenceManager.getDefaultSharedPreferences(
								ShowProjectsActivity.this);
						
						boolean syncIsOn = sp.getBoolean("SYNC_ON_OFF", true);
						if (syncIsOn) {
							Intent intent = new Intent(ShowProjectsActivity.this, SyncActivity.class);
							intent.setAction(Intent.ACTION_SYNC);
							startActivity(intent);
						}
						
						// New project should be set in preferences
						Log.i(TAG, "Preferences= " + sp.getAll().toString());
						
						finish();
					}
				});
		// If user cancels the selection, close dialog
		alt_bld.setNegativeButton(R.string.alert_dialog_cancel,
						new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						Log.i(TAG, "User cancelled project selection");
						// No project should be set in preferences
						Context appCont = getApplicationContext();
						SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(appCont);
						Log.i(TAG, "Preferences= " + sp.getAll().toString());
						dialog.cancel();
						
					}
				});
		alt_bld.show();
	}
	/**
	 * Called when the user clicks on a project in the list.  Sets the project id in the shared
	 * preferences so it can be remembered when the application is closed
	 */
	public void onClick(View v) {
		Intent i = new Intent(this, NewProjectActivity.class);;
		switch (v.getId()) {

		case R.id.idAddProjButton:
			startActivityForResult(i,NEW_PROJECT);

			break;
		}
		
	}
	
	/**
     * This method is used to block the user from selecting to go back or to
     * press on the menu button without selecting a project. The user will be
     * prompted to select a project in these cases. If the user previously
     * selected a project, the action will be allowed.
     * @param keyCode is an integer representing which key is pressed
     * @param event is a KeyEvent that is not used here
     * @return a boolean telling whether or not the operation was successful
     */
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK || 
                keyCode == KeyEvent.KEYCODE_MENU) {
        	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
           	// If user previously selected a project than allow action 
        	if (sp.getInt("PROJECT_ID",0) == 0) {
	            Utils.showToast(this, "Please select a project");
	            return true;
        	}
        }
        return super.onKeyDown(keyCode, event);
    }
    
}