/*
 * File: SyncHistory.java
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
 * Object that corresponds to the sync_history table.
 *
 */
public class SyncHistory {
	
	public static final String TAG = "SyncHistory";

	public static final String ID = "id";
	public static final String TIME = "time";
	public static final String SERVER = "server";


	@DatabaseField(columnName = ID, generatedId = true)
	protected int id;
	@DatabaseField(columnName = TIME)
	protected Date time;
	@DatabaseField(columnName = SERVER)
	protected String server;
	
	public SyncHistory() {
		
	}
	
	public SyncHistory(String server) {
		time = new Date();
		this.server = server;
	}
	
	/**
	 * Creates the table for this class.
	 * 
	 * @param connectionSource
	 */
	public static void createTable(ConnectionSource connectionSource) {
		Log.i(TAG, "Creating SyncHistory table");
		try {
			TableUtils.createTable(connectionSource, SyncHistory.class);
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
	public String getServer() {
		return server;
	}
	public void setServer(String server) {
		this.server = server;
	}

	@Override
	public String toString() {
		return "SyncHistory [id=" + id + ", time=" + time + ", server=" + server + "]";
	}
}
