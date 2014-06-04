/*
 * File: Camera.java
 * 
 * Copyright (C) 2009 The Humanitarian FOSS Project (http://www.hfoss.org)
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

package org.hfoss.posit.android.functionplugin.camera;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.database.DbHelper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.util.Base64;
import android.util.Log;

/**
 * Utility class used primarily by the Camera function plugin.
 */
public class Camera {

	public static final String TAG = "Camera";
	static final int THUMBNAIL_TARGET_SIZE = 320; //width and height of thumbnail data
		
	/**
	 * Saves the Base64 string of the phonto to internal memory 
	 * @param guid
	 * The GUID of the find
	 * @param img_data
	 * The Base64 String of the photo
	 * @param context
	 * Application context
	 * @return True if successful, false otherwise
	 */
	public static boolean savePhoto(String guid, String img_data, Context context){
	    FileOutputStream fos;
	    try {
	    	
			fos = context.openFileOutput(guid, Context.MODE_PRIVATE);
		    fos.write(img_data.getBytes());
		    fos.close();
		    return true;
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	    return false;
	}
	/**
	 * Read image's Base64 string back from internal storage and decode it to Bitmap
	 * @param guid
	 * The GUID of the find
	 * @param context
	 * Activity context
	 * @return Bitmap if successful, null otherwise
	 */
	public static Bitmap getPhotoAsBitmap(String guid, Context context){
		
		String str = getPhotoAsString(guid, context);
		if(str != null){
			//decode the Base64 string to bitmap
			byte[] c = Base64.decode(str, Base64.DEFAULT);
		    Bitmap bmp = BitmapFactory.decodeByteArray(c, 0, c.length);
		    return bmp;
		}
		else{
			return null;
		}
	}
	
	/**
	 * Read image's Base64 string back from internal storage
	 * @param guid
	 * The GUID of the find
	 * @param context
	 * Activity context
	 * @return Base64 String of the image or null
	 */
	public static String getPhotoAsString(String guid, Context context){
	    FileInputStream fis;
	    String content = "";
	    String filename_fullpath = "/data/data/" + context.getPackageName() + "/files/" + guid;
//	    Log.i(TAG, "fullpath=" + filename_fullpath);
	    File file = new File(filename_fullpath);
	    if (file.exists()){
		    try {
		    	fis = context.openFileInput(guid);
		    	byte[] input = new byte[fis.available()];
		    	while (fis.read(input) != -1) {}
		    	content += new String(input);
		    	fis.close();
			    return content;
		    } catch (FileNotFoundException e) {
		    	e.printStackTrace();
		    } catch (IOException e) {
		    	e.printStackTrace(); 
		    }
	    }
	    return null;
	}
	
	/**
	 * Returns the Base64 String of the thumbnail version of the photo
	 * @param guid
	 * The GUID of the find
	 * @param context
	 * Activity context
	 * @return String if successful or null otherwise
	 */
	public static String getPhotoThumbAsString(String guid, Context context){
		
		Bitmap bmp = getPhotoAsBitmap(guid, context);
		if(bmp != null){
		    //extract thumbnail from the original Bitmap
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Bitmap thumbCameraPic = ThumbnailUtils.extractThumbnail(bmp, THUMBNAIL_TARGET_SIZE, THUMBNAIL_TARGET_SIZE);
			thumbCameraPic.compress(Bitmap.CompressFormat.JPEG, 100, baos);
			byte[] b = baos.toByteArray();
			String thumbPicStr = Base64.encodeToString(b, Base64.DEFAULT); //thumbnail in base64 string
		    return thumbPicStr;
		}
		else{
			return null;
		}
	}
	
	/**
	 * Returns whether or not the find's image is out of sync
	 * @param find
	 * Find containing photo to be checked
	 * @param context
	 * Activity context
	 * @return True if photo is synced, false if not
	 */
	public static boolean isPhotoSynced(Find find, Context context){
	    String filename_fullpath = "/data/data/" + context.getPackageName() + "/files/" + find.getGuid();
	    File file = new File(filename_fullpath);
	    Date imageLastModified = new Date(file.lastModified());
	    Date findLastSynced = DbHelper.getDbManager(context).getTimeOfLastSync();
	    boolean synced = imageLastModified.before(findLastSynced);
		
		return synced;
	}
}
