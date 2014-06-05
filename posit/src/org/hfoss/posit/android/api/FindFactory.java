/*
 * File: FindFactory.java
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

/**
 * Currently unused Factory for creating Finds.
 * 
 * TODO: Incorporate into the plugin architecture.
 *
 */
public abstract class FindFactory implements FindProviderInterface {
	protected FindFactory(){}
	
	public static void initIntance()throws Exception{
		throw new Exception("This method must be overwritten in subclass.");
	}
	
	public static FindFactory getInstance() throws Exception{
		throw new Exception("This method must be overwritten in subclass.");
	}
}
