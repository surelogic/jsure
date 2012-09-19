package com.surelogic.dropsea.irfree;

import com.surelogic.dropsea.IDrop;

public abstract class DropMatcher {
	private final String[] labels;
	
	protected DropMatcher(String... labels) {
		if (labels == null) {
			throw new IllegalArgumentException("Null labels");
		}
		for(String l : labels) {
			if (l == null) {
				throw new IllegalArgumentException("Null label");
			}
			if (l.length() != 7) {
				throw new IllegalArgumentException("Incorrect length: "+l);
			}
		}
		this.labels = labels;
	}
	
	final int numPasses() {
		return labels.length;
	}
	
	/**
	 * @return A String of length 7
	 */
	final String getLabel(int pass) {
		return labels[pass];
	}
	
	protected abstract boolean match(int pass, IDrop n, IDrop o);
}
