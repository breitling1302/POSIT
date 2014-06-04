/*
 * File: DbHelper.java
 * 
 * Copyright (C) 2011 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Source Information Tool.
 *
 * This is free software; you can redistribute it and/or modify
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

package org.hfoss.posit.android.api.database;

import org.hfoss.posit.android.api.Find;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import android.content.Context;

/**
 * Utility class that facilitates the use of the ORMlite framework for 
 * managing the database. Provides static methods that retrieve and release
 * the DbManager (ORM) object. 
 * 
 * Call getDbManager() to get an instance of the DbManager. Do
 * some database work, then you must call releaseDbManager() to clean things up.
 * 
 * If you are within an Activity, you can extend OrmLiteBaseActivity and use
 * this.getHelper() to get an instance of DbManager, and from there, there is no
 * need to call releaseHelper(), because the superclass handles it for you.
 */
public class DbHelper {

	public static DbManager dbManager;

	public static DbManager getDbManager(Context context) {
		if (dbManager == null) {
			dbManager = (DbManager) OpenHelperManager.getHelper(context);
			return dbManager;
		} else
			return dbManager;
	}

	public static void releaseDbManager() {
		dbManager = null;
		OpenHelperManager.releaseHelper();
	}

	public static Dao<Find, Integer> getFindDao(Context context) {
		return getDbManager(context).getFindDao();
	}

}
