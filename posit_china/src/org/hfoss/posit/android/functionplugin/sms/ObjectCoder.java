/*
 * File: ObjectCoder.java
 * 
 * Copyright (C) 2009 The Humanitarian FOSS Project (http://www.hfoss.org)
 * 
 * This file is part of POSIT, Portable Open Souce Information Tool.
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

package org.hfoss.posit.android.functionplugin.sms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Scanner;

import android.util.Base64;

/**
 * This class contains a collection of static functions that are used to encode
 * or decode objects as Strings for the purpose of SMS transmission. Of
 * particular note are the functions encode() and decode() which will encode or
 * decode objects of any supported type. Currently, all Serializable objects are
 * supported, but the Serialization of some objects could be very lengthy.
 * 
 * @author Ryan McLeod
 * 
 */
abstract public class ObjectCoder {
	public static final String TAG = "SmsObjectCoder";
	public static final char[] RESERVED_CHARS = { '\\', ',', '!' };
	public static final char ESCAPE_CHAR = '\\';
	public static final char NULL_CHAR = '!';
	public static final Class[] TOSTRING_TYPES = { Byte.class, Byte[].class,
			Integer.class, Float.class, Short.class, String.class,
			Double.class, Long.class, Boolean.class };
	private static final String CHAR_SET = "utf-8";

