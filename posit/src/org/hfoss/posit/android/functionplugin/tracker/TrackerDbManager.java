package org.hfoss.posit.android.functionplugin.tracker;

import java.sql.SQLException;
import java.util.List;

import org.hfoss.posit.android.api.FindHistory;
import org.hfoss.posit.android.api.User;
import org.hfoss.posit.android.api.database.DbManager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

public class TrackerDbManager extends DbManager {

	public static final String TAG = "TrackerDbManager";
	public static final String DATABASE_NAME = "tracker";
	public static final int DATABASE_VERSION = 1;
	
	public static final String EXPEDITION_GPS_POINTS_TABLE = "points";
	public static String EXPEDITION;
	public static final String GPS_SYNCED = "synced";
	public static String GPS_POINT_LATITUDE;
	public static String GPS_POINT_LONGITUDE;
	public static String GPS_POINT_ALTITUDE;

	public static final String EXPEDITION_POINTS = "expedition_points";
	public static final String EXPEDITION_SYNCED = "expedition_synced";
	public static final String EXPEDITION_REGISTERED = "expedition_registered";
	public static final int EXPEDITION_NOT_REGISTERED = 0;	
	public static final int EXPEDITION_IS_REGISTERED = 1;
	public static final int FIND_IS_SYNCED = 1;
	public static final int FIND_NOT_SYNCED = 0;
	public static final String EXPEDITION_PROJECT_ID = "project_id";
	public static final String EXPEDITION_ROW_ID = null;
	public static final String EXPEDITION_NUM = null;
	public static final String GPS_POINT_SWATH = null;
	public static final String GPS_TIME = null;
	public static final String EXPEDITION_GPS_POINT_ROW_ID = null;

	public String[] track_data;
	public int[] track_views;


	// DAO objects used to access the Db tables
	private Dao<Expedition, Integer> expeditionDao = null;
	private Dao<Points, Integer> pointsDao = null;


	public TrackerDbManager(Context context) {
		super(context);
//		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		Log.i(TAG, "TrackerDbManager constructor");
	}

//	public TrackerDbManager(Context context, String databaseName,
//			CursorFactory factory, int databaseVersion) {
//		super(context, databaseName, factory, databaseVersion);
//		// TODO Auto-generated constructor stub
//	}



	/**
	 * Invoked automatically if the Db doesn't exist. 
	 */
	@Override
	public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
		Log.i(TAG, "onCreate()");
		Expedition.createTable(connectionSource, getExpeditionDao());
		Points.createTable(connectionSource, getPointsDao());
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVerions,
			int newVersion) {
		Log.i(TAG, "onUpdate()");
		try {
			TableUtils.dropTable(connectionSource, Expedition.class, true);
			TableUtils.dropTable(connectionSource, Points.class, true);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Returns the Database Access Object (DAO) for the Expedition class. It
	 * will create it or just give the cached value.
	 */
	public Dao<Expedition, Integer> getExpeditionDao() {
		if (expeditionDao == null) {
			try {
				expeditionDao = getDao(Expedition.class);
			} catch (SQLException e) {
				Log.e(TAG, "Get user DAO failed.");
				e.printStackTrace();
			}
		}
		return expeditionDao;
	}

	/**
	 * Returns the Database Access Object (DAO) for the Expedition class. It
	 * will create it or just give the cached value.
	 */
	public Dao<Points, Integer> getPointsDao() {
		if (pointsDao == null) {
			try {

				pointsDao = getDao(Points.class);
			} catch (SQLException e) {
				Log.e(TAG, "Get user DAO failed.");
				e.printStackTrace();
			}
		}
		return pointsDao;
	}
//	public Cursor fetchExpeditionsByProjectId(int mProjectId) {
//		// TODO Auto-generated method stub
//		return null;
//	}

	public List<? extends Expedition> fetchExpeditionsByProjectId(int mProjectId) {
		// TODO Auto-generated method stub
		return null;
	}	
	
	public int addNewExpedition(ContentValues values) {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean updateGPSPoint(long rowId, ContentValues vals) {
		// TODO Auto-generated method stub
		return false;
	}

	public int updateExpedition(int mExpeditionNumber, ContentValues expVals) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int addNewGPSPoint(ContentValues resultGPSPoint) {
		// TODO Auto-generated method stub
		return 0;
	}

}
