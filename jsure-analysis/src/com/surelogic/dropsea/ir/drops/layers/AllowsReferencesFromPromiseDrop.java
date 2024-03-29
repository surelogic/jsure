/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.dropsea.ir.drops.layers;

import com.surelogic.aast.layers.*;

import edu.cmu.cs.fluid.ir.IRNode;

public class AllowsReferencesFromPromiseDrop extends AbstractReferenceCheckDrop<AllowsReferencesFromNode> 
{
	public AllowsReferencesFromPromiseDrop(AllowsReferencesFromNode a) {
		super(a);
	}
	@Override
  public int getResultMessageKind() {
		return 350;
	}
	@Override
  public Object[] getArgs(IRNode binding, IRNode type, IRNode context) {
		return new Object[] { binding, type };
	}
	@Override
  public boolean isPartOf(IRNode type) {
		throw new UnsupportedOperationException();
	}	
}
