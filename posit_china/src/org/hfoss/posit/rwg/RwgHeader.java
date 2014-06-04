package org.hfoss.posit.rwg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.BitSet;

import android.util.Log;

/**
 *
 * Implements the Random Walk Gossip header
 *
 * 0     -     15 16     -      31
 * -------------------------------
 *| packetLength | type | hops    |
 * -------------------------------
 *| groupSize    | sequenceNumber | 
 * -------------------------------
 *|           origin              | mac
 * ------------------------------- 
 *|              |   target       | mac
 * -------------------------------
 *|                               | mac
 * -------------------------------
 *|           sender              |
 * -------------------------------
 *|              |    visited     |
 * -------------------------------
 *|           ...                 | 256 bitv
 *|           ...                 | 
 * -------------------------------
 *|           recentVisited       | 256 bitv
 *|           ...                 |
 * -------------------------------
 *
 * packetLength: length of packet in bytes
 *
 * type: REQF - Request to forward , ACK - Acknowledgement, OKTF - Ok to forward, 
 *       BF - be silent
 *
 * hops: number of hops made by the message
 *
 * groupSize: to know how many nodes the message should be delivered to
 *
 * sequenceNumber + origin: unique identifier of a message
 *
 * target: used when a specific node node is intended to receive the message
 *
 * sender: node that sent the message
 *
 * visited: bitvector(256) which is used to indicate which nodes that have seen the message
 *
 * recentVisited: bitvector(256) which indicates recently infected nodes
 *
 */
public class RwgHeader implements Serializable {
	public static final String TAG = "Adhoc";

	public static final String PROTOCOL_RWG = "RWG";

	private String protocol = PROTOCOL_RWG;
	
	private short packetLength=0;
	private byte messageType=0;
	private byte hops=0;
	private long TTL=0;

	private short groupSize=0;
	private short sequenceNumber=0;
	private String origin;   // MAC Addresses
	private String target; 
	private String sender; 
	private BitSet visited;
	private BitSet recentVisited;
	
	/**
	 * Default constructor
	 */
	public RwgHeader() {
		packetLength = 128;
		messageType = Constants.REQF;
		hops = 0;
		TTL = Constants.TTL;
		groupSize = (short) RwgManager.getGroupSize();

		sequenceNumber = 0;
		origin = new String("10:01:10:01:10:01");
		target = new String("20:02:20:02:20:02");
		sender = new String("30:03:30:03:30:03");
		visited = new BitSet(Constants.BIT_VECTOR_SIZE);
		recentVisited = new BitSet(Constants.BIT_VECTOR_SIZE);
	}
	
	/**
	 * Returns a deep copy of this RwgHeader.
	 * @return
	 */
	public RwgHeader copy() {
		RwgHeader temp = new RwgHeader();
		temp.protocol = this.protocol;
		temp.packetLength = this.packetLength;
		temp.messageType = this.messageType;
		temp.hops = this.hops;
		temp.TTL = this.TTL;
		temp.groupSize = this.groupSize;
		temp.sequenceNumber = this.sequenceNumber;
		temp.origin = new String(this.origin);
		temp.target = new String(this.target);
		temp.sender = new String(this.sender); 
		temp.visited = new BitSet(Constants.BIT_VECTOR_SIZE);
		temp.visited.or(this.visited);
		temp.recentVisited = new BitSet(Constants.BIT_VECTOR_SIZE);
		temp.recentVisited.or(this.recentVisited);
		
		return temp;
	}
	
	/**
	 * Creates an RwgHeader for an ACK packet. 
	 * @param origin
	 * @param packetBuff
	 * @return
	 */
	public static RwgHeader createACK(String origin, RwgPacketBuffer packetBuff) {
		RwgHeader header= new RwgHeader();
		header.packetLength = 128;  // Bogus
		header.messageType = Constants.ACK;
		header.hops = 0;
		header.TTL = 0;
		header.groupSize = (short) RwgManager.getGroupSize();

		header.sequenceNumber = packetBuff.getActive_reqf().getReqf().getSequenceNumber();
		header.origin = new String(packetBuff.getActive_reqf().getReqf().getOrigin());
		header.target = new String(packetBuff.getActive_reqf().getReqf().getSender());
		header.sender = new String(origin);
		header.recentVisited.or(packetBuff.getActive_reqf().getReqf().getRecentVisited());		
		header.visited.or(packetBuff.getActive_reqf().getReqf().getVisited());		

		return header;
	}
	
