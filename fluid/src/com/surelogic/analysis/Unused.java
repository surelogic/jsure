package com.surelogic.analysis;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * A placeholder for use with ConcurrentAnalysis type arguments
 * @author Edwin
 */
public final class Unused implements ICompUnitContext {
	private Unused() {}

	public IRNode getCompUnit() {
		return null;
	}
}
