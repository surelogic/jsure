/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea.drops.layers;

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

	public boolean isPartOf(IRNode type) {
		return getAAST().check(type);
	}
	public int getResultMessageKind() {
		throw new UnsupportedOperationException();
	}
	public Object[] getArgs(IRNode binding, IRNode type, IRNode context) {
		throw new UnsupportedOperationException();
	}
}
