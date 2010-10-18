package edu.cmu.cs.fluid.sea;

/**
 * Drops for reporting warnings
 */
public class WarningDrop extends InfoDrop {
	public WarningDrop(String t) {
		super(t);
	}
	@Deprecated
	public WarningDrop() {
		this(null);
	}
}