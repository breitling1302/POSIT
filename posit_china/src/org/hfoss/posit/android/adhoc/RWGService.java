package org.hfoss.posit.android.adhoc;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.hfoss.posit.android.Find;
import org.hfoss.posit.android.ListFindsActivity;
import org.hfoss.posit.android.PositMain;
import org.hfoss.posit.android.R;
import org.hfoss.posit.android.utilities.Utils;
import org.hfoss.third.CoreTask;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

public class RWGService extends Service implements RWGConstants {

	private static final String TAG = "RWGService";
	private static PositMain ACTIVITY = null;
	private static Process procRWG = null;

	private CoreTask coretask = null;

	private Context mContext;

	private ProgressDialog mProgressDialog;
	private Thread netStartUpThread;

	private Thread netHandleIncomingThread;
	private boolean stopThread;

	private static BufferedReader br;
	private static BufferedWriter bw;
	private char[] buff;
	private String incoming = "";

	NotificationManager mNotificationManager;

	public static int newFindsNum = 0;

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
	}

	public static void setActivity(PositMain activity) {
		ACTIVITY = activity;
	}

	public static boolean isRunning() {
		int procId = findProcessId("rwgexec");

		return (procId != -1);

	}

	public static int findProcessId(String command) {
		int procId = -1;

		Runtime r = Runtime.getRuntime();

		Process procPs = null;

		try {

			procPs = r.exec(SHELL_CMD_PS);

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					procPs.getInputStream()));
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.indexOf(command) != -1) {
					StringTokenizer st = new StringTokenizer(line, " ");
					st.nextToken(); // proc owner
					procId = Integer.parseInt(st.nextToken().trim());
					break;
				}
			}

		} catch (Exception e) {
			Log.e(TAG, "error: " + e.getMessage(), e);
		}

		return procId;
	}

	public Process doCommand(String command, String arg1) {
		Runtime r = Runtime.getRuntime();
		Process child = null;
		try {
			child = r.exec(command + ' ' + arg1);
		} catch (Exception e) {
			Log.e(TAG, "error: " + e.getMessage(), e);
		}
		return child;
	}

	private void runRWG() throws IOException {
		// Get the user's group size setting
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(this);
		int groupSize = Integer.parseInt(sp.getString("ADHOC_GROUPSIZE", "3"));

		Log.i("Adhoc", "Set groupSize to " + groupSize);

		Log.i("RWGService", "runRWG()");
		DataOutputStream os = null;

		/*
		 * We need to run rwgexec as root, so we get a su shell. The pipes need
		 * to be in the current working directory when rwgexec is started, so we
		 * cd to /data/rwg first.
		 */

		try {
			killRWGProcess();
			procRWG = Runtime.getRuntime().exec("su");
			os = new DataOutputStream(procRWG.getOutputStream());
			os.writeBytes("chmod 777 files" + "\n");
			os.writeBytes("cd " + POSIT_HOME + "files" + "\n");
			os.writeBytes("chmod 777 rwgexec" + "\n");
			os.writeBytes("./rwgexec -t -g " + groupSize
					+ " -h 99 -l 3600 -i tiwlan0 > trace.txt" + "\n");
			os.close();
			Log.i("RWGService", "tried to open rwgexec");
		} catch (Exception e) {
			Log.d("RWG", "Unexpected error - Here is what I know: "
					+ e.getMessage());
		}
	}

	public void initRWG() {
		try {
			boolean binaryExists = new File(RWG_BINARY_INSTALL_PATH).exists();

			if (!binaryExists) {
				RWGBinaryInstaller installer = new RWGBinaryInstaller();
				installer.start(false);

				binaryExists = new File(RWG_BINARY_INSTALL_PATH).exists();
				if (binaryExists) {
					Utils.showToast(ACTIVITY, "RWG binary installed!");
					Log.i(TAG, "Binary installed!");
				} else {
					Utils.showToast(ACTIVITY, "RWG binary install FAILED!");
					Log.i(TAG, "Binary install failed.");
					return;
				}
			} else
				Log.i(TAG, "Binary already exists");

			Log.i(TAG, "Setting permission on RWG binary");
			doCommand(SHELL_CMD_CHMOD, CHMOD_EXE_VALUE + ' '
					+ RWG_BINARY_INSTALL_PATH);
			killRWGProcess();

			// doCommand(SHELL_CMD_RM,RWG_LOG_PATH);

			Log.i(TAG, "Starting RWG process");

			runRWG();
		} catch (Exception e) {

			Log.w(TAG, "unable to start RWG Process", e);

			e.printStackTrace();
		}
	}

	private void killRWGProcess() {

		if (procRWG != null) {
			Log.i(TAG, "shutting down RWG process...");

			procRWG.destroy();

			try {
				procRWG.waitFor();
			} catch (Exception e) {
				e.printStackTrace();
			}

			int exitStatus = procRWG.exitValue();
			Log.i(TAG, "Tor exit: " + exitStatus);

			procRWG = null;
		}

		int procId = findProcessId(RWG_BINARY_INSTALL_PATH);

		if (procId != -1) {
			Log.i(TAG, "Found RWG PID=" + procId + " - killing now...");

			doCommand(SHELL_CMD_KILLALL, procId + "");
		}

	}

	public int killProcessRunning(String processName) throws Exception {
		int pid = -1;
		Process process = null;
		process = Runtime.getRuntime().exec("ps");
		BufferedReader in = new BufferedReader(new InputStreamReader(process
				.getInputStream()));
		String line = null;
		while ((line = in.readLine()) != null) {
			if (line.contains(processName)) {
				Log.i("LINE", line);
				line = line.substring(line.indexOf(' '));
				line = line.trim();
				pid = Integer.parseInt(line.substring(0, line.indexOf(' ')));
				Runtime.getRuntime().exec("kill -9 " + pid);
			}
		}
		in.close();
		process.waitFor();
		return pid;
	}

	@Override
	public void onDestroy() {
		try {
			stopThread = true;
			killProcessRunning("./rwgexec");
		} catch (Exception e) {
			Log.e("endRWG()", e.toString(), e);
		}
		procRWG.destroy();
	}

	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
	}

	public void disableWifi() {
		boolean done = false;
		while (!done && !Thread.currentThread().isInterrupted()) {
			PositMain.wifiManager.setWifiEnabled(false);
			// Waiting for interface-shutdown
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// nothing
			}
			if (PositMain.wifiManager.isWifiEnabled())
				done = false;
			else
				done = true;
		}
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		Log.i(TAG, "starting RWG");
		// initRWG();
		long start = System.currentTimeMillis();
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Log.i(TAG, "Creating AdhocClient");
		mContext = ACTIVITY;
		mProgressDialog = ProgressDialog.show(mContext, "Please wait",
				"Enabling Ad-hoc mode with RWG", true, true);
		coretask = new CoreTask();
		coretask.setPath(mContext.getApplicationContext().getFilesDir()
				.getParent());
		Log.i("path", coretask.DATA_FILE_PATH);
		this.checkDirs();
		boolean filesetOutdated = coretask.filesetOutdated();
		if (binariesExists() == false || filesetOutdated) {
			if (coretask.hasRootPermission()) {
				Log.i(TAG, "Installing Binaries");
				installBinaries();
			} else {
				this.openNotRootDialog();
			}
		}

		Log.i(TAG, "Starting network thread...");
		this.netStartUpThread = new Thread(new NetworkStart());
		this.netStartUpThread.start();

		Log.i(TAG, "Starting HandleIncoming thread...");
		this.netHandleIncomingThread = new Thread(new HandleIncoming(),
				"Listening Thread");
		stopThread = false;
		this.netHandleIncomingThread.start();
		Log.i(TAG, "Done starting network services!");
	}

	private void checkDirs() {
		File dir = new File(this.coretask.DATA_FILE_PATH);
		if (dir.exists() == false) {
			Utils.showToast(mContext, "Application data-dir does not exist!");
		} else {
			dir = new File(this.coretask.DATA_FILE_PATH + "/bin");
			if (dir.exists() == false) {
				if (!dir.mkdir()) {
					Utils.showToast(mContext, "Couldn't create bin-directory!");
				}
			}
			dir = new File(this.coretask.DATA_FILE_PATH + "/var");
			if (dir.exists() == false) {
				if (!dir.mkdir()) {
					Utils.showToast(mContext, "Couldn't create var-directory!");
				}
			}
			dir = new File(this.coretask.DATA_FILE_PATH + "/conf");
			if (dir.exists() == false) {
				if (!dir.mkdir()) {
					Utils
							.showToast(mContext,
									"Couldn't create conf-directory!");
				}
			}
		}
	}

	public boolean binariesExists() {
		File file = new File(this.coretask.DATA_FILE_PATH + "/bin/tether");
		if (file.exists()) {
			return true;
		}
		return false;
	}

	public void installBinaries() {
		List<String> filenames = new ArrayList<String>();
		// tether
		this.copyBinary(this.coretask.DATA_FILE_PATH + "/bin/netcontrol",
				R.raw.netcontrol);
		filenames.add("netcontrol");
		// dnsmasq
		this.copyBinary(this.coretask.DATA_FILE_PATH + "/bin/dnsmasq",
				R.raw.dnsmasq);
		try {
			this.coretask.chmodBin(filenames);
		} catch (Exception e) {
			Utils.showToast(mContext,
					"Unable to change permission on binary files!");
		}
		// dnsmasq.conf
		this.copyBinary(this.coretask.DATA_FILE_PATH + "/conf/dnsmasq.conf",
				R.raw.dnsmasq_conf);
		// tiwlan.ini
		this.copyBinary(this.coretask.DATA_FILE_PATH + "/conf/tiwlan.ini",
				R.raw.tiwlan_ini);
		Utils.showToast(mContext, "Binaries and config-files installed!");
	}

	private void copyBinary(String filename, int resource) {
		File outFile = new File(filename);
		InputStream is = mContext.getResources().openRawResource(resource);
		byte buf[] = new byte[1024];
		int len;
		try {
			OutputStream out = new FileOutputStream(outFile);
			while ((len = is.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			out.close();
			is.close();
		} catch (IOException e) {
			Utils.showToast(mContext, "Couldn't install file - " + filename
					+ "!");
		}
	}

	private void openNotRootDialog() {
		LayoutInflater li = LayoutInflater.from(mContext);
		View view = li.inflate(R.layout.norootview, null);
		new AlertDialog.Builder(mContext).setTitle("Not Root!").setView(view)
				.setNegativeButton("Close",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
							}
						}).setNeutralButton("Override",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								installBinaries();
							}
						}).show();
	}

	public void enableAdhocClient() {
		if (this.coretask.runRootCommand(this.coretask.DATA_FILE_PATH
				+ "/bin/netcontrol start_client "
				+ this.coretask.DATA_FILE_PATH)) {
		}
	}

	public void disableAdhocClient() {
		if (this.coretask
				.runRootCommand(this.coretask.DATA_FILE_PATH
						+ "/bin/netcontrol stop_client "
						+ this.coretask.DATA_FILE_PATH)) {
		}
	}

	public void enableWifi() {

		Log.i(TAG, "Enabling wifi...");

		boolean done = false;
		while (!done && !Thread.currentThread().isInterrupted()) {
			WifiManager wm;

			wm = PositMain.wifiManager;
			wm.setWifiEnabled(true);
			
			// Waiting for interface-shutdown
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// nothing
			}
			if (wm.isWifiEnabled())
				done = true;
			else
				done = false;
		}
		Log.i(TAG, "Enabled Wifi");

	}

	class NetworkStart implements Runnable {
		// @Override
		public void run() {
			Log.i(TAG, "Starting NetworkStart");
			enableWifi();

			disableAdhocClient();
			Log.i(TAG, "Enabling AdhocClient");
			enableAdhocClient();
			Log.i(TAG, "Enabled AdhocClient");

			Log.i(TAG, "Starting rwgexec");
			Looper.prepare();
			if (!RWGService.isRunning()) {

				initRWG();
			}
			try {
				Thread.sleep(2000);
			} catch (Exception e) {
				System.out.println(e.getClass().toString());
			}
			Log.i(TAG, "AFTER SERVICE IS STARTED");


			/*
			 * Communication is done with 2 pipes, input and output. These must
			 * be in the current working directory when rwgexec is started.
			 * 
			 * The pipes are named from the point of view of rwg. Thus, the
			 * sense of the pipes is switched from the point of view of our app
			 * using them. This is why we read from the output pipe and write to
			 * the input pipe.
			 */
			// open Output Pipe

			while (!RWGService.isRunning())
				;

			Log.i("ADHOC_CLIENT", "It's running!");
			try {
				Log.i("ADHOCCLIENT", "DOING INPUT PIPE");
				FileOutputStream fos = new FileOutputStream(
						"/data/data/org.hfoss.posit.android/files/input");
				Log.i("ADHOCCLIENT", "1");
				BufferedOutputStream bos = new BufferedOutputStream(fos);
				Log.i("ADHOCCLIENT", "2");
				PrintWriter pw = new PrintWriter(bos);
				Log.i("ADHOCCLIENT", "3");
				bw = new BufferedWriter(pw);
				Log.i("ADHOCCLIENT", "DID INPUT PIPE");
			} catch (Exception e2) {
				Log.i("ADHOCCLIENT", e2.toString(), e2);
				System.out.println(e2.getClass().toString());
			}

			try {
				FileInputStream fis = new FileInputStream(
						"/data/data/org.hfoss.posit.android/files/output");
				InputStreamReader isr = new InputStreamReader(fis, "ASCII");
				br = new BufferedReader(isr);
				Log.i("ADHOCCLIENT", "DID OUTPUT PIPE");
			} catch (Exception e1) {
				Log.i("ADHOCCLIENT", e1.toString(), e1);
			}

			Log.i("ADHOCCLIENT", "GETS TO NETWORK CONFIG DONE");
			Log.i("ADHOCCLIENT", "AFTER NET CONFIG DONE");

			try {
				Thread.sleep(2000);
			} catch (Exception e) {
				System.out.println(e.getClass().toString());
			}

			mProgressDialog.dismiss();
			notifyRWGOn();
			return;
		}
	}

	class HandleIncoming implements Runnable {
		// @Override
		public void run() {
			Log.i(TAG, "Starting HandleIncoming");

			boolean pipeOpen = false;
			buff = new char[1];

			Log.i("THREAD", Thread.currentThread().getName());

			while (!pipeOpen && !stopThread) {
				try {
					if (br.ready()) {
						Log.i(TAG, "Pipe is ready");

						pipeOpen = true;
						break;
					}
				} catch (Exception e1) {

				}
				try {
					Thread.sleep(1000);
				} catch (Exception e2) {
				}
			}

			Looper.prepare();
			while (!stopThread) { //

				// Log.i("Listening...","GREAT");
				try {

					if (br.ready()) {
						incoming = "";

						while (br.read(buff, 0, 1) > 0) {
							if (buff[0] == '\r')
								break;
							incoming = incoming + Character.toString(buff[0]);
						}

						parseAndSave(incoming);
					}

					Thread.sleep(1000);

				} catch (Exception e) {
					System.out.println(e.getClass().toString());
				}
			}
			return;
		}
	}

	String bigFind = "";
	boolean isBig = false;

	private void parseAndSave(String incoming) {
		Log.i(TAG, "Parsing string:" + incoming);
		if (incoming.charAt(0) == '<') {
			int index = incoming.indexOf("{");
			if (index != -1)
				incoming = incoming.substring(index);
			Log.i(TAG, "Parsing string:" + incoming);
		}

		try {
			JSONObject obj = new JSONObject(incoming);
			ContentValues content = new ContentValues();
			String longStr = obj.getString("findLong");

			// HACK: One dev phone sends null Long and lat
			if (!longStr.equals("")) {
				double longitude = Double
						.parseDouble(obj.getString("findLong"));
				content
						.put(mContext.getString(R.string.longitudeDB),
								longitude);
				double latitude = Double.parseDouble(obj.getString("findLat"));
				content.put(mContext.getString(R.string.latitudeDB), latitude);
			} else {
				content.put(mContext.getString(R.string.latitudeDB), 0);
				content.put(mContext.getString(R.string.longitudeDB), 0);
			}
			long findId = obj.getLong("findId");
			content.put(mContext.getString(R.string.idDB), findId);
			int projectId = obj.getInt("projectId");
			String name = obj.getString("name");
			content.put(mContext.getString(R.string.nameDB), name);
			String description = obj.getString("description");
			content
					.put(mContext.getString(R.string.descriptionDB),
							description);
			content.put(mContext.getString(R.string.projectId), projectId);
			content.put(mContext.getString(R.string.adhocDB), 1);

			content.put("guid", findId);

			Find find = new Find(mContext);
			find.insertToDB(content, null);

			notifyNewFind(name, description);
		} catch (JSONException e) {
			Log.e("JSONError", e.toString());
		} catch (NumberFormatException e) {
			Log.e(TAG, e.toString());
		}
	}

	public void notifyRWGOn() {

		CharSequence tickerText = "Ad-hoc Mode On"; // ticker-text
		long when = System.currentTimeMillis(); // notification time
		Context context = getApplicationContext(); // application Context
		CharSequence contentTitle = "Ad-hoc mode"; // expanded message title

		Intent notificationIntent = new Intent(this, ListFindsActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);

		// the next two lines initialize the Notification, using the
		// configurations above
		Notification notification = new Notification(R.drawable.ic_menu_share,
				tickerText, when);
		notification.setLatestEventInfo(context, contentTitle,
				"RWG is running", contentIntent);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.flags |= Notification.FLAG_NO_CLEAR;
		mNotificationManager.notify(Utils.ADHOC_ON_ID, notification);
	}

	public void notifyNewFind(String name, String description) {
		newFindsNum++;

		CharSequence tickerText = "New RWG Find"; // ticker-text
		long when = System.currentTimeMillis(); // notification time
		Context context = getApplicationContext(); // application Context
		CharSequence contentTitle = "New RWG Find"; // expanded message title
		CharSequence contentText = "";
		if (newFindsNum == 1) {
			contentText = "Name: " + name + " | Description: " + description; // expanded
																				// message
																				// text
		} else {
			contentText = newFindsNum + " unviewed RWG Finds";
		}
		Intent notificationIntent = new Intent(this, ListFindsActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);

		// the next two lines initialize the Notification, using the
		// configurations above
		Notification notification = new Notification(R.drawable.icon,
				tickerText, when);
		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);
		notification.defaults |= Notification.DEFAULT_SOUND;
		notification.defaults |= Notification.DEFAULT_VIBRATE;
		notification.defaults |= Notification.DEFAULT_LIGHTS;
		notification.ledARGB = 0xff0000ff;
		notification.ledOnMS = 300;
		notification.ledOffMS = 1000;
		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		mNotificationManager.notify(Utils.NOTIFICATION_ID, notification);
	}

	public static void send(String sendMessage) {
		try {
			Log.i(TAG, "sending message:" + sendMessage);
			File f = new File("/files/results.txt");
			if (f.createNewFile() || f.exists()) {
				FileWriter results = new FileWriter(f, true);
				results.append("Sent (" + Utils.getTimestamp() + "): "
						+ sendMessage + "\n");
				results.flush();
				results.close();
			}

			bw.write(sendMessage);
			bw.flush();
		} catch (Exception e) {
			Log.e(TAG, e.getClass().toString());
		}

	}

	@Override
	public boolean onUnbind(Intent intent) {
		// TODO Auto-generated method stub
		return super.onUnbind(intent);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
