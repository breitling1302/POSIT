package org.hfoss.posit.android.bluetooth;

import java.io.Serializable;
import java.util.List;

import org.hfoss.posit.android.provider.PositDbHelper;

import android.content.ContentValues;
import android.content.Context;

public class BluetoothFindTO implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 464482466319686475L;

	// Globally unique id for the find
	private String mGuid;

	// Other information about the
	// TODO
	private long mId;
	private String mName;
	private String mDescription;
	private String mLongitude;
	private String mLatitude;
	private int mSynced;
	
	public BluetoothFindTO() {
		// Do nothing
	}

	public BluetoothFindTO(Context context, String guid) {
		mGuid = guid;

		PositDbHelper dbHelper = new PositDbHelper(context);

		ContentValues values = dbHelper.fetchFindDataByGuId(guid, null);

		mId = values.getAsLong(PositDbHelper.FINDS_ID);
		mName = values.getAsString(PositDbHelper.FINDS_NAME);
		mDescription = values.getAsString(PositDbHelper.FINDS_DESCRIPTION);
		mLongitude = values.getAsString(PositDbHelper.FINDS_LONGITUDE);
		mLatitude = values.getAsString(PositDbHelper.FINDS_LATITUDE);
		mSynced = values.getAsInteger(PositDbHelper.FINDS_SYNCED);

		dbHelper.close();
	}

	public BluetoothFindTO(long id, String guid, String name, String desc,
			String longitude, String latitude, int synced) {
		mId = id;
		mGuid = guid;
		mName = name;
		mDescription = desc;
		mLongitude = longitude;
		mLatitude = latitude;
		mSynced = synced;
	}

	public long getId() {
		return mId;
	}

	public String getGuid() {
		return mGuid;
	}

	public String getName() {
		return mName;
	}

	public String getDescription() {
		return mDescription;
	}

	public String getLongitude() {
		return mLongitude;
	}

	public String getLatitude() {
		return mLatitude;
	}

	public int getSyncedState() {
		return mSynced;
	}

	public List<ContentValues> getPhotos() {
		return null;
	}

}
