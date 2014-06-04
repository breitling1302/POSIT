package org.hfoss.posit.rwg;

import java.io.IOException;
import java.net.BindException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.BitSet;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.hfoss.adhoc.AdhocData;
import org.hfoss.adhoc.AdhocFind;
import org.hfoss.adhoc.AdhocService;
import org.hfoss.adhoc.UdpReceiver;
import org.hfoss.posit.android.Find;
import org.hfoss.posit.android.ListFindsActivity;
import org.hfoss.posit.android.R;
import org.hfoss.posit.android.provider.PositDbHelper;
import org.hfoss.posit.android.utilities.Utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.util.Log;

/**
 * Class used by the lower (UDP) layer to send messages to the application layer.
 * @see http://code.google.com/p/adhoc-on-android/
 */
public class RwgReceiver implements Runnable {
	public static final String TAG = "Adhoc";
	private Queue<Message> receivedMessages;
	private RwgManager rwgManager;
	private UdpReceiver udpReceiver;
	private String mMyMacAddress;
	private int mHash;

	private Thread receiverThread;
	private Context mContext;
	private NotificationManager mNotificationManager;
	public static int newFindsNum = 0;
	private volatile boolean keepRunning = true;

	public RwgReceiver(Context context, RwgManager rwgManager) throws SocketException, UnknownHostException, BindException {
		mContext = context;
		this.rwgManager = rwgManager;
		
		mMyMacAddress = AdhocService.getMacAddress(context);
		mHash = RwgManager.rwgHash(mMyMacAddress);

		mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
		receivedMessages = new ConcurrentLinkedQueue<Message>();
		udpReceiver = new UdpReceiver(this, mHash);
	}

	public void startThread(){
		keepRunning = true;
		udpReceiver.startThread();
		receiverThread = new Thread(this);
		receiverThread.start();
	}

	/**
	 * Stops the receiver thread.
	 */
	public void stopThread() {
		keepRunning = false;
		udpReceiver.stopThread();
		receiverThread.interrupt();
	}

	public void run() {
		Looper.prepare();

		while (keepRunning) {
			try {
				synchronized (receivedMessages) {
					while (receivedMessages.isEmpty()) {
						receivedMessages.wait();
					}
				}
				Message msg = receivedMessages.poll();
				Log.d(TAG, mHash + " rwgReceiver received a packet");
				
				if (!handleMessage(msg)) {
					Log.e(TAG, mHash + " RWG: Error handling incoming message");
				}
			} catch (InterruptedException e) {
				Log.i(TAG, mHash + " RwgReceived thread interrupted");
			}
		}
	}

