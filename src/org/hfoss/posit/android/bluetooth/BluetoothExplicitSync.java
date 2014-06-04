package org.hfoss.posit.android.bluetooth;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.hfoss.posit.android.Find;
import org.hfoss.posit.android.Log;
import org.hfoss.posit.android.R;
import org.hfoss.posit.android.provider.PositDbHelper;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the main Activity that displays the finds list to sync over Bluetooth
 */
public class BluetoothExplicitSync extends ListActivity {
	// Debugging
	private static final String TAG = "BluetoothExplicitSync";
	private static final boolean D = true;

	// Message types sent from the BluetoothExplicitSyncService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Key names received from the BluetoothExplicitSyncService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;

	// Layout Views
	private TextView mTitle;

	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the explicit sync services
	private BluetoothExplicitSyncService mExplicitSyncService = null;

	private SelectFindListAdapter mSFLAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (D)
			Log.e(TAG, "+++ ON CREATE +++");

		// Set up the window layout
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.bluetooth_main);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.custom_title);

		// Set up the custom title
		mTitle = (TextView) findViewById(R.id.title_left_text);
		mTitle.setText(R.string.app_name);
		mTitle = (TextView) findViewById(R.id.title_right_text);

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}

	}

	@Override
	public void onStart() {
		super.onStart();
		if (D)
			Log.e(TAG, "++ ON START ++");

		// If BT is not on, request that it be enabled.
		// setupSync() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the sync list
		} else {
			if (mExplicitSyncService == null)
				setupSync();
		}
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		if (D)
			Log.e(TAG, "+ ON RESUME +");

		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity
		// returns.
		if (mExplicitSyncService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't
			// started already
			if (mExplicitSyncService.getState() == BluetoothExplicitSyncService.STATE_NONE) {
				// Start the Bluetooth services
				mExplicitSyncService.start();
			}
		}
	}

	/**
	 * Setup the adapter, view, and service
	 */
	private void setupSync() {
		Log.d(TAG, "setupSync()");

		mSFLAdapter = new SelectFindListAdapter(this);

		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(this);
		int project_id = sp.getInt("PROJECT_ID", 0);

		PositDbHelper dbHelper = new PositDbHelper(this);
		Cursor c = dbHelper.fetchFindsByProjectId(project_id);

		c.moveToFirst();

		while (!c.isAfterLast()) {
			mSFLAdapter.addItem(
				new SelectFind(
					c.getString(c.getColumnIndexOrThrow(PositDbHelper.FINDS_GUID)),
					false,
					c.getLong(c.getColumnIndexOrThrow(PositDbHelper.FINDS_ID)),
					c.getString(c.getColumnIndexOrThrow(PositDbHelper.FINDS_NAME)),
					c.getString(c.getColumnIndexOrThrow(PositDbHelper.FINDS_DESCRIPTION))));
		
			c.moveToNext();
		}

		c.close();
		dbHelper.close();

		setListAdapter(mSFLAdapter);

		// Initialise the BluetoothExplictSyncService to perform Bluetooth
		// connections
		mExplicitSyncService = new BluetoothExplicitSyncService(this, mHandler);
	}

	/**
	 * Toggle the checkbox of the item clicked
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Log.i(TAG, "List item clicked: " + position);
		mSFLAdapter.toggleState(position);
	}

	/**
	 * Select all the items in the list
	 * 
	 * @param view
	 */
	public void selectAll(View view) {
		mSFLAdapter.selectAll();
	}

	/**
	 * Deselect all the items in the list
	 * 
	 * @param view
	 */
	public void selectNone(View view) {
		mSFLAdapter.deselectAll();
	}

	/**
	 * Start the explicit sync over bluetooth
	 * 
	 * @param view
	 */
	public void sendSelected(View view) {
		// Check that we're actually connected before trying anything
		if (mExplicitSyncService.getState() != BluetoothExplicitSyncService.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
					.show();
			return;
		}
		
		String[] guids = mSFLAdapter.getSelectedGuids();

		if (guids.length > 0) {
			Toast.makeText(getApplicationContext(),
					"Syncing with connected device.", Toast.LENGTH_SHORT)
					.show();

			for (String guid : guids) {
				sendFind(new BluetoothFindTO(this, guid));
			}
		} // We have no work to be done if none are selected
	}

	@Override
	public synchronized void onPause() {
		super.onPause();
		if (D)
			Log.e(TAG, "- ON PAUSE -");
	}

	@Override
	public void onStop() {
		super.onStop();
		if (D)
			Log.e(TAG, "-- ON STOP --");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Stop the Bluetooth services
		if (mExplicitSyncService != null)
			mExplicitSyncService.stop();
		if (D)
			Log.e(TAG, "--- ON DESTROY ---");
	}

	// Ensure that we can be detected by other Bluetooth devices during a scan
	private void ensureDiscoverable() {
		if (D)
			Log.d(TAG, "ensure discoverable");
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	// Pack each find into a transfer object and pass the bytes to the service
	// to send over Bluetooth
	private void sendFind(BluetoothFindTO findTO) {
		

		// Check that there's actually something to send
		if (findTO != null) {
			// Pack into a message and tell the BluetoothExplicitSyncService to
			// write
			byte[] message = null;
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			try {
				ObjectOutputStream oos = new ObjectOutputStream(bos);
				oos.writeObject(findTO);
				oos.flush();
				oos.close();
				bos.close();
				message = bos.toByteArray();
			} catch (IOException ex) {
				Log.e(TAG, "Exception during explicit sync");
			}
			mExplicitSyncService.write(message);
		}
	}
	
	private ContentValues unpackFindInfo(BluetoothFindTO findTO){
		SharedPreferences sp = PreferenceManager
		.getDefaultSharedPreferences(this);
		int project_id = sp.getInt("PROJECT_ID", 0);
		
		// Set the information to be inserted into the db
		ContentValues cv = new ContentValues();
		cv.put(PositDbHelper.FINDS_ID, findTO.getId());
		cv.put(PositDbHelper.FINDS_GUID, findTO.getGuid());
		// TODO: adds to the current project, change to make more sense
		cv.put(PositDbHelper.FINDS_PROJECT_ID, project_id);
		cv.put(PositDbHelper.FINDS_NAME, findTO.getName());
		cv.put(PositDbHelper.FINDS_DESCRIPTION, findTO.getDescription());
		cv.put(PositDbHelper.FINDS_LATITUDE, findTO.getLatitude());
		cv.put(PositDbHelper.FINDS_LONGITUDE, findTO.getLongitude());
		cv.put(PositDbHelper.FINDS_SYNCED, findTO.getSyncedState());
		// TODO: find revision?
		
		return cv;
	}
	
	private boolean receiveFind(BluetoothFindTO findTO) {
		PositDbHelper dbh = new PositDbHelper(this);
		boolean success = false;
		// TODO: send images over bluetooth
		List<ContentValues> photosList = null;
		String guid = findTO.getGuid();

		ContentValues cv = unpackFindInfo(findTO);

		// Update the DB
		if (dbh.containsFind(guid)) {
			success = dbh.updateFind(guid, cv, photosList);
			Log.i(TAG, "Updating existing find");
		} else {
			Find newFind = new Find(this, guid);
			success = newFind.insertToDB(cv, photosList);
			Log.i(TAG, "Adding a new find");
		}
		if (!success) {
			Log.i(TAG, "Error recording sync stamp");
		} else {
			Log.i(TAG, "Recorded timestamp stamp");
		}
		dbh.close();
		
		return success;
	}

	// The Handler that gets information back from the
	// BluetoothExplicitSyncService
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				if (D)
					Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothExplicitSyncService.STATE_CONNECTED:
					mTitle.setText(R.string.title_connected_to);
					mTitle.append(mConnectedDeviceName);
					break;
				case BluetoothExplicitSyncService.STATE_CONNECTING:
					mTitle.setText(R.string.title_connecting);
					break;
				case BluetoothExplicitSyncService.STATE_LISTEN:
				case BluetoothExplicitSyncService.STATE_NONE:
					mTitle.setText(R.string.title_not_connected);
					break;
				}
				break;
			case MESSAGE_WRITE:
				// TODO: sending a message
				break;
			case MESSAGE_READ:
				byte[] newFindBytes = (byte[]) msg.obj;
				BluetoothFindTO newFindTO = null;
				try {
					ByteArrayInputStream bis = new ByteArrayInputStream(
							newFindBytes);
					ObjectInputStream ois = new ObjectInputStream(bis);
					newFindTO = (BluetoothFindTO) ois.readObject();
				} catch (IOException ex) {
					Log.e(TAG, "IO exception on receiving message.");
				} catch (ClassNotFoundException ex) {
					Log.e(TAG, "Class not found on receiving message.");
				}

				if (receiveFind(newFindTO)) {
					Toast.makeText(getApplicationContext(), R.string.bt_successful_recv, Toast.LENGTH_SHORT)
					.show();
				} else {
					Toast.makeText(getApplicationContext(), R.string.bt_unsuccessful_recv, Toast.LENGTH_SHORT)
					.show();
				}

				break;
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				String address = data.getExtras().getString(
						BluetoothDeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// Get the BluetoothDevice object
				BluetoothDevice device = mBluetoothAdapter
						.getRemoteDevice(address);
				// Attempt to connect to the device
				mExplicitSyncService.connect(device);
			}
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up the sync list
				setupSync();
			} else {
				// User did not enable Bluetooth or an error occured
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.bluetooth_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.scan:
			// Launch the DeviceListActivity to see devices and do scan
			Intent serverIntent = new Intent(this,
					BluetoothDeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			return true;
		case R.id.discoverable:
			// Ensure this device is discoverable by others
			ensureDiscoverable();
			return true;
		}
		return false;
	}

}