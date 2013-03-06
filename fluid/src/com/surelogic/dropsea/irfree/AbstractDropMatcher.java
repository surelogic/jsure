package com.surelogic.dropsea.irfree;

public abstract class AbstractDropMatcher implements IDropMatcher {
	private final String name;
	private final boolean warnIfMatched;
	
	protected AbstractDropMatcher(String l, boolean warn) {
		name = l;
		warnIfMatched = warn;
	}

	@Override
  public final String getLabel() {
		return name;
	}
	
	@Override
  public final boolean warnIfMatched() {
		return warnIfMatched;
	}
}
