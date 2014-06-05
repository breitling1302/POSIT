/*
 * File: PluginEnabledStatus.java
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

/**
 * 
 * Used to store the enabled status of a plugin. The plugin is identified by 
 * the pluginName field and its enabled status is identified by the enabled 
 * field. 
 *
 */
public class PluginEnabledStatus {
	
	/**
	 * A unique name associated with a plugin
	 */
	protected String pluginName;
	
	protected boolean enabled;
	
	public PluginEnabledStatus(String pluginName, boolean enabled)
	{
		this.pluginName = pluginName;
		this.enabled = enabled;
	}
	
	public String getPluginName()
	{
		return this.pluginName;
	}
	
	public boolean getEnabled()
	{
		return this.enabled;
	}
	
	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("PLUGIN_NAME=").append(this.pluginName);
		sb.append(", ").append("PLUGIN_ENABLED=").append(this.enabled);
		return sb.toString();
	}

}