	/**
	 * Creates an RwgHeader for an OKTF packet
	 * @param origin
	 * @param packetBuff
	 * @return
	 */
	public static RwgHeader createOKTF(String origin, RwgPacketBuffer packetBuff) {
		RwgHeader header = new RwgHeader();
		RwgHeader lastWaiting = packetBuff.getWaiting()[packetBuff.getWTail() % packetBuff.getWaiting().length].getReqf();

		/*checks that the reqf actually exisits..*/
		if (packetBuff.getWTail() == packetBuff.getWFront()) {
			Log.i(TAG, RwgManager.rwgHash(origin) + "createOKTF: exiting, waiting is empty");
			packetBuff.getWaiting()[packetBuff.getWTail() % packetBuff.getWaiting().length] = null;
			return null;
		} else if (packetBuff.getWaiting()[packetBuff.getWTail() % packetBuff.getWaiting().length] == null) {
			Log.i(TAG, RwgManager.rwgHash(origin) + "createOKTF: exiting, null value in waiting");
			packetBuff.setWTail(packetBuff.getWTail() + 1);
			return null;
		} else {
			packetBuff.getActive_reqf().setReqf(packetBuff.getWaiting()[packetBuff.getWTail() % packetBuff.getWaiting().length].getReqf());
		   
			// Find ACKs belonging to the last reqf in waiting buffer
			int ackC = 0;
			int tackC = 0;
			RwgHeader acks[] = packetBuff.getAck();
			RwgHeader tempAcks[] = new RwgHeader[packetBuff.getAck().length];
			String lastOrigin = lastWaiting.getOrigin();
			short lastSeqNo = lastWaiting.getSequenceNumber();
			
			// Loop through the ack buffer to find acks matching the reqf
			for (; ackC < tempAcks.length; ackC++) {
				if (acks[ackC] != null) {
					if (RwgManager.rwgMatchIds(lastOrigin, lastSeqNo, acks[ackC].getOrigin(), acks[ackC].getSequenceNumber())) {
						tempAcks[tackC] = acks[ackC];     // Copy ACK to tempACK
						acks[ackC] = null;                // Remove the ACK not needed anymore
						packetBuff.setAck_counter(packetBuff.getAck_counter() - 1); // NOTE couldn't find this in C code
						tackC++;
					}
				}
			}
			if (tackC == 0) {
				Log.i(TAG, RwgManager.rwgHash(origin) + "createOKTF: exiting, No matching ACKS in the buffer");
				packetBuff.getWaiting()[packetBuff.getWTail() % packetBuff.getWaiting().length] = null;
				packetBuff.setWTail(packetBuff.getWTail() + 1);
				return null;
			}
			// Chose random ACK
			int ackno = (int)(Math.random() % tackC);
			RwgHeader activeReqf = packetBuff.getActive_reqf().getReqf();
			header.setMessageType(Constants.OKTF);
			header.setHops(activeReqf.getHops());
			header.setTTL((short) 0);
			header.setGroupSize(activeReqf.getGroupSize());
			header.setSequenceNumber(activeReqf.getSequenceNumber());
			header.setOrigin(activeReqf.getOrigin());
			header.setTarget(tempAcks[ackno].getSender());
			header.setSender(origin);
			
			// Update recentVisited and visited with the incoming ACKS, for the oktf and the reqf saved in the buffer, 
			// also free all the acks.. since there is no more use for them*/
			tackC = tackC -1;
			for (; -1 < tackC; tackC--) {
				RwgManager.rwgBitvectorUpdate(header.getRecentVisited(), tempAcks[tackC].getRecentVisited());
				RwgManager.rwgBitvectorUpdate(packetBuff.getActive_reqf().getReqf().getRecentVisited(), tempAcks[tackC].getRecentVisited());
				RwgManager.rwgBitvectorUpdate(header.getVisited(), tempAcks[tackC].getVisited());
				RwgManager.rwgBitvectorUpdate(packetBuff.getActive_reqf().getReqf().getRecentVisited(), tempAcks[tackC].getVisited());
				tempAcks[tackC] = null;
			}
			RwgManager.rwgBitvectorUpdate(header.getVisited(), header.getRecentVisited());
			RwgManager.rwgBitvectorUpdate(packetBuff.getActive_reqf().getReqf().getVisited(), packetBuff.getActive_reqf().getReqf().getRecentVisited());		

			// Check the hop limit, update visited if it exceeds the limit and zero out recent visited and hops
			if (header.getHops() > Constants.HOPS) {
				header.setHops((byte) 0);
				activeReqf.setHops((byte) 0);
				header.setRecentVisited(new BitSet(Constants.BIT_VECTOR_SIZE));
				activeReqf.setRecentVisited(new BitSet(Constants.BIT_VECTOR_SIZE));
			}
		}
		// Set old pointers to NULL and increase the w_tail
		
		packetBuff.getWaiting()[packetBuff.getWTail() % packetBuff.getWaiting().length] = null;
		packetBuff.setWTail(packetBuff.getWTail() + 1);
		
		return header;
	}
	
