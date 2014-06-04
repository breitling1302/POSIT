/*
 * File: FindPlugin.java
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

import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.activity.FindActivity;
import org.hfoss.posit.android.api.activity.ListFindsActivity;
import org.hfoss.posit.android.api.activity.SettingsActivity;
import org.hfoss.posit.android.api.fragment.FindFragment;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

import android.app.Activity;
import android.util.Log;

/**
 * A FindPlugin extends the BasicFind.  Its data is
 * typically an extension of the data in the Posit Db.
 *
 */
public class FindPlugin extends Plugin {

	public static String mAddFindLayout = null;
	public static String mListFindLayout = null;
	public static String mMainIcon = null;
	public static String mAddButtonLabel = null;
	public static String mListButtonLabel = null;
	public static String mExtraButtonLabel = null;
	public static String mExtraButtonLabel2 = null;
		
	protected Class<Find> mFindClass;
	protected Class<FindActivity> mFindActivityClass = null;
	protected Class<FindFragment> mFindFragmentClass = null;
	protected Class<ListFindsActivity> mListFindsActivityClass = null;
	protected Class<Activity> mExtraActivityClass = null;
	protected Class<Activity> mExtraActivityClass2 = null;
	protected Class<Activity> mLoginActivityClass = null;
	
	public FindPlugin(Activity activity, Node node) throws DOMException, ClassNotFoundException {
		mMainActivity = activity;
		
		name = node.getAttributes().getNamedItem("name").getTextContent();
		type = node.getAttributes().getNamedItem("type").getTextContent();

		String package_name = node.getAttributes().getNamedItem("package").getTextContent();
		String find_factory_name = node.getAttributes().getNamedItem("find_factory").getTextContent();
		String db_manager_name = node.getAttributes().getNamedItem("find_data_manager").getTextContent();
		String findclass_name = node.getAttributes().getNamedItem("find_class").getTextContent();
		String findactivity_name = node.getAttributes().getNamedItem("find_activity_class").getTextContent();
		String findfragment_name = node.getAttributes().getNamedItem("find_fragment_class").getTextContent();
		String listfindsactivity_name = node.getAttributes().getNamedItem("list_finds_activity_class").getTextContent();
		String extra_activity_name = node.getAttributes().getNamedItem("extra_activity_class").getTextContent();
		String extra_activity_name2 = node.getAttributes().getNamedItem("extra_activity_class2").getTextContent();
				
		Node aNode = node.getAttributes().getNamedItem("login_activity_class");
		String login_activity_name = "";
		if (aNode != null) 
			login_activity_name = node.getTextContent();
		//String login_activity_name = node.getAttributes().getNamedItem("login_activity_class").getTextContent();

		mMainIcon = node.getAttributes().getNamedItem("main_icon").getTextContent();
		mAddButtonLabel = node.getAttributes().getNamedItem("main_add_button_label").getTextContent();
		mListButtonLabel = node.getAttributes().getNamedItem("main_list_button_label").getTextContent();
		mExtraButtonLabel = node.getAttributes().getNamedItem("main_extra_button_label").getTextContent();
		mExtraButtonLabel2 = node.getAttributes().getNamedItem("main_extra_button_label2").getTextContent();
		mPreferences = node.getAttributes().getNamedItem("preferences_xml").getTextContent();
		mAddFindLayout = node.getAttributes().getNamedItem("add_find_layout").getTextContent();
		mListFindLayout = node.getAttributes().getNamedItem("list_find_layout").getTextContent();
		
		
		@SuppressWarnings({ "rawtypes" })
		Class new_class = Class.forName(find_factory_name);
		//mFindFactory = (FindFactory)new_class.getMethod("getInstance", null).invoke(null, null);
		
		new_class = Class.forName(db_manager_name);
		//mDbManager = (DbManager)new_class.getMethod("getInstance", null).invoke(null, null);

		mFindClass = (Class<Find>)Class.forName(findclass_name);
		mFindActivityClass = (Class<FindActivity>)Class.forName(findactivity_name);
		mFindFragmentClass = (Class<FindFragment>)Class.forName(findfragment_name);
		mListFindsActivityClass = (Class<ListFindsActivity>)Class.forName(listfindsactivity_name);
		if (!login_activity_name.equals(""))
			mLoginActivityClass = (Class<Activity>)Class.forName(login_activity_name); // Changed
		if (!extra_activity_name.equals(""))	
			mExtraActivityClass = (Class<Activity>) Class
					.forName(package_name + "."	+ extra_activity_name);
		if (!extra_activity_name2.equals(""))
			mExtraActivityClass2 = (Class<Activity>) Class
					.forName(package_name + "."	+ extra_activity_name2);
			
		Log.i(TAG,"Loading preferences for Settings Activity");
		SettingsActivity.loadPluginPreferences(mMainActivity, mPreferences);

		// Remove break to load more than one plugin
		//break;		
	}

