package org.hfoss.posit.rwg;

import java.io.IOException;
import java.net.BindException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.hfoss.adhoc.AdhocData;
import org.hfoss.adhoc.AdhocFind;
import org.hfoss.adhoc.UdpSender;

import android.util.Log;

/**
 * Class used by the RWG layer to send messages from the application layer to 
 * the UDP layer.
 * @see http://code.google.com/p/adhoc-on-android/
 */
public class RwgSender implements Runnable {
	private static final String TAG = "Adhoc";

	private static Queue<AdhocData<AdhocFind>> userMessagesFromPhone;

	private final static Object queueLock = new Integer(0);
	private UdpSender udpSender;
	private volatile boolean keepRunning = true;
	private Thread senderThread;
	private RwgManager rwgManager;
	private String macAddr;
	private int mHash;


	public RwgSender(String macAddr, RwgManager rwgManager) throws SocketException, UnknownHostException, BindException {
	    this.macAddr = macAddr;
	    mHash = RwgManager.rwgHash(macAddr);
		udpSender = new UdpSender(mHash);
		userMessagesFromPhone = new ConcurrentLinkedQueue<AdhocData<AdhocFind>>();

		this.rwgManager = rwgManager;
	}

	public void startThread(){
		keepRunning = true;
		senderThread = new Thread(this);
		senderThread.start();
	}

	public void stopThread(){
		keepRunning = false;
		senderThread.interrupt();
		udpSender.closeSocket();
	}

