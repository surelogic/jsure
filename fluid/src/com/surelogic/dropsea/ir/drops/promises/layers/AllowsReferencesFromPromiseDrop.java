/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.dropsea.ir.drops.promises.layers;

import com.surelogic.aast.layers.*;

import edu.cmu.cs.fluid.ir.IRNode;

public class AllowsReferencesFromPromiseDrop extends AbstractReferenceCheckDrop<AllowsReferencesFromNode> 
{
	public AllowsReferencesFromPromiseDrop(AllowsReferencesFromNode a) {
		super(a);
	}
	public int getResultMessageKind() {
		return 350;
	}
	public Object[] getArgs(IRNode binding, IRNode type, IRNode context) {
		return new Object[] { binding, type };
	}
	public boolean isPartOf(IRNode type) {
		throw new UnsupportedOperationException();
	}	
}
