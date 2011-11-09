package edu.cmu.cs.fluid.sea;

/**
 * Drops for reporting warnings
 * NOT for scrubber warnings -- use PromiseWarningDrop instead
 */
public class WarningDrop extends InfoDrop {
	public WarningDrop(String t) {
		super(t);
	}
	
	public static final Factory factory = new Factory() {
		public InfoDrop create(String type) {
			return new WarningDrop(type);
		}
	};
}