package edu.cmu.cs.fluid.java.bind;

import com.surelogic.NotThreadSafe;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Caches info about the operator/parent/etc for a given IRNode
 * Intended to be used in a single thread
 * 
 * @author Edwin
 */
@NotThreadSafe
public class NodeContext {
	IRNode node;
	Operator op;
	IRNode parent;
	Operator pop;
	IRNode gparent;
	Operator gop;
	
	NodeContext(IRNode n) {
		node = n;
		op = JJNode.tree.getOperator(n);
	}
	
	void reset(IRNode n) {
		node = n;
		op = JJNode.tree.getOperator(n);
		parent = gparent = null;
		pop = gop = null;
	}
	
	void moveToParent() {
		node = parent;
		op = pop;
		parent = gparent;
		pop = gop;
		gparent = null;
		gop = null;
	}
}
