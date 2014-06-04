package org.hfoss.posit.rwg;

import org.hfoss.adhoc.AdhocData;
import org.hfoss.adhoc.AdhocFind;


/**
 * Stores a collection of buffers and waiting lists used to manage the 
 * RWG protocol.
 * @author rmorelli
 *
 */
public class RwgPacketBuffer {
	
	public static final String TAG = "Adhoc";
	public static final int BUFFER_SIZE = 128;

	// Counters
	private int wake_counter = 0;
	private int ack_counter = 0;
	private int reqf_counter = 0;
	private int temp_reqf_counter = 0;

	// Arrays
	private ReqForwardInfo wake[]; // keeps track of REFQs that should be sent (because of wake)
	private RwgHeader ack[];  // keeps track of incoming ACKs
	private ReqForwardInfo reqf[];
	private ReqForwardInfo temp_reqf[];
	private ReqForwardInfo waiting[];
	private int wFront = 0;
	private int wTail = 0;
	
	private ActiveReqForwardInfo active_reqf;
	     
	public RwgPacketBuffer() {
		wake = new ReqForwardInfo[BUFFER_SIZE];
		ack = new RwgHeader[BUFFER_SIZE];
		waiting = new ReqForwardInfo[BUFFER_SIZE];
		reqf = new ReqForwardInfo[BUFFER_SIZE];
		temp_reqf = new ReqForwardInfo[BUFFER_SIZE];
		
		reqf_counter = 0;
		temp_reqf_counter = 0;
		ack_counter = 0;
		wake_counter = 0;
		wFront = 0;
		wTail = 0;

		active_reqf = new ActiveReqForwardInfo();
		
		wake = new ReqForwardInfo[BUFFER_SIZE]; // keeps track of REFQs that should be sent (because of wake)
		reqf = new ReqForwardInfo[BUFFER_SIZE];
		temp_reqf = new ReqForwardInfo[BUFFER_SIZE];
		for (int k = 0; k < wake.length; k++) {
			wake[k] = new ReqForwardInfo();
			reqf[k] = new ReqForwardInfo();
			temp_reqf[k] = new ReqForwardInfo();
		}
		ack = new RwgHeader[BUFFER_SIZE];  // keeps track of incoming ACKs
		waiting = new ReqForwardInfo[BUFFER_SIZE];
		for (int k = 0; k < ack.length; k++) {
			ack[k] = new RwgHeader();
			waiting[k] = new ReqForwardInfo();
		}
	}
	
	
	
	
	public int getTemp_reqf_counter() {
		return temp_reqf_counter;
	}




	public void setTemp_reqf_counter(int temp_reqf_counter) {
		this.temp_reqf_counter = temp_reqf_counter;
	}




	public ReqForwardInfo[] getTemp_reqf() {
		return temp_reqf;
	}




	public void setTemp_reqf(ReqForwardInfo[] temp_reqf) {
		this.temp_reqf = temp_reqf;
	}




	public int getWake_counter() {
		return wake_counter;
	}




	public void setWake_counter(int wake_counter) {
		this.wake_counter = wake_counter;
	}




	public ReqForwardInfo[] getWake() {
		return wake;
	}




	public void setWake(ReqForwardInfo[] wake) {
		this.wake = wake;
	}




	public int getWFront() {
		return wFront;
	}

	public void setWFront(int front) {
		wFront = front;
	}

	public int getWTail() {
		return wTail;
	}

	public void setWTail(int tail) {
		wTail = tail;
	}

	public int getReqf_counter() {
		return reqf_counter;
	}

	public void setReqf_counter(int reqf_counter) {
		this.reqf_counter = reqf_counter;
	}

	public ReqForwardInfo[] getReqf() {
		return reqf;
	}

	public void setReqf(ReqForwardInfo[] reqf) {
		this.reqf = reqf;
	}

	public ActiveReqForwardInfo getActive_reqf() {
		return active_reqf;
	}

