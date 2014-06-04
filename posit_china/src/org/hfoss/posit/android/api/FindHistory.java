/*
 * File: FindHistory.java
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

package org.hfoss.posit.android.api;

import java.sql.SQLException;
import java.util.Date;

import android.util.Log;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

/**
 * Base class of the find_history table, which keeps track
 * of changes to Finds.
 *
 */
public class FindHistory {
	
	public static final String TAG = "FindHistory";

	public static final String ID = "id";
	public static final String TIME = "time";
	public static final String FIND = "find";
	public static final String FIND_ACTION = "find_action";
	
	public static final String ACTION_CREATE = "create";
	public static final String ACTION_UPDATE = "update";
	public static final String ACTION_DELETE = "delete";

	@DatabaseField(columnName = ID, generatedId = true)
	protected int id;
	@DatabaseField(columnName = TIME)
	protected Date time;
	@DatabaseField(columnName = FIND, foreign=true)
	protected Find find;
	@DatabaseField(columnName = FIND_ACTION)
	protected String findAction;

	/**
	 * Default constructor required by OrmLite.
	 */
	public FindHistory() {

	}
	
	public FindHistory(Find find, String action){
		this.find = find;
		this.findAction = action;
		time = new Date();
	}

	/**
	 * Creates the table for this class.
	 * 
	 * @param connectionSource
	 */
	public static void createTable(ConnectionSource connectionSource) {
		Log.i(TAG, "Creating FindHistory table");
		try {
			TableUtils.createTable(connectionSource, FindHistory.class);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public Find getFind() {
		return find;
	}

	public void setFind(Find find) {
		this.find = find;
	}

	public String getFindAction() {
		return findAction;
	}

	public void setFindAction(String findAction) {
		this.findAction = findAction;
	}

	@Override
	public String toString() {
		return "FindHistory [id=" + id + ", time=" + time + ", find=" + find + ", findAction=" + findAction
				+ "]";
	}

}
