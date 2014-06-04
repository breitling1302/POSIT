package org.hfoss.posit.rwg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.hfoss.adhoc.AdhocData;
import org.hfoss.adhoc.AdhocFind;

import android.util.Log;

public class RwgPacket implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String TAG = "Adhoc";
	private EthernetHeader ethrHeader;
	private RwgHeader rwgHeader;
	private AdhocData<AdhocFind> adhocData;
	
	private String destAddress;  // Mac Addresses
	private byte pduType;
	private String sourceAddress;
	private int packetID;

	public RwgPacket(){}

	public RwgPacket(EthernetHeader ethrHeader, RwgHeader hdr, AdhocData<AdhocFind> data) {
		this.ethrHeader = ethrHeader;
		rwgHeader = hdr;
		adhocData = data;
		pduType = hdr.getMessageType();
		destAddress = hdr.getTarget();
		sourceAddress = hdr.getSender();
	}
	
	public RwgHeader getRwgHeader() {
		return rwgHeader;
	}

	public void setRwgHeader(RwgHeader rwgHeader) {
		this.rwgHeader = rwgHeader;
	}

	public AdhocData<AdhocFind> getAdhocData(){
		return adhocData;
	}

	public byte[] getData(){
		try {
			return adhocData.writeToBytes();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
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
	public static RwgPacket readFromBytes(byte[] bytes) 
					throws IOException,	ClassNotFoundException {
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		ObjectInputStream ois = new ObjectInputStream(bais);
		RwgPacket data = (RwgPacket)ois.readObject();

		// For development/debug 
		Log.d(TAG, "RwgPacket.EthrHeader= " + data.ethrHeader);
		Log.d(TAG, "RwgPacket.RwgHeader = " + data.rwgHeader);
		Log.d(TAG, "RwgPacket.AdhocData = " + data.adhocData);

		return data;
	}	

	public EthernetHeader getEthernetHeader() {
		return ethrHeader;
	}
	
	public String getSourceNodeAddress(){
		return sourceAddress;
	}

	public String getDestinationAddress() {
		return destAddress;
	}

	public byte[] toBytes() {
		return toString().getBytes();
	}

	@Override
	public String toString(){
		String result = "";
		String type = "";
		switch (pduType) {
		case Constants.REQF:
			type = "REQF";
			break;
		case Constants.ACK:
			type = "ACK";
			break;
		case Constants.BS:
			type = "BS";
			break;
		case Constants.OKTF:
			type = "OKTF";
			break;
		default:
			type = "UNK";
			break;
		}
		result = type+";"+sourceAddress+";"+destAddress+";";
		if (adhocData == null)  // Could be null in non-REQF packets
			result += "null";
		else 
			result += adhocData.toString();
		return result;
	}

	public int getPacketID() {
		return packetID;
	}

}
