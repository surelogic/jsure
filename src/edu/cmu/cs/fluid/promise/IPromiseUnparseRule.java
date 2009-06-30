package edu.cmu.cs.fluid.promise;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaUnparser;

/**
 * Rule to decide how to unparse a given IStorage
 * 
 * Problem: there could be multiple tags mapped to one IStorage
 * (Assume only one operator type is mapped?
 */
public interface IPromiseUnparseRule extends IPromiseRule {
  /** 
   * Returns the name of the promise being unparsed.
   * Assumed to be a constant String
   */
  String name();
  
	void unparse(final IRNode node, final JavaUnparser u);
}
