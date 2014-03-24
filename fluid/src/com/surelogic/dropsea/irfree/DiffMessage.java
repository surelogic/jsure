package com.surelogic.dropsea.irfree;

import com.surelogic.dropsea.IDrop;

// Used for the oracle diff view
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
	
	@Override
  public IDrop getDrop() {
		return null;
	}
	
//	@Override
	@Override
  public String getText() {
		return text;
	}
}
