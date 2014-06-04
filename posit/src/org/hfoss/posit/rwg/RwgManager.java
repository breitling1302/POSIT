package org.hfoss.posit.rwg;

import java.util.BitSet;

import org.hfoss.posit.rwg.RwgPacketBuffer.ReqForwardInfo;

import android.util.Log;

public class RwgManager implements Runnable {
	
	// Constants (See also Constants.java)
	private static final String TAG = "Adhoc";

	public static final int RWG_ETHER_TYPE = 0x1111; // shows that an eth frame carries a rwg packet as payload
	public static final String BROADCAST_ADDR = "192.168.2.255";
	public static final int MTU = 1024; // maximum transmission unit (bytes)

	// Flags
	public static boolean SEND_ACK  = false;
	public static  boolean SEND_REQF_N = false; // create new from input
	public static  boolean  SEND_REQF_F = false; // forward from buffer
	public static  boolean  SEND_REQF_R = false; // send random when network is silent
	public static  boolean SEND_OKTF = false;
	public static  boolean  SEND_BS = false;
	public static  boolean  LISTEN_ACK = false;
	public static  boolean  RETRANSMIT = false;
	public static  boolean  SEND_RETRANSMIT = false;
	public static  boolean  TRACE = false; // print trace
	public static  boolean  WO_MODE = false; // decides what will be written on the output pipe
	
	private volatile boolean keepRunning;  // formerly LOOP
	private Thread managerThread;
	
	// Timers
	private long silentTimer;
	private long refreshReqfTimer;


    private static Object sequenceNumberLock = 0;
	private static short nodeSequenceNumber = Constants.FIRST_SEQUENCE_NUMBER;
	
	private String mMacAddress = "";
	private static short myHash;
	
	private RwgPacketBuffer mPacketBuffer;

	private static int mGroupSize = Constants.GROUP_SIZE;
	
	public RwgManager(String macAddr, int groupSize) {
		mGroupSize = groupSize;
		mMacAddress = macAddr;
		myHash = (short) rwgHash(mMacAddress);
		mPacketBuffer = new RwgPacketBuffer();
	}
	
	public void startThread(){
		keepRunning = true;
		managerThread = new Thread(this);
		managerThread.start();
	}

	/**
	 * Stops the receiver thread.
	 */
	public void stopThread() {
		keepRunning = false;
		managerThread.interrupt();
	}
	
	public static int getGroupSize() {
		return mGroupSize;
	}
	