	/**
	 * Handles the incoming message according to the RWG Protocol. 
	 * @see rwg_receive.c rwg_handle_incoming()
	 * @param msg
	 * @return
	 */
	private boolean handleMessage(Message msg) {
		RwgPacket rwgPacket = null;
		RwgPacketBuffer packetBuff = rwgManager.getPacketBuffer();

		try {
			rwgPacket = RwgPacket.readFromBytes(msg.data);
		} catch (IOException e) {
			Log.e(TAG, mHash + " I/O error reading bytes from packet");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			Log.e(TAG, mHash + " ClassNotFound error reading bytes from packet");
			e.printStackTrace();
		}

		EthernetHeader ethrHdr = null;
		RwgHeader rwgHeader = null; 
		ethrHdr = rwgPacket.getEthernetHeader();

		if (ethrHdr.getProtocol().equals(Constants.RWG_PROTOCOL)) {

			// HUH: C code contains a check here that the packet isn't just an ethernet header with no RWG packet
			rwgHeader = rwgPacket.getRwgHeader();
			if (rwgHeader == null) {
				Log.e(TAG, mHash + " Packet does not contain an RWG Header");
				return true;

			}

			String senderMac = rwgPacket.getSourceNodeAddress();
			Log.d(TAG, mHash + " MyMac = " + mMyMacAddress + " senderMac = " + senderMac);
			if (senderMac.equalsIgnoreCase(mMyMacAddress)) {
				Log.d(TAG, mHash + " Ignoring packet -- looks like mine");
				return true;
			} else {
				Log.d(TAG, mHash + " Received a packet from sender = " + senderMac);
			}

			// What type of RWG packet was received
			int rwgType = rwgHeader.getMessageType();
			Log.i(TAG, mHash + " Handling incoming message: packetBuff= " + packetBuff);

			switch(rwgType) {
			case Constants.REQF:
				if (handleREQF(rwgHeader, rwgPacket,  packetBuff)) 
					return true;
				else
					break;
			case Constants.ACK:
				if (packetBuff.getWTail() != packetBuff.getWFront()) {
					Log.i(TAG, mHash + " handleMessage(): Type ACK has arrived");
					return handleACK(rwgHeader, packetBuff);
				} else {
					Log.e(TAG, mHash + " handleMessage(): Type ACK arrived but not waiting for one");
				}				 
				break;
			case Constants.OKTF:
				Log.i(TAG, mHash + " handleMessage(): Type OKTF has arrived");
				return handleOKTF(rwgHeader,packetBuff);
			case Constants.BS:
				Log.i(TAG, mHash + " handleMessage(): Type BS has arrived");
				return handleBS(rwgHeader,packetBuff);
			default:
				Log.e(TAG, mHash + " handleMessage(): Incorrect RWG protocol type");
			break;
			}

		} else {
			Log.i(TAG, mHash + " handleMessage(): Not an RWG packet.  Ignoring");
			return true;
		}	

		return false;
	}

	private boolean handleACK(RwgHeader rwgHeader, RwgPacketBuffer packetBuff) {
		Log.i(TAG, mHash + " handleACK() handling an ACK");
		
		// Check if the ACK target matches this nodes mac addr
		if (rwgHeader.getTarget().equals(mMyMacAddress)) {
			Log.i(TAG, mHash + " handleACK() ACK target DOES match my MACaddr = " + mMyMacAddress);
			
			// Save a copy of the ACK
			RwgHeader copyRwgHeader = rwgHeader.copy();
			packetBuff.getAck()[packetBuff.getAck_counter() % packetBuff.getAck().length] = copyRwgHeader;
			packetBuff.setAck_counter(packetBuff.getAck_counter() + 1);
			
			// Free old ACK
			if (packetBuff.getAck()[packetBuff.getAck_counter() % packetBuff.getAck().length] != null) {
				packetBuff.getAck()[packetBuff.getAck_counter() % packetBuff.getAck().length] = null;
			}
			
			return true;
		} else {
			Log.i(TAG, mHash + " handleACK() ACK target DOES NOT match my MACaddr = " + mMyMacAddress);
		}
		return false;
	}

	/**
	 * handles incoming BS (be silent) packet.
	 * @param rwgHeader
	 * @param packetBuff
	 */
	private boolean handleBS(RwgHeader rwgHeader, RwgPacketBuffer packetBuff) {
		Log.i(TAG, mHash + " Handling BS packet");
		
		String origin = rwgHeader.getOrigin();
		short seqNo = rwgHeader.getSequenceNumber();

		// If this packet already exists in REQF buffer, update the visited vectors
		int match = RwgManager.rwgMatchPacketId(origin, seqNo, packetBuff);
		if (match >= 0) {
			Log.i(TAG, mHash + " handleBS: packet matches a previous REQF");
			RwgManager.rwgBitvectorUpdate(packetBuff.getReqf()[match].getReqf().getVisited(),
					rwgHeader.getVisited());
			RwgManager.rwgBitvectorUpdate(packetBuff.getReqf()[match].getReqf().getRecentVisited(),
					rwgHeader.getRecentVisited());
			return true;
		}
		return false;
	}

