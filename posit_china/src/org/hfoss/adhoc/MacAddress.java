package org.hfoss.adhoc;

import java.io.Serializable;

/**
 * Convenience class for storing MacAddresses and getting byte arrays and String
 * representations
 * 
 * @author pgautam
 * 
 */
public class MacAddress implements Serializable {
	// ONLY use ZERO_MAC for debugging/ can cause confusion
	public static final String ZERO_MAC = "00:00:00:00:00:00";
	private String macAddress = null;

	public MacAddress(String mac) {

		macAddress = mac;
	}

	/**
	 * gets the unsignedbytes and converts to hex string format that's saved
	 * 
	 * @param unsignedBytes
	 */

	private long unsignedByteToLong(byte b) {
	    return (long) b & 0xFF;
	}
	

	/**
	 * gets the long value from byte array
	 * @param addr
	 */
	private long byte2Long(byte addr[]) {
	    long address = 0;
		if (addr != null) {
		    if (addr.length == 6) {
			address = unsignedByteToLong(addr[5]);
			address |= (unsignedByteToLong(addr[4]) << 8);
			address |= (unsignedByteToLong(addr[3]) << 16);
			address |= (unsignedByteToLong(addr[2]) << 24);
			address |= (unsignedByteToLong(addr[1]) << 32);
			address |= (unsignedByteToLong(addr[0]) << 40);
		    } 
		} 
		return address;
	}	
	

	private String bytesToString(byte[] bytes,char ch){
		StringBuffer sb = new StringBuffer( 17 );
		for ( int i=44; i>=0; i-=4 ) {
			int nibble =  ((int)( byte2Long(bytes) >>> i )) & 0xf;
			char nibbleChar = (char)( nibble > 9 ? nibble + ('A'-10) : nibble + '0' );
			sb.append( nibbleChar );
			if ( (i & 0x7) == 0 && i != 0 ) {
				sb.append( ch );
			}
		}
		return sb.toString();	
	}
	
	public MacAddress(byte[] bytes) {
		if (bytes.length < 6) {
			throw new UnsupportedOperationException(
					"MacAddresses are 6 bytes long. Double check your input!");
		}

//		Log.i(TAG, " BigInteger string = " + s);

		macAddress = "";
		for (int i = 0; i < 6; i++){
			macAddress += Integer.toString( ( bytes[i] & 0xff ) + 0x100, 16).substring( 1 );
			if (i < 5) {
				macAddress += ":";
			}
		}
	}

	public MacAddress() {
		macAddress = ZERO_MAC;
	}

	/**
	 * convert to byteArray as required by functions
	 * 
	 * @return
	 */
	 public byte[] tosignedByteArray() {
	 if (macAddress == null) {
	 throw new NullPointerException("No MacAddress Set");
	 }
	 String[] macAddr = macAddress.toUpperCase().split(":");
	 byte[] ub = new byte[6];
	 for (int i = 0; i < macAddr.length; i++) {
	 char[] chars = macAddr[i].toCharArray();
	 int c = 0;
	 c = (int) (Character.isDigit(chars[0]) ? (chars[0] - '0')
	 : (chars[0] - 'A' + 10));
	 c <<= 4; // left shift by 4 bits a.k.a multiply by 16
	
	 c += (int) (Character.isDigit(chars[1]) ? (chars[1] - '0')
	 : (chars[1] - 'A' + 10));
	
	 ub[i] = (byte)c;
	 }
	 return ub;
	 }

	@Override
	public boolean equals(Object o) {
		return o.toString().equals(macAddress);
	}

	/**
	 * returns the String representation of bytes
	 */
	@Override
	public String toString() {
		return macAddress;
	}

}
