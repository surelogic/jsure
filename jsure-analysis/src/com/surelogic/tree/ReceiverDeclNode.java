/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.tree;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.tree.Operator;

public class ReceiverDeclNode extends SyntaxTreeNode {
	IRNode receiver;
	
	ReceiverDeclNode(Operator op, IRNode[] children) {
		super(op, children);
	    if (this.receiver == null) {
	        this.receiver = Constants.undefinedNode;
	    }
	}

	ReceiverDeclNode(Operator op) {
		super(op);
	    if (this.receiver == null) {
	        this.receiver = Constants.undefinedNode;
	    }
	}
	
	@Override
	public synchronized void destroy() {
		super.destroy();
		this.receiver = null;
	}
}
