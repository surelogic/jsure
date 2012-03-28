package com.surelogic.annotation.parse;

import com.surelogic.parse.TreeToken;

/**
 * Indicates an illegal special token
 * 
 * @author Edwin
 */
public class BadTokenException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	final int location;
	
	public BadTokenException(TreeToken tt, String msg) {
		super(msg);
		location = tt.getCharPositionInLine();
	}
	
	public int getCharLocation() {
		return location;
	}
}
