/*
 * File: ShFind.java
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
package org.hfoss.posit.android.plugin.sh;

import java.sql.SQLException;

import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.database.DbManager;
import org.hfoss.posit.android.plugin.sh.ShFind;

import android.util.Log;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTable;
import com.j256.ormlite.table.TableUtils;

/**
 * ShFind is an experimental Find Plugin for Second Helpings,
 * a non-profit organization that picks up left over food 
 * from sources and drops it off at destinations.
 * 
 * This version was written as a proof-of-concept.
 * 
 * Note that we use the FIND_TABLE_NAME rather than the class name.
 */
@DatabaseTable(tableName = DbManager.FIND_TABLE_NAME)
public class ShFind extends Find {

	public static final String TAG = "ShFind";
	public static final String STOP_TYPE = "stopType";
	public static final int PICKUP = 0;
	public static final int DROPOFF = 1;
	public static final int NOVALUE = -1;

	/**
	 * stopType is the only additional field and only
	 * additional Db column beyond those inherited from Find.
	 */
	@DatabaseField(columnName = STOP_TYPE)
	protected int stopType = NOVALUE;   // 0 = pickup, 1 = dropoff

	/**
	 * Default constructor required by OrmLite.
	 */
	public ShFind() {
	}

	/**
	 * Creates the table for this class. This happens automatically
	 * when the first Db operation is performed. 
	 * 
	 * @param connectionSource
	 */
	public static void createTable(ConnectionSource connectionSource) {
		Log.i(TAG, "Creating ShFind table");
		try {
			TableUtils.createTable(connectionSource, ShFind.class);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public int getStopType() {
		return stopType;
	}

	public void setStopType(int stopType) {
		this.stopType = stopType;
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
		sb.append(STOP_TYPE).append("=").append(stopType).append(",");
		return sb.toString();
	}

}
