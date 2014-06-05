/*
 * File: AcdiVocaDbHelper.java
 * 
 * Copyright (C) 2011 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of the ACDI/VOCA plugin for POSIT, Portable Open Search 
 * and Identification Tool.
 *
 * This plugin is free software; you can redistribute it and/or modify
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

package org.hfoss.posit.android.plugin.acdivoca;


import java.sql.SQLException;

import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.plugin.acdivoca.AcdiVocaFind;
import org.hfoss.posit.android.plugin.acdivoca.AcdiVocaMessage;
import org.hfoss.posit.android.plugin.acdivoca.AcdiVocaUser;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * The class is the interface with the Database. 
 *  It controls all Db access 
 *  and directly handles all Db queries.
 */
public class AcdiVocaDbManager extends DbManager {


	private static final String TAG = "AcdiVocaDbManager";

	public static final String HELPER_CLASS = "org.hfoss.posit.android.plugin.acdivoca.AcdiVocaDbManager";
//	private static final String DATABASE_NAME ="posit";
//	public static final int DATABASE_VERSION = 2;
//	
//	public static final int DELETE_FIND = 1;
//	public static final int UNDELETE_FIND = 0;
//	public static final String WHERE_NOT_DELETED = " " + AcdiVocaFind.DELETED + " != " + DELETE_FIND + " ";
//	public static final String DATETIME_NOW = "`datetime('now')`";
//
//	public static final String FINDS_HISTORY_TABLE = "acdi_voca_finds_history";
//	public static final String HISTORY_ID = "_id" ;	

	// DAO objects used to access the Db tables
	private Dao<AcdiVocaUser, Integer> avUserDao = null;
	private Dao<AcdiVocaFind, Integer> acdiVocaFindDao = null;
	private Dao<AcdiVocaMessage, Integer> acdiVocaMessageDao = null;
	
	/**
	 * Constructor just saves and opens the Db.
	 * @param context
	 */
	public AcdiVocaDbManager(Context context) {
		super(context);
	}
	
	/**
	 * Invoked automatically if the Database does not exist.
	 */
	@Override
	public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
	
		try {
			Log.i(TAG, "onCreate");
			//super.onCreate(db, connectionSource);
			AcdiVocaFind.createTable(connectionSource);
			AcdiVocaMessage.createTable(connectionSource);
			AcdiVocaUser.createTable(connectionSource, getAvUserDao());
			
		} catch (SQLException e) {
			Log.e(TAG, "Can't create database", e);
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
		try {
			Log.i(TAG, "onUpgrade");
			TableUtils.dropTable(connectionSource, AcdiVocaUser.class, true);
			TableUtils.dropTable(connectionSource, AcdiVocaFind.class, true);
			TableUtils.dropTable(connectionSource, AcdiVocaMessage.class, true);
			// after we drop the old databases, we create the new ones
			onCreate(db, connectionSource);
		} catch (SQLException e) {
			Log.e(TAG, "Can't drop databases", e);
			throw new RuntimeException(e);
		}
	}
//		
	/**
	 * Returns the Database Access Object (DAO) for the AcdiVocaUser class. 
	 * It will create it or just give the cached value.
	 */
	public Dao<AcdiVocaUser, Integer> getAvUserDao() throws SQLException {
		if (avUserDao == null) {
			avUserDao = getDao(AcdiVocaUser.class);
		}
		return avUserDao;
	}
	
	/**
	 * Returns the Database Access Object (DAO) for the AcdiVocaFind class. 
	 * It will create it or just give the cached value.
	 */
	public Dao<AcdiVocaFind, Integer> getAcdiVocaFindDao() throws SQLException {
		if (acdiVocaFindDao == null) {
			acdiVocaFindDao = getDao(AcdiVocaFind.class);
		}
		return acdiVocaFindDao;
	}
	
	/**
	 * Returns the Database Access Object (DAO) for the AcdiVocaFind class. 
	 * It will create it or just give the cached value.
	 */
	public Dao<AcdiVocaMessage, Integer> getAcdiVocaMessageDao() throws SQLException {
		if (acdiVocaMessageDao == null) {
			acdiVocaMessageDao = getDao(AcdiVocaMessage.class);
		}
		return acdiVocaMessageDao;
	}
	
	/**
	 * Close the database connections and clear any cached DAOs.
	 */
	@Override
	public void close() {
		super.close();
		avUserDao = null;
		acdiVocaFindDao = null;
		acdiVocaMessageDao = null;
	}

}
