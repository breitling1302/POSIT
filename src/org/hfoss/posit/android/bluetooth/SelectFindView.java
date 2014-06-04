package org.hfoss.posit.android.bluetooth;

import org.hfoss.posit.android.R;
import org.hfoss.posit.android.provider.PositDbHelper;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SelectFindView extends LinearLayout {
	private static final String TAG = "SelectFindView";
	private static final boolean D = false;

	// Field information
	private TextView mName;
	private ImageView mImage;
	private CheckBox mCheckBox;
	private SelectFind mSelectFind;

	public SelectFindView(Context context, SelectFind selectFind) {
		super(context);

		// TODO: inflate from xml
		
		// Layout
		this.setOrientation(HORIZONTAL);
		mSelectFind = selectFind;
		
		// Add image
		PositDbHelper myDbHelper = new PositDbHelper(context);
		ContentValues values = myDbHelper.getImages(selectFind.getId());
		mImage = new ImageView(context);
		mImage.setAdjustViewBounds(true);
		mImage.setMaxHeight(50);
		mImage.setMaxWidth(50);
		mImage.setScaleType(ImageView.ScaleType.FIT_XY);
		mImage.setImageResource(R.drawable.person_icon);
		if (values != null && values.containsKey(PositDbHelper.PHOTOS_IMAGE_URI)) {
			String strUri = values.getAsString(PositDbHelper.PHOTOS_IMAGE_URI);
			if (D) Log.i(TAG,"setViewValue strUri=" + strUri);
			if (strUri != null) {
				if (D) Log.i(TAG,"setViewValue strUri=" + strUri);
				Uri iUri = Uri.parse(strUri);
				mImage.setImageURI(iUri);
			}
		}
		myDbHelper.close();
		mImage.setPadding(0, 0, 0, 0);
		addView(mImage, new LinearLayout.LayoutParams(
				50, 50));
		
		// Add Checkbox
		mCheckBox = new CheckBox(context);
		mCheckBox.setPadding(0, 0, 0, 0);
		// Initial state of checkbox
		mCheckBox.setChecked(selectFind.getState());
		// Add checkbox to self
		RelativeLayout.LayoutParams cbParams = new RelativeLayout.LayoutParams(40, 40);
		cbParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		addView(mCheckBox, cbParams);
		
		// Add Name
		mName = new TextView(context);
		mName.setText(selectFind.getName());
		mName.setSingleLine(true);
		mName.setPadding(3, 0, 70, 0);
		mName.setTextSize(20);
		RelativeLayout.LayoutParams nameParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		nameParams.addRule(RelativeLayout.RIGHT_OF, mCheckBox.getId());
		addView(mName, nameParams);

		mName.setFocusable(false);
		mImage.setFocusable(false);
		mCheckBox.setFocusable(false);
	}

	public void setText(String words) {
		mName.setText(words);
	}

	public void setSelectFindState(boolean state) {
		mSelectFind.setState(state);
		mCheckBox.setChecked(mSelectFind.getState());
	}
	
	public void toggleSelectFindState() {
		setSelectFindState(!mSelectFind.getState());
	}

}
