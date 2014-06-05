package org.hfoss.posit.android.functionplugin.tracker;

/*
 * File: AcdiVocaDbUser.java
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

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.R;
//import org.hfoss.posit.android.plugin.acdivoca.AcdiVocaUser;
//import org.hfoss.posit.android.plugin.acdivoca.AcdiVocaUser.UserType;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

public class Expedition {

	/**
	 * The User object for creating and persisting data for the user table in
	 * the database.
	 */

	public static final String TAG = "Expedition";
	public static final String EXPEDITION_ROW_ID = "id";
	public static final String EXPEDITION_NUM = "expedition_number";
	public static final String EXPEDITION_PROJECT_ID = "project_id"; 
	public static final String EXPEDITION_POINTS = "expedition_points";
	public static final String EXPEDITION_SYNCED = "expedition_synced";
	public static final String EXPEDITION_REGISTERED = "expedition_registered";
	public static final int EXPEDITION_NOT_REGISTERED = 0;
	public static final int EXPEDITION_IS_REGISTERED = 1;


	/**
	 * The fields annotated with @DatabaseField are persisted to the Db.
	 */
	// id is generated by the database and set on the object automagically
	@DatabaseField(generatedId = true)
	int id;
	@DatabaseField(columnName = EXPEDITION_NUM)
	protected int expedition_num;
	@DatabaseField(columnName = EXPEDITION_PROJECT_ID)
	protected int project_id;
	@DatabaseField(columnName = EXPEDITION_POINTS)
	protected int points;
	@DatabaseField(columnName = EXPEDITION_SYNCED)
	protected int is_synced;
	@DatabaseField(columnName = EXPEDITION_REGISTERED)
	protected int is_registered;

	Expedition() {
		// needed by ormlite
	}

	public Expedition(int project_id) {
		this.project_id = project_id;
	}
	
	public Expedition(ContentValues values) {
		expedition_num = values.getAsInteger(EXPEDITION_NUM);
		project_id = values.getAsInteger(EXPEDITION_PROJECT_ID);
		is_registered = values.getAsInteger(EXPEDITION_REGISTERED);
	}

	/**
	 * Creates the table associated with this object. And creates the default
	 * users. The table's name is 'user', same as the class name.
	 * 
	 * @param connectionSource
	 */
	public static void createTable(ConnectionSource connectionSource,
			Dao<Expedition, Integer> dao) {
		try {
			TableUtils.createTable(connectionSource, Expedition.class);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public int getPoints() {
		return points;
	}

	public void setPoints(int points) {
		this.points = points;
	}

	public int getIs_synced() {
		return is_synced;
	}

	public void setIs_synced(int is_synced) {
		this.is_synced = is_synced;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("id=").append(id);
		sb.append(", ").append("project_id=").append(project_id);
		sb.append(", ").append("points").append(points);
		sb.append(", ").append("is_synced").append(is_synced);
		sb.append(", ").append("is_registered").append(is_registered);
		return sb.toString();
	}
}
