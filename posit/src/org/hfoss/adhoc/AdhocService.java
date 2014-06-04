/*
 * File: AdhocService.java
 * 
 * Copyright (C) 2010 The Humanitarian FOSS Project (http://www.hfoss.org)
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
package org.hfoss.adhoc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

import org.hfoss.posit.android.ListFindsActivity;
import org.hfoss.posit.android.PositMain;
import org.hfoss.posit.android.R;
import org.hfoss.posit.android.utilities.Utils;
import org.hfoss.posit.rwg.RwgManager;
import org.hfoss.posit.rwg.RwgReceiver;
import org.hfoss.posit.rwg.RwgSender;
import org.hfoss.third.CoreTask;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Adhoc service implements the RWG protocol running in either INFRASTRUCTURE_MODE
 * in which it relies on a shared hotspot (with default SSID = 'hfoss') or true
 * ADHOC_MODE in which it communicates peer-to-peer in true adhoc mode with other
 * phones on the (default) 192.168.2 subnet.  ADHOC_MODE requires a rooted phone. 
 * Currently phones in ADHOC_MODE cannot communicate with phones in INFRASCTRUCTURE_MODE.
 * 
 * Currently both INFRASTRUCTURE_MODE and ADHOC_MODE have been tested on G1 phones. 
 * INFRASTRUCTURE_MODE has also been tested on Nexus One. 
 * 
 * INFRASTRUCTURE_MODE (non-rooted phones):
 * 
 * In this mode the phones share a common WiFi SSID. The current default is "hfoss" running 
 * on a Nexus One phone in hotspot mode. The phones must be connected to the SSID through
 * the phone's WiFi. The program will try to do this, but it may take a couple of tries. It
 * works best if the phone is put on Wifi before starting POSIT adhoc mode.  Phones connected
 * to the common SSID communicate by broadcasting Datagram packets to the 255.255.255.255 
 * broadcast address on port 8888.  (See UdpSender and UdpReceiver)
 * 
 * 
 * ADHOC_MODE (rooted phones only):
 * 
 * The ADHOC_MODE code is based on code 
 * adapted from the Android Wifi Tether project (http://code.google.com/p/android-wifi-tether/) 
 * and on code adapted from adhoc-on-android project (http://code.google.com/p/adhoc-on-android/)
 * and on the work of D. Gurecki (http://www4.ncsu.edu/~dwgureck/csc714/).
 * 
 * Setup/Configuration: The configuration file for setting up the tiwlan0 interface (G1 phones)
 * is located in ./res/raw/tiwlan_ini. This file is copied into the phone's data directory as:
 *   /data/data/org.hfoss.posit.android/conf/tiwlan.ini  
 *   
 * The default tiwlan.ini configuration for G1 has WiFiAdhoc set to 0:  WiFiAdhoc = 0
 * Our tiwlan.ini replaces this line with:
 *    WiFiAdhoc = 1
 *    dot11DesiredChannel = 6
 *    dot11DesiredSSID = G1Tether
 *    dot11DesiredBSSType = 0
 *  
 * WiFi mode: Root-level commands are used to put the phone in adhoc mode by configuring the
 * WiFi interface (tiwlan0) using tiwlan.ini
 * 
 *     insmod wlan.ko
 *     wlan_loader -f /system/etc/wifi/Fw1251r1c.bin -e /proc/calibration -i /data/data/org.hfoss.posit.android/conf/tiwlan.ini
 *     ifconfig tiwlan0 " + MY_IP + " netmask 255.255.255.0
 *     ifconfig tiwlan0 up
 *     
 * The phone's IP (MY_IP) is constructed from 192.168.2 + ID where ID is a hash of either the 
 * phone's MAC Address or its IMEI. The IMEI is used when the MAC cannot be acquired from the 
 * Wifi Manager.
 * 
 * Sending Datagram Packets:  Datagram packets are broadcast to the subnet broadcast address 192.168.2.255
 * and port number 8888 is used as the receiving port.  (See UdpSender and UdpReceiver)
 * 
 * RWG Protocol: The RWG protocol is implemented as a separate layer that sits on top of the UDP layer.
 * started by AdhocService assuming the initial configuration goes well.
 * 
 * @author rmorelli
 *
 */
public class AdhocService extends Service {
	protected static final String TAG = "Adhoc";

