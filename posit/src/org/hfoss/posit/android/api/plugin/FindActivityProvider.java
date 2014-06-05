/*
 * File: FindActivityProvider.java
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

package org.hfoss.posit.android.api.plugin;

import org.hfoss.posit.android.api.activity.FindActivity;
import org.hfoss.posit.android.api.activity.ListFindsActivity;

import android.app.Activity;

/**
* Convenience class to quickly get right find activity object without 
* having to do lots of chained calls  
*/
public class FindActivityProvider {
	private FindActivityProvider(){} // don't instantiate this class
	
	public static Class<FindActivity> getFindActivityClass(){
		return FindPluginManager.mFindPlugin.getmFindActivityClass();
	}

	public static Class<ListFindsActivity> getListFindsActivityClass(){
		return FindPluginManager.mFindPlugin.getmListFindsActivityClass();
	}
	
	public static Class<Activity> getLoginActivityClass(){
		return FindPluginManager.mFindPlugin.getmLoginActivityClass();
	}

	public static Class<Activity> getExtraActivityClass(){
		return FindPluginManager.mFindPlugin.getmExtraActivityClass();
	}
	
	public static Class<Activity> getExtraActivityClass2(){
		return FindPluginManager.mFindPlugin.getmExtraActivityClass2();
	}
}
