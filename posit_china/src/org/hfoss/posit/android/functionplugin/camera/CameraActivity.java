/*
 * File: CameraActivity.java
 * 
 * Copyright (C) 2011 The Humanitarian FOSS Project (http://www.hfoss.org)
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

package org.hfoss.posit.android.functionplugin.camera;

import java.io.ByteArrayOutputStream;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.activity.ListFindsActivity;
import org.hfoss.posit.android.api.plugin.AddFindPluginCallback;
import org.hfoss.posit.android.api.plugin.ListFindPluginCallback;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

/**
 * This class calls the camera application and returns the Base64 string representation of the image
 *
 */

public class CameraActivity extends Activity 
	implements AddFindPluginCallback, ListFindPluginCallback {

	public static final String TAG="CameraActivity";
	public static final String PREFERENCES_IMAGE = "Image";
	static final int TAKE_CAMERA_REQUEST = 1000;
	private static String img_str = null; //stores base64 string of the image
	
	private ImageView photo;
			
    /** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    
		//launch the camera
		Intent pictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
		//get the full picture
		pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString());
		//how to handle the picture taken
		startActivityForResult(pictureIntent, TAKE_CAMERA_REQUEST);
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		switch(requestCode){
		case TAKE_CAMERA_REQUEST:
			if(resultCode == Activity.RESULT_CANCELED){
			}
			else if(resultCode == Activity.RESULT_OK){
				//handle photo taken
				Bitmap cameraPic = (Bitmap) data.getExtras().get("data");
				
				//encode to base64 string
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				cameraPic.compress(Bitmap.CompressFormat.JPEG, 100, baos);
				byte[] b = baos.toByteArray();
				img_str = Base64.encodeToString(b, Base64.DEFAULT);

				photo = new ImageView(this);
			    photo.setImageBitmap(cameraPic);//display the retrieved image
			    photo.setVisibility(View.VISIBLE);

			    //pass base64 string to calling function
				Intent intent=new Intent();  
				intent.putExtra("Photo", img_str);
				setResult(RESULT_OK, intent);
	    		finish();
			}
			break;
		}
	}

	/**
	 * Required for function plugins. Set the Finds thumbnail.
	 */
	public void listFindCallback(Context context, Find find, View view) {
		//Display the thumbnail picture beside the find
		//or a default image if there isn't one
		ImageView iv = (ImageView) view.findViewById(R.id.find_image);
		Bitmap bmp = Camera.getPhotoAsBitmap(find.getGuid(), context);
		if(bmp != null){
		    iv.setImageBitmap(bmp);
		}
		else{
		    iv.setImageResource(R.drawable.device_access_camera);
		}		
	}
	/**
	 * Required for function plugins. Unused here.
	 */
	public void menuItemSelectedCallback(Context context, Find find, View view,
			Intent intent) {
		// TODO Auto-generated method stub
		
	}
	/**
	 * Display the image in FindActivity's view.
	 */
	public void onActivityResultCallback(Context context, Find find, View view, Intent intent) {
		Log.i(TAG, "onActivityResultCallback");
		if (intent != null) {
			// do we get an image back?
			if (intent.getStringExtra("Photo") != null) {
				ImageView photo = (ImageView) view.findViewById(R.id.photo);
				String img_str = intent.getStringExtra("Photo");
				byte[] c = Base64.decode(img_str, Base64.DEFAULT);
				Bitmap bmp = BitmapFactory.decodeByteArray(c, 0, c.length);
				photo.setImageBitmap(bmp);// display the retrieved image
				photo.setVisibility(View.VISIBLE);
			}
		}
	}

	/**
	 * If this Find has a camera image, display it. The photo ImageView is
	 * an INVISIBLE view in the Basic interface
	 */
	public void displayFindInViewCallback(Context context, Find find, View view) {
		Log.i(TAG, "displayFindInViewCallback");
		Bitmap bmp = Camera.getPhotoAsBitmap(find.getGuid(), context);
		ImageView photo = (ImageView) view.findViewById(R.id.photo);

		if (bmp != null) {
			// we have a picture to display
			if (photo != null) {
				photo.setImageBitmap(bmp);
				photo.setVisibility(View.VISIBLE);
			}
		} else {
			// we don't have a picture to display. Nothing should show up, but
			// this is to make sure.
			if (photo != null)
				photo.setVisibility(View.INVISIBLE);
		}		
	}

	/**
	 * Called from FindActivity after a Find has been saved. Saves the
	 * image to a file.
	 */
	public void afterSaveCallback(Context context, Find find, View view, boolean isSaved) {
		// if the find is saved, we can save/update the picture to the phone
		if (isSaved) {
			// do we even have an image to save?
			if (img_str != null) {
				Log.i(TAG, "There is an image to save.");
				if (Camera.savePhoto(find.getGuid(), img_str, context)) {
					Log.i(TAG, "Successfully saved photo to phone with guid: "
							+ find.getGuid());
				} else {
					Log.i(TAG, "Failed to save photo to phone with guid: "
							+ find.getGuid());
				}
			}
		}
	
	}	
	
	/**
	 * Called from FindActivity once a FindActivity has been finished.
	 */
	public void finishCallback(Context context, Find find, View view) {
		img_str = null;
	}
	
	
	
}