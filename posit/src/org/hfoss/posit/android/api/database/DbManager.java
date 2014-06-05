/*
 * File: DbManager.java
 * 
 * Copyright (C) 2011 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Source Information Tool.
 *
 * This is free software; you can redistribute it and/or modify
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

package org.hfoss.posit.android.api.database;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.hfoss.posit.android.Constants;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.FindHistory;
import org.hfoss.posit.android.api.SyncHistory;
import org.hfoss.posit.android.api.User;
import org.hfoss.posit.android.api.plugin.FindPluginManager;
import org.hfoss.posit.android.functionplugin.tracker.Expedition;
import org.hfoss.posit.android.functionplugin.tracker.Points;
//import org.hfoss.posit.android.plugin.acdivoca.AcdiVocaFind;

import com.j256.ormlite.android.apptools.OrmLiteBaseListActivity;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * The class is the interface with the SqlLite Database as mediated
 * by the OrmLite Object-relational-mapper.  It controls all Db access 
 * and handles all Db queries.
 * 
 * This class should be included as the type in all OrmLiteBaseActivities:
 * -- e.g., ListFindsActivity extends OrmLiteBaseListActivity<DbManager>.
 * 
 * When so used, an DbManager object can be retrieved using the getHelper()
 * method.
 */
public class DbManager extends OrmLiteSqliteOpenHelper {

	private static final String TAG = "DbManager";
	
	// DAO objects used to access the Db tables
	private Dao<User, Integer> userDao = null;
	private Dao<Find, Integer> findDao = null;
	private Dao<FindHistory, Integer> findHistoryDao = null;
	private Dao<SyncHistory, Integer> syncHistoryDao = null;
		
	// DAO objects used to access the Db tables
	private Dao<Expedition, Integer> expeditionDao = null;
	private Dao<Points, Integer> pointsDao = null;

	// Static constants used in Db queries
	public static final String DATABASE_NAME = "posit";
	public static final int DATABASE_VERSION = 2;
	public static final String FIND_TABLE_NAME = "find"; // All find extensions should use this table name

	public static final int DELETE_FIND = 1;
	public static final int UNDELETE_FIND = 0;
//	public static final String WHERE_NOT_DELETED = " " + AcdiVocaFind.DELETED + " != " + DELETE_FIND + " ";
	public static final String DATETIME_NOW = "`datetime('now')`";

	public static final String FINDS_HISTORY_TABLE = "acdi_voca_finds_history";
	public static final String HISTORY_ID = "_id";
	
	public static final String EXPEDITION_GPS_POINTS_TABLE = "points";
	public static String EXPEDITION = "expedition";
	public static final String GPS_SYNCED = "synced";
	public static String GPS_POINT_LATITUDE = "latitude";
	public static String GPS_POINT_LONGITUDE = "longitude";
	public static String GPS_POINT_ALTITUDE = "altitude";
	public static final int POINT_IS_SYNCED = 1;
	public static final int POINT_NOT_SYNCED = 0;
	
	public static final String EXPEDITION_POINTS = "expedition_points";
	public static final String EXPEDITION_SYNCED = "expedition_synced";
	public static final String EXPEDITION_REGISTERED = "expedition_registered";
	public static final int EXPEDITION_NOT_REGISTERED = 0;	
	public static final int EXPEDITION_IS_REGISTERED = 1;
	public static final int FIND_IS_SYNCED = 1;
	public static final int FIND_NOT_SYNCED = 0;
	public static final String EXPEDITION_PROJECT_ID = "project_id";
	public static final String EXPEDITION_ROW_ID = "_id";
	public static final String EXPEDITION_NUM = "expedition_number";
	public static final String GPS_POINT_SWATH = "swath";
	public static final String GPS_TIME = "time";
	public static final String EXPEDITION_GPS_POINT_ROW_ID = "_id";

	/**
	 * Constructor just saves and opens the Db.
	 * 
	 * @param context
	 */
	public DbManager(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		Log.i(TAG, "constructor");
	}

	/**
	 * Invoked automatically if the Database does not exist. It 
	 * creates the Db tables. 
	 */
	@Override
	public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
		Log.i(TAG, "onCreate");
		Class<Find> findClass = FindPluginManager.mFindPlugin.getmFindClass();
		try {
			findClass.getMethod("createTable", ConnectionSource.class).invoke(null, connectionSource);
		} catch (Exception e) {
			e.printStackTrace();
		}
		User.createTable(connectionSource, getUserDao());
		FindHistory.createTable(connectionSource);
		SyncHistory.createTable(connectionSource);
		