	private boolean handleOKTF(RwgHeader rwgHeader, RwgPacketBuffer packetBuff) {
		Log.i(TAG, mHash + " Handling OKTF packet");

		String origin = rwgHeader.getOrigin();
		short seqNo = rwgHeader.getSequenceNumber();
		
		// Update the visted and recent visited vector if the REQF exists in the REQF buffer
	
		int match = RwgManager.rwgMatchPacketId(origin, seqNo, packetBuff);
		if (match >= 0) {
			Log.i(TAG, mHash + " handleOKTF: packet matches a previous REQF");
			RwgManager.rwgBitvectorUpdate(packetBuff.getReqf()[match].getReqf().getVisited(),
					rwgHeader.getVisited());
			RwgManager.rwgBitvectorUpdate(packetBuff.getReqf()[match].getReqf().getRecentVisited(),
					rwgHeader.getRecentVisited()); 
		} else {
			Log.i(TAG, mHash + " handleOKTF: no match in REQF");
			return false;
		}
		
		// If the OKTF target matches this nodes mac addr make its REQF active
		if (rwgHeader.getTarget().equals(mMyMacAddress)) {
			Log.i(TAG, mHash + " handleOKFT() OKTF target DOES match my MACaddr = " + mMyMacAddress);
			packetBuff.getActive_reqf().setReqf(packetBuff.getReqf()[match].getReqf());
			packetBuff.getActive_reqf().setReqf_pos(match);
		} else {
			Log.i(TAG, mHash + " handleOKTF() OKTF target DOES NOT match my MACaddr = " + mMyMacAddress);
			return false;
		}
		// Check if the visited is greater than groupSize
		if (packetBuff.getReqf()[match].getReqf().getVisited().cardinality() >=
			packetBuff.getReqf()[match].getReqf().getGroupSize()) {
			Log.i(TAG, mHash + " handleOKTF() group size reached, will not forward");
			return false;
		}

		// Set flag to forward
		Log.i(TAG, mHash + " handleOKTF scheduling SEND_REQF_F");
		RwgManager.SEND_REQF_F = true;
		return true;
	}

