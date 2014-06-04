package org.hfoss.posit.rwg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import android.util.Log;

public class EthernetHeader implements Serializable {
	public static final String TAG = "Adhoc";
	
	private String source;
	private String destination;
	private String protocol;
	
	public EthernetHeader (String protocol, String source, String destination) {
		this.source = source;
		this.destination = destination;
		this.protocol = protocol;
	}
	
	
	public String getSource() {
		return source;
	}


	public void setSource(String source) {
		this.source = source;
	}


	public String getDestination() {
		return destination;
	}


	public void setDestination(String destination) {
		this.destination = destination;
	}


	public String getProtocol() {
		return protocol;
	}


	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}


	public String toString() {
		return protocol + ";" + source + ";" + destination;
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
	
	/**
	 * Reads an instance of AdhocData from a serialized byte stream.
	 * @param bais
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public EthernetHeader readFromBytes(byte[] bytes) 
					throws IOException,	ClassNotFoundException {
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		ObjectInputStream ois = new ObjectInputStream(bais);
		EthernetHeader data = (EthernetHeader)ois.readObject();

		// For development/debug 
		Log.d(TAG, "protocol = " + data.protocol);
		Log.d(TAG, "source = " + data.source);
		Log.d(TAG, "destination = " + data.destination);
		return data;
	}
}
