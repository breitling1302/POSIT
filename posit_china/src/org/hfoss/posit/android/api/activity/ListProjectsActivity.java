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
package org.hfoss.posit.android.api.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


import org.hfoss.posit.android.R;
import org.hfoss.posit.android.sync.Communicator;
import org.hfoss.posit.android.sync.SyncAdapter;

import com.actionbarsherlock.app.SherlockListActivity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Toast;

/**
 * This activity shows a list of all the projects on the server that the phone is registered with,
 * and allows the user to pick one from the list.  When the user picks one, the phone automatically
 * syncs with the server to get all the finds from that project
 * 
 *
 */
public class ListProjectsActivity extends SherlockListActivity implements OnClickListener{

	private static final String TAG = "ShowProjectsActivity";
	private static final int CONFIRM_PROJECT_CHANGE = 0;
	static final int NEW_PROJECT = 1;
	private int mClickedPosition = 0;

	private ArrayList<HashMap<String, Object>> projectList;
	Handler handler = new Handler();
	private RadioGroup mRadio;	

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

		Communicator.attemptGetProjects(handler, this);

	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
	//	tryToRegister();
	}

	private void showProjects(List<HashMap<String,Object>> projects) {
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
	
	public void onShowProjectsResult(ArrayList<HashMap<String,Object>> projects, Boolean result) {
		Log.i(TAG, "The result of showing projects was: " + result);
		projectList = projects;
		showProjects(projectList);
	}
	

	
	/**
	 * Reports as much information as it can about the error.
	 * @param str
	 */
	private void reportNetworkError(String str) {
		Log.i(TAG, "Registration Failed: " + str);
		Toast.makeText(this, "Registration Failed: " + str, Toast.LENGTH_LONG).show();
		finish();
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == NEW_PROJECT) {
			Communicator.attemptGetProjects(handler, this);
		}
	}

	public void onListItemClick(ListView lv, View v, int position, long idFull){
		mClickedPosition = position;
		ArrayAdapter mAdapter = (ArrayAdapter) this.getListAdapter();
		String projectId = (String) projectList.get(mClickedPosition).get("id");
		int id  = Integer.parseInt(projectId);
		String projectName = (String) projectList.get(mClickedPosition).get("name");
		
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		int currentProjectId = sp.getInt(getString(R.string.projectPref),0);
		
		if (id == currentProjectId){
			Toast.makeText(this, "'" + projectName + "' is already the current project.", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		
		Editor editor = sp.edit();

		editor.putInt(getString(R.string.projectPref), id);
		editor.putString(getString(R.string.projectNamePref), projectName);
		editor.commit();

		showDialog(CONFIRM_PROJECT_CHANGE);
	}
	/**
	 * Called when the user clicks on a project in the list.  Sets the project id in the shared
	 * preferences so it can be remembered when the application is closed
	 * 
	 * TODO: NewProjectActivity isn't re-added yet
	 */
	public void onClick(View v) {
		Intent i = new Intent(this, NewProjectActivity.class);;
		switch (v.getId()) {

		case R.id.idAddProjButton:
			startActivityForResult(i,NEW_PROJECT);
			break;

			
		}
		
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreateDialog(int)
	 * Confirms with the user that they have changed their project and automatically syncs with the server
	 * to get all the project finds
	 */
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case CONFIRM_PROJECT_CHANGE:
			return new AlertDialog.Builder(this)
			.setIcon(R.drawable.icon)
			.setTitle("You have changed your project to: " 
					+ (String) projectList.get(mClickedPosition).get("name"))
					.setPositiveButton(R.string.alert_dialog_ok, 
							new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(
									ListProjectsActivity.this);
							
							boolean syncIsOn = sp.getBoolean("SYNC_ON_OFF", true);
							if (syncIsOn) {
								AccountManager manager = AccountManager.get(getApplicationContext());
								Account[] accounts = manager.getAccountsByType(SyncAdapter.ACCOUNT_TYPE);
								// Just pick the first account for now.. TODO: make this work for multiple accounts of same type?
								Bundle extras = new Bundle();
								ContentResolver.requestSync(accounts[0], getResources().getString(R.string.contentAuthority), extras);
							}
							finish();
						}
					}).create();
		default:
			return null;
		}
	}



}