/*
 * File: ActiveFuncPluginChangeEventListener.java
 * 
 * Copyright (C) 2012 The Humanitarian FOSS Project (http://www.hfoss.org)
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

import java.util.EventListener;

public interface ActiveFuncPluginChangeEventListener extends EventListener {
	/**
	 * Handler function that is triggered when a function plugin is enabled or disabled.
	 * 
	 * @param plugin Instance of the plugin that is being enabled/disabled
	 * @param enabled True if the plugin is being enabled and false if it is 
	 * being disabled.
	 */
	 public void handleActiveFuncPluginChangeEvent(FunctionPlugin plugin, boolean enabled);
}
