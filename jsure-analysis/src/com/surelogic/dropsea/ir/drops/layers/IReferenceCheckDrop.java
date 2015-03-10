/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.dropsea.ir.drops.layers;

import edu.cmu.cs.fluid.ir.IRNode;

public interface IReferenceCheckDrop {
	boolean check(IRNode type);
	int getResultMessageKind();
	Object[] getArgs(IRNode binding, IRNode type, IRNode context);
	boolean isPartOf(IRNode type);
}
