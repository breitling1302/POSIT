/*
 * File: PositMain.java
 * 
 * Copyright (C) 2009 The Humanitarian FOSS Project (http://www.hfoss.org)
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
package org.hfoss.posit.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hfoss.posit.android.api.LocaleManager;
import org.hfoss.posit.android.api.User;
import org.hfoss.posit.android.api.activity.ListProjectsActivity;
import org.hfoss.posit.android.api.activity.MapFindsActivity;
import org.hfoss.posit.android.api.activity.OrmLiteBaseFragmentActivity;
import org.hfoss.posit.android.api.activity.SettingsActivity;
import org.hfoss.posit.android.api.authentication.AuthenticatorActivity;
import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.api.plugin.ActiveFuncPluginChangeEventListener;
import org.hfoss.posit.android.api.plugin.FindActivityProvider;
import org.hfoss.posit.android.api.plugin.FindPluginManager;
import org.hfoss.posit.android.api.plugin.FunctionPlugin;
import org.hfoss.posit.android.background.BackgroundListener;
import org.hfoss.posit.android.background.BackgroundManager;
import org.hfoss.posit.android.background.GetAuthKeyCallable;
import org.hfoss.posit.android.background.GetProjectsCallable;
import org.hfoss.posit.android.background.SetProjectCallable;
import org.hfoss.posit.android.sync.Communicator;
import org.hfoss.posit.android.sync.SyncAdapter;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
//import org.hfoss.posit.android.plugin.acdivoca.AttributeManager;

/**
 * Implements the main activity and the main screen for the POSIT application.
 */
public class PositMain extends OrmLiteBaseFragmentActivity<DbManager> implements android.view.View.OnClickListener, ActiveFuncPluginChangeEventListener, OnNavigationListener {

	private static final String TAG = "PositMain";

	private static final int CONFIRM_EXIT = 0;
	private SharedPreferences mSharedPrefs;
	private Editor mSpEditor;	
	private ArrayList<FunctionPlugin> mMainMenuPlugins = null;
	private ArrayAdapter<HashMap<String,Object>> projectsList = null;
	private List<HashMap<String,Object>> projectsHash = null;
	private FunctionPlugin mMainLoginPlugin = null;
	
	// A list of function plugins for this activity
	private ArrayList<FunctionPlugin> mMainButtonPlugins = null;
	
	// A list of services for Posit Main
	private ArrayList<Class<Service>> mServices = null;

	/**
	 * Handles the app's initialization.
	 */ 
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "Creating");

		FindPluginManager pluginManagerInstance = FindPluginManager.initInstance(this);
		pluginManagerInstance.addActiveFuncPluginChangeEventListener(this);

		// NOTE: This is AcdiVoca stuff and should be put in a plugin
		// AcdiVocaSmsManager.initInstance(this);
