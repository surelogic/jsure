/*
 * Created on Oct 10, 2003
 *
 */
package edu.cmu.cs.fluid.promise;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Rule for computing the binding for a given AST node.
 * 
 * @author chance
 */
public interface IPromiseBindRule extends IPromiseRule {
	/**
	 * Return a binding for a declaration (with a registered Operator).
	 * 
	 * TODO Should this be changed to the new bindings?
	 */
	IRNode getBinding(Operator op, IRNode use);
}