	/**
	 * Creates a BS (be silent) header
	 * @param origin
	 * @param packetBuff
	 * @return
	 */
	public static RwgHeader createBS(String origin, RwgPacketBuffer packetBuff){
		RwgHeader header= packetBuff.getActive_reqf().getReqf();
		header.sender = new String(origin);
		header.messageType = Constants.BS;
		return header;
	}
	
	/**
	 * Creates a random REQF used when the network is silent.
	 * @param origin
	 * @param packetBuff
	 * @return
	 */
	public static RwgHeader createReqfR(String origin, RwgPacketBuffer packetBuff) {
		RwgHeader header= null;

		// Choose the REQF with least marks in the visited list

		int matches = 0;
		int matchesPrevious = 0;
		int prevCtr = 0;
		int ctr = packetBuff.getReqf_counter() - 1;
		if (ctr > -1) {
			matches = 0;
			matchesPrevious = packetBuff.getReqf()[ctr].getReqf().getVisited().cardinality();
			prevCtr = ctr;
			for (; -1 < ctr; ctr--) {
				matches = packetBuff.getReqf()[ctr].getReqf().getVisited().cardinality();
				if (matchesPrevious > matches) {
					matchesPrevious = matches;
					prevCtr = ctr;
				}
			}

			// Copy, return, and set active_reqf if visited < groupSize, and change the sender of the packet
			// NOTE: Seems to me, we should return null if there are no REQFs in the buffer??
			if (packetBuff.getReqf()[prevCtr].getReqf().getGroupSize() > matchesPrevious) {
				long stamp = System.currentTimeMillis();
				header = packetBuff.getReqf()[prevCtr].getReqf();

				// Make sure the TTL has not expired before sending this packet
				if (header.getTTL() - (stamp - packetBuff.getReqf()[prevCtr].getArrivedAt()) < 0) {
					return null;
				}

				Log.d(TAG, "create REQF-R :  reqf = " + prevCtr + " visited = " 
						+ matches + " prevVisited = " + matchesPrevious);

				header.setSender(origin);
				header.setTTL(header.getTTL() - (stamp - packetBuff.getReqf()[prevCtr].getArrivedAt()));
				packetBuff.getReqf()[prevCtr].setArrivedAt(stamp);

				// Stores the pointer to the REQF in the waiting buffer (REQFs that are waiting for ACKs)

				packetBuff.getWaiting()[packetBuff.getWFront() % packetBuff.getWaiting().length] = packetBuff.getReqf()[prevCtr];
				packetBuff.getReqf()[prevCtr].setWStamp(stamp);
				packetBuff.getReqf()[prevCtr].setWait_pos(packetBuff.getWFront() % packetBuff.getWaiting().length);
				packetBuff.setWFront(packetBuff.getWFront() + 1);

				// Make sure front wont reach the tail (when the buffers get full)
				if (packetBuff.getWFront() % packetBuff.getWaiting().length 
						== packetBuff.getWTail() % packetBuff.getWaiting().length
						&& packetBuff.getWFront() != packetBuff.getWTail())  {
					packetBuff.getWaiting()[packetBuff.getWTail() % packetBuff.getWaiting().length] = null;
					packetBuff.setWTail(packetBuff.getWTail() + 1);
				}
				return header;
			}
		}
		return null;
	}	
	
