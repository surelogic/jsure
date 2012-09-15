/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.dropsea.ir.drops.layers;

import com.surelogic.aast.layers.MayReferToNode;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.util.VisitUtil;

public class MayReferToPromiseDrop extends AbstractReferenceCheckDrop<MayReferToNode> {
	public MayReferToPromiseDrop(MayReferToNode a) {
		super(a);
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
