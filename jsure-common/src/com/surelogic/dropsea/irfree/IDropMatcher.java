package com.surelogic.dropsea.irfree;

import com.surelogic.dropsea.IDrop;

public interface IDropMatcher {
	String getLabel();
	boolean useHashing();
	int hash(IDrop n);
	boolean match(IDrop n, IDrop o);
	boolean warnIfMatched();
}
