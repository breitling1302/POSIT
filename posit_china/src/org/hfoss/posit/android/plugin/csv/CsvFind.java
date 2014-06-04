/*
 * File: CsvFind.java
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
package org.hfoss.posit.android.plugin.csv;

import java.sql.SQLException;

import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.database.DbManager;

import android.util.Log;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTable;
import com.j256.ormlite.table.TableUtils;

/**
 * A CsvFind is a Find that can be constructed from a CSV File
 * (i.e., a spread sheet).  It requires that the CSV be comma-delimited
 * and contain no columns with embedded commas. 
 *
 */
@DatabaseTable(tableName = DbManager.FIND_TABLE_NAME)
public class CsvFind extends Find {

	public static final String TAG = "CsvFind";
	public static final String URL = "url";
	public static final String PHONE = "phone";
	public static final String ADDR = "address1";
	public static final String ADDR2 = "address2";
	public static final String CITY = "city";
	public static final String ZIP = "zip";
	public static final String CLOSING = "closing";
	public static final String RATES = "rates";
	public static final String SPECIALS = "specials";

	
	/**
	 * Two fields extend the Basic Find fields
	 */
	@DatabaseField(columnName = URL)
	protected String url = "";
	@DatabaseField(columnName = PHONE)
	protected String phone = "";	
	@DatabaseField(columnName = ADDR)
	protected String address1 = "";		
	@DatabaseField(columnName = ADDR2)
	protected String address2 = "";
	@DatabaseField(columnName = CITY)
	protected String city = "";	
	@DatabaseField(columnName = ZIP)
	protected String zip = "";	
	
	@DatabaseField(columnName = CLOSING)
	protected String closing = "";	
	@DatabaseField(columnName = RATES)
	protected String rates = "";	
	@DatabaseField(columnName = SPECIALS)
	protected String specials = "";	
	
	public CsvFind() {
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
			TableUtils.createTable(connectionSource, CsvFind.class);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}
	
	

	public String getAddress1() {
		return address1;
	}

	public void setAddress1(String street) {
		this.address1 = street;
	}

	public String getAddress2() {
		return address2;
	}

	public void setAddress2(String address2) {
		this.address2 = address2;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getZip() {
		return zip;
	}

	public void setZip(String zip) {
		this.zip = zip;
	}
	
	public String getFullAddress() {
		if (address2.trim().length() != 0)
			return address1 + "," + address2 + "," + city + ",NY," + zip;
		else 
			return address1 + "," + city + ",NY," + zip;
	}

	public String getClosing() {
		return closing;
	}

	public void setClosing(String closing) {
		this.closing = closing;
	}

	public String getRates() {
		return rates;
	}

	public void setRates(String rates) {
		this.rates = rates;
	}

	public String getSpecials() {
		return specials;
	}

	public void setSpecials(String specials) {
		this.specials = specials;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(ORM_ID).append("=").append(id).append(",");
		sb.append(GUID).append("=").append(guid).append(",");
		sb.append(NAME).append("=").append(name).append(",");
		sb.append(PHONE).append("=").append(phone).append(",");
		sb.append(URL).append("=").append(url).append(",");
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
		return sb.toString();
	}

}
