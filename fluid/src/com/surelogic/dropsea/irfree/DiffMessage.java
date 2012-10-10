package com.surelogic.dropsea.irfree;

import com.surelogic.dropsea.IDrop;

public class DiffMessage extends AbstractDiffNode {
	private final String text;
	
	public DiffMessage(String msg) {
		this(msg, null);
	}
	
	DiffMessage(String msg, Status s) {
		text = msg;
		status = s;
	}
	
	public IDrop getDrop() {
		return null;
	}
	
	@Override
	public String getText() {
		return text;
	}
}