		Expedition.createTable(connectionSource, getExpeditionDao());
		Points.createTable(connectionSource, getPointsDao());

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
		try {
			Log.i(TAG, "onUpgrade");
			TableUtils.dropTable(connectionSource, User.class, true);
			TableUtils.dropTable(connectionSource, FindHistory.class, true);
			TableUtils.dropTable(connectionSource, SyncHistory.class, true);
			
			TableUtils.dropTable(connectionSource, Expedition.class, true);
			TableUtils.dropTable(connectionSource, Points.class, true);

			Class<Find> findClass = FindPluginManager.mFindPlugin.getmFindClass();
			TableUtils.dropTable(connectionSource, findClass, true);

			// after we drop the old databases, we create the new ones
			onCreate(db, connectionSource);
		} catch (SQLException e) {
			Log.e(TAG, "Can't drop databases", e);
			throw new RuntimeException(e);
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
	 * Returns the Database Access Object (DAO) for the Points class. It
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
	
	/**
	 * Returns the Database Access Object (DAO) for the AcdiVocaUser class. It
	 * will create it or just give the cached value.
	 */
	public Dao<User, Integer> getUserDao() {
		if (userDao == null) {
			try {
				userDao = getDao(User.class);
			} catch (SQLException e) {
				Log.e(TAG, "Get user DAO failed.");
				e.printStackTrace();
			}
		}
		return userDao;
	}

	/**
	 * Returns the Database Access Object (DAO) for the Find class. It will
	 * create it or just give the cached value.
	 */
	public Dao<Find, Integer> getFindDao() {
		if (findDao == null) {
			Class<Find> findClass = FindPluginManager.mFindPlugin.getmFindClass();
			try {
				findDao = getDao(findClass);
			} catch (SQLException e) {
				Log.e(TAG, "Get find DAO failed.");
				e.printStackTrace();
			}
		}
		return findDao;
	}

	/**
	 * Returns the Database Access Object (DAO) for the FindHistory class. It
	 * will create it or just give the cached value.
	 */
	public Dao<FindHistory, Integer> getFindHistoryDao() {
		if (findHistoryDao == null) {
			try {
				findHistoryDao = getDao(FindHistory.class);
			} catch (SQLException e) {
				Log.e(TAG, "Get find DAO failed.");
				e.printStackTrace();
			}
		}
		return findHistoryDao;
	}

	/**
	 * Returns the Database Access Object (DAO) for the AcdiVocaUser class. It
	 * will create it or just give the cached value.
	 */
	public Dao<SyncHistory, Integer> getSyncHistoryDao() {
		if (syncHistoryDao == null) {
			try {
				syncHistoryDao = getDao(SyncHistory.class);
			} catch (SQLException e) {
				Log.e(TAG, "Get user DAO failed.");
				e.printStackTrace();
			}
		}
		return syncHistoryDao;
	}
	

	/**
	 * Returns a list of Expeditions for a given project.
	 * @param projectId, the Id of the given project.
	 * @return 
	 */
	public List<? extends Expedition> fetchExpeditionsByProjectId(int projectId) {
		List<Expedition> list = null;
		try {
			QueryBuilder<Expedition, Integer> builder = getExpeditionDao().queryBuilder();
			Where<Expedition, Integer> where = builder.where();
			where.eq(Find.PROJECT_ID, projectId);
			PreparedQuery<Expedition> preparedQuery = builder.prepare();

			list = getExpeditionDao().query(preparedQuery);
		} catch (SQLException e) {
			Log.e(TAG, "Database error getting finds: " + e.getMessage());
		}		return list;		
	}	

	/**
	 * Creates a new expedition in the Db given its content values.
	 * @param values, the Expedition's attr=val pairs.
	 * @return
	 */
	public int addNewExpedition(ContentValues values) {
		Expedition expedition = new Expedition(values);
		int rows = 0;
		try {
			rows = getExpeditionDao().create(expedition);
			if (rows == 1) {
				Log.i(TAG, "Inserted Expedition:  " + expedition.toString());
			} else {
				Log.e(TAG, "Db Error inserting Expedition: " + expedition.toString());
				rows = 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rows;
	}
	
	/**
	 * Looks up a Point by its row id.
	 * 
	 * @param rowId the id of the point to look up
	 * @return the point
	 */
	public Points getPointById(int rowId) {
		Points point = null;
		try {
			point = getPointsDao().queryForId(rowId);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return point;
	}
	
	/**
	 * Delete's all the point associated with a given Expedition.
	 * @param id, the id of the given Expedition.
	 * @return
	 */
	public boolean deleteExpeditionPoints(int id) {
		List<Points> allPoints = getPointsByExpeditionId(id);
		int size = allPoints.size();
		int rows = 0;
		try {
			Iterator iterator = allPoints.iterator();
			while (iterator.hasNext()) {
				rows += getPointsDao().delete( (Points)iterator.next());
			}
			//rows = getPointsDao().delete(allPoints);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.i(TAG, "rows = " + rows + " size= " + size);
		return rows == size;
	}

	/**
	 * Get an expedition's points.
	 * @param expeditionId, the expedition's id.
	 * @return
	 */
	public List<Points> getPointsByExpeditionId(int expeditionId) {
		List<Points> list = null;
		try {
			QueryBuilder<Points, Integer> builder = getPointsDao().queryBuilder();
			Where<Points, Integer> where = builder.where();
			where.eq(Points.EXPEDITION, expeditionId);
			PreparedQuery<Points> preparedQuery = builder.prepare();
			list = getPointsDao().query(preparedQuery);
		} catch (SQLException e) {
			Log.e(TAG, "Database error getting finds: " + e.getMessage());
		}
		return list;
	}
	
	/**
	 * Get a list of points that haven't been sent to the server.
	 * @param expeditionId, the Expedition's Id.
	 * @return
	 */
	public List<Points> getUnsyncedPointsByExpeditionId(int expeditionId) {
		List<Points> list = null;
		try {
			QueryBuilder<Points, Integer> builder = getPointsDao().queryBuilder();
			Where<Points, Integer> where = builder.where();
			where.eq(Points.EXPEDITION, expeditionId);
			where.and();
			where.eq(Points.GPS_SYNCED, POINT_NOT_SYNCED);
			PreparedQuery<Points> preparedQuery = builder.prepare();
			list = getPointsDao().query(preparedQuery);
		} catch (SQLException e) {
			Log.e(TAG, "Database error getting finds: " + e.getMessage());
		}
		return list;
	}

	/**
	 * Updates a given point -- mostly marking it synced.
	 * @param rowId, the point's id.
	 * @param vals, the update values.
	 * @return
	 */
	public boolean updateGPSPoint(int rowId, ContentValues vals) {
		Log.i(TAG, "Updating GPS Point, rowId = " + rowId);
		int rows = 0;
		Points point = getPointById(rowId);
		try {
			if (vals.containsKey(GPS_SYNCED))
				point.setSynced(vals.getAsInteger(GPS_SYNCED));
			rows = getPointsDao().update(point);
			if (rows == 1) {
				Log.i(TAG, "Updated Expedition:  " + this.toString());
			} else {
				Log.e(TAG, "Db Error updating Expedition: " + this.toString());
				rows = 0;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rows == 1;
	}

	/**
	 * Delete an expedition given its number (not its row id).
	 * @param expNum, the expedition's Posit Number, as returned from
	 * the server.
	 * @return
	 */
	public boolean deleteExpedition(int expNum) {
		Log.i(TAG, "Deleting Expedition, expNum = " + expNum);
		int rows = 0;
		Expedition expedition = getExpeditionByExpeditionNumber(expNum);
		try {
			rows = getExpeditionDao().delete(expedition);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rows == 1;
	}

	/**
	 * Update the given expedition, either updating its points count
	 *  or marking it synced.
	 * @param expNum, the expedition's number as assigned by Posit server.
	 * @param values
	 * @return
	 */
	public int updateExpedition(int expNum, ContentValues values) {
		Log.i(TAG, "Updating Expedition, expNum = " + expNum);
		int rows = 0;
		Expedition expedition = getExpeditionByExpeditionNumber(expNum);
		try {
			if (values.containsKey(EXPEDITION_POINTS))
					expedition.setPoints(values.getAsInteger(EXPEDITION_POINTS));
			if (values.containsKey(EXPEDITION_SYNCED))
				expedition.setIs_synced(values.getAsInteger(EXPEDITION_SYNCED));
			rows = getExpeditionDao().update(expedition);
			if (rows == 1) {
				Log.i(TAG, "Updated Expedition :  " + this.toString());
			} else {
				Log.e(TAG, "Db Error updating Expedition: " + this.toString());
				rows = 0;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rows;
	}
	
	/**
	 * Looks up an Expedition by its Expedition number.
	 * 
	 * @param expNum, the expedition's number as returned from the server
	 * @return the expedition
	 */
	public Expedition getExpeditionByExpeditionNumber(int expNum) {
		Expedition expedition = null;
		try {
			QueryBuilder<Expedition, Integer> builder = getExpeditionDao().queryBuilder();
			Where<Expedition, Integer> where = builder.where();
			where.eq(Expedition.EXPEDITION_NUM, expNum);
			PreparedQuery<Expedition> query = builder.prepare();
			expedition = getExpeditionDao().queryForFirst(query);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return expedition;
	}

	/**
	 * Inserts a GPS value into the Db and returns its row Id
	 * @param values, a ContentValues with lat, long, etc. 
	 * @return the point's row Id
	 */
	public int addNewGPSPoint(ContentValues values) {
		Points point = new Points(values);
		int rows = 0;
		int rowId = 0;
		try {
			rows = getPointsDao().create(point);
			if (rows == 1) {
				Log.i(TAG, "Inserted Point:  " + point.toString());
				rowId = point.getId();
			} else {
				Log.e(TAG, "Db Error inserting Point: " + point.toString());
				rowId = 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rowId;
	}

	

	/**
	 * Fetches all finds currently in the database.
	 * 
	 * @return A list of all the finds.
	 */
	public List<? extends Find> getAllFinds() {
		List<Find> list = null;
		try {
			list = getFindDao().queryForAll();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;

	}

	/**
	 * Returns a list of Finds associated with a given project.
	 * @param projectId, the given project's Id as assigned by Posit server.
	 * @return
	 */
	public List<Find> getFindsByProjectId(int projectId) {
		List<Find> list = null;
		try {
			QueryBuilder<Find, Integer> builder = getFindDao().queryBuilder();
			Where<Find, Integer> where = builder.where();
			where.eq(Find.PROJECT_ID, projectId);
			PreparedQuery<Find> preparedQuery = builder.prepare();

			list = getFindDao().query(preparedQuery);
		} catch (SQLException e) {
			Log.e(TAG, "Database error getting finds: " + e.getMessage());
		}
		return list;
	}

	/**
	 * Looks up a find by its row (OrmLite) ID.
	 * 
	 * @param id the id of the find to look up
	 * @return the find
	 */
	public Find getFindById(int id) {
		Find find = null;
		try {
			find = getFindDao().queryForId(id);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return find;
	}
	
	/**
	 * Looks up a find by its GUID, globally unique Id.
	 * 
	 * @param guid the guid of the find to look up
	 * @return the find
	 */
	public Find getFindByGuid(String guid) {
		Find find = null;
		try {
			QueryBuilder<Find, Integer> builder = getFindDao().queryBuilder();
			Where<Find, Integer> where = builder.where();
			where.eq(Find.GUID, guid);
			PreparedQuery<Find> query = builder.prepare();
			find = getFindDao().queryForFirst(query);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return find;
	}

	/**
	 * Inserts the list of Finds returning the number inserted.
	 * @param finds
	 * @return
	 */
	public int insertAll(List<Find> finds) {
		int count = 0;
		Iterator<Find> it = finds.iterator();
		while (it.hasNext()) {
			Find find = it.next();
			count += insert(find);
		}
		return count;
	}
	
	
	/**
	 * Inserts this find into the database and updates the FindHistory
	 *  table.
	 * 
	 * @param find, the Find to be inserted
	 * @return the number of rows inserted.
	 */

	public int insert(Find find) {
		int rows = 0;
		try {
			find.setAction(FindHistory.ACTION_CREATE);
			rows = getFindDao().create(find);
			if (rows == 1) {
				Log.i(TAG, "Inserted find:  " + find.toString());
				recordChangedFind(new FindHistory(find, FindHistory.ACTION_CREATE));
			} else {
				Log.e(TAG, "Db Error inserting find: " + find.toString());
				rows = 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rows;
	}
	
	/**
	 * Inserts this find into the database without making an entry
	 * in FindHistory.  Used for syncing.
	 * 
	 * @param find, the Find object to be inserted
	 * @return the number of rows inserted.
	 */

	public int insertWithoutHistory(Find find) {
		int rows = 0;
		try {
			find.setAction(FindHistory.ACTION_CREATE);
			find.setStatus(Constants.SUCCEEDED);
			rows = getFindDao().create(find);
			if (rows == 1) {
				Log.i(TAG, "Inserted find:  " + find.toString());
			} else {
				Log.e(TAG, "Db Error inserting find: " + find.toString());
				rows = 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rows;
	}

	/**
	 * Updates the Db for the given find.
	 * 
	 * @param find, the updated Find object
	 * @return the number of rows updated.
	 */
	public int update(Find find) {
		int rows = 0;
		try {
			Log.i(TAG, "Updating: " + find);
			if (find.getStatus() == Constants.SUCCEEDED)
				find.setAction(FindHistory.ACTION_UPDATE);
			rows = getFindDao().update(find);
			if (rows == 1) {
				Log.i(TAG, "Updated find:  " + find.toString());
				recordChangedFind(new FindHistory(find, FindHistory.ACTION_UPDATE));
			} else {
				Log.e(TAG, "Db Error updating find: " + find.toString());
				rows = 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rows;
	}
	
	/**
	 * Updates the Db for the given Find without 
	 * adding an entry to FindHistory.  Used for syncing.
	 * 
	 * @param find, the updated Find.
	 * @return the number of rows updated.
	 */
	public int updateWithoutHistory(Find find) {
		int rows = 0;
		try {
			find.setAction(FindHistory.ACTION_UPDATE);
			find.setStatus(Constants.SUCCEEDED);   // Marks it synced
			rows = getFindDao().update(find);
			if (rows == 1) {
				Log.i(TAG, "Updated find:  " + find.toString());
			} else {
				Log.e(TAG, "Db Error updating find: " + find.toString());
				rows = 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rows;
	}

	/**
	 * Deletes a find from the Db. Note that OrmLite truly
	 *  removes the Find.  It doesn't just mark it deleted. 
	 * 
	 * @param find, the Find being deleted.  
	 * @return the number of rows deleted. 
	 */
	public int delete(Find find) {
		int rows = 0;
		try {
			rows = getFindDao().delete(find);
			if (rows == 1) {
				Log.i(TAG, "Deleted find:  " + find.toString());
				recordChangedFind(new FindHistory(find, FindHistory.ACTION_DELETE));
			} else {
				Log.e(TAG, "Db Error deleting find: " + find.toString());
				rows = 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rows;

	}
	
	/**
	 * Deletes all finds with the given project Id.
	 * @param projectID
	 * @return a boolean representing success or failure.
	 * NOTE:  There appears to be a bug in OrmLite's delete(Collection).
	 * Rather than returning the number of rows deleted it seems to return 1 on success??
	 */
	public boolean deleteAll(int projectID) {
		int rows = 0;
		List<Find> allFinds = getFindsByProjectId(projectID);
		int size = allFinds.size();
		try {
			Iterator iterator = allFinds.iterator();
			while (iterator.hasNext()) {
				rows += getFindDao().delete((Find)iterator.next());
			}
			if (rows == size)
				Log.i(TAG, "Deleted all finds:  " + rows);
			else {
				Log.e(TAG, "Db Error deleting all finds " + rows);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rows == size;
	}

	/**
	 * Updates the status of the sync--will eventually use this to restart
	 * interrupted syncs?
	 * 
	 * @param find
	 * @param status
	 *            Constants.TRANSACTING, Constants.SUCCEEDED, or
	 *            Constants.FAILED
	 * @return number of rows updated, 1 if successful
	 */
	public int updateStatus(Find find, int status) {
		int rows = 0;
		try {
			find.setStatus(status);
			rows = getFindDao().update(find);
			if (rows == 1) {
				Log.i(TAG, "Updated find status:  " + this.toString());
			} else {
				Log.e(TAG, "Db Error updating find: " + this.toString());
				rows = 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rows;
	}

	/**
	 * Updates the sync operation in progress for this find during the last
	 * sync.
	 * 
	 * @param find
	 * @param operation Constants.POSTING, Constants.UPDATING, or Constants.DELETING
	 * @return number of rows updated, 1 if successful
	 */

	public int updateSyncOperation(Find find, int operation) {
		int rows = 0;
		try {
			find.setSyncOperation(operation);
			rows = getFindDao().update(find);
			if (rows == 1) {
				Log.i(TAG, "Updated find sync operation:  " + this.toString());
			} else {
				Log.e(TAG, "Db Error updating sync operation in find: " + this.toString());
				rows = 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return rows;
	}
	
	/**
     * Returns a list of all Finds that have been created,
     * updated or deleted since the last sync with the server.  Each
     * change to a Find is recorded in find_history.
     * 
     * Policy: We don't send the server those finds that were created
     *  and then deleted since the last sync.  Finds are not removed
     *  from the Db, but just marked for deletion.
     *  
     * TODO: Fix this, doesn't seem to get finds from the specific project id. Just
	 * returns all finds changed since the last sync.
	 * 
	 * @param projectId
	 *            the id of the project
	 * @return a list of Finds
	 */

	public List<Find> getChangedFinds(int projectId) {
		List<Find> finds = null;
		try {

			Date lastSync = getTimeOfLastSync();

			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
			String lastSyncString = formatter.format(lastSync);
						
			// Query taken from old POSIT DbHelper.java.. not sure it works
			// properly.
			// I don't think it's properly getting the finds from your current
			// project.
			// It seems to return all changed finds from all the projects.
			
			// This query gets all changes except deletions.
			GenericRawResults<String[]> raw = getFindHistoryDao().queryRaw(
					"SELECT DISTINCT findhistory" + "." + FindHistory.FIND + ",findhistory" + "."
							+ FindHistory.FIND_ACTION + " FROM findhistory, find" + " WHERE find." + Find.PROJECT_ID
							+ " = " + projectId + " AND " + "findhistory" + "." + FindHistory.FIND_ACTION + "!= '"
							+ FindHistory.ACTION_DELETE + "' AND findhistory." + FindHistory.TIME + " > '"
							+ lastSyncString + "'");

			List<String[]> results = raw.getResults();
			Log.i(TAG, "# change results = " + results.size());
			finds = new ArrayList<Find>();

			for (String[] result : results) {
				int findId = Integer.parseInt(result[0]);
				Find find = getFindDao().queryForId(findId);
				finds.add(find);
			}

			Log.i(TAG, "Changed finds: " + finds);

		} catch (SQLException e) {
			Log.e(TAG, "Db error getting finds changed since last sync: " + e.getMessage());
		}
		return finds;
	}

	/**
	 * Gets the date of the last sync.
	 * 
	 * @return the date
	 */
	public Date getTimeOfLastSync() {
		Date lastSync = null;
		try {
			QueryBuilder<SyncHistory, Integer> builder = getSyncHistoryDao().queryBuilder();
			builder.orderBy(SyncHistory.TIME, false);
			PreparedQuery<SyncHistory> query = builder.prepare();
			SyncHistory syncHistory = getSyncHistoryDao().queryForFirst(query);
			if (syncHistory == null) // Never synced before
				lastSync = new Date(0); // Jan. 1st 1970 lol TODO: Better way to
										// do this?
			else
				lastSync = syncHistory.getTime();
		} catch (SQLException e) {
			Log.e(TAG, "Db error getting time of last sync: " + e.getMessage());
		}
		return lastSync;
	}

	/**
	 * Used to track syncs whenever they occur.
	 * 
	 * @param findHistory
	 * @return the number of rows changed if successful
	 */
	public int recordSync(SyncHistory syncHistory) {
		int rows = 0;
		try {
			rows = getSyncHistoryDao().create(syncHistory);
		} catch (SQLException e) {
			Log.e(TAG, "Db error recording a sync in sync_history: " + e.getMessage());
		}
		return rows;
	}

	/**
	 * Used to record a change in the find history table whenever a find is
	 * updated, deleted, or created.
	 * 
	 * @param findHistory
	 * @return the number of rows changed if successful
	 */
	private int recordChangedFind(FindHistory findHistory) {
		int rows = 0;
		try {
			rows = getFindHistoryDao().create(findHistory);
		} catch (SQLException e) {
			Log.e(TAG, "Db error inserting a find history: " + e.getMessage());
		}
		return rows;
	}

	/**
	 * Close the database connections and clear any cached DAOs.
	 */
	@Override
	public void close() {
		super.close();
		userDao = null;
		findDao = null;
	}

}
