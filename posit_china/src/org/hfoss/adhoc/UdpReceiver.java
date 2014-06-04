/*
 * File: UdpReceiver.java
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
import java.net.SocketException;
import java.net.UnknownHostException;

import org.hfoss.posit.rwg.RwgReceiver;

import android.util.Log;

/**
 * Class running as a separate thread, and responsible for receiving data packets over the UDP protocol.
 * @author Rabie
 * Adapted from:
 * @see  http://code.google.com/p/adhoc-on-android/
 *
 */
public class UdpReceiver implements Runnable {
	private static final String TAG = "Adhoc";

	private DatagramSocket mDatagramSocket;

	private volatile boolean keepRunning = true;
	private Thread udpReceiverthread;
	private RwgReceiver parent;
	private int mHash;

	public UdpReceiver(RwgReceiver parent, int hashAddr) throws SocketException, UnknownHostException, BindException {
		this.parent = parent;
		mHash = hashAddr;
		mDatagramSocket = new DatagramSocket(AdhocService.DEFAULT_PORT_BCAST);
		mDatagramSocket.setSoTimeout(0);            // Infinite timeout
	}

	public void startThread(){
		keepRunning = true;
		udpReceiverthread = new Thread(this);
		udpReceiverthread.start();
	}

	public void stopThread(){
		keepRunning = false;
		mDatagramSocket.close();
		udpReceiverthread.interrupt();
	}

	public void run(){
		while(keepRunning) {
			try {
				// 52kb buffer
				byte[] buffer = new byte[52000];
				DatagramPacket packet = new DatagramPacket(buffer,buffer.length);
	
				mDatagramSocket.receive(packet);  // This blocks indefinitely
				String ip = packet.getAddress().toString();
				int port = packet.getPort();
			    Log.i(TAG, mHash + " Received packet socket addr= " + packet.getSocketAddress().toString());
				
				Log.i(TAG, mHash +  " updReceiver received a packet ip=" + ip + " port= " + port);

			    byte[] payload = new byte[packet.getLength()];

			    System.arraycopy(packet.getData(), 0, payload, 0, packet.getLength());

			    parent.addMessage(payload);
			    
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		Log.i(TAG, mHash +  " Exiting the receiver loop");
		AdhocService.notifyAdhocOff();
	}

}