	/**
	 * Forwards an existing REQF after getting an OKTF or sending a REQF from the
	 * wake buffer.
	 * @param origin
	 * @param packetBuff
	 * @return
	 */
	public static RwgHeader createReqfF(String origin, RwgPacketBuffer packetBuff) {
		RwgHeader header = null;
		
		if (packetBuff.getActive_reqf().getReqf() != null &&
				packetBuff.getReqf_counter() > packetBuff.getActive_reqf().getReqf_pos()) {
			
			// Increase hop counter, change sender address, and update visited list
			header = packetBuff.getActive_reqf().getReqf();
			header.setSender(origin);
			RwgManager.rwgBitvectorUpdate(header.getVisited(), header.getRecentVisited());
			
			// Check whether the REQF was from the wake buffer
			if (packetBuff.getWake_counter() > 0 
					&& packetBuff.getWake()[packetBuff.getWake_counter() -1] != null
					&& packetBuff.getActive_reqf().getReqf() == packetBuff.getWake()[packetBuff.getWake_counter()-1].getReqf()) {
				packetBuff.getWake()[packetBuff.getWake_counter()-1].setWait(0);
				packetBuff.getWake()[packetBuff.getWake_counter()-1] = null;
				packetBuff.setWake_counter(packetBuff.getWake_counter() - 1);
			} else {
				header.setHops((byte) (header.getHops() + 1));
			}
			
			long stamp = System.currentTimeMillis();
					
			// Make sure the TTL has not expired before sending this packet
			if (header.getTTL() - (stamp - packetBuff.getReqf()[packetBuff.getActive_reqf().getReqf_pos()].getArrivedAt()) < 0) {
				return null;
			}

			// Store the pointer to the reqf_info in the waiting buffer
			// Stores the pointer to the REQF in the waiting buffer (REQFs that are waiting for ACKs)

			packetBuff.getWaiting()[packetBuff.getWFront() % packetBuff.getWaiting().length] = 
				packetBuff.getReqf()[packetBuff.getActive_reqf().getReqf_pos()];
			packetBuff.getReqf()[packetBuff.getActive_reqf().getReqf_pos()].setWStamp(stamp);
			packetBuff.getReqf()[packetBuff.getActive_reqf().getReqf_pos()].setWait_pos(packetBuff.getWFront() % packetBuff.getWaiting().length);
			packetBuff.setWFront(packetBuff.getWFront() + 1);
			
			
			// Make sure front wont reach the tail (when the buffers get full)
			if (packetBuff.getWFront() % packetBuff.getWaiting().length 
					== packetBuff.getWTail() % packetBuff.getWaiting().length
					&& packetBuff.getWFront() != packetBuff.getWTail())  {
				packetBuff.getWaiting()[packetBuff.getWTail() % packetBuff.getWaiting().length] = null;
				packetBuff.setWTail(packetBuff.getWTail() + 1);
			}
						
		} else {
			// This may happen when TTL has expired and the REQF has been removed since the refreshing of the REQF buffer 
			//	does not update the wake buffer, so the wake counter(-1) may be pointing at a NULL element*/
			packetBuff.getWake()[packetBuff.getWake_counter() -1].setWait(0);
			packetBuff.setWake_counter(packetBuff.getWake_counter() - 1);
			return null;
		}
		return header;
	}
	
	/**
	 * Creates a REQF_N header.  New REQFs.
	 * @see send.c
	 * @param origin
	 * @param packetBuff
	 * @return
	 */
	public static RwgHeader createREQF(String origin, RwgPacketBuffer packetBuff) {
		// A skeletal RwgHeader was created in RwgSender.
		RwgHeader header = packetBuff.getActive_reqf().getReqf();
		header.messageType = Constants.REQF;
		header.sequenceNumber = RwgManager.getNextSequenceNumber();
		header.TTL = Constants.TTL;
		header.hops = 1;
		header.groupSize = (short) RwgManager.getGroupSize();
		header.origin = origin;
		header.sender = origin;
		header.target = "255.255.255.255"; // Broadcast
				
		// Set the bit for this node
		header.visited.set(RwgManager.rwgHash(origin));
		header.recentVisited.set(RwgManager.rwgHash(origin));
		
		// Store the pointer to the reqf_info in the reqf buffer
		packetBuff.getWaiting()[packetBuff.getWFront() % packetBuff.getWaiting().length] 
		                        = packetBuff.getReqf()[packetBuff.getReqf_counter()];
		
		// Saves the position that the reqf pointer is placed within the waiting buffer, in the reqf buffer
		packetBuff.getReqf()[packetBuff.getReqf_counter()].setWait_pos(packetBuff.getWFront() % packetBuff.getWaiting().length);
		
		// Set the waiting timestamp in the reqf_info
		// NOTE: Our timestamp differs from C code
		packetBuff.getReqf()[packetBuff.getReqf_counter()].setWStamp(System.currentTimeMillis());
		
		packetBuff.setWFront(packetBuff.getWFront() + 1);
		
		// Make sure front doesn't reach the tail (when the buffers get full)
		int len = packetBuff.getWaiting().length;
		if (packetBuff.getWFront() % len == packetBuff.getWTail() % len
				&& packetBuff.getWFront() != packetBuff.getWTail())  {
			packetBuff.getWaiting()[packetBuff.getWTail() % len] = null;
			packetBuff.setWTail(packetBuff.getWTail() + 1);
		}
		
		// Stores the pointer to the reqf in the reqf buffer, and saves the time stamp
		packetBuff.getReqf()[packetBuff.getReqf_counter()].setReqf(header);
		packetBuff.getReqf()[packetBuff.getReqf_counter()].setArrivedAt(System.currentTimeMillis());
		packetBuff.setReqf_counter(packetBuff.getReqf_counter() + 1);
		
		Log.i(TAG, "packetBuff= " + packetBuff);
		Log.i(TAG, "createREQF(): reqf_counter = " + packetBuff.getReqf_counter());

		return header;
	}
	