//		AttributeManager.init();

		// NOTE: Not sure if this is the best way to do this -- perhaps these kinds of prefs
		//  should go in the plugins_preferences.xml
		// A newly installed POSIT should have no shared prefs. Set the default phone pref if
		// and server prefs if not already set.
		Log.i(TAG, "Setting default Prefs");
		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		try {
			String phone = mSharedPrefs.getString(getString(R.string.smsPhoneKey), "");
			if (phone.equals("")) {
				mSpEditor = mSharedPrefs.edit();
				mSpEditor.putString(getString(R.string.smsPhoneKey), getString(R.string.default_phone));
				mSpEditor.commit();
			}
			String server = mSharedPrefs.getString(getString(R.string.serverPref), "");
			if (server.equals("")) {
				mSpEditor = mSharedPrefs.edit();
				mSpEditor.putString(getString(R.string.serverPref), getString(R.string.defaultServer));
				mSpEditor.commit();
			}
			Log.i(TAG, "Preferences= " + mSharedPrefs.getAll().toString());
		} catch (ClassCastException e) {
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
		}
		
		updateReferencesToActivePlugins();
		
		// Check if an account exists
		resolveAccount();

		// Login Extension Point
		// Run login plugin, if necessary
		Log.i(TAG, "Checking if login preference active");
		if (mMainLoginPlugin != null) {
			Log.i(TAG, "Starting login preference");
			Intent intent = new Intent();
			Class<Activity> loginActivity = mMainLoginPlugin.getActivity();
			intent.setClass(this, loginActivity);
			intent.putExtra(User.USER_TYPE_STRING, User.UserType.USER.ordinal());
			Log.i(TAG, "Starting login activity for result");
			if (mMainLoginPlugin.getActivityReturnsResult()) 
				this.startActivityForResult(intent, mMainLoginPlugin.getActivityResultAction());
			else
				this.startActivity(intent);
		} else {
			Log.i(TAG, "Login preference NOT active");
		}
		
		// Start all active services, some of which are defined by plugins. 
		Log.i(TAG, "Starting active services");
		mServices = FindPluginManager.getAllServices();
		for (Class<Service> s : mServices) {
			Log.i(TAG,"Starting service " + s.getSimpleName());
			this.startService(new Intent(this, s));
		}
	}

    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
    	String projectName = (String) projectsHash.get(itemPosition).get("name");
    	
        BackgroundListener<Boolean> bkgListener
        = new BackgroundListener<Boolean>() {
            @Override
            public void onBackgroundResult(Boolean response)
            {
                if (response) {
                    String projectName = (String)getExtra(0);
                    Toast.makeText(PositMain.this,
                            "Project changed to " + projectName,
                            Toast.LENGTH_LONG).show();
                }
            }
        };
        bkgListener.putExtra(projectName);
        
        BackgroundManager.runTask(
                new SetProjectCallable(this, projectsHash, itemPosition),
                bkgListener);
        
        return true;
    }
    
    public class ProjectsListAdapter extends ArrayAdapter<HashMap<String,Object>> {
		protected List<HashMap<String, Object>> items;
		Context context;
		
		public ProjectsListAdapter(Context context, int textViewResourceId,
				List<HashMap<String, Object>> list) {
			super(context, textViewResourceId, list);
			this.items = list;
			this.context = context;
		}
		
    	@Override
    	public View getDropDownView(int position, View convertView, ViewGroup parent) {
    		return getCustomView(position, convertView, parent);
    	}
		
    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) {
    		return getCustomView(position, convertView, parent);
    	}
		
    	public View getCustomView(int position, View convertView, ViewGroup parent) {
    		LayoutInflater inflater=getLayoutInflater();
    		View row=inflater.inflate(R.layout.sherlock_spinner_dropdown_item, parent, false);
    		TextView label=(TextView)row.getRootView();
    		label.setText((String) items.get(position).get("name"));

    		return row;
    	}
    }
	
	/*
	 * If there's no account configured, start account setup.
	 */
	private void resolveAccount() {
		AccountManager accountManager = AccountManager.get(this);
		final int numAccount = (accountManager.getAccountsByType(SyncAdapter.ACCOUNT_TYPE)).length;
		
		if (numAccount == 0) {
			Intent i = new Intent(this, AuthenticatorActivity.class);
			this.startActivity(i);
		}	
	}


	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "Resuming");
		LocaleManager.setDefaultLocale(this); // Locale Manager should be in the API
		//enable when server preference is tied to account
		//resolveAccount();
		startPOSIT();
		
		BackgroundManager.runTask(new GetProjectsCallable(this),
		        new BackgroundListener<List<HashMap<String, Object>>>() {
		    @Override
            public void onBackgroundResult(List<HashMap<String, Object>> res)
            {
                projectsHash = res;
                if (projectsHash != null) {
                    int selectedProj = mSharedPrefs.getInt(
                            PositMain.this.getString(R.string.projectPref), 0);
                    int selectedIndex = 0;
                    
                    for (int i = 0; i < projectsHash.size(); i++) {
                        if (Integer.parseInt((String) projectsHash.get(i)
                                .get("id")) == selectedProj) {
                            selectedIndex = i;
                        }
                    }
                    
                    //Add spinner to actionbar
                    Context context = getSupportActionBar().getThemedContext();
                    projectsList = new ProjectsListAdapter(context,
                            R.layout.sherlock_spinner_item, projectsHash);
                    projectsList.setDropDownViewResource(
                            R.layout.sherlock_spinner_dropdown_item);
                    ActionBar ab = getSupportActionBar();
                    ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
                    ab.setListNavigationCallbacks(projectsList, PositMain.this);
                    ab.setSelectedNavigationItem(selectedIndex);
                }
            }
		});
		
	}

	/**
	 * Initializes the PositMain UI, which can vary depending on which plugins
	 * are active.
	 */
	private void startPOSIT() {
		setContentView(R.layout.main);

		// Install the app's custom icon, based on the active plugin.
		if (FindPluginManager.mFindPlugin.mMainIcon != null) {
			Log.i(TAG, "Installing custom icon");
			final ImageView mainLogo = (ImageView) findViewById(R.id.Logo);
			int resID = getResources().getIdentifier(FindPluginManager.mFindPlugin.mMainIcon, "drawable", this.getPackageName());
			mainLogo.setImageResource(resID);
		}

		Log.i(TAG, "Installing Add button");
		// Create and customize the AddButton.  A plugin can make this button go
		// away by setting its label to "invisible".
		if (FindPluginManager.mFindPlugin.mAddButtonLabel != null 
				&& !FindPluginManager.mFindPlugin.mAddButtonLabel.equals(getString(R.string.invisible))) {
			final Button addFindButton = (Button)findViewById(R.id.addFindButton);
			int resid = this.getResources()
					.getIdentifier(FindPluginManager.mFindPlugin.mAddButtonLabel, "string", getPackageName());

			if (addFindButton != null) {
				addFindButton.setTag(resid);
				addFindButton.setText(resid);
				addFindButton.setOnClickListener(this);
			}
		} else {
			Button b = (Button)findViewById(R.id.addFindButton);
			b.setVisibility(View.GONE);
			b = (Button)findViewById(R.id.listFindButton);  // HACK: for Csv plugin
			b.setWidth(200);                                // Makes the button bigger.
			b.setHeight(100);                               // and adds color scheme.
			b.setTextSize(20);                              // Should be generlized in Specs
			b.setTextColor(Color.BLUE);
		}

		Log.i(TAG, "Installing List button");
		// Create and customize the ListButton.  A plugin can make this button go
		// away by setting its label to "invisible".
		if (FindPluginManager.mFindPlugin.mListButtonLabel != null
				&& !FindPluginManager.mFindPlugin.mListButtonLabel.equals(getString(R.string.invisible))) {
			final Button listFindButton = (Button) findViewById(R.id.listFindButton);
			//final ImageButton listFindButton = (ImageButton) findViewById(R.id.listFindButton);
			int resid = this.getResources().getIdentifier(FindPluginManager.mFindPlugin.mListButtonLabel, "string",
					getPackageName());
			if (listFindButton != null) {
				listFindButton.setTag(resid);
				listFindButton.setText(resid);
				listFindButton.setOnClickListener(this);
			}
		} else {
			Button b = (Button)findViewById(R.id.listFindButton);
			b.setVisibility(View.GONE);
		}
		
		// Activate extra buttons if necessary.
		mMainButtonPlugins = FindPluginManager.getFunctionPlugins(FindPluginManager.MAIN_BUTTON_EXTENSION);
		Log.i(TAG, "Installing Extra buttons");
		for (FunctionPlugin plugin : mMainButtonPlugins) {
			Log.i(TAG, "Installing Extra button " + plugin.getName());
			int buttonID = getResources().getIdentifier(plugin.getName(), "id", getPackageName());
			Button button = (Button) findViewById(buttonID);
			button.setVisibility(Button.VISIBLE);
			button.setOnClickListener(this);
		}
	}

	// Lifecycle methods just generate Log entries to help debug and understand flow

	@Override
	protected void onPause() {
		super.onPause();
		Log.i(TAG, "Pausing");
	}


	@Override
	protected void onStart() {
		super.onStart();
		Log.i(TAG, "Starting");
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Log.i(TAG, "Restarting");
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.i(TAG, "Stopping");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "Destroying");
	}

	
	/**
	 * Generic onActivityResult designed to handle plugin activities that require
	 * a result.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i(TAG, "onActivityResult resultcode = " + resultCode);
		
		// Login Extension Point result
		if (mMainLoginPlugin != null && requestCode == mMainLoginPlugin.getActivityResultAction()) {
			if (resultCode == Activity.RESULT_OK) {
				Toast.makeText(this, getString(R.string.toast_thankyou), Toast.LENGTH_SHORT).show();
			} else {
				finish();				
			}
		} else 
			super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * Handles clicks on PositMain's buttons.
	 */
	public void onClick(View view) {
		BackgroundListener<String> bkgListener =
            new BackgroundListener<String>() {
            @Override
            public void onBackgroundResult(String authKey)
            {
                SharedPreferences sp =
                  PreferenceManager.getDefaultSharedPreferences(PositMain.this);
                View view = (View)getExtra(0);
                
        		if (authKey == null) {
        			Toast.makeText(PositMain.this,
                        "You must go to Android > Settings > Accounts " +
        			    "& Sync to set up an account before you use POSIT.",
        			    Toast.LENGTH_LONG).show();
        			return;
        		}
        		
        		if (sp.getString(getString(R.string.projectNamePref), "").equals("")) {
        			Toast.makeText(PositMain.this,
                        "To get started, you must choose a project.",
                        Toast.LENGTH_LONG).show();
        			Intent i =
        			    new Intent(PositMain.this, ListProjectsActivity.class);
        			startActivity(i);
        		} else {
        			Intent intent = new Intent();
        
        			switch (view.getId()) {
        			case R.id.addFindButton:
        				intent.setClass(PositMain.this,
        				    FindActivityProvider.getFindActivityClass());
        				intent.setAction(Intent.ACTION_INSERT);
        				startActivity(intent);
        				break;
        			case R.id.listFindButton:
        				intent = new Intent();
        				intent.setClass(PositMain.this,
        				    FindActivityProvider.getListFindsActivityClass());
        				startActivity(intent);
        				break;
        				
        			// The default case handles all Function plugins
        			default:
        				for (FunctionPlugin plugin: mMainButtonPlugins) {
        					int buttonID =
        					    getResources().getIdentifier(plugin.getName(),
        					        "id", getPackageName());
        					if (view.getId() == buttonID) {
        						intent = new Intent(PositMain.this,
        						    plugin.getmMenuActivity());
        						/* Add specific info based on each button */
        						String menuTitle = plugin.getmMenuTitle();
        						if (menuTitle.equals("Extra Button")) {
        							intent.setAction(Intent.ACTION_INSERT);
        						} else if (menuTitle.equals("Extra Button 2")) {
        							intent.setAction(Intent.ACTION_SEND);
        						}
        						/* Start the Activity */
        						if (plugin.getActivityReturnsResult())
        							startActivityForResult(intent,
        							    plugin.getActivityResultAction());
        						else
        							startActivity(intent);
        					}
        				}
        				break;
        			}
        		}
            }
		};
		
		bkgListener.putExtra(view);
		
		BackgroundManager.runTask(new GetAuthKeyCallable(this), bkgListener);
		
	}

	/**
	 * Creates the menu options for the PositMain screen. Menu items are
	 * inflated from a resource file.
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.positmain_menu, menu);	
		return true;
	}

	/**
	 * Prepares custom menus based on plugin specs. 
	 * 
	 * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		// Re-inflate to force localization.
		menu.clear();
		MenuInflater inflater = getSupportMenuInflater();
		if (mMainMenuPlugins.size() > 0) {
			for (FunctionPlugin plugin: mMainMenuPlugins) {
				MenuItem item = menu.add(plugin.getmMenuTitle());
				int id = getResources().getIdentifier(
						plugin.getmMenuIcon(), "drawable", "org.hfoss.posit.android");
				item.setIcon(id);
			}
		}
		inflater.inflate(R.menu.positmain_menu, menu);

        // TODO: This is AcdiVoca stuff that eventually needs to go
		// into a plugin.
//		MenuItem adminMenu = menu.findItem(R.id.admin_menu_item);
//		 Log.i(TAG, "UserType = " + AppControlManager.getUserType());
//		 Log.i(TAG, "distribution stage = " +
//		 AppControlManager.getDistributionStage());
//		 // Hide the ADMIN menu from regular users
//		 if (AppControlManager.isRegularUser() ||
//		 AppControlManager.isAgriUser())
//		 adminMenu.setVisible(false);
//		 else
//		 adminMenu.setVisible(true);

		return super.onPrepareOptionsMenu(menu);

	}

	/**
	 * Manages the selection of menu items.
	 * 
	 * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Log.i(TAG, "onMenuItemSelected " + item.toString());
		
		switch (item.getItemId()) {
		case R.id.settings_menu_item:
			startActivity(new Intent(this, SettingsActivity.class));
			break;
		case R.id.map_finds_menu_item:
			startActivity(new Intent(this, MapFindsActivity.class));
			break;
		// case R.id.admin_menu_item:
		// startActivity(new Intent(this, AcdiVocaAdminActivity.class));
		// break;
		case R.id.about_menu_item:
			startActivity(new Intent(this, AboutActivity.class));
			break;
			
		default:
		    BackgroundListener<String> bkgListener =
		            new BackgroundListener<String>() {
                @Override
                public void onBackgroundResult(String authKey)
                {
                    MenuItem item = (MenuItem)getExtra(0);
                    
                    // An AuthKey is required.  Nothing can be done until the
                    // phone sets up a POSITx account using the Android
                    // settings.  This is like setting up a Gmail account.
                    if (authKey == null) {
                        Toast.makeText(PositMain.this,
                            "You must go to Android > Settings > Accounts & " + 
                            "Sync to set up an account before you use POSIT.",
                            Toast.LENGTH_LONG).show();
                        return;
                    }
                    
                    if (mMainMenuPlugins.size() > 0){
                        for (FunctionPlugin plugin: mMainMenuPlugins) {
                            if (item.getTitle().equals(plugin.getmMenuTitle()))
                                startActivity(new Intent(PositMain.this,
                                    plugin.getmMenuActivity()));
                        }
                    }
                }
            };
            
            bkgListener.putExtra(item);
		    
			BackgroundManager.runTask(new GetAuthKeyCallable(this), bkgListener);
			break;
		}
		return true;
	}

	/**
	 * Intercepts the back key (KEYCODE_BACK) and displays a confirmation dialog
	 * when the user tries to exit POSIT.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			showDialog(CONFIRM_EXIT);
			return true;
		}
		Log.i("code", keyCode + "");
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Creates a dialog to confirm that the user wants to exit POSIT.
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case CONFIRM_EXIT:
			return new AlertDialog.Builder(this).setIcon(R.drawable.alerts_and_states_warning).setTitle(R.string.exit)
					.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							// User clicked OK so do some stuff
							finish();
						}
					}).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
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
		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		switch (id) {
		case CONFIRM_EXIT:
			d.setTitle(R.string.exit);
			// d.setButton(DialogInterface.BUTTON_POSITIVE,
			// getString(R.string.alert_dialog_ok), new
			// DialogInterface.OnClickListener() {
			// public void onClick(DialogInterface dialog,
			// int whichButton) {
			// // User clicked OK so do some stuff
			// finish();
			// }
			// } );
			// d.setButton(DialogInterface.BUTTON_NEGATIVE,
			// getString(R.string.alert_dialog_cancel), new
			// DialogInterface.OnClickListener() {
			// public void onClick(DialogInterface dialog,
			// int whichButton) {
			// /* User clicked Cancel so do nothing */
			// }
			// } );
			needsabutton = d.getButton(DialogInterface.BUTTON_POSITIVE);
			needsabutton.setText(R.string.alert_dialog_ok);
			needsabutton.invalidate();

			needsabutton = d.getButton(DialogInterface.BUTTON_NEGATIVE);
			needsabutton.setText(R.string.alert_dialog_cancel);
			needsabutton.invalidate();

			break;
		}
	}

	/**
	 * Makes sure RWG is stopped before exiting the Activity
	 * 
	 * @see android.app.Activity#finish()
	 */
	@Override
	public void finish() {
		Log.i(TAG, "finish()");
		super.finish();
	}

	public void handleActiveFuncPluginChangeEvent(FunctionPlugin plugin, boolean enabled) {
		updateReferencesToActivePlugins();
		
		//TODO: decide what to do when someone enables/disables a login plugin.
		
		if (enabled)
		{
			//Enabled services for enabled plugin
			ArrayList<Class<Service>> newServices = plugin.getmServices();
			for (Class<Service> s : newServices) {
				Log.i(TAG,"Starting service " + s.getSimpleName());
				this.startService(new Intent(this, s));
				
			}
			
			mServices.addAll(newServices);
		} 
		else
		{
			//Disable services for disabled plugin
			ArrayList<Class<Service>> exsistingServices = plugin.getmServices();
			for (Class<Service> s : exsistingServices) {
				Log.i(TAG,"Stopping service " + s.getSimpleName());
				this.stopService(new Intent(this, s));
			}
		}
	}
	
	/**
	 * Gets the active plugins form FindPluginManager and updates this calss's references to the necessary plugins.
	 */
	private void updateReferencesToActivePlugins()
	{
		// Import all active plug-ins and attach this activity's plugins
		Log.i(TAG, "Import active plugins");
		mMainMenuPlugins = FindPluginManager.getFunctionPlugins(FindPluginManager.MAIN_MENU_EXTENSION);
		Log.i(TAG, "# main menu plugins = " + mMainMenuPlugins.size());
		mMainLoginPlugin = FindPluginManager.getFunctionPlugin(FindPluginManager.MAIN_LOGIN_EXTENSION);	
	}
}