package com.surelogic.dropsea.irfree;

import com.surelogic.dropsea.IDrop;

public class DiffMessage extends AbstractDiffNode {
	private final String text;
	
	public DiffMessage(String msg) {
		this(msg, Status.N_A);
	}
	
	DiffMessage(String msg, Status s) {
		if (s == null) {
			throw new IllegalArgumentException();
		}
		text = msg;
		status = s;
	}
	
	public IDrop getDrop() {
		return null;
	}
	
//	@Override
	public String getText() {
		return text;
	}
}
