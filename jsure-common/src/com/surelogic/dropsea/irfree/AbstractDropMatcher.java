package com.surelogic.dropsea.irfree;

import com.surelogic.dropsea.IDrop;

public abstract class AbstractDropMatcher implements IDropMatcher {
	private final String name;
	private final boolean warnIfMatched;
	private final boolean useHashing;
	
	protected AbstractDropMatcher(String l, boolean warn, boolean hash) {
		name = l;
		warnIfMatched = warn;
		useHashing = hash;
	}

	@Override
  public final String getLabel() {
		return name;
	}
	
	@Override
  public final boolean warnIfMatched() {
		return warnIfMatched;
	}
	
	public boolean useHashing() {
		return useHashing;
	}
	
	public int hash(IDrop d) {
		return 0;
	}
}
