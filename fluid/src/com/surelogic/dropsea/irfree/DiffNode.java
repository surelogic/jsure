package com.surelogic.dropsea.irfree;

import com.surelogic.dropsea.IDrop;

/**
 * Encapsulating a drop 
 * 
 * @author Edwin
 */
public class DiffNode extends AbstractDiffNode {
	final IDrop drop;
	
	DiffNode(IDrop d) {
		if (d == null) {
			throw new IllegalArgumentException();
		}
		drop = d;
	}

	@Override
	public String getText() {
		return drop.getMessage();
	}
}
