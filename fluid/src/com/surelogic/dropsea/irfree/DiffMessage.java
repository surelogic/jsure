package com.surelogic.dropsea.irfree;

public final class DiffMessage extends AbstractDiffNode {
	private final String text;
	
	public DiffMessage(String msg) {
		this(msg, null);
	}
	
	DiffMessage(String msg, Status s) {
		text = msg;
		status = s;
	}
	
	@Override
	public String getText() {
		return text;
	}
}
