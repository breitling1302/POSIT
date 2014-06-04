package org.hfoss.posit.rwg;

public interface Constants {
	
	public static final String RWG_PROTOCOL = "RWG";
	
	// RWG Protocol Data Unit (PDUs) types
	public static final byte REQF = 1;
	public static final byte ACK = 2;
	public static final byte OKTF = 3;
	public static final byte BS = 4;
	
	// RWG Parameters (Eventually Should be made preference settings)
	public static final long TTL = 600 * 1000;  // 600 seconds

	public static final short GROUP_SIZE = 3;  // The k in k-delivered
	public static final int BIT_VECTOR_SIZE = 256;
	public static final int HOPS = 7;
	
	// Timer Parameters
	public static final int REQF_REFRESH_TIMER_INTERVAL = 7000;   // millisecs = 7 seconds
	public static final int WAIT_BUFFER_TIMER_INTERVAL = 100;     // 0.1 seconds
	public static final int SILENT_TIMER_INTERVAL = 5000;
	public static final int MAIN_THREAD_SLEEP_INTERVAL = 5000;
	
	//Broadcast ID
	public static final int MAX_BROADCAST_ID = Integer.MAX_VALUE;
	public static final int FIRST_BROADCAST_ID = 0;
	
	//Sequence Numbers
	public static final int MAX_SEQUENCE_NUMBER = Short.MAX_VALUE;
    public static final int INVALID_SEQUENCE_NUMBER = -1;
    public static final int UNKNOWN_SEQUENCE_NUMBER = 0;
    public static final int FIRST_SEQUENCE_NUMBER = 1;
    public static final int SEQUENCE_NUMBER_INTERVAL = (Short.MAX_VALUE / 2);
	
	// user package max size equivalent 54kb
	public static final int MAX_PACKAGE_SIZE = 54000;
	
}