	/**
	 * 
	 * Receives user data from the application layer or the network, creates
	 * RWG packets, and sends them to the network (UDP) layer for transmission.
	 * This is like the listen loop in the C version
	 */
	public void run() {
		RwgPacketBuffer packetBuff = rwgManager.getPacketBuffer();
		RwgHeader reqf = new RwgHeader();
		RwgHeader rwgHeader = null;
		AdhocData<AdhocFind> userData = null;		
		RwgPacket rwgPacket = null;
		EthernetHeader ethrHdr = null;

		while(keepRunning){
			try {
				synchronized(queueLock){
					while(userMessagesFromPhone.isEmpty()
							&& !RwgManager.SEND_ACK
							&& !RwgManager.SEND_BS
							&& !RwgManager.SEND_OKTF
							&& !RwgManager.SEND_REQF_F
							&& !RwgManager.SEND_REQF_N
							&& !RwgManager.SEND_REQF_R) {
						queueLock.wait();         // Wait here for a notify from the queue
					}
				}	
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			

			// Check whether any user data was received from this phone
			userData = userMessagesFromPhone.peek();

			if (userData != null){
				Log.i(TAG, mHash + " RwgSender: New data on message queue");
				reqf = new RwgHeader();
				packetBuff.getActive_reqf().setReqf(reqf);
				packetBuff.getActive_reqf().setReqf_pos(packetBuff.getReqf_counter());
				packetBuff.getActive_reqf().setUserData(userData);
				RwgManager.SEND_REQF_N = true;
				Log.i(TAG, mHash + " RwgSender.sendUserDataPacket(): SEND_REQF_N is set");

				//handleUserData(userData);
				//it is expected that the queue still has the same userDataHeader object as head
				userMessagesFromPhone.poll();
				rwgPacket = null;
			} 
			// Check whether RWG tasks have been scheduled
			if(RwgManager.SEND_ACK){
				Log.i(TAG, mHash + " RwgSender: SEND_ACK");
				RwgManager.SEND_ACK = false;
				rwgHeader = RwgHeader.createACK(macAddr, packetBuff);
				userData = null;
				ethrHdr = new EthernetHeader(Constants.RWG_PROTOCOL,rwgHeader.getSender(),rwgHeader.getTarget());
				rwgPacket = new RwgPacket(ethrHdr, rwgHeader, userData);
				Log.i(TAG, mHash + " RwgSender.ACK.rwgPacket= " + rwgPacket.toString());
				
			}else if(RwgManager.SEND_REQF_N){
				Log.i(TAG, mHash + " RwgSender: SEND_REQF_N");
				RwgManager.SEND_REQF_N = false;

				// Creates the RwgHeader including UserData from the RwgPacketBuffer
				rwgHeader = RwgHeader.createREQF(macAddr,packetBuff);

				// Retrieve the userData from the packet buffer
				userData = packetBuff.getActive_reqf().getUserData();
				//String sender = "192.168.2." + mHash;
				ethrHdr = new EthernetHeader(Constants.RWG_PROTOCOL,rwgHeader.getSender(),RwgManager.BROADCAST_ADDR);

				rwgPacket = new RwgPacket(ethrHdr, rwgHeader, userData);
				Log.i(TAG, mHash + " RwgSender.REQF-N.rwgPacket= " + rwgPacket.toString());
				
			}else if(RwgManager.SEND_REQF_F){
				Log.i(TAG, mHash + " RwgSender: SEND_REQF_F");
				RwgManager.SEND_REQF_F = false;
				rwgPacket = null;

				// Creates the RwgHeader including UserData from the RwgPacketBuffer
				rwgHeader = RwgHeader.createREQF(macAddr,packetBuff);
				if (rwgHeader != null) {
					// Retrieve the userData from the packet buffer
					userData = packetBuff.getActive_reqf().getUserData();
					ethrHdr = new EthernetHeader(Constants.RWG_PROTOCOL,rwgHeader.getSender(),RwgManager.BROADCAST_ADDR);
					rwgPacket = new RwgPacket(ethrHdr, rwgHeader, userData);
				} 		
			}else if(RwgManager.SEND_REQF_R){
				//Log.i(TAG, mHash + " RwgSender: SEND_REQF_R");
				RwgManager.SEND_REQF_R = false;

				rwgPacket = null;
				rwgHeader = RwgHeader.createReqfR(macAddr, packetBuff);
				if (rwgHeader != null) {
					userData = packetBuff.getActive_reqf().getUserData();
					ethrHdr = new EthernetHeader(Constants.RWG_PROTOCOL,rwgHeader.getSender(),RwgManager.BROADCAST_ADDR);

					rwgPacket = new RwgPacket(ethrHdr, rwgHeader, userData);
					Log.i(TAG, mHash + " RwgSender.REQF-R.rwgPacket= " + rwgPacket.toString());
				}

			}else if(RwgManager.SEND_OKTF){
				Log.i(TAG, mHash + " RwgSender: OKTF");
				RwgManager.SEND_OKTF = false;
				rwgPacket = null;
				rwgHeader = RwgHeader.createOKTF(macAddr, packetBuff);
				if (rwgHeader != null)  {
					userData = null;
					ethrHdr = new EthernetHeader(Constants.RWG_PROTOCOL,rwgHeader.getSender(),RwgManager.BROADCAST_ADDR);
					rwgPacket = new RwgPacket(ethrHdr, rwgHeader, userData);
					Log.i(TAG, mHash + " RwgSender.OKTF.rwgPacket= " + rwgPacket.toString());
				} else {
					Log.i(TAG, mHash + " Ignoring OKTF request");

				}
				
			}else if(RwgManager.SEND_BS){
				Log.i(TAG, mHash + " RwgSender: BS");
				RwgManager.SEND_BS = false;
				rwgPacket = null;
				rwgHeader = RwgHeader.createBS(macAddr, packetBuff);
				if (rwgHeader != null) {
					userData = null;
					ethrHdr = new EthernetHeader(Constants.RWG_PROTOCOL,rwgHeader.getSender(),RwgManager.BROADCAST_ADDR);
					rwgPacket = new RwgPacket(ethrHdr, rwgHeader, userData);
					Log.i(TAG, mHash + " rwgPacket= " + rwgPacket.toString());
				}
			}else{
				Log.e(TAG, mHash + " RwgSender: SHOULD NOT GET HERE (besides during exit)");
			}

			// Send the packet that was constructed above
			try {
				if (rwgPacket != null) {
					Log.i(TAG, mHash + " RwgSender sending packet to UDP");
					boolean result = udpSender.sendPacket(rwgPacket);
					if (result) {
						Log.i(TAG, mHash + " RwgSender sent packet to UDP");
					} else {
						Log.e(TAG, mHash + " RwgSender: Something went wrong at send packet to UDP");
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}    	
	}

	/**
	 * Used by the application layer to queue messages that are to be handled by 
	 * the RWG protocol.
	 * @param userPacket
	 */
	public static void queueUserMessageFromNode(AdhocData<AdhocFind> userPacket){
		userMessagesFromPhone.add(userPacket);
		Log.i(TAG, " Queued user message");
		synchronized (queueLock) {
			queueLock.notify();
		}
	}
	
	/**
	 * This method just notifies the sender thread.
	 */
	public static void queueRwgMessage() {

		synchronized (queueLock) {
			queueLock.notify();
		}
	}


}
