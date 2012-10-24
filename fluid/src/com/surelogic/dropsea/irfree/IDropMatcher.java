package com.surelogic.dropsea.irfree;

import com.surelogic.dropsea.IDrop;

public interface IDropMatcher {
	String getLabel();
	boolean match(IDrop n, IDrop o);
	boolean warnIfMatched();
}
