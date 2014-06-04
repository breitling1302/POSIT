/*
 * File: FuncPluginManagerActivity.java
 * 
 * Copyright (C) 2012 The Humanitarian FOSS Project (http://www.hfoss.org)
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

package org.hfoss.posit.android.api.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hfoss.posit.android.R;
import org.w3c.dom.Node;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity for enabling and disabling function plugins. This Activity is 
 * accessible from the "Plugin Manager" item in the settings menu.
 *
 */
public class FuncPluginManagerActivity extends ListActivity {

	PluginStatusListAdapter listAdapter;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    this.listAdapter = new PluginStatusListAdapter(FuncPluginManagerActivity.this);
	    setListAdapter(this.listAdapter);
	}

	/**
	 * Adapter for populating this activities list with the appropriate plugin info.
	 *
	 */
	private class PluginStatusListAdapter extends BaseAdapter
	{
		private List<PluginEnabledStatus> pluginEnabledStatusList;
		private Context context;
		private int wer = 0;
		public PluginStatusListAdapter(Context context)
		{
			this.context = context;
			
			this.pluginEnabledStatusList = new ArrayList<PluginEnabledStatus>();
			
			//Iterate through the plugin info in the shared preferences 
			//FUNC_PLUGIN_PREFS and store it in pluginEnabledStatusList.
			SharedPreferences funcPluginPrefs = context.getSharedPreferences(FindPluginManager.FUNC_PLUGIN_PREFS, MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE);
			Map<String, ?> funcPluginPrefsMap = funcPluginPrefs.getAll();
			for(String curFuncPluginName : funcPluginPrefsMap.keySet()){
				boolean curFuncPluginIsEnabled = funcPluginPrefs.getBoolean(curFuncPluginName, false); 
				this.pluginEnabledStatusList.add( new PluginEnabledStatus(curFuncPluginName, curFuncPluginIsEnabled));
			}
			
			//TODO: Sort list
		}
		
		public int getCount() {
			return this.pluginEnabledStatusList.size();
		}

		public Object getItem(int position) {
			return this.pluginEnabledStatusList.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View pluginStatusRowView;
			
			if (convertView == null)
			{
				LayoutInflater li = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				pluginStatusRowView = li.inflate(R.layout.plugin_status, parent, false);
			}
			else
			{
				pluginStatusRowView = convertView;
			}
			
			TextView pluginNameView = (TextView)pluginStatusRowView.findViewById(R.id.plugin_status_name);
            CheckBox pluginEnabledView = (CheckBox)pluginStatusRowView.findViewById(R.id.plugin_status_enabled);
            ImageButton pluginDescriptionBtn = (ImageButton)pluginStatusRowView.findViewById(R.id.plugin_status_description);
            
            //Set handler for enabling/disabling a plugin.
            pluginEnabledView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
		
            	public void onCheckedChanged(CompoundButton button, boolean checked) {
            		RelativeLayout pluginStatusRow = (RelativeLayout) button.getParent();
            		TextView pluginNameView = (TextView)pluginStatusRow.findViewById(R.id.plugin_status_name);
            		String pluginName = (String) pluginNameView.getText();
					
					FindPluginManager.getInstance().UpdateFuncPluginEnabledState(pluginName, checked);
				} 
        
            });
            
            pluginDescriptionBtn.setOnClickListener( new OnClickListener() {
				
				public void onClick(View v) {
					RelativeLayout pluginStatusRow = (RelativeLayout) v.getParent();
            		TextView pluginNameView = (TextView)pluginStatusRow.findViewById(R.id.plugin_status_name);
            		String pluginName = (String) pluginNameView.getText();

            		Node pluginInfo = FindPluginManager.getInstance().GetStaticPluginInfoFromPluginName(pluginName);
            		String description = pluginInfo.getAttributes().getNamedItem("description").getTextContent();
            		
					AlertDialog.Builder builder = new AlertDialog.Builder(context);
					builder.setTitle(pluginName)
						   .setMessage(description)
						   .setPositiveButton("OK", null)
						   .show();
				}
			});
            
            pluginNameView.setText(this.pluginEnabledStatusList.get(position).getPluginName());
            pluginEnabledView.setChecked(this.pluginEnabledStatusList.get(position).getEnabled());

			return pluginStatusRowView;
		}

	}
}