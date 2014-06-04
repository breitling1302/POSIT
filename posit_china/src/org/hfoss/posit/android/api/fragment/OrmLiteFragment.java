/*
 * File: OrmLiteFragment.java
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

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragment;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;

public class OrmLiteFragment<H extends OrmLiteSqliteOpenHelper> extends SherlockFragment {
public static String TAG= "OrmLiteFragment";
	
	private volatile H helper;
	private volatile boolean created = false;
	private volatile boolean destroyed = false;

	/**
	 * Get a helper for this action.
	 */
	public H getHelper() {
		if (helper == null) {
			if (!created) {
				throw new IllegalStateException("A call has not been made to onCreate() yet so the helper is null");
			} else if (destroyed) {
				throw new IllegalStateException(
						"A call to onDestroy has already been made and the helper cannot be used after that point");
			} else {
				throw new IllegalStateException("Helper is null for some unknown reason");
			}
		} else {
			return helper;
		}
	}

	/**
	 * Get a connection source for this action.
	 */
	public ConnectionSource getConnectionSource() {
		return getHelper().getConnectionSource();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (helper == null) {
			helper = getHelperInternal(getActivity());
			created = true;
		}
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "On destroy");
		super.onDestroy();
		if (helper != null && helper.isOpen())
			releaseHelper(helper);
		destroyed = true;
	}

	/**
	 * This is called internally by the class to populate the helper object instance. This should not be called directly
	 * by client code unless you know what you are doing. Use {@link #getHelper()} to get a helper instance. If you are
	 * managing your own helper creation, override this method to supply this activity with a helper instance.
	 * 
	 * <p>
	 * <b> NOTE: </b> If you override this method, you most likely will need to override the
	 * {@link #releaseHelper(OrmLiteSqliteOpenHelper)} method as well.
	 * </p>
	 */
	protected H getHelperInternal(Context context) {
		@SuppressWarnings("unchecked")
		H newHelper = (H) OpenHelperManager.getHelper(context);
		return newHelper;
	}

	/**
	 * Release the helper instance created in {@link #getHelperInternal(Context)}. You most likely will not need to call
	 * this directly since {@link #onDestroy()} does it for you.
	 * 
	 * <p>
	 * <b> NOTE: </b> If you override this method, you most likely will need to override the
	 * {@link #getHelperInternal(Context)} method as well.
	 * </p>
	 */
	protected void releaseHelper(H helper) {
		OpenHelperManager.releaseHelper();
		helper = null;
	}
}
