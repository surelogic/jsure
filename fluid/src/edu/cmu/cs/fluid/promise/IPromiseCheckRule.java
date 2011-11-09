/*
 * Created on Oct 24, 2003
 *
 */
package edu.cmu.cs.fluid.promise;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Rule for doing sanity checking on a given AST node.
 * Reports warnings and error via the IPromiseCheckReport parameter.
 *
 * Design note: 
 *   This could actually check the whole subtree, if desired.
 * 
 * @author chance
 */
public interface IPromiseCheckRule extends IPromiseRule {
  /**
   * Checks the promises associated with this AST node (subtree)
   * for problems.
   *  
   * @param op The operator of the promisedFor node
   * @param promisedFor The AST node being checked
   * @param report The callback for reporting warnings and errors
   * @return true if everything is ok.
   */
	boolean checkSanity(Operator op, IRNode promisedFor, IPromiseCheckReport report);
}
