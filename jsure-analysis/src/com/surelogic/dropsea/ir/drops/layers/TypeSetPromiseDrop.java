/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.dropsea.ir.drops.layers;

import com.surelogic.aast.layers.*;

import edu.cmu.cs.fluid.ir.IRNode;

public class TypeSetPromiseDrop extends AbstractReferenceCheckDrop<TypeSetNode> {
	public TypeSetPromiseDrop(TypeSetNode a) {
		super(a);
	}
	@Override
	public boolean isIntendedToBeCheckedByAnalysis() {
		return false;
	}	
	public String getId() {
		return getAAST().getId();
	}

	@Override
  public boolean isPartOf(IRNode type) {
		return getAAST().check(type);
	}
	@Override
  public int getResultMessageKind() {
		throw new UnsupportedOperationException();
	}
	@Override
  public Object[] getArgs(IRNode binding, IRNode type, IRNode context) {
		throw new UnsupportedOperationException();
	}
}