	public static final String MAC_ADDRESS = "Mac Address";
	public static final int MAX_PACKET_SIZE = 2048; // 1 K
	public static final String IP_SUBNET = "192.168.2.";
	public static final int DEFAULT_PORT_BCAST = 8888;
	public static final int ADHOC_NOTIFICATION = 1;
	public static final int NEWFIND_NOTIFICATION = 2;
	public static final String MODE = "Mode";
	public static final int MODE_ADHOC = 1;           // Rooted phones only
	public static final int MODE_INFRASTRUCTURE = 2;  // Uses hotspot
	private static final int START_STICKY = 1;

	public static AdhocService adhocInstance = null;
	private static PositMain mActivity = null;

	private static NotificationManager mNotificationManager;

	private Notification mNotification;

	private String mAppDataPath;  // Where our configuration files are

	private boolean mInAdhocMode = false;
	private static int mAdhocMode;
	private WifiManager mWifi;
	private WifiInfo mWifiInfo;

	private static String mMacAddress = "";
	private static short myHash;
	private RwgReceiver mRwgReceiver;
	private RwgSender mRwgSender;
	private RwgManager mRwgManager;
	private CoreTask mCoretask;
	private ProgressDialog mProgressDialog;
	
	private int mGroupSize;
	private String mSSID;


	@Override
	public void onCreate() {
		super.onCreate();

		if (mActivity != null) {
			mProgressDialog = ProgressDialog.show(mActivity, "Please wait", "Enabling Ad-hoc mode with RWG", true,true);
		}

		// Get Preferences
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		mGroupSize = Integer.parseInt(sp.getString("Group Size", "3"));
		mSSID = sp.getString("SSID", "hfoss");
		Log.i(TAG, "Preferences SSID= " + mSSID + " Group Size = " + mGroupSize );

		// Create notification manager.
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}

	public static void setActivity(PositMain activity) {
		mActivity = activity;
	}

	public static int getAdhocMode() {
		return mAdhocMode;
	}

	/**
	 * Copies the configuration a file to the phone.
	 * @param filename
	 * @param resource
	 */
	private void copyBinary(String filename, int resource) {
		File outFile = new File(filename);
		InputStream is = this.getResources().openRawResource(resource);
		byte buf[]=new byte[1024];
		int len;
		try {
			OutputStream out = new FileOutputStream(outFile);
			while((len = is.read(buf))>0) {
				out.write(buf,0,len);
			}
			out.close();
			is.close();
		} catch (IOException e) {
			Log.e(TAG, "Couldn't install file - " + filename + "!");
			Utils.showToast(this,"Couldn't install file - "+filename+"!");
		}
	}

	/**
	 * Disables phone's Wifi if it is connected.  Needed to put the
	 * WiFi into adhoc mode. 
	 */
	public void disableWifi() {
		if (mWifi.isWifiEnabled()) {
			mWifi.setWifiEnabled(false);
			try {
				do {
					Thread.sleep(3000);
				} while(mWifi.isWifiEnabled());	 
			} catch (InterruptedException e) {
				// nothing
			}
		}
		Log.d(TAG, "Wifi should now be disabled!");    	
	}

