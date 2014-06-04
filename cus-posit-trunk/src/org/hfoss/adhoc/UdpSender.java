/*
 * File: UpdSender.java
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


import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import org.hfoss.posit.rwg.RwgPacket;
import android.util.Log;


/**
 * Sends packets from the protocol later over a UDP socket. Adapted from:
 * @see  http://code.google.com/p/adhoc-on-android/
 */
public class UdpSender {
	private static final String TAG = "Adhoc";
	private int mHash;

	private DatagramSocket datagramSocket;	
	
	public UdpSender(int hashAddr) throws SocketException, UnknownHostException, BindException{
		mHash = hashAddr;
	    datagramSocket = new DatagramSocket(8881);
	}

	/**
	 * Sends data over a UDP socket. 
	 * Called from RWG layer.
	 * @param data is the packet which is to be sent. 
	 * @throws IOException  
	 */
	public boolean sendPacket(RwgPacket packet) throws IOException {
		Log.e(TAG,  mHash + " UdpSender: sendPacket() sending data = " + packet);

		byte[] payload = packet.writeToBytes();  // Serialize the data (might throw I/O)

		if (payload.length <= AdhocService.MAX_PACKET_SIZE) {
			broadcast(payload);
			return true; 
		} else {
			Log.e(TAG, mHash + " sendPacket:  Packet length=" + payload.length + " exceeds max size, not sent");
			return false;
		}
	}
	
	/**
	 * A special definition exists for the IP broadcast address 255.255.255.255. 
	 * It is the broadcast address of the zero network (0.0.0.0), which in 
	 * Internet Protocol standards stands for this network, i.e. the local network. 
	 * Transmission to this address is limited by definition, in that it 
	 * does not cross the routers connecting the local network to the Internet.
	 * @param bytes
	 */
	private void  broadcast (byte[] bytes){
		Log.i(TAG, mHash + " broadcast() bytes size =" + bytes.length);

		InetAddress IPAddress;
		try {
			if (AdhocService.getAdhocMode() == AdhocService.MODE_INFRASTRUCTURE) {
				IPAddress = InetAddress.getByName("255.255.255.255");
			} else {
				IPAddress = InetAddress.getByName("192.168.2.255");  // Broadcast address 
			}
			Log.i(TAG,  mHash + " Sending to IPAddress = " + IPAddress + " port = " + AdhocService.DEFAULT_PORT_BCAST);
			DatagramPacket packet = new DatagramPacket(bytes, bytes.length,IPAddress, AdhocService.DEFAULT_PORT_BCAST);
			datagramSocket.setBroadcast(true);
			datagramSocket.send(packet);
		} catch (UnknownHostException e) {
			Log.e(TAG, mHash + " UnknownHostException");
			e.printStackTrace();
		} catch (SocketException e) {
			Log.e(TAG, mHash + " SocketException");
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, mHash + " IOException");
			e.printStackTrace();
		}
	}
	
	public void closeSocket(){
		datagramSocket.close();
	}

}