	/**
	 * Handles a REQF packet.
	 * @param rwgHeader
	 * @param rwgPacket
	 * @param packetBuff
	 * @return
	 */
	private boolean handleREQF(RwgHeader rwgHeader, RwgPacket rwgPacket, RwgPacketBuffer packetBuff) {
		Log.i(TAG, mHash + " Processing a REQF packet");

		String origin = rwgHeader.getOrigin();
		short seqNo = rwgHeader.getSequenceNumber();
		BitSet visited = rwgHeader.getVisited();
		BitSet rVisited = rwgHeader.getRecentVisited();

		// If this packet already exists in REQF buffer, update the visited vectors
		int match = RwgManager.rwgMatchPacketId(origin, seqNo, packetBuff);
		if (match >= 0) {
			Log.i(TAG, mHash + " handleREQF: packet already exists in REQF buffer");
			RwgManager.rwgBitvectorUpdate(packetBuff.getReqf()[match].getReqf().getVisited(),
					rwgHeader.getVisited());
			RwgManager.rwgBitvectorUpdate(packetBuff.getReqf()[match].getReqf().getRecentVisited(),
					rwgHeader.getRecentVisited());
		}

		// Checks whether this node has messages that the REQF sender does not have
		String sender = rwgHeader.getSender(); 
		rwgWake(sender, packetBuff);

		short hashedAddr = (short)RwgManager.getMyHash();
		Log.i(TAG, mHash + " handleREQF: hashedAddr = " + hashedAddr);

		// If already visited but not recent visited 
		// (a message will empty recentVisited when hops > hopLimit)
		if (RwgManager.rwgBitvectorLookup(visited, hashedAddr) && 
				!RwgManager.rwgBitvectorLookup(rVisited,hashedAddr)) {
			Log.i(TAG, mHash + " handleREQF: Is already visited but not recentVisited");

			//change the recentVisited vector and change the sender of the stored packet
			// (so the ACK will be sent to the correct node)
			if (match >= 0) {
				packetBuff.getReqf()[match].getReqf().setRecentVisited(rwgHeader.getRecentVisited());
				packetBuff.getReqf()[match].getReqf().setSender(rwgHeader.getSender());
				RwgManager.rwgSetBitvector(packetBuff.getReqf()[match].getReqf().getRecentVisited(), 
						hashedAddr);
				packetBuff.getActive_reqf().setReqf(packetBuff.getReqf()[match].getReqf());
				packetBuff.getActive_reqf().setReqf_pos(match);
				Log.i(TAG, mHash + " RwgReceiver.handleREQF : Scheduling an ACK");
				RwgManager.SEND_ACK = true;
				RwgSender.queueRwgMessage();
				return true;
			} else {
				Log.i(TAG, mHash + " handleREQF(): REQF does not exist in buffer (perhaps restarted node)");
			}
		} else if (RwgManager.rwgBitvectorLookup(visited, hashedAddr)) {  
			// If already visited
			// Find buffered REFQ with matching packetid and schedule a send BS if groupSize < visted nodes
			Log.i(TAG, mHash + " handleREQF(): Is already visited");
			if (match >= 0) {
				//check size of visited list, save packet id and schedule send bs
				if (packetBuff.getReqf()[match].getReqf().getVisited().cardinality() 
						>= packetBuff.getReqf()[match].getReqf().getGroupSize()) {
					packetBuff.getActive_reqf().setReqf(packetBuff.getReqf()[match].getReqf());
					packetBuff.getActive_reqf().setReqf_pos(match);
					Log.i(TAG, mHash + " group size reached, scheduling a BS");
					RwgManager.SEND_BS = true;
					RwgSender.queueRwgMessage();
					return true;
				} 
				return false;
			} else {
					Log.i(TAG, mHash + " handleREQF():REQF does not exist in buffer (perhaps restarted node)");
			}
		} else if (!RwgManager.rwgBitvectorLookup(rVisited, hashedAddr)) {
			// if not already recent visited
			Log.i(TAG, mHash + " handleREQF(): Not recent visited");
			if (match < 0) {
				// update recentVisited and visited
				rwgHeader.getRecentVisited().set(hashedAddr);
				rwgHeader.getVisited().set(hashedAddr);

				// Send the packet to the application layer
				AdhocData<AdhocFind> adhocData = null;
				adhocData = rwgPacket.getAdhocData(); 
				if (adhocData != null) {
					adhocFindPacketReceived(adhocData, rwgPacket.getSourceNodeAddress());
				} else {
					Log.i(TAG, mHash + "REQF has no data");
				}

				// Copy the packet and add a pointer to the copy in the packetBuffer
				RwgHeader copyRwgHeader = rwgHeader.copy();
				packetBuff.getReqf()[packetBuff.getReqf_counter()].setReqf(copyRwgHeader);
				packetBuff.getReqf()[packetBuff.getReqf_counter()].setWait(0);
				// Set the time stamp arrived at in buffer
				//packetBuff.getReqf()[packetBuff.getReqf_counter()].getArrivedAt().setSeconds(System.currentTimeMillis());
				//packetBuff.getReqf()[packetBuff.getReqf_counter()].getArrivedAt().setU_seconds(System.currentTimeMillis());
				packetBuff.getReqf()[packetBuff.getReqf_counter()].setArrivedAt(System.currentTimeMillis());
				packetBuff.getReqf()[packetBuff.getReqf_counter()].setReqf_pos(packetBuff.getReqf_counter());
				packetBuff.getActive_reqf().setReqf(packetBuff.getReqf()[packetBuff.getReqf_counter()].getReqf());
				packetBuff.getActive_reqf().setReqf_pos(packetBuff.getReqf_counter());
				packetBuff.setReqf_counter(packetBuff.getReqf_counter() + 1);
				// Check if group size has been reached
				if (rwgHeader.getVisited().cardinality() >= rwgHeader.getGroupSize()) {
					Log.i(TAG, mHash + " group size reached, scheduling a BS");
					RwgManager.SEND_BS = true;
					RwgSender.queueRwgMessage();
					return true;
				}
			} else {
				//this has to be done due to possible missed ACKs
				Log.i(TAG, mHash + " handleREQF(): REQF exists in buffer but not recentVisited/visited, updates recentVisited");
				packetBuff.getReqf()[match].getReqf().getRecentVisited().set(hashedAddr);
				packetBuff.getReqf()[match].getReqf().getVisited().set(hashedAddr);
				packetBuff.getReqf()[match].getReqf().setSender(rwgHeader.getSender());
				packetBuff.getActive_reqf().setReqf(packetBuff.getReqf()[match].getReqf());
				packetBuff.getActive_reqf().setReqf_pos(match);
				Log.i(TAG, mHash + " scheduling a BS");
				RwgManager.SEND_BS = true;
				RwgSender.queueRwgMessage();
				return true;
			}
			// Schedule an ACK
			Log.i(TAG, mHash + " RwgReceiver.handleREQF dropthru: Scheduling an ACK");
			RwgManager.SEND_ACK = true;
			RwgSender.queueRwgMessage();
			return true;
		}
		return false;
	}
	
