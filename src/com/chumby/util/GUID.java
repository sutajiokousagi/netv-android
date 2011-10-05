package com.chumby.util;

import java.util.Date;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Manages and generates Global Unique Identifiers
 * @author dmaxwell
 *
 */
public class GUID {
	/**
	 * Computes the MD5 hash of a {@link String}
	 * @param s {@link String} from which to compute the hash
	 * @return {@link String} the hash represented in hexadecimal
	 */
	public static String md5(String s) {
		String result = null;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(s.getBytes());
			result = toHex(md.digest());
		} catch (NoSuchAlgorithmException ex) {

		}
		return result;
	}
	
	/**
	 * Converts a byte array to hexadecimal
	 * @param a byte array to convert
	 * @return {@link String} of hexadecimal digits
	 */
	private static String toHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (int i = 0; i < a.length; i++) {
            sb.append(Character.forDigit((a[i] & 0xf0) >> 4, 16));
            sb.append(Character.forDigit(a[i] & 0x0f, 16));
        }
        return sb.toString();
    }

	/**
	 * Computes a GUID from a {@link String}
	 * @param s {@link String} to convert
	 * @return {@link String} the GUID as a formatted hexadecimal String
	 */
	public static String guidOf(String s) {
		return GUID.asGUID(GUID.md5(s));
	}

	/**
	 * Formats a 32-character {@link String} as a GUID
	 * @param s {@link String} the hexadecimal String
	 * @return {@link String} the formatted GUID
	 */
	public static String asGUID(String s) {
		return s.substring(0,8)+"-"+s.substring(8,12)+"-"+s.substring(12,16)+"-"+s.substring(16,20)+"-"+s.substring(20);
	}

	/**
	 * Generate a GUID based on the system time, with millisecond resolution
	 * @return {@link String} a new GUID
	 */
	public static String generate() {
		return GUID.guidOf(Long.toString((new Date()).getTime()));
	}
}