	public void setActive_reqf(ActiveReqForwardInfo active_reqf) {
		this.active_reqf = active_reqf;
	}

	public ReqForwardInfo[] getWaiting() {
		return waiting;
	}

	public void setWaiting(ReqForwardInfo[] waiting) {
		this.waiting = waiting;
	}

	public int getAck_counter() {
		return ack_counter;
	}

	public void setAck_counter(int ack_counter) {
		this.ack_counter = ack_counter;
	}

	public RwgHeader[] getAck() {
		return ack;
	}

	public void setAck(RwgHeader[] ack) {
		this.ack = ack;
	}

	public String toString() {
		return "wc=" + wake_counter + "|" 
		+ "acks=" + ack_counter + "|"
		+ "rqfs=" + reqf_counter + "|"
		+ "trqfs=" + temp_reqf_counter + "|"
		+ "wF=" + wFront + "|"
		+ "wT=" + wTail;

	}

	class ActiveReqForwardInfo {
		private RwgHeader reqf;
		private AdhocData<AdhocFind> userData;		
		private int reqf_pos;
		
		public ActiveReqForwardInfo() {
			reqf = new RwgHeader();
			reqf_pos = 0;
		}
		
		public AdhocData<AdhocFind> getUserData() {
			return userData;
		}

		public void setUserData(AdhocData<AdhocFind> userData) {
			this.userData = userData;
		}
		
		public RwgHeader getReqf() {
			return reqf;
		}
		public void setReqf(RwgHeader reqf) {
			this.reqf = reqf;
		}
		public int getReqf_pos() {
			return reqf_pos;
		}
		public void setReqf_pos(int reqf_pos) {
			this.reqf_pos = reqf_pos;
		}

	}
	  
		/**
		 * Class for request forward information
		 */
		class ReqForwardInfo {
			//private String reqf;
			private RwgHeader reqf;
			private long arrivedAt;
			private long wStamp;
			private  int wake; // 1 if reqf is in wake buffer
			private  int wait; // 1 if reqf is waiting on ACK
			private  int wake_pos;
			private  int wait_pos;
			private  int reqf_pos;
			
			public ReqForwardInfo() {
				reqf = new RwgHeader();		
				arrivedAt = System.currentTimeMillis();
				wStamp = System.currentTimeMillis();
			}
			
			public RwgHeader getReqf() {
				return reqf;
			}

			public void setReqf(RwgHeader reqf) {
				this.reqf = reqf;
			}

			public long getArrivedAt() {
				return arrivedAt;
			}

			public void setArrivedAt(long arrivedAt) {
				this.arrivedAt = arrivedAt;
			}

			public long getWStamp() {
				return wStamp;
			}

			public void setWStamp(long stamp) {
				wStamp = stamp;
			}
			public int getWake() {
				return wake;
			}
			public void setWake(int wake) {
				this.wake = wake;
			}
			public int getWait() {
				return wait;
			}
			public void setWait(int wait) {
				this.wait = wait;
			}
			public int getWake_pos() {
				return wake_pos;
			}
			public void setWake_pos(int wake_pos) {
				this.wake_pos = wake_pos;
			}
			public int getWait_pos() {
				return wait_pos;
			}
			public void setWait_pos(int wait_pos) {
				this.wait_pos = wait_pos;
			}
			public int getReqf_pos() {
				return reqf_pos;
			}
			public void setReqf_pos(int reqf_pos) {
				this.reqf_pos = reqf_pos;
			}
		}
			  
		/**
		 * Timestamp class	 
		 */
		class TimeStamp{
			  private long seconds;
			  private long u_seconds;
			public long getSeconds() {
				return seconds;
			}
			public void setSeconds(long seconds) {
				this.seconds = seconds;
			}
			public long getU_seconds() {
				return u_seconds;
			}
			public void setU_seconds(long u_seconds) {
				this.u_seconds = u_seconds;
			}
		}
		
	  
}
