package org.hfoss.posit.rwg;

public class BadPduFormatException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public BadPduFormatException(){
		
	}
	
	public BadPduFormatException(String message){
		super(message);
	}

}
