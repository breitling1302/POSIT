package org.hfoss.posit.android.sync;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.hfoss.posit.android.Constants;
import org.hfoss.posit.android.R;
import org.hfoss.posit.android.api.Find;
import org.hfoss.posit.android.api.FindHistory;
import org.hfoss.posit.android.api.SyncHistory;
import org.hfoss.posit.android.api.database.DbHelper;
import org.hfoss.posit.android.functionplugin.camera.Camera;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class SyncServer extends SyncMedium{
	public final int CONNECTION_TIMEOUT = 3000;
	public final int SOCKET_TIMEOUT = 5000;
	
	public final String RESULT_FAIL = "false";
	
	private final String SERVER_PREF = "serverKey";
	private final String PROJECT_PREF = "projectKey";
	private static final String TAG = "SyncServer";
	private static final String COLUMN_IMEI = "imei";
	
	//Additional columns for photo table
	private static final String COLUMN_GUID = "guid"; 				//guid of the find
	private static final String COLUMN_IDENTIFIER = "identifier"; 	//does not seem to be useful
	private static final String COLUMN_PROJECT_ID = "project_id"; 	//project id of the find
	private static final String COLUMN_MIME_TYPE = "mime_type"; 	//data type, in this case, "image/jpeg"
	private static final String COLUMN_DATA_FULL = "data_full"; 	//data for the image, takes Base64 string of image
	private static final String COLUMN_DATA_THUMBNAIL = "data_thumbnail"; //data for the image, take Base 64 string of image
	
	private String m_server;
	private String m_imei;
	
	public SyncServer(Context context){
		m_context = context;
		initSettings();
	}
	
	private void initSettings(){
		initPreferences();
		initTelephony();
		initAuthKey();
	}
	
	private void initPreferences(){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(m_context);
		m_server = prefs.getString(SERVER_PREF, "");
		m_projectId = prefs.getInt(PROJECT_PREF, 0);
	}
	
	private void initTelephony(){
		TelephonyManager telephonyManager = (TelephonyManager) m_context.getSystemService(Context.TELEPHONY_SERVICE);
		m_imei = telephonyManager.getDeviceId();
	}
	
	private void initAuthKey(){
		m_authKey = Communicator.getAuthKey( m_context );
	}
	
	public List<HashMap<String,Object>> getProjects(){
		ArrayList<HashMap<String, Object>> list = null;

		String url = m_server + "/api/listMyProjects?authKey=" + m_authKey;

		String responseString = Communicator.doHTTPGET(url);
		Log.i(TAG, responseString);

		if (!responseString.contains("Error")) {
			try {
				list = (ArrayList<HashMap<String, Object>>) (new ResponseParser(responseString).parseList());
			} catch (JSONException e) {
				Log.i(TAG, "getProjects JSON exception " + e.getMessage());
				list = null;
			}
		}
		
		return list;
	}
	
	public List<String> getProjectStrings(List<HashMap<String,Object>> projects) {
		Iterator<HashMap<String, Object>> it 	 = projects.iterator();
		ArrayList<String> projList 				 = new ArrayList<String>();
		
		while( it.hasNext() ) {
			HashMap<String,Object> next = it.next();
			projList.add((String)(next.get("name")));
		}
		return projList;
	}
	
	public boolean setProject( HashMap<String,Object> newProject ){
		String projectId 		= (String) newProject.get("id");
		String projectName 		= (String) newProject.get("name");
		String projectPref  	= m_context.getString(R.string.projectPref);
		String projectNamePref 	= m_context.getString(R.string.projectNamePref);
		int id  				= Integer.parseInt(projectId);
		boolean success 		= true;

		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(m_context);
		int currentProjectId = sp.getInt(projectPref,0);
		
		if (id == currentProjectId){
			success = false;
		}
		else {
			Editor editor = sp.edit();

			editor.putInt(projectPref, id);
			editor.putString(projectNamePref, projectName);
			editor.commit();
		}
		
		return success;
	}
	
	public List<String> getFindsNeedingSync(){
		String serverFindsIds = getServerFindsNeedingSync();
		List<String> finds = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(serverFindsIds, ",");
		
		while(st.hasMoreElements()){
			finds.add(st.nextElement().toString());
		}
		
		return finds;
	}
	
	private String getServerFindsNeedingSync() {
		String response = "";
		
		String url = m_server + "/api/getDeltaFindsIds?authKey=" + m_authKey + "&imei=" + m_imei + "&projectId=" + m_projectId;
		Log.i(TAG, "getDeltaFindsIds URL=" + url);

		try {
			response = Communicator.doHTTPGET(url);
		} catch (Exception e) {
			Log.i(TAG, e.getMessage());
		}
		Log.i(TAG, "serverFindsNeedingSync = " + response);

		return response;
	}
	
	public String retrieveRawFind( String guid ){
		String url = m_server + "/api/getFind?guid=" + guid + "&authKey=" + m_authKey;
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		pairs.add(new BasicNameValuePair("guid", guid));
		pairs.add(new BasicNameValuePair("imei", m_imei));

		String responseString = Communicator.doHTTPPost(url, pairs);

		Log.i(TAG, "getRemoteFindById = " + responseString);
		
		return responseString;
	}

	public boolean sendFind( Find find ){
		boolean success = false;
		String url = createActionBasedUrl( find );
		List<NameValuePair> pairs = getNameValuePairs(find);
		
		BasicNameValuePair pair = new BasicNameValuePair("imei", m_imei);
		pairs.add(pair);
		
		success = transmitFind( find, url, pairs );
		
		if( success ){
			Log.i(TAG, "transmitFind synced find id: " + find.getId());
			DbHelper.getDbManager(m_context).updateStatus(find, Constants.SUCCEEDED);
		}
		else{
			Log.i(TAG, "transmitFind failed to sync find id: " + find.getId());
			DbHelper.getDbManager(m_context).updateStatus(find, Constants.FAILED);
		}
		
		transmitImage( find );
		
		DbHelper.releaseDbManager();
		return success;
	}
	
	private String createActionBasedUrl( Find find ){
		String url = "";
		String action = find.getAction();
		
		if( action.equals( FindHistory.ACTION_CREATE ) ){
			url = m_server + "/api/createFind?authKey=" + m_authKey;
		}
		else if( action.equals( FindHistory.ACTION_UPDATE ) ){
			url = m_server + "/api/updateFind?authKey=" + m_authKey;
		}
		else{
			Log.e(TAG, "Find object does not contain an appropriate action: " + find);
		}
		
		return url;
	}
	
	private List<NameValuePair> getNameValuePairs(Find find) {
		List<NameValuePair> pairs = null;
		if (find.getClass().getName().equals(Find.class.getName())) {
			pairs = getNameValuePairs(find, find.getClass());
		}
		else {
			String extendedDataPairs = getNameValuePairs(find, find.getClass()).toString();
			pairs = getNameValuePairs(find, find.getClass().getSuperclass());
			pairs.add(new BasicNameValuePair("data", extendedDataPairs));
		}
		return pairs;
	}
	
	/**
	 * Returns a list on name/value pairs for the Find.  Should work for Plugin Finds as
	 * well as Basic Finds.  
	 * @param find
	 * @param clazz
	 * @return
	 */
	private List<NameValuePair> getNameValuePairs(Find find, Class clazz) {
		Field[] fields = clazz.getDeclaredFields();

		List<NameValuePair> nvp = new ArrayList<NameValuePair>();
		String methodName = "";
		String value = "";

		for (Field field : fields) {
//			Log.i(TAG, "class= " + clazz + " field = " + field);
			if (!Modifier.isFinal(field.getModifiers())) {
				String key = field.getName();
				methodName = "get" + key.substring(0, 1).toUpperCase() + key.substring(1);
				value = "";

				try {
					Class returnType = clazz.getDeclaredMethod(methodName, null).getReturnType();
					if (returnType.equals(String.class))
						value = (String) clazz.getDeclaredMethod(methodName, null).invoke(find, (Object[]) null);
					else if (returnType.equals(int.class))
						value = String.valueOf((Integer) clazz.getDeclaredMethod(methodName, null).invoke(find,
								(Object[]) null));
					else if (returnType.equals(double.class))
						value = String.valueOf((Double) clazz.getDeclaredMethod(methodName, null).invoke(find,
								(Object[]) null));
					else if (returnType.equals(boolean.class))
						value = String.valueOf((Boolean) clazz.getDeclaredMethod(methodName, null).invoke(find,
								(Object[]) null));

				} catch (IllegalArgumentException e) {
					Log.e(TAG, e + ": " + e.getMessage());
				} catch (SecurityException e) {
					Log.e(TAG, e + ": " + e.getMessage());
				} catch (IllegalAccessException e) {
					Log.e(TAG, e + ": " + e.getMessage());
				} catch (InvocationTargetException e) {
					Log.e(TAG, e + ": " + e.getMessage());
				} catch (NoSuchMethodException e) {
					Log.e(TAG, e + ": " + e.getMessage());
				}
				nvp.add(new BasicNameValuePair(key, value));
			}
		}
		return nvp;
	}
	
	private boolean transmitFind( Find find, String url, List<NameValuePair> pairs ){
		boolean success = true;
		try {
			String responseString = Communicator.doHTTPPost(url, pairs);
			success = responseString.indexOf("True") != -1;
			DbHelper.getDbManager(m_context).updateStatus(find, Constants.TRANSACTING);
			DbHelper.getDbManager(m_context).updateSyncOperation(find, Constants.POSTING);
		} catch (Exception e) {
			Log.i(TAG, e.getMessage());
			Toast.makeText(m_context, e.getMessage(), Toast.LENGTH_LONG).show();
			DbHelper.getDbManager(m_context).updateStatus(find, Constants.FAILED);
			success = false;
		}
		
		return success;
	}
	
	private void transmitImage( Find find ){
		if(Camera.isPhotoSynced(find, m_context) == false){
			HashMap<String, String> sendMap = createImageMap( find );
			Communicator.sendMedia(sendMap, m_context);
	 	}
	}
	
	private HashMap<String, String> createImageMap( Find find ){
		HashMap<String, String> sendMap = new HashMap<String, String>();
		
		sendMap.put(COLUMN_IMEI, m_imei);
		sendMap.put(COLUMN_GUID, find.getGuid());
		sendMap.put(COLUMN_IDENTIFIER,Integer.toString(find.getId()));
		sendMap.put(COLUMN_PROJECT_ID,Integer.toString(find.getProject_id()));
		sendMap.put(COLUMN_MIME_TYPE, "image/jpeg");
		
		String fullPicStr = Camera.getPhotoAsString(find.getGuid(), m_context);
		String thumbPicStr = Camera.getPhotoThumbAsString(find.getGuid(), m_context);
		
		sendMap.put(COLUMN_DATA_FULL, fullPicStr);
		sendMap.put(COLUMN_DATA_THUMBNAIL, thumbPicStr);
		
		return sendMap;
	}
	
	public boolean postSendTasks(){
		boolean success = true;
		
		success &= recordSyncOnServer();
		success &= recordSyncOnDevice();
		
		return success;
	}
	
	private boolean recordSyncOnServer() {
		String url = m_server + "/api/recordSync?authKey=" + m_authKey + "&imei=" + m_imei + "&projectId=" + m_projectId;
		Log.i(TAG, "recordSync URL=" + url);
		String responseString = "";

		try {
			responseString = Communicator.doHTTPGET(url);
		} catch (Exception e) {
			Log.i(TAG, e.getMessage());
			e.printStackTrace();
			return false;
		}
		Log.i(TAG, "HTTPGet recordSync response = " + responseString);
		return true;
	}
	
	private boolean recordSyncOnDevice(){
		int success = 0;
		success = DbHelper.getDbManager(m_context).recordSync(new SyncHistory("idkwhatthisissupposedtobe"));
		return success != 0;
	}
	
	public Find convertRawToFind( String rawFind ){
		Find newFind = new Find();
		ContentValues cv = getCvFromRaw( rawFind );
		newFind.updateObject(cv);
		
		retrieveImage( rawFind );
		
		return newFind;
	}
	
	private ContentValues getCvFromRaw( String rawFind ){
		ContentValues cv = new ContentValues();

		Log.i(TAG, "getRemoteFindById = " + rawFind);
		try {
			JSONObject jobj = new JSONObject(rawFind);
			String findJson = jobj.getString("find");
			JSONObject find = new JSONObject(findJson);
			
			fillCvWithBasicData( cv, find );
			fillCvWithExtendedData( cv, jobj );
			
			return cv;
		} catch (JSONException e) {
			Log.i(TAG, "JSONException " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			Log.i(TAG, "Exception " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
	
	private void fillCvWithBasicData( ContentValues cv, JSONObject find ){
		try{
			cv.put(Find.GUID, 			find.getString(Find.GUID)		);
			cv.put(Find.PROJECT_ID, 	find.getInt(Find.PROJECT_ID)	);
			cv.put(Find.NAME, 			find.getString(Find.NAME)		);			
			cv.put(Find.DESCRIPTION, 	find.getString(Find.DESCRIPTION));
			cv.put(Find.TIME, 			find.getString("add_time")		);
			cv.put(Find.TIME, 			find.getString("modify_time")	);
			cv.put(Find.LATITUDE, 		find.getDouble(Find.LATITUDE)	);
			cv.put(Find.LONGITUDE, 		find.getDouble(Find.LONGITUDE)	);
			cv.put(Find.REVISION, 		find.getInt(Find.REVISION)		);
		} catch(JSONException e) {
			Log.i(TAG, "JSONException " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void fillCvWithExtendedData( ContentValues cv, JSONObject jobj ){
		try{
			if (jobj.has(Find.EXTENSION)) {
				String extradata = jobj.getString(Find.EXTENSION);		
				Log.i(TAG, "extradata = " + extradata);
				if ( !extradata.equals("null") )
					addExtraDataToContentValues(cv, extradata);
			}
		} catch(JSONException e) {
			Log.i(TAG, "JSONException " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * The data has the form: [attr=value, ...] or 'null'
	 * @param cv
	 * @param data
	 */
	private void addExtraDataToContentValues(ContentValues cv, String data) {
		Log.i(TAG, "data = " + data  + " " + data.length());
		if (data.equals("null")) 
			return;
		data = data.trim();
		data = data.substring(1,data.length()-1);
		StringTokenizer st = new StringTokenizer(data,",");
		while (st.hasMoreElements()) {
			String attrvalpair = (String) st.nextElement();
			String attr = attrvalpair.substring(0,attrvalpair.indexOf("="));
			attr = attr.trim();
			String val = attrvalpair.substring(attrvalpair.indexOf("=")+1);
			val = val.trim();
			Log.i(TAG, "Putting " + attr + "=" + val + " into CV");
			if (Integer.getInteger(val) != null)
				cv.put(attr, Integer.parseInt(val));
			else
				cv.put(attr, val);
		}
	}
	
	private void retrieveImage( String rawFind ){
		try{
			JSONObject jobj = new JSONObject(rawFind);
			if (jobj.has("images")) {
				String imageIds = jobj.getString("images");		
				Log.i(TAG, "imageIds = " + imageIds);
				
				String imageId = parseImageIds(imageIds); 
				
				if(imageId != null){
					if(getImageOnServer(imageId)){
						Log.i(TAG, "Successfully retrieved image.");
					}
					else{
						Log.i(TAG, "Failed to retrieve image.");
					}
				}
			}
		} catch(JSONException e ) {
			Log.i(TAG, "JSONException " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			Log.i(TAG, "Exception " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * The data has the form: ["1","2", ...] or '[]'
	 * @param data
	 * the list of image ids
	 * @return the last image id in the list or null
	 */
	private String parseImageIds(String data) {
		Log.i(TAG, "imageIdData = " + data  + " " + data.length());
		if (data.equals("[]")){ 
			return null;
		}
		data = cleanImageData( data );
		StringTokenizer st = new StringTokenizer(data,","); //in the form "123"
		String imgId = null; 								//only care about one image for this version of posit
		while (st.hasMoreElements()) {
			imgId = (String) st.nextElement();
			Log.i(TAG, "Is this with quotes: " + imgId);
			imgId = imgId.substring(1, imgId.indexOf('"',1)); // removes quotes. find the second quote in the string
			Log.i(TAG, "Is this without quotes: " + imgId);
		}
		Log.i(TAG, "Planning to fetch imageId " + imgId + " for a find");
		return imgId;
	}
	
	private String cleanImageData(String data){
		String cleaned = data.trim();
		cleaned = cleaned.substring(1, cleaned.length()-1); //removing brackets
		return cleaned;
	}
	
	 /**
	 * Retrieve the specified image id from the server and save it to the phone
	 * @param imageId
	 * the id of the image to query
	 * @param context
	 * the application context
	 * @return true if successful, false otherwise
	 */
	private boolean getImageOnServer(String imageId) throws FileNotFoundException, IOException {
		//TODO: Communicator.getAuthKey(m_context) might be returning m_authKey
		//		There are other cases where this happens to, they could be cleaned up
		String imageUrl = m_server + "/api/getPicture?id=" + imageId + "&authKey=" + Communicator.getAuthKey(m_context);
		HashMap<String, String> sendMap = createSendMap();
		String imageResponseString 		= Communicator.doHTTPPost(imageUrl, sendMap);

		return saveImageData( imageResponseString );
	 }
	
	private HashMap<String, String> createSendMap(){
		//TODO: If Communicator.getIMEI is just returning m_imei, then
		//		use this code in createImageMap
		HashMap<String, String> sendMap = new HashMap<String, String>();
		sendMap.put(COLUMN_IMEI, Communicator.getIMEI(m_context));
		return sendMap;
	}
	
	private boolean saveImageData( String imageResponseString ){
		boolean success = true;
		
		if (imageResponseString.equals(RESULT_FAIL)){
			success = false;
		}
		else{
			Log.i(TAG, "imageResponseString = " + imageResponseString);

			try {
				JSONObject jobj = new JSONObject(imageResponseString);
				String guid = jobj.getString(Find.GUID);
				String imgData = jobj.getString("data_full");
				
				Camera.savePhoto(guid, imgData, m_context);
			} catch (JSONException e) {
				Log.i(TAG, "Unable to save image data.");
				e.printStackTrace();
				success = false;
			}
		}
		
		return success;
	}
	
}
