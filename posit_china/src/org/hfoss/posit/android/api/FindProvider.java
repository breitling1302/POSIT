/*
 * File: FindProvider.java
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

import org.hfoss.posit.android.api.plugin.FindPluginManager;

import android.content.Context;

/**
* Convenience class to quickly get right find object 
* without having to type.
* 
* TODO: Make this functional or delete it.
*/
public class FindProvider{
	private FindProvider(){}
	
//	public static Find createNewFind(Context context){
//		return FindPluginManager.getInstance().getFindFactory().createNewFind(context);
//	}
//	
//	public static Find createNewFind(Context context, long id){
//		return FindPluginManager.getInstance().getFindFactory().createNewFind(context, id);
//	}
//	
//	public static Find createNewFind(Context context, String guid){
//		return FindPluginManager.getInstance().getFindFactory().createNewFind(context, guid);
//	}
}
