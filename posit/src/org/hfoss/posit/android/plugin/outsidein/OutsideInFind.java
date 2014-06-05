/*
 * File: OutsideInFind.java
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
package org.hfoss.posit.android.plugin.outsidein;

import java.sql.SQLException;

import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.database.DbManager;

import android.util.Log;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTable;
import com.j256.ormlite.table.TableUtils;

/**
 * OutsideInFind is an experimental Find Plugin for 
 * OutsideIn, a non-profit social service agency in Portland, OR. 
 * This app is for a mobile needle exchange program. So the
 * Finds are anonymous.
 * 
 * This version was written as a proof-of-concept.
 * 
 * Note that we use the FIND_TABLE_NAME rather than the class name.
 */
@DatabaseTable(tableName = DbManager.FIND_TABLE_NAME)
public class OutsideInFind extends Find {

	public static final String TAG = "OutsideInFind";
	public static final String SYRINGES_IN = "syringes_in";
	public static final String SYRINGES_OUT = "syringes_out";
	public static final String IS_NEW = "is_new";
	public static final String IS_LOGGED = "is_logged"
;
	@DatabaseField(columnName = SYRINGES_IN)
	protected int syringesIn;
	@DatabaseField(columnName = SYRINGES_OUT)
	protected int syringesOut;
	@DatabaseField(columnName = IS_NEW)
	protected boolean isNew;
	@DatabaseField(columnName = IS_LOGGED)
	protected boolean isLogged = false;  // Unlogged by default

	public OutsideInFind() {
		// Necessary by ormlite
	}
	
	/**
	 * Creates the table for this class.
	 * 
	 * @param connectionSource
	 */
	public static void createTable(ConnectionSource connectionSource) {
		Log.i(TAG, "Creating OutsideinFind table");
		try {
			TableUtils.createTable(connectionSource, OutsideInFind.class);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public int getSyringesIn() {
		return syringesIn;
	}

	public void setSyringesIn(int syringesIn) {
		this.syringesIn = syringesIn;
	}

	public int getSyringesOut() {
		return syringesOut;
	}

	public void setSyringesOut(int syringesOut) {
		this.syringesOut = syringesOut;
	}

	public boolean getIsNew() {
		return isNew;
	}

	public void setIsNew(boolean isNew) {
		this.isNew = isNew;
	}
	
	public boolean getIsLogged() {
		return isLogged;
	}

	public void setIsLogged(boolean isLogged) {
		this.isLogged = isLogged;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(ORM_ID).append("=").append(id).append(",");
		sb.append(GUID).append("=").append(guid).append(",");
		sb.append(NAME).append("=").append(name).append(",");
		sb.append(PROJECT_ID).append("=").append(project_id).append(",");
		sb.append(LATITUDE).append("=").append(latitude).append(",");
		sb.append(LONGITUDE).append("=").append(longitude).append(",");
		if (time != null)
			sb.append(TIME).append("=").append(time.toString()).append(",");
		else
			sb.append(TIME).append("=").append("").append(",");
		if (modify_time != null)
			sb.append(MODIFY_TIME).append("=").append(modify_time.toString())
					.append(",");
		else
			sb.append(MODIFY_TIME).append("=").append("").append(",");
		//sb.append(REVISION).append("=").append(revision).append(",");
		sb.append(IS_ADHOC).append("=").append(is_adhoc).append(",");
		//sb.append(ACTION).append("=").append(action).append(",");
		sb.append(DELETED).append("=").append(deleted).append(",");
		sb.append(SYRINGES_IN).append("=").append(syringesIn).append(",");
		sb.append(SYRINGES_OUT).append("=").append(syringesOut).append(",");
		sb.append(IS_NEW).append("=").append(isNew).append(",");
		sb.append(IS_LOGGED).append("=").append(isLogged).append(",");
		return sb.toString();
	}

}