	// This is the old onStart method that will be called on the pre-2.0
	// platform.  On 2.0 or later we override onStartCommand() so this
	// method will not be called.
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		Log.i(TAG, "AdhocService,  Starting, id " + startId);
		handleStartUp(intent);
	}

	/**
	 * Replaces onStart(Intent, int) in pre 2.2 versions.
	 * @param intent
	 * @param flags
	 * @param startId
	 * @return
	 */
	public int onStartCommand(Intent intent, int flags, int startId) {

		Log.i(TAG, "AdhocService,  Starting, id " + startId);
		handleStartUp(intent);

		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	/**
	 * Handles the start up of the service for both 2.2 and pre-2.2 versions. 
	 * The service can be run in either INFRASTRUCTURE_MODE, in which it
	 * relies on a hotspot in the vicinity. Or, for ROOTED phones, in ADHOC_MODE,
	 * in which it communicates peer-to-peer in true adhoc fashion.  In either case
	 * the phone needs a unique ID.  If it is connected to WiFi, the phone's MAC
	 * address can be used.  Otherwise we use its IMEI.  This is mostly useful
	 * for ADHOC_MODE where the phones share a subnet of 192.168.2.NN where NN is
	 * the hash of the phone's Unique ID. 
	 * @param intent  The intent received from PositMain
	 */
	private void handleStartUp(Intent intent) {
		mAdhocMode = intent.getIntExtra(MODE, MODE_INFRASTRUCTURE);

		// Get WiFi status
		mWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		mWifiInfo = mWifi.getConnectionInfo();
		mMacAddress = mWifiInfo.getMacAddress();

		// If we can't get the phone's unique MAC address, use its IMEI as a unique ID
		if (mMacAddress == null) {
			TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			mMacAddress = tm.getDeviceId();
		}
		// The hash of its MAC or IMEI is used as the IP address with 192.168.2.myHash
		myHash = (short) RwgManager.rwgHash(mMacAddress);
		Log.i(TAG, "MAC Address = " + mMacAddress + " myHash = " + myHash);

		boolean success = false;
		try {
			if (mAdhocMode == MODE_INFRASTRUCTURE) {
				success = initializeInfrastructureMode(mMacAddress, mSSID);
			} else {
				success = initializeAdhocMode();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Start RWG if the WiFi was set up correctly
		if (success) {
			success = startRwg();
			if (!success) {
				stopSelf();
			} else {
				notifyAdhocOn();
				mInAdhocMode = true;
			}
		} else {
			Log.e(TAG, "Adhoc service aborting");
		}
		mProgressDialog.dismiss();
	}


	/**
	 * Starts the RWG Protocol. RWG (Random Walk Gossip) is a routing protocol designed for 
	 * disaster scenarios and other situations where the phone cannot rely on mobile 
	 * infrastructure. It is designed to be used on partially or intermittently connected 
	 * networks. It supports communication in situations where phones have no information
	 * about the network. 
	 * 
	 * @see M. Asplund, T. de Lanerolle, C. Fei, P. Gautam, R. Morelli, S. Nadjm-Tehrani, 
	 * and G. Nykvist, Wireless Ad Hoc Dissemination for Search and Rescue, 
	 * in 7th International Conference on Information Systems for Crisis Response 
	 * and Management, ISCRAM, May 2010. (http://www.ida.liu.se/~rtslab/publications/publications.shtml)
	 * 
	 * @see M. Asplund and S. Nadjm-Tehrani, A Partition-tolerant Manycast Algorithm for 
	 * Disaster Area Networks, in 28th International Symposium on Reliable Distributed Systems, 
	 * IEEE, Sept. 2009. (http://www.ida.liu.se/~rtslab/publications/publications.shtml)
	 * 
	 * @see M. Asplund and S. Nadjm-Tehrani, Random walk gossip-based manycast with partition 
	 * detection (fast abstract), in Proceedings of the 2008 International Conference on 
	 * Dependable Systems and Networks (DSN'08), IEEE Computer Society, June 2008.
	 * (http://www.ida.liu.se/~rtslab/publications/publications.shtml)
	 * @return
	 */
	private boolean startRwg() {
		boolean success = true;

		try {
			mRwgManager = new RwgManager(mMacAddress, mGroupSize);
			mRwgReceiver = new RwgReceiver(this, mRwgManager);
			mRwgSender = new RwgSender(mMacAddress, mRwgManager);
		} catch (BindException e) {
			success = false;
			Log.e(TAG, "Bind exception");
			e.printStackTrace();
		} catch (SocketException e) {
			success = false;
			Log.e(TAG, "Socket exception");
			e.printStackTrace();
		} catch (UnknownHostException e) {
			success = false;
			Log.e(TAG, "UnknownHost exception");
			e.printStackTrace();
		}
		if (success)  {
			mRwgManager.startThread();
			mRwgReceiver.startThread();
			mRwgSender.startThread();
			adhocInstance = this;			
		}
		return success;
	}

	/**
	 * Tries to put the phone in infrastructure mode with SSID = ssid
	 * @param macAddr  this phone's MacAddress
	 * @param ssid the network's ssid
	 * @return true iff the phone successfully connects to SSID network
	 * @throws Exception
	 */
	private boolean initializeInfrastructureMode(String macAddr, String ssid) throws Exception {
		WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		int sleep = 0;

		// Make sure WiFi is enabled.
		Log.i(TAG, "Enabling WiFi");
		while (!wifi.isWifiEnabled() && sleep < 10000) {  // Wait for no more than 10 secondes
			wifi.setWifiEnabled(true);
			sleep += 1000;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} 
		}
		if (!wifi.isWifiEnabled()) {
			Log.e(TAG, "ERROR: Unable to initialize Wifi. Aborting");
			Utils.showToast(this, "Wifi problem: Unable to initialize Wifi ");
			return false;
		}
		Log.i(TAG, "WifiEnabled = " + wifi.isWifiEnabled());

		WifiInfo info = wifi.getConnectionInfo();
		Log.i(TAG, "Wifi info " + info);
		String currentSSID = info.getSSID();
		Log.d(TAG, "Current SSID=" + currentSSID + " Requested SSID= " + ssid);
		if (currentSSID == null || !currentSSID.equals("\"" + ssid + "\"")) { //SSID contains quotes

			List<WifiConfiguration> configs = wifi.getConfiguredNetworks();
			int myId = -1;
			for (int k = 0; k < configs.size(); k++) {
				WifiConfiguration config = configs.get(k);
				Log.d(TAG, "Searching for SSID= " + ssid + " found SSID= " + config.SSID); 
				if (configs.get(k).SSID.equals("\"" + ssid + "\"")){  //SSID contains quote marks! 
					myId = configs.get(k).networkId;
					break;
				}
			}
			if (myId == -1) {  // Create a new configuration
				Log.i(TAG, "ERROR: network " + ssid + " not found");
				Utils.showToast(this, "Wifi problem: network " + ssid + " not found");
				return false;

				//throw new Exception("ERROR: network " + ssid + " not found");
			}

			Log.i(TAG, "Trying to enable network with SSID = " + ssid + " netId = " + myId);
			boolean ok = false;
			ok = wifi.enableNetwork(myId, true);
			if (!ok) {
				Log.i(TAG, "Cannot enable  "  + ssid);
			} 
			info = wifi.getConnectionInfo();
			Log.i(TAG, "Wifi info " + info);

			sleep = 0;

			while ( !info.getSupplicantState().equals(SupplicantState.COMPLETED)
					&& !info.getSupplicantState().equals(SupplicantState.ASSOCIATED)
					&& !info.getSupplicantState().equals(SupplicantState.ASSOCIATING)
					&& sleep <= 10000) {
				Log.d(TAG, "Waiting for network connection, supplicant state = " 
						+ info.getSupplicantState().toString());
				Thread.sleep(1000);
				sleep += 1000;
			}
			
			if (sleep >= 10000) {
				Log.i(TAG, "Timed out trying to connect with SSID = " + ssid);
				Utils.showToast(this, "Wifi problem: Timed out trying to connect to " + ssid);
				return false;
			} else {
				Log.i(TAG, "Enabled hfoss network, SSID = " + wifi.getConnectionInfo().getSSID());
			}
		}
		Log.i(TAG, "Wifi info " + info);
		return true;
	}

	/**
	 * Performs all the methods required to start the phone in ADHOC_MODE. 
	 * This mode requires root access.  If the phone has root access, the
	 * configuration file (tiwlan.ini) is loaded from resources (if not already
	 * loaded) and then a series of root-level commands are run that switch the
	 * phone's default tiwlan0 interface to one that runs in adhoc mode. 
	 * @return
	 */
	private boolean initializeAdhocMode() {
		Log.i(TAG, "Initializing Adhoc mode");

		// Setup Coretask 
		mCoretask = new CoreTask();
		mCoretask.setPath(this.getApplicationContext().getFilesDir().getParent());
		mAppDataPath = getApplicationContext().getFilesDir().getParent();
		Log.d(TAG, "Current directory is "+ mAppDataPath);

		if (mCoretask.hasRootPermission()) {
			Log.i(TAG, "This phone IS ROOTED.");

			// Check for configuration file and load it from resources if necessary
			File file = new File(mAppDataPath + "/conf/tiwlan.ini");
			if (!file.exists()) {
				Log.i(TAG, "Copying tiwlan.ini from resources.");
				copyBinary(mAppDataPath + "/conf/tiwlan.ini", R.raw.tiwlan_ini);
			}
		}
		else {
			Utils.showToast(this,"Sorry. This phone is NOT ROOTED. Try infrasctructure mode.");
			Log.i(TAG, "This phone IS NOT ROOTED");
			return false;
		}

		Log.i(TAG, "Disabling wifi");
		disableWifi();

		Log.i(TAG, "Starting WiFi in Adhoc mode");
		boolean ok = mCoretask.runRootCommand("insmod /system/lib/modules/wlan.ko");

		Log.i(TAG, "Loaded wlan kernel module, insmod wlan.ko ok = " + ok);
		ok = mCoretask.runRootCommand("wlan_loader -f /system/etc/wifi/Fw1251r1c.bin -e /proc/calibration -i /data/data/org.hfoss.posit.android/conf/tiwlan.ini");
		Log.i(TAG, "Configured wlan kernel module, wlan_loader... ok = " + ok);

		String myIp = IP_SUBNET  + myHash;
		Log.i(TAG, "Configuring the network interface with IP = " + myIp);
		ok = mCoretask.runRootCommand("ifconfig tiwlan0 " + myIp + " netmask 255.255.255.0");
		Log.i(TAG, "Configured network interface, ifconfig ... ok = " + ok);

		Log.i(TAG, "Starting Wifi");
		ok = mCoretask.runRootCommand("ifconfig tiwlan0 up");
		Log.i(TAG, "Started Wifi ... ok = " + ok);

		return ok;  
	}

	/**
	 * Disables Wifi. This is necessary to run in ad-hoc mode. This is called in initAdhocMode()
	 * and also from onDestroy().
	 * @return
	 */
	private boolean stopWifi() {
		boolean ok = mCoretask.runRootCommand("ifconfig tiwlan0 down");
		if (ok) {
			Log.i(TAG, "Shut down adhoc network");
		} else {
			Log.e(TAG, "Unable to shut down adhoc network");
		}
		ok = mCoretask.runRootCommand("wlan_loader -f /system/etc/wifi/Fw1251r1c.bin -e /proc/calibration -i /system/etc/wifi/tiwlan.ini");
		if (ok) {
			Log.i(TAG, "Reconfigured kernel module wlan");
		} else {
			Log.e(TAG, "Unable to reconfigure kernel module wlan");
		}
		ok = mCoretask.runRootCommand("rmmod wlan");
		if (ok) {
			Log.i(TAG, "Removed kernel module , rmmod wlan");
		} else {
			Log.i(TAG, "Unable to remove kernel module , rmmod wlan");
		}
		return ok;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// If a ROOTED phone and in MODE_ADHOC mode
		if (mAdhocMode == MODE_ADHOC) {
			stopWifi();
		}
		if (mInAdhocMode) {
			adhocInstance = null;
			mRwgManager.stopThread();
			mRwgReceiver.stopThread();
			mRwgSender.stopThread();
			notifyAdhocOff();
		}
		mInAdhocMode = false;
		Log.d(TAG,"Destroyed Adhoc service");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/**
	 * Posts an 'Adhoc On' message and an icon in the status bar.
	 */
	public void notifyAdhocOn() {
		CharSequence tickerText = "Ad-hoc Mode On";              // ticker-text
		long when = System.currentTimeMillis();         // notification time
		Context context = getApplicationContext();      // application Context
		CharSequence contentTitle = "Ad-hoc mode";  // expanded message title

		Intent notificationIntent = new Intent(this, ListFindsActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		// The next two lines initialize the Notification, using the configurations above
		mNotification = new Notification(R.drawable.ic_menu_share, tickerText, when);
		mNotification.setLatestEventInfo(context, contentTitle, "Adhoc is running", contentIntent);
		mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
		mNotification.flags |= Notification.FLAG_NO_CLEAR;
		mNotificationManager.notify(ADHOC_NOTIFICATION, mNotification);
	}

	/**
	 * Removes the 'Adhoc On' message and an icon from the status bar.
	 */
	public static void notifyAdhocOff() {
		mNotificationManager.cancel(AdhocService.ADHOC_NOTIFICATION);
		mNotificationManager.cancel(AdhocService.NEWFIND_NOTIFICATION);
	}

	/**
	 * Returns the phone's MAC addresss.
	 * @param cxt
	 * @return
	 */
	public static String getMacAddress(Context cxt) {
		return mMacAddress;
	}


	public static boolean isRunning() {
		return adhocInstance  != null;
	}

}
