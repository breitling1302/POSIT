/*
 * File: ClpListFindsFragment.java
 * 
 * Copyright (C) 2012 The Humanitarian FOSS Project (http://www.hfoss.org)
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
package org.hfoss.posit.android.plugin.clp;

import java.util.List;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.fragment.FindFragment;
import org.hfoss.posit.android.api.fragment.ListFindsFragment;
import org.hfoss.posit.android.api.plugin.FindPluginManager;
import org.hfoss.posit.android.functionplugin.camera.Camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

public class ClpListFindsFragment extends ListFindsFragment {
	
	@Override
	protected void displayFind(int index, String action, Bundle extras, FindFragment findFragment) {
		super.displayFind(index, action, extras, new ClpFindFragment());
	}
	
	/**
	 * Sets up a custom list adapter specific to Clp finds.
	 */
	@Override
	protected ListAdapter setUpAdapter() {

		List<? extends Find> list = this.getHelper().getAllFinds();

		int resId = getResources().getIdentifier(FindPluginManager.mFindPlugin.mListFindLayout,
			    "layout", getActivity().getPackageName());
		
		ClpFindsListAdapter adapter = new ClpFindsListAdapter(getActivity(),
				resId, list);

		return adapter;
	}

	/**
	 * Adapter for displaying finds, extends FindsListAdapter to 
	 * take care of displaying the extra fields in OutsideInFind.
	 * 
	 */
	private class ClpFindsListAdapter extends FindsListAdapter{

		public ClpFindsListAdapter(Context context, int textViewResourceId,
				List list) {
			super(context, textViewResourceId, list);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.list_row, null);
			}
			ClpFind find = (ClpFind)items.get(position);
			if (find != null) {
				TextView tv = null;

				tv = (TextView) v.findViewById(R.id.name);
				tv.setText(String.valueOf(find.getName()));

				tv = (TextView) v.findViewById(R.id.description_id);
				tv.setText(String.valueOf(find.getDescription()));
				
				tv = (TextView) v.findViewById(R.id.latitude);
				tv.setText(String.valueOf(find.getLatitude()));
				tv = (TextView) v.findViewById(R.id.longitude);
				tv.setText(String.valueOf(find.getLongitude()));
				tv = (TextView) v.findViewById(R.id.id);
				tv.setText(Integer.toString(find.getId()));
				tv = (TextView) v.findViewById(R.id.status);
				tv.setText(find.getStatusAsString());			
				tv = (TextView) v.findViewById(R.id.time);
				tv.setText(find.getTime().toLocaleString());
				
				//Display the thumbnail picture beside the find
				//or a default image if there isn't one
				ImageView iv = (ImageView) v.findViewById(R.id.find_image);
				Bitmap bmp = Camera.getPhotoAsBitmap(find.getGuid(), getActivity());
				if(bmp != null){
				    iv.setImageBitmap(bmp);
				}
				else{
				    iv.setImageResource(R.drawable.device_access_camera);
				}
			}
			return v;
		}
	}
}
