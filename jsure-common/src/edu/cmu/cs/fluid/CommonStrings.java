/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/CommonStrings.java,v 1.5 2008/09/05 15:57:43 chance Exp $*/
package edu.cmu.cs.fluid;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.surelogic.common.SLUtility;

public final class CommonStrings {
	public static final String MINUS_ONE = "-1";
	public static final String ZERO = "0";
	public static final String ONE  = "1";
	public static final String TWO  = "2";
	public static final String THREE = "3";
	private static Map<String,String> pool = new ConcurrentHashMap<String,String>(8);
	static {
		String[] strings = {
			ZERO, ONE, SLUtility.JAVA_LANG_OBJECT, "java.lang.String", "String", "i",
			"T", "add", "size", "equals", "toString", "java.io.IOException",
			"Object", "isEmpty", "get", "IllegalArgumentException", "System",
			"StringBuilder", "next", "hasNext", "Boolean", "Integer", "put",
			"iterator"
		};
		for(String s : strings) {
			pool.put(s, s);
		}
	}
	/**
	 * Check if the String is already in the pool,
	 * and if so, reuse it
	 */
	public static String pool(String value) {
		String pooled = pool.get(value);
		return pooled == null ? value : pooled;
	}
	
	public static boolean interned(String value) {
		return pool.containsKey(value);
	}

	/**
	 * Add the String to the pool if not already in it
	 */
	public static String intern(String value) {
		if (value == null) {
			return null;
		}
		String pooled = pool.get(value);
		if (pooled == null) {
			pooled = value;
			pool.put(value, value);					
		}
		return pooled;
	}	
	public static String valueOf(int i) {
		switch (i) {
		case -1: 
			return MINUS_ONE;
		case 0:
			return ZERO;
		case 1:
			return ONE;
		case 2:
			return TWO;
		case 3:
			return THREE;
		default:
			return Integer.toString(i);	
		}
	}
}

