/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea.drops.layers;

import com.surelogic.aast.layers.MayReferToNode;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.PromiseDrop;

public class MayReferToPromiseDrop extends PromiseDrop<MayReferToNode> implements IReferenceCheckDrop {
	public MayReferToPromiseDrop(MayReferToNode a) {
		super(a);
	}
	@Override
	protected void computeBasedOnAST() {
		setMessage(getAST().toString());
	}
	public boolean check(IRNode type) {
		return getAST().check(type);
	}
	public int getResultMessageKind() {
		return 350;
	}
	public Object[] getArgs(IRNode binding, IRNode type, IRNode context) {
		return new Object[] { binding, VisitUtil.getClosestType(context) };
	}
	public boolean isPartOf(IRNode type) {
		throw new UnsupportedOperationException();
	}	
}
