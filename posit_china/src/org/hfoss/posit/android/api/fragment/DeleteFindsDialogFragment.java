/*
 * File: DeleteFindsFragment.java
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

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.activity.FindActivity;
import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.api.plugin.FindPluginManager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.widget.Toast;

/**
 * A dialog used to confirm deletion of all finds or a single find.
 *
 */
public class DeleteFindsDialogFragment extends OrmLiteDialogFragment<DbManager> {
	protected int mNum;

	public static final int CONFIRM_DELETE_ALL_FINDS_DIALOG = 0;
	public static final int CONFIRM_DELETE_FIND_DIALOG = 1;
	
	/**
	 * Returns a dialog which asks the user to confirm delete of a find
	 * or multiple finds.
	 * 
	 * @param num	the type of DeleteFindsDialogFragment to display
	 * @return		a instance of DeleteFindsDialogFragment
	 */
	public static DeleteFindsDialogFragment newInstance(int num) {
		DeleteFindsDialogFragment f = new DeleteFindsDialogFragment();
		
		//Supply num input as argument
		Bundle args = new Bundle();
		args.putInt("num", num);
		f.setArguments(args);
		
		return f;
	}
	
	/**
	 * Returns a dialog which asks the user to confirm delete of a find
	 * or multiple finds.
	 * 
	 * @param num	the type of DeleteFindsDialogFragment to display
	 * @param findID	the id of the find which is to be deleted
	 * @return		a instance of DeleteFindsDialogFragment
	 */
	public static DeleteFindsDialogFragment newInstance(int num, int findID) {
		DeleteFindsDialogFragment f = new DeleteFindsDialogFragment();
		
		//Supply num input as argument
		Bundle args = new Bundle();
		args.putInt("num", num);
		args.putInt(Find.ORM_ID, findID);
		f.setArguments(args);
		
		return f;
	}
	
	/**
	 * Returns the view associated with the dialog
	 * 
	 * @param savedInstanceState	a bundle which could contain prior data
	 * @return		an alert dialog which is used to confirm deletion
	 */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mNum = getArguments().getInt("num");
		
		switch (mNum) {
		case CONFIRM_DELETE_ALL_FINDS_DIALOG:
			return new AlertDialog.Builder(getActivity()).setIcon(R.drawable.alerts_and_states_warning)
			.setTitle(R.string.confirm_delete)
			.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// User clicked OK so do some stuff
					if (deleteAllFind()) {
						getActivity().finish();
					}
				}
			}).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// User clicked cancel so do nothing
				}
			}).create();
		case CONFIRM_DELETE_FIND_DIALOG:
			return new AlertDialog.Builder(getActivity()).setIcon(
				R.drawable.alerts_and_states_warning).setTitle(
				R.string.alert_dialog_2).setPositiveButton(
				R.string.alert_dialog_ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int whichButton) {
						// User clicked OK so do some stuff
						if (deleteFind()) {
							//TODO: Handle this better
							getActivity().finish();
						}
					}
				}).setNegativeButton(R.string.alert_dialog_cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,
							int whichButton) {
						// User clicked cancel so do nothing
					}
				}).create();
		default:
			return null;
		}
	}

	/**
	 * Deletes a find which the user was prompted to do so.
	 * 
	 * @return	a boolean indicated that the find was deleted
	 */
	protected boolean deleteFind() {
		int rows = 0;
		String guid = null;
		// Get the appropriate find class from the plugin manager and
		// make an instance of it.
		Class<Find> findClass = FindPluginManager.mFindPlugin.getmFindClass();
		Find find = null;

		try {
			find = findClass.newInstance();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (java.lang.InstantiationException e) {
			e.printStackTrace();
		}

		find.setId(getArguments().getInt(Find.ORM_ID));

		// store the guid of this find so that I can delete photos on phone
		find = getHelper().getFindById(find.getId());
		guid = find.getGuid();

		rows = getHelper().delete(find);

		if (rows > 0) {
			Toast.makeText(getActivity(), R.string.deleted_from_database,
					Toast.LENGTH_SHORT).show();

			// delete photo if it exists
			if (getActivity().deleteFile(guid)) {
				Log.i(TAG, "Image with guid: " + guid + " deleted.");
			}

//			this.startService(new Intent(this, ToDoReminderService.class));
		} else {
			Toast.makeText(getActivity(), R.string.delete_failed,
					Toast.LENGTH_SHORT).show();
		}

		return rows > 0;

	}
	
	/**
	 * Deletes all the finds within the project which the user has
	 * confirmed.
	 * 
	 * @return	a boolean indicating a successful deletion
	 */
	protected boolean deleteAllFind() {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		int projectId = prefs.getInt(getString(R.string.projectPref), 0);
		boolean success = getHelper().deleteAll(projectId);
		if (success) {
			Toast.makeText(getActivity(), R.string.deleted_from_database, Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(getActivity(), R.string.delete_failed, Toast.LENGTH_SHORT).show();
		}
		return success;
	}
}
