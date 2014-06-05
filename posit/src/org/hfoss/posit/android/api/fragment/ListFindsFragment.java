/*
 * File: ListFindsFragment.java
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
package org.hfoss.posit.android.api.fragment;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.activity.MapFindsActivity;
import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.api.plugin.FindPlugin;
import org.hfoss.posit.android.api.plugin.FindPluginManager;
import org.hfoss.posit.android.api.plugin.FunctionPlugin;
import org.hfoss.posit.android.api.plugin.ListFindPluginCallback;
import org.hfoss.posit.android.sync.SyncActivity;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

public class ListFindsFragment extends OrmLiteListFragment<DbManager> {
	private static final String TAG = "ListFindsFragment";
	protected ArrayList<FunctionPlugin> mListMenuPlugins = null;

	public static final String ACTION_LIST_FINDS = "list_finds";

	private static List<? extends Find> finds;
	
	protected static FindsListAdapter mAdapter = null;
	
	private boolean mIsDualPane;
	private int mCurrCheckPosition;
	
	/**
	 * Called when the Activity starts.
	 * 
	 * @param savedInstanceState
	 *            contains the Activity's previously frozen state. In this case
	 *            it is unused.
	 * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mListMenuPlugins = FindPluginManager.getFunctionPlugins(FindPluginManager.LIST_MENU_EXTENSION);
		Log.i(TAG, "# of List menu plugins = " + mListMenuPlugins.size());
		
		setHasOptionsMenu(true);
		
		View findFrame = getActivity().findViewById(R.id.find);
		mIsDualPane = findFrame != null && findFrame.getVisibility() == View.VISIBLE;
		
		if (savedInstanceState != null) {
			mCurrCheckPosition = savedInstanceState.getInt("currChoice", 0);
		}
		
		if (mIsDualPane) {
			getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			displayFind(mCurrCheckPosition, Intent.ACTION_INSERT, null, null);
		}
	}
	
	/**
	 * Starts FindActivty or replaces second side in pane with FindFragment
	 * for find creation.
	 */
	private void addFind() {
		if (mIsDualPane) {
			FindFragment findFragment = null;
			try {
				findFragment = (FindPluginManager.mFindPlugin.getmFindFragmentClass()).newInstance();
			} catch (java.lang.InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (findFragment==null) {
				Toast.makeText(getActivity(), "can't create find fragment", Toast.LENGTH_LONG).show();
				return;
			}
			
			Bundle extras = new Bundle();
			extras.putString("ACTION", Intent.ACTION_INSERT);
			
			findFragment.setArguments(extras);
			
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.replace(R.id.find, findFragment);
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			ft.commit();
		} else {
			Intent intent = new Intent(getActivity(),
			FindPluginManager.mFindPlugin.getmFindActivityClass());
			intent.setAction(Intent.ACTION_INSERT);
			startActivity(intent);
		}
	}
	
	/**
	 * 
	 * @param index
	 * @param action
	 * @param extras
	 * @param findFragment
	 */
	protected void displayFind(int index, String action, Bundle extras, FindFragment findFragment) {
		mCurrCheckPosition = index;
		
		if (mIsDualPane) {
			getListView().setItemChecked(mCurrCheckPosition, true);
			
			if(extras == null) {
				extras = new Bundle();
			}
			extras.putString("ACTION", action);
			
			if (findFragment == null) {
				findFragment = new FindFragment();
			}
				
			findFragment.setArguments(extras);
			
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.replace(R.id.find, findFragment);
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			ft.commit();
		} else {
			Intent intent = new Intent(getActivity(),
					FindPluginManager.mFindPlugin.getmFindActivityClass());
			int ormId = extras.getInt(Find.ORM_ID);
			intent.putExtra(Find.ORM_ID, ormId);
			intent.setAction(action);
			startActivity(intent);
		}
	}

	/* Called when the activity is ready to start interacting with the user. It
	 * is at the top of the Activity stack.
	 * 
	 * @see android.support.v4.app.Fragment#onResume()
	 */
	@Override
	public void onResume() {
		super.onResume();
		Log.i(TAG, "onResume()");
		mAdapter = (FindsListAdapter) setUpAdapter();
		fillList(mAdapter);
	}
	
	/* Called when the Activity is paused.
	 * 
	 * @see android.support.v4.app.Fragment#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currChoice", mCurrCheckPosition);
    }
	
	public void onGetChangedFindsResult(String finds) {
		Log.i(TAG, "Got changed finds: " + finds);
	}
	
	protected ListAdapter setUpAdapter() {

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		int projectId = prefs.getInt(getString(R.string.projectPref), 0);
		
		finds = this.getHelper().getFindsByProjectId(projectId);
		
		int resId = getResources().getIdentifier(
				FindPlugin.mListFindLayout, "layout", getActivity().getPackageName());

		FindsListAdapter adapter = new FindsListAdapter(getActivity(), resId, finds);

		return adapter;
	}
	
	/**
	 * Puts the items from the DB table into the rows of the view.
	 */
	private void fillList(ListAdapter adapter) {
		setListAdapter(adapter);

		ListView lv = getListView();
		lv.setTextFilterEnabled(true);
		lv.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				TextView tv = (TextView) view.findViewById(R.id.id);
				int ormId = Integer.parseInt((String) tv.getText());
				Bundle extras = new Bundle();
				extras.putInt(Find.ORM_ID, ormId);
				displayFind(position, Intent.ACTION_EDIT, extras, null);
			}
		});
	}

	/**
	 * Creates the menus for this activity.
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		
		if (mListMenuPlugins.size() > 0) {
			for (FunctionPlugin plugin: mListMenuPlugins) {
				MenuItem item = menu.add(plugin.getmMenuTitle());
				int id = getResources().getIdentifier(
						plugin.getmMenuIcon(), "drawable", "org.hfoss.posit.android");
//				Log.i(TAG, "icon =  " + plugin.getmMenuIcon() + " id =" + id);
				item.setIcon(id);
				//item.setIcon(android.R.drawable.ic_menu_mapmode);				
			}
		}
		inflater.inflate(R.menu.list_finds_menu, menu);
	}

	/**
	 * Handles the various menu item actions.
	 * 
	 * @param featureId
	 *            is unused
	 * @param item
	 *            is the MenuItem selected by the user
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Boolean result = false;
		super.onOptionsItemSelected(item);
		Log.i(TAG, "onOptionsItemSelected()");

		Intent intent;
		switch (item.getItemId()) {
		case R.id.add_find_menu_item:
			Log.i(TAG, "Add find menu item");
			addFind();
			result = true;
			break;
		case R.id.sync_finds_menu_item:
			Log.i(TAG, "Sync finds menu item");
			startActivityForResult(new Intent(getActivity(), SyncActivity.class), 0);
			result = true;
			break;
		case R.id.map_finds_menu_item:
			Log.i(TAG, "Map finds menu item");
			intent = new Intent();
			intent.setAction(ACTION_LIST_FINDS);
			intent.setClass(getActivity(), MapFindsActivity.class);			
			startActivity(intent);
			result = true;
			break;

		case R.id.delete_finds_menu_item:
			Log.i(TAG, "Delete all finds menu item"); 
			showDialog(DeleteFindsDialogFragment.CONFIRM_DELETE_ALL_FINDS_DIALOG);
			result = true;
			break;
			
		default:
			if (mListMenuPlugins.size() > 0){
				for (FunctionPlugin plugin: mListMenuPlugins) {
					if (item.getTitle().equals(plugin.getmMenuTitle()))
					{
						startActivity(new Intent(getActivity(), plugin.getmMenuActivity()));
						result = true;
					}
				}
			}
			break;
		}
		return result;
	}
	
	public static void syncCallback() {
		Log.i(TAG, "Notified sync callback");
		mAdapter.notifyDataSetChanged();
	}
	
	public void showDialog(int num) {
		
		// DialogFragment.show() will take care of adding the fragment
	    // in a transaction.  We also want to remove any currently showing
	    // dialog, so make our own transaction and take care of that here.
	    FragmentTransaction ft = getFragmentManager().beginTransaction();
	    Fragment prev = getFragmentManager().findFragmentByTag("dialog");
	    if (prev != null) {
	        ft.remove(prev);
	    }
	    ft.addToBackStack(null);

	    // Create and show the dialog.
	    DeleteFindsDialogFragment newFragment = DeleteFindsDialogFragment.newInstance(num);
	    newFragment.show(ft, "dialog");
	}

	/**
	 * Returns a list of Finds.
	 */
	public static List<? extends Find> getFinds() {
		return finds;
	}
	
	/**
	 * Adapter for displaying finds.
	 * 
	 * @param <Find>
	 */
	protected class FindsListAdapter extends ArrayAdapter<Find> {
		protected List<? extends Find> items;
		Context context;

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public FindsListAdapter(Context context, int textViewResourceId, List list) {
			super(context, textViewResourceId, list);
			this.items = list;
			this.context = context;
		}

		
		@Override
		public void notifyDataSetChanged() {
			super.notifyDataSetChanged();
		}


		@SuppressWarnings("unchecked")
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

				int resId = getResources().getIdentifier(
						FindPlugin.mListFindLayout, "layout",
						getActivity().getPackageName());
				v = vi.inflate(resId, null);
			}
			Find find = items.get(position);
			if (find != null) {
				TextView tv = (TextView) v.findViewById(R.id.name);
				tv.setText(find.getName());
				tv = (TextView) v.findViewById(R.id.latitude);
				String latitude = String.valueOf(find.getLatitude());
				if (!latitude.equals("0.0")) {
					latitude = latitude.substring(0, 7);
				}
				tv.setText(getText(R.string.latitude) + " " + latitude);
				tv = (TextView) v.findViewById(R.id.longitude);
				String longitude = String.valueOf(find.getLongitude());
				if (!longitude.equals("0.0")) {
					longitude = longitude.substring(0, 7);
				}
				tv.setText(getText(R.string.longitude) + " " + longitude);
				tv = (TextView) v.findViewById(R.id.id);
				tv.setText(Integer.toString(find.getId()));
				tv = (TextView) v.findViewById(R.id.time);
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				
				String time = dateFormat.format(find.getTime());
    			tv.setText(getText(R.string.timeLabel) + " " + time);
				
				tv = (TextView) v.findViewById(R.id.status);
				tv.setText(find.getStatusAsString());
				tv = (TextView) v.findViewById(R.id.description_id);
				String description = find.getDescription();
				if (description.length() <= 50) {
					tv.setText(description);
				} else {
					tv.setText(description.substring(0,49)+" ...");
				}

				ArrayList<FunctionPlugin> plugins = FindPluginManager.getFunctionPlugins();
				
				// Call each plugin's callback method to update view
				for (FunctionPlugin plugin: plugins) {
//					Log.i(TAG, "Call back for plugin=" + plugin);
					Class<ListFindPluginCallback> callbackClass = null;
					Object o;
					try {
						String className = plugin.getListFindCallbackClass();
						if (className != null) {
							callbackClass = (Class<ListFindPluginCallback>) Class.forName(className);
							o = (ListFindPluginCallback) callbackClass.newInstance();
							((ListFindPluginCallback) o).listFindCallback(context,find,v);
						}
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (java.lang.InstantiationException e) {
						e.printStackTrace();
					}
				}
			}
			return v;
		}
	}
}
