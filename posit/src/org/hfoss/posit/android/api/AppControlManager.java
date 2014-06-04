/*
 * File: AppControlManager.java
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

import org.hfoss.posit.android.R;
//import org.hfoss.posit.android.plugin.acdivoca.AcdiVocaUser;
//import org.hfoss.posit.android.plugin.acdivoca.AcdiVocaUser.UserType;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * This class was designed specifically for AcdiVoca and is
 * used for cases where a phone is shared by multiple users.
 * 
 * It distinguishes between super, admin, and regular users. 
 * It manages user types/authentication and the visibility
 * of certain UI elements to different user types. Plugins can extend
 * and add methods to control this.
 * 
 * TODO: Incorporate this into the Plugin architecture.
 */
public class AppControlManager {
	
	public static final String TAG = "AppControlManager";
	private static AppControlManager sInstance = null;

	private static int sLoggedInUserTypeOrdinal;
//	private static UserType sLoggedInUserType;
	
	public static AppControlManager getInstance(){
		assert(sInstance != null);
		
		return sInstance;
	}
	
	// ----------------   UserType Control Methods ------------------
	// Static utility methods used to control menus and buttons based on user type.
	
	public static void setUserType(int userTypeOrdinal) {
		sLoggedInUserTypeOrdinal = userTypeOrdinal;
	}
	
	/**
	 * Returns the type of the currently logged in user
	 */
//	public static UserType getUserType() {
//		if (sLoggedInUserTypeOrdinal == UserType.ADMIN.ordinal())
//			return UserType.ADMIN;
//		else
//			return UserType.USER;
//	}
	
	/**
	 * Returns the user type as an ordinal (int) values
	 */
	public static int getUserTypeOrdinal() {
		return sLoggedInUserTypeOrdinal;
	}
	
	
//	public static boolean isAdminUser() {
//		return sLoggedInUserTypeOrdinal == UserType.ADMIN.ordinal();
//	}
//	
//	public static boolean isRegularUser() {
//		return sLoggedInUserTypeOrdinal == UserType.USER.ordinal();
//	}
}