	public static String getmAddFindLayout() {
		return mAddFindLayout;
	}

	public static void setmAddFindLayout(String mAddFindLayout) {
		FindPlugin.mAddFindLayout = mAddFindLayout;
	}

	public static String getmListFindLayout() {
		return mListFindLayout;
	}

	public static void setmListFindLayout(String mListFindLayout) {
		FindPlugin.mListFindLayout = mListFindLayout;
	}

	public static String getmMainIcon() {
		return mMainIcon;
	}

	public static void setmMainIcon(String mMainIcon) {
		FindPlugin.mMainIcon = mMainIcon;
	}

	public static String getmAddButtonLabel() {
		return mAddButtonLabel;
	}

	public static void setmAddButtonLabel(String mAddButtonLabel) {
		FindPlugin.mAddButtonLabel = mAddButtonLabel;
	}

	public static String getmListButtonLabel() {
		return mListButtonLabel;
	}

	public static void setmListButtonLabel(String mListButtonLabel) {
		FindPlugin.mListButtonLabel = mListButtonLabel;
	}

	public static String getmExtraButtonLabel() {
		return mExtraButtonLabel;
	}

	public static void setmExtraButtonLabel(String mExtraButtonLabel) {
		FindPlugin.mExtraButtonLabel = mExtraButtonLabel;
	}

	public static String getmExtraButtonLabel2() {
		return mExtraButtonLabel2;
	}

	public static void setmExtraButtonLabel2(String mExtraButtonLabel2) {
		FindPlugin.mExtraButtonLabel2 = mExtraButtonLabel2;
	}

	public Class<Find> getmFindClass() {
		return mFindClass;
	}

	public void setmFindClass(Class<Find> mFindClass) {
		this.mFindClass = mFindClass;
	}

	public Class<FindActivity> getmFindActivityClass() {
		return mFindActivityClass;
	}
	
	public Class<FindFragment> getmFindFragmentClass() {
		return mFindFragmentClass;
	}

	public void setmFindActivityClass(Class<FindActivity> mFindActivityClass) {
		this.mFindActivityClass = mFindActivityClass;
	}

	public Class<ListFindsActivity> getmListFindsActivityClass() {
		return mListFindsActivityClass;
	}

	public void setmListFindsActivityClass(
			Class<ListFindsActivity> mListFindsActivityClass) {
		this.mListFindsActivityClass = mListFindsActivityClass;
	}

	public Class<Activity> getmExtraActivityClass() {
		return mExtraActivityClass;
	}

	public void setmExtraActivityClass(Class<Activity> mExtraActivityClass) {
		this.mExtraActivityClass = mExtraActivityClass;
	}

	public Class<Activity> getmExtraActivityClass2() {
		return mExtraActivityClass2;
	}

	public void setmExtraActivityClass2(Class<Activity> mExtraActivityClass2) {
		this.mExtraActivityClass2 = mExtraActivityClass2;
	}

	public Class<Activity> getmLoginActivityClass() {
		return mLoginActivityClass;
	}

	public void setmLoginActivityClass(Class<Activity> mLoginActivityClass) {
		this.mLoginActivityClass = mLoginActivityClass;
	}
	
}
