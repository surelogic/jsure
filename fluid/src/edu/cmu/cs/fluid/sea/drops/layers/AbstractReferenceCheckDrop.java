/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea.drops.layers;

import com.surelogic.aast.layers.AbstractLayerMatchRootNode;
import com.surelogic.analysis.layers.Messages;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;

public abstract class AbstractReferenceCheckDrop<A extends AbstractLayerMatchRootNode> extends PromiseDrop<A> 
implements IReferenceCheckDrop {
	AbstractReferenceCheckDrop(A ast) {
		super(ast);
		setCategory(Messages.DSC_LAYERS_ISSUES);
		setMessage(getAAST().toString());
	}
	
	public final boolean check(IRNode type) {
		return getAAST().check(type);
	}
}