	/**
	 * Determines whether a particular object is of a type for which it makes
	 * sense to use toString() to encode it. For some objects, toString() may be
	 * longer than we want, or may not encapsulate all the data needed to
	 * rebuild the object.
	 * 
	 * @param o
	 *            The object to be checked
	 * @return True if the object is one of the toString() appropriate types,
	 *         false otherwise.
	 */
	private static Boolean isToStringType(Object o) {
		for (Class c : TOSTRING_TYPES) {
			if (c.isInstance(o)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Encodes a Serializable object as a string.
	 * 
	 * @param o
	 *            The object to be encoded.
	 * @return The string encoding of the object.
	 * @throws IOException
	 */
	private static String serToString(Serializable o) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(o);
		oos.close();
		return new String(Base64.encode(baos.toByteArray(), Base64.NO_PADDING
				| Base64.NO_WRAP), CHAR_SET);
	}

	/**
	 * Decodes a string into a Serializable object.
	 * 
	 * @param s
	 *            A string encoding a Serializable object.
	 * @return An Object.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private static Object serFromString(String s) throws IOException,
			ClassNotFoundException {
		byte[] data = Base64.decode(s.getBytes(CHAR_SET), Base64.NO_PADDING
				| Base64.NO_WRAP);
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(
				data));
		Object o = ois.readObject();
		ois.close();
		return o;
	}

	/**
	 * Specialized toString function for date. (Date.toString() has
	 * unnecessarily long output and may be location specific)
	 * 
	 * @param date
	 *            A java.util.Date object
	 * @return a String encoding of the date
	 */
	// private static String dateToString(Date date) {
	// String output = date.toGMTString();
	// output = output.substring(0,output.length() - 4);
	// return date.toGMTString();
	// }
	//	
	// /**
	// * Specialized decoding function for date.
	// */
	// private static String dateFromString(String code) {
	// Date date = new Date();
	// Scanner dateScanner = new Scanner(code);
	// if (!dateScanner.hasNextInt()) return null;
	// date.setDate(dateScanner.nextInt());
	// if (!dateScanner.hasNext()) return null;
	// date.setMonth(month)
	//		
	// }

	/**
	 * Takes a particular object and outputs its String encoding for SMS. Simply
	 * calling toString() on the object is insufficient, since such a String may
	 * contain reserved characters we will later use to parse the message. We
	 * must escape these characters. We may also want to use our own encoding
	 * methods for special cases (for instance, in order to output a shorter
	 * representation.)
	 * 
	 * @param val
	 *            The object that needs to be encoded.
	 * @return The encoding of the object, or null if the object couldn't be
	 *         encoded.
	 */
	static final public String encode(Object val) {
		if (val == null) {
			return String.valueOf(NULL_CHAR);
		}

		String code;
		if (isToStringType(val)) {
			// Currently, all of the supported types are serializable, but the
			// theory is that toString() is more human readable (and usually
			// shorter), so we use it whenever it makes sense to do so.
			code = val.toString();
			// } else if (val instanceof Date) {
			// code = dateToString((Date) val);
		} else if (val instanceof Serializable) {
			// If all else fails, we may be able to serialize it as a string.
			try {
				code = serToString((Serializable) val);
			} catch (IOException e) {
				// Probably not actually serializable, encoding fails.
				return null;
			}
		} else {
			// Not a legal type, encoding fails.
			return null;
		}
		// Escape reserved characters
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < code.length(); i++) {
			char c = code.charAt(i);
			Boolean reserved = false;
			for (char d : RESERVED_CHARS) {
				if (c == d) {
					reserved = true;
					break;
				}
			}
			// The escape character itself must also be escaped
			if (c == ESCAPE_CHAR)
				reserved = true;
			if (reserved) {
				builder.append("\\" + c);
			} else {
				builder.append(c);
			}
		}
		code = builder.toString();
		return code;
	}

	/**
	 * Helper function that checks a type to see if it implements the
	 * Serializable interface. (Note: just because this returns true doesn't
	 * mean it will actually serialize at run-time)
	 * 
	 * @param type
	 *            The type to be checked.
	 * @return True if the class implements Serializable, false otherwise.
	 */
	static final private Boolean isSerializable(Class<Object> type) {
		Class[] interfaces = type.getInterfaces();
		for (Class c : interfaces) {
			if (c == Serializable.class) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Helper function for decode(). Contains a bunch of type-specific code for
	 * decoding objects.
	 * 
	 * @return Object if we could decode it, null if it is not one of our types.
	 */
	static final private Object decodeTypeSpec(String code, Class<Object> type)
			throws NumberFormatException {
		Object obj;
		if (type.equals(Byte.class)) {
			obj = Byte.valueOf(code);
		} else if (type.equals(byte.class)) {
			obj = (byte) Byte.valueOf(code);
		} else if (type.equals(int.class)) {
			obj = (int) Integer.valueOf(code);
		} else if (type.equals(Integer.class)) {
			obj = Integer.valueOf(code);
		} else if (type.equals(float.class)) {
			obj = (float) Float.valueOf(code);
		} else if (type.equals(Float.class)) {
			obj = Float.valueOf(code);
		} else if (type.equals(short.class)) {
			obj = (short) Short.valueOf(code);
		} else if (type.equals(Short.class)) {
			obj = Short.valueOf(code);
		} else if (type.equals(Byte[].class)) {
			byte[] ba;
			try {
				ba = code.getBytes(CHAR_SET);
			} catch (UnsupportedEncodingException e) {
				// Shouldn't happen
				e.printStackTrace();
				return null;
			}
			Byte[] byteArray = new Byte[ba.length];
			for (int i = 0; i < ba.length; i++) {
				byteArray[i] = new Byte(ba[i]);
			}
			obj = byteArray;
		} else if (type.equals(byte[].class)) {
			try {
				obj = code.getBytes(CHAR_SET);
			} catch (UnsupportedEncodingException e) {
				// Shouldn't happen
				e.printStackTrace();
				return null;
			}
		} else if (type.equals(String.class)) {
			obj = code;
		} else if (type.equals(double.class)) {
			obj = (double) Double.valueOf(code);
		} else if (type.equals(Double.class)) {
			obj = Double.valueOf(code);
		} else if (type.equals(long.class)) {
			obj = (long) Long.valueOf(code);
		} else if (type.equals(Long.class)) {
			obj = Long.valueOf(code);
		} else if (type.equals(boolean.class)) {
			if (code.toUpperCase().equals("TRUE")) {
				obj = (boolean) true;
			} else if (code.toUpperCase().equals("FALSE")) {
				obj = (boolean) false;
			} else {
				throw new NumberFormatException();
			}
		} else if (type.equals(Boolean.class)) {
			if (code.toUpperCase().equals("TRUE")) {
				obj = (Boolean) true;
			} else if (code.toUpperCase().equals("FALSE")) {
				obj = (Boolean) false;
			} else {
				throw new NumberFormatException();
			}
		} else {
			obj = null;
		}
		return obj;
	}

	/**
	 * Attempts to decode an Object from a string.
	 * 
	 * @param code
	 *            The string encoding of the object.
	 * @param type
	 *            The type of object to be decoded.
	 * @return An Object from the decoded String.
	 * @throws IllegalArgumentException
	 *             if the object could not be decoded.
	 */
	static final public Object decode(String code, Class<Object> type)
			throws IllegalArgumentException {
		if (code.equals(String.valueOf(NULL_CHAR)))
			return null;
		// Un-escape characters
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < code.length(); i++) {
			char c = code.charAt(i);
			if (c == ESCAPE_CHAR) {
				// Skip this character and add next character (if it exists)
				i++;
				if (i < code.length())
					c = code.charAt(++i);
			}
			builder.append(c);
		}
		code = builder.toString();
		// Now actually decode the object
		Object obj = null;
		try {
			// Do a type specific decoding
			obj = decodeTypeSpec(code, type);
			if (obj == null) {
				// If all else fails, try to de-serialize it
				if (isSerializable(type)) {
					try {
						obj = serFromString(code);
					} catch (IOException e) {
						// Probably not actually serializable
						throw new IllegalArgumentException();
					} catch (ClassNotFoundException e) {
						// Probably not actually serializable
						throw new IllegalArgumentException();
					}
				} else {
					// Unsupported type
					throw new IllegalArgumentException();
				}
			}
		} catch (NumberFormatException e) {
			// String doesn't match the type it's supposed to be
			throw new IllegalArgumentException();
		}
		return obj;
	}
}
