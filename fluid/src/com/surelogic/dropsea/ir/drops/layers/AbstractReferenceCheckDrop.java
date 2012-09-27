/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.dropsea.ir.drops.layers;

import com.surelogic.aast.layers.AbstractLayerMatchRootNode;
import com.surelogic.analysis.layers.Messages;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;

public abstract class AbstractReferenceCheckDrop<A extends AbstractLayerMatchRootNode> extends PromiseDrop<A> 
implements IReferenceCheckDrop {
	AbstractReferenceCheckDrop(A ast) {
		super(ast);
		setCategorizingString(Messages.DSC_LAYERS_ISSUES);
		setMessage(12, getAAST().toString());
	}
	
	public final boolean check(IRNode type) {
		return getAAST().check(type);
	}
}
