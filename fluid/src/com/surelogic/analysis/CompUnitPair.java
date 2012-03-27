package com.surelogic.analysis;

import edu.cmu.cs.fluid.ir.IRNode;

public class CompUnitPair implements ICompUnitContext {
	final IRNode compUnit, node;
	
	public CompUnitPair(IRNode cu, IRNode n) {
		compUnit = cu;
		node = n;
	}
	
	public IRNode getCompUnit() {
		return compUnit;
	}
	public IRNode getNode() {
		return node;
	}
}