	/**
	 * Write this object to a serialized byte array
	 * @param baos
	 * @throws IOException
	 */
	public byte[] writeToBytes() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(this);
		oos.flush();
		return baos.toByteArray();
	}
	
	private String msgToString(byte msg) {
		if (msg == Constants.REQF) 
			return "REQF";
		else if (msg == Constants.BS)
			return "BS";
		else if (msg == Constants.OKTF)
			return "OKTF";
		else if (msg == Constants.ACK)
			return "ACK";
		else 
			return "???";
	}
	
	public String toString() {
		return "" + 
			protocol + ";" +
			packetLength + ";" +
			msgToString(messageType) + ";" +
			hops + ";" +
			TTL + ";" +
			groupSize + ";" +
			sequenceNumber + ";" +
			origin + ";" +
			target + ";" +
			sender + ";" +
			visited + ";" + 
			recentVisited + ";";
	}
	/**
	 * Reads an instance of AdhocData from a serialized byte stream.
	 * @param bais
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static RwgHeader readFromBytes(byte[] bytes) 
					throws IOException,	ClassNotFoundException {
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		ObjectInputStream ois = new ObjectInputStream(bais);
		RwgHeader data = (RwgHeader)ois.readObject();

		// For development/debug 
		Log.d(TAG, "protocol = " + data.protocol);
		Log.d(TAG, "packetLength " + data.packetLength);
		Log.d(TAG, "messageType = " + data.messageType);
		Log.d(TAG, "hops = " + data.hops);
		Log.d(TAG, "TTL = " + data.TTL);
		Log.d(TAG, "groupSize = " + data.groupSize);
		Log.d(TAG, "sequenceNumber = " + data.sequenceNumber);
		Log.d(TAG, "origin = " + data.origin);
		Log.d(TAG, "target = " + data.target);
		Log.d(TAG, "sender = " + data.sender);
		return data;
	}
	
	public String getProtocol() {
		return protocol;
	}
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	public short getPacketLength() {
		return packetLength;
	}
	public void setPacketLength(short packetLength) {
		this.packetLength = packetLength;
	}
	public byte getMessageType() {
		return messageType;
	}
	public void setMessageType(byte messageType) {
		this.messageType = messageType;
	}
	public byte getHops() {
		return hops;
	}
	public void setHops(byte hops) {
		this.hops = hops;
	}
	public long getTTL() {
		return TTL;
	}
	public void setTTL(long l) {
		TTL = l;
	}
	public short getGroupSize() {
		return groupSize;
	}
	public void setGroupSize(short groupSize) {
		this.groupSize = groupSize;
	}
	public short getSequenceNumber() {
		return sequenceNumber;
	}
	public void setSequenceNumber(short sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}
	public String getOrigin() {
		return origin;
	}
	public void setOrigin(String origin) {
		this.origin = origin;
	}
	public String getTarget() {
		return target;
	}
	public void setTarget(String target) {
		this.target = target;
	}
	public String getSender() {
		return sender;
	}
	public void setSender(String sender) {
		this.sender = sender;
	}
	public BitSet getVisited() {
		return visited;
	}
	public void setVisited(BitSet visited) {
		this.visited = visited;
	}
	public BitSet getRecentVisited() {
		return recentVisited;
	}
	public void setRecentVisited(BitSet recentVisited) {
		this.recentVisited = recentVisited;
	}
	
}
