/*
 * File: AdhocFind.java
 * 
 * Copyright (C) 2010 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Search and Identification Tool.
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
package org.hfoss.adhoc;

import java.io.Serializable;
import org.hfoss.posit.android.provider.PositDbHelper;
import android.content.ContentValues;

/**
 * Holds the data for an adhoc Find -- i.e. a Find that will be sent to 
 *  other devices through an adhoc mesh network. It must be serializable 
 *  so it can easily be converted to/from an array of bytes.
 *
 */
public class AdhocFind implements Serializable {
	public static final String TAG = "Adhoc";	
	private String id;   
	private String name; 
	private String description; 
	private String latitude; 
	private String longitude; 
	private String projectId; 
	
	public AdhocFind(ContentValues values) {
		name = values.getAsString(PositDbHelper.FINDS_NAME);
		description = values.getAsString(PositDbHelper.FINDS_DESCRIPTION);
		longitude = values.getAsString(PositDbHelper.FINDS_LONGITUDE);
		latitude = values.getAsString(PositDbHelper.FINDS_LATITUDE);
		id = values.getAsString(PositDbHelper.FINDS_GUID);
		projectId = values.getAsString(PositDbHelper.FINDS_PROJECT_ID); 
	}
	
	public String toString() {
		return id + " " + name + " " + description + 
			" " + latitude + " " + longitude + " " + projectId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}
}
