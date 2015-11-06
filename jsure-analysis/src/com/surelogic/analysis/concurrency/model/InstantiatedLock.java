package com.surelogic.analysis.concurrency.model;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * An instantiations of a
 * {@link ModelLock}s: in particular, for non-static locks, they are associated
 * with an expression that represents that object whose lock must be held.
 *
 * <p>These are associated with a parse tree node indicating the region access
 * that requires the lock or the expression/declaration that acquires a lock.
 */
public interface InstantiatedLock {
  /**
   * Get the parse tree node of the region access that causes the need for 
   * the lock or that causes the lock to be held.
   */
  public IRNode getSource();
}