	public void run() {
		silentTimer = System.currentTimeMillis();
		refreshReqfTimer = System.currentTimeMillis();
		
		while (keepRunning) {
			
			long stamp = System.currentTimeMillis();
			
			// Refresh the reqf_buffer every 7 seconds, to remove reqfs with Time To Live < 0
			// Must be done to avoid overflow"
			if (refreshReqfTimer + Constants.REQF_REFRESH_TIMER_INTERVAL < System.currentTimeMillis()) {
				refreshReqfBuffer(mPacketBuffer);
				refreshReqfTimer = System.currentTimeMillis();
			} 
			
			
			// Checks if there are any reqfs in the waiting buffer (waiting for ACKS)
			// Checks the timestamp (waits 0.1s). Sets SEND_OKTF and signals the other thread
			
			else if (mPacketBuffer.getWTail() != mPacketBuffer.getWFront() 
					&& mPacketBuffer.getWaiting()[mPacketBuffer.getWTail() % mPacketBuffer.getWaiting().length] != null
					&& checkTimeStamp(mPacketBuffer.getWaiting()[mPacketBuffer.getWTail() % mPacketBuffer.getWaiting().length].getWStamp(), 
							stamp, Constants.WAIT_BUFFER_TIMER_INTERVAL)){
				SEND_OKTF = true;
				RwgSender.queueRwgMessage();
				silentTimer = System.currentTimeMillis();
			}
			
			// If the network has been silent for 5 seconds send a random REQF

			else if (silentTimer + Constants.SILENT_TIMER_INTERVAL < System.currentTimeMillis()) {
								refreshReqfBuffer(mPacketBuffer);
				SEND_REQF_R = true;
				RwgSender.queueRwgMessage();
				silentTimer = System.currentTimeMillis();
			}
			// Check if there is any packets on the socket or the named pipe*/
			// In C version rwg_listen is called her to listen to the sockets and pipes
			// And there is also a clause for sleeping for 5 seconds if nothing is happening
		
			else {
				try {
					Thread.sleep(Constants.MAIN_THREAD_SLEEP_INTERVAL);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void refreshReqfBuffer(RwgPacketBuffer packetBuff) {
		long stamp = System.currentTimeMillis();
		int reqfCtr = 0;
		long ttlCheck = 0;
		packetBuff.setTemp_reqf_counter(0);
		
		for (reqfCtr = 0; reqfCtr < packetBuff.getReqf_counter(); reqfCtr++) {
			
			ReqForwardInfo rwgReqfInfo = packetBuff.getReqf()[reqfCtr];
			RwgHeader rwgHdr = rwgReqfInfo.getReqf();
			if (rwgHdr != null) {
				ttlCheck = (rwgHdr.getTTL() -  (stamp - rwgReqfInfo.getArrivedAt()));
			}
			if (rwgHdr != null 
					&& ((ttlCheck < 0) 
					|| (rwgHdr.getGroupSize()  <= rwgHdr.getVisited().cardinality()))) {
				
				Log.d(TAG, myHash + " refreshReqfBuffer removing packet " + rwgHdr.getOrigin() + "/" + rwgHdr.getSequenceNumber());

				// N/A ? To make sure that there are no pointers pointing to the freed addr at the heap
				
				if (packetBuff.getWaiting()[packetBuff.getReqf()[reqfCtr].getWait_pos()] == rwgReqfInfo) {
					packetBuff.getWaiting()[packetBuff.getReqf()[reqfCtr].getWait_pos()] = null;
				}
				
				// N/A To make sure that there are no pointers pointing to the freed addr at the heap

				if (packetBuff.getWake()[packetBuff.getReqf()[reqfCtr].getWake_pos()] == rwgReqfInfo) {
					packetBuff.getWake()[packetBuff.getReqf()[reqfCtr].getWake_pos()] = null;
				}	
				
				// Remove the packet, since TTL < 0 and the REQF is not in the waiting buffer or the wake buffer
				rwgHdr = null;
				rwgReqfInfo.setReqf(null);
				rwgReqfInfo.setArrivedAt(0);
				rwgReqfInfo.setWait(0);
				rwgReqfInfo.setWake(0);
			} else if (packetBuff.getReqf()[reqfCtr].getReqf() != null) {
				// Move to temp buffer, set new timestamp, change the pointers in the wake/ waiting buffer to the correct addresses
				packetBuff.getTemp_reqf()[packetBuff.getTemp_reqf_counter()] = rwgReqfInfo;
				packetBuff.getTemp_reqf()[packetBuff.getTemp_reqf_counter()].setArrivedAt(stamp);
				packetBuff.getTemp_reqf()[packetBuff.getTemp_reqf_counter()].setReqf_pos(packetBuff.getTemp_reqf_counter());
				
				if (packetBuff.getWaiting()[packetBuff.getReqf()[reqfCtr].getWait_pos()] == packetBuff.getReqf()[reqfCtr]) {
					packetBuff.getWaiting()[packetBuff.getReqf()[reqfCtr].getWait_pos()] = packetBuff.getReqf()[packetBuff.getTemp_reqf_counter()];
				}
				
				if (packetBuff.getWake()[packetBuff.getReqf()[reqfCtr].getWake_pos()] == packetBuff.getReqf()[reqfCtr]) {
					packetBuff.getWake()[packetBuff.getReqf()[reqfCtr].getWake_pos()] = packetBuff.getReqf()[packetBuff.getTemp_reqf_counter()];
				}
				
				if (ttlCheck >= 0) {
					packetBuff.getReqf()[reqfCtr].getReqf().setTTL(ttlCheck);
				} else {
					packetBuff.getReqf()[reqfCtr].getReqf().setTTL(0);
				}
				packetBuff.setTemp_reqf_counter(packetBuff.getTemp_reqf_counter() + 1);
			}
		}

		// Replace the real REQF buffer with the temporary reqf buffer
		// NOTE: should this be a deep copy?? C used memcopy
		packetBuff.setReqf_counter(packetBuff.getTemp_reqf_counter());
		if (packetBuff.getReqf_counter() != 0) {
			packetBuff.setReqf(packetBuff.getTemp_reqf());
		}
	}
	
	private boolean checkTimeStamp(long t1, long t2, int interval) {
		return t2 - t1 > interval;
	}

	public RwgPacketBuffer getPacketBuffer() {
		return mPacketBuffer;
	}
	
	public static short getMyHash() {
		return myHash;
	}

	
	/**
	 * Increments and set the sequence number before returning the new value. 
	 * @return returns the next sequence number
	 */
	public static short getNextSequenceNumber(){
		synchronized (sequenceNumberLock) {
			if(nodeSequenceNumber == Constants.UNKNOWN_SEQUENCE_NUMBER
					|| nodeSequenceNumber == Constants.MAX_SEQUENCE_NUMBER	){
				
				nodeSequenceNumber = Constants.FIRST_SEQUENCE_NUMBER;
			}
			else{
				nodeSequenceNumber++;	
			}
			return nodeSequenceNumber;
		}
	}
	
	/**
	 * Matches REQF packets against REQF packet buffer returning a position in the buffer
	 * if a match is found and -1 otherwise.
	 * @see rwg_send.c 
	 * @param origin
	 * @param seqNo
	 * @param packetBuff
	 * @return the matching REQF's position in buffer or -1 if no match
	 */ 
	public static int rwgMatchPacketId(String origin, short seqNo, RwgPacketBuffer packetBuff) {
		int rc = 0;
		if (packetBuff.getReqf_counter() > 0) {
			rc = packetBuff.getReqf_counter() -1;
		} else
			return -1;
		
		for (; rc >= 0; rc--) {
			String org = packetBuff.getReqf()[rc].getReqf().getOrigin();
			short seq = packetBuff.getReqf()[rc].getReqf().getSequenceNumber();
			if (origin.equals(org) && seqNo == seq)
				return rc;
		}
		return -1;
	}	
	
	public static boolean rwgMatchIds(String src1, short seq1, String src2, short seq2) {
		return src1.equals(src2) && seq1 == seq2;
	}
	
	
	/**
	 * Peforms an or on two bit vectors.
	 * @see rwg_receive.c
	 * @param bitset1
	 * @param bitset2
	 */
	public static void rwgBitvectorUpdate(BitSet bitset1, BitSet bitset2) {
		bitset1.or(bitset2);
	}

	public static void rwgSetBitvector(BitSet bitset, short pos) {
		bitset.set(pos);
	}
	
	
	public static boolean rwgBitvectorLookup(BitSet bitset, short pos) {
		return bitset.get(pos);
	}
	
	/**
	 * @param addr a phone's MAC address. A simple polynomial has is used.
	 * @return
	 */
	public static int rwgHash(String addr) {
		final int A = 33;
	    int L = addr.length();
		long sum = 0;
		for (int k = 0; k < addr.length(); k++) {
			sum += ((int)addr.charAt(k) * (int)Math.pow(A, L));
			L--;
		}
		return (int)(sum % Constants.BIT_VECTOR_SIZE);
		
	}
	
	/*FUNC: Called to exit protocol*/
	public void exit() {
		keepRunning = false;
	}
	
}