	/**
	 * Wakes up messages that the sender of an incoming REQF hasn't received.
	 * @param sender
	 * @param packetBuff
	 */

	public void rwgWake (String sender, RwgPacketBuffer packetBuff) {
		short pos = (short) RwgManager.rwgHash(sender);
		int ctr = packetBuff.getReqf_counter() - 1;
		for (; -1 < ctr; ctr--) {
			// This is for copying pointers to the wake buffer
			if ( !RwgManager.rwgBitvectorLookup(packetBuff.getReqf()[ctr].getReqf().getVisited(), pos) 
					&& !RwgManager.rwgBitvectorLookup(packetBuff.getReqf()[ctr].getReqf().getRecentVisited(), pos)) {
				
				// Check if packet already exists in the wake/waiting buffer
				// Also check that the buffer is not full
				if (packetBuff.getReqf()[ctr].getWake() != 1 
						&& packetBuff.getWake_counter() < packetBuff.getWake().length
						&& packetBuff.getWaiting()[packetBuff.getReqf()[ctr].getWait_pos()] != packetBuff.getReqf()[ctr]) {
					Log.i(TAG, mHash + " Waking message " + packetBuff.getReqf()[ctr].toString());
					packetBuff.getWake()[packetBuff.getWake_counter()] = packetBuff.getReqf()[ctr];
					packetBuff.getWake()[packetBuff.getWake_counter()].setWake(1);
					packetBuff.getReqf()[ctr].setWake_pos(packetBuff.getWake_counter()); // Will be used when reqf buff is refreshed
					packetBuff.setWake_counter(packetBuff.getWake_counter() + 1);
				}
			}
		}
	}
	
	/**
	 * Method used by the lower network layer to queue messages for this layer
	 * 
	 * @param senderNodeAddress Is the address of the node that sent a message
	 * @param msg is an array of bytes which contains the sent data
	 */
	public void addMessage(byte[] msg) {
		receivedMessages.add(new Message(msg));
		synchronized (receivedMessages) {
			receivedMessages.notify();
		}
	}

