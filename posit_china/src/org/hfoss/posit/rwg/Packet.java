package org.hfoss.posit.rwg;

public interface Packet {
		
	public byte[] toBytes();
	
	public String toString();
	
	public String getDestinationAddress(); // MAC Address
}
