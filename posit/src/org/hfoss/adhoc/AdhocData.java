/*
 * File: AdhocData.java
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import android.content.Context;
import android.util.Log;

/**
 * The payload for an RWG packet.
 *
 * @param <T>, for POSIT T will usually be an AdhocFind.
 */
public class AdhocData<T> implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final String PROTOCOL_RWG = "RWG";
	private static final String TAG = "Adhoc";
	
	private T message;

	public AdhocData() {
		message = (T)"Hello";
	}
	
	
	public AdhocData(Context cxt, T msg) {
		this();
		message = (T)msg;
		String mac = AdhocService.getMacAddress(cxt);
	}
	
	public AdhocData(T msg) {
		message = msg;
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
	public static AdhocData readFromBytes(byte[] bytes) 
					throws IOException,	ClassNotFoundException {
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		ObjectInputStream ois = new ObjectInputStream(bais);
		AdhocData data = (AdhocData)ois.readObject();

		// For development/debug 
		Log.d(TAG, "message = " + data.message);
		return data;
	}	

	public T getMessage() {
		return message;
	}

	public void setMessage(T message) {
		this.message = message;
	}

	@Override
	public String toString() {
		String result = "null";
		try {
			result = String.format("%s %s",
					message.toString().getBytes().length, 
					message.toString());
		} catch (NullPointerException ne) {
			Log.e(TAG,
					"NullPointerException when creating string for AdhocData "
							+ ne.getMessage());
			ne.printStackTrace();
		}
		return result;
	}
}