	/**
	 * Handles an AdhocFind when received
	 * @param userData is the received packet
	 * @param senderNodeAddress the originator of the message
	 */
	private void adhocFindPacketReceived(AdhocData<AdhocFind> data, String senderMac) {
		Log.d(TAG, mHash + " Received RWG packet, data = " + data);

		// Is this my own packet?					
		//MacAddress senderMac = data.getSender();
		//String senderMac = data.getSender();
		Log.d(TAG, mHash + " MyMac = " + mMyMacAddress + " senderMac = " + senderMac);
		if (senderMac.equalsIgnoreCase(mMyMacAddress)) {
			Log.d(TAG, mHash + " Ignoring packet -- looks like mine");
		} else {
			Log.d(TAG, mHash + " Received a packet from sender = " + senderMac);

			// Now do something with the RWG packet
			ContentValues values = new ContentValues();
			
			AdhocFind adhocFind = (AdhocFind)data.getMessage();
			values.put(PositDbHelper.FINDS_NAME, adhocFind.getName());
			values.put(PositDbHelper.FINDS_DESCRIPTION, adhocFind.getDescription());
			values.put(PositDbHelper.FINDS_LONGITUDE, adhocFind.getLongitude());
			values.put(PositDbHelper.FINDS_LATITUDE, adhocFind.getLatitude());
			values.put(PositDbHelper.FINDS_GUID, adhocFind.getId());
			values.put(PositDbHelper.FINDS_PROJECT_ID, adhocFind.getProjectId());
			values.put(PositDbHelper.FINDS_SYNCED, PositDbHelper.FIND_NOT_SYNCED);
			values.put(PositDbHelper.FINDS_IS_ADHOC, 1);

			Find find = new Find(mContext);
			if (find.exists(adhocFind.getId())) {
				Log.i(TAG, mHash + " Find already exists");
				find.updateToDB(adhocFind.getId(), values);
				Utils.showToast(mContext, "Updating existing adhoc find");
			} else {
				find.insertToDB(values, null);
				Utils.showToast(mContext, "Saving new adhoc find");
			}
			notifyNewFind(adhocFind.getName(), adhocFind.getDescription());
			Log.d(TAG, mHash + " Inserted find into POSIT Db");
		}
	}
	
	/**
	 * Notifies the user that an adhoc find has been received.
	 * @param name
	 * @param description
	 */
    public void notifyNewFind(String name, String description) {
    	newFindsNum++;
    	
    	CharSequence tickerText = "New RWG Find";              // ticker-text
    	long when = System.currentTimeMillis();         // notification time
    	CharSequence contentTitle = "New RWG Find";  // expanded message title
    	CharSequence contentText= "";
    	if(newFindsNum==1) {
    		contentText = "Name: "+name+" | Description: "+description;      // expanded message text
    	}
    	else {
    		contentText = newFindsNum+" unviewed RWG Finds";
    	}
    	Intent notificationIntent = new Intent(mContext, ListFindsActivity.class);
    	PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);

    	// the next two lines initialize the Notification, using the configurations above
    	Notification notification = new Notification(R.drawable.icon, tickerText, when);
    	notification.setLatestEventInfo(mContext, contentTitle, contentText, contentIntent);
    	notification.defaults |= Notification.DEFAULT_SOUND;
    	notification.defaults |= Notification.DEFAULT_VIBRATE;
    	notification.defaults |= Notification.DEFAULT_LIGHTS;
    	notification.ledARGB = 0xff0000ff;
    	notification.ledOnMS = 300;
    	notification.ledOffMS = 1000;
    	notification.flags |= Notification.FLAG_SHOW_LIGHTS;
    	notification.flags |= Notification.FLAG_AUTO_CANCEL;
    	mNotificationManager.notify(AdhocService.NEWFIND_NOTIFICATION, notification);
    }


	/**
	 * A class to contain the received data from a lower network layer (UDP). Objects
	 *         of this type are stored in a receiving queue for later processing
	 * @author Rabie 
	 * @see http://code.google.com/p/adhoc-on-android/
	 */
	private class Message {

		private byte[] data;
		
		public Message(byte[] data) {
			this.data = data;
		}

	}
}
