package edu.cmu.cs.fluid.sea;

/**
 * Drops for reporting warnings
 * NOT for scrubber warnings -- use PromiseWarningDrop instead
 */
public class WarningDrop extends InfoDrop {
	public WarningDrop(String t) {
		super(t);
	}
}