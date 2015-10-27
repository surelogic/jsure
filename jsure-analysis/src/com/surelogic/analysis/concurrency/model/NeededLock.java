package com.surelogic.analysis.concurrency.model;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * A lock that needs to be held before a particular region can be accessed.
 * Originates from a specific region access.  These are instantiations of
 * {@link ModelLock}s: in particular, for non-static locks, they are associated
 * with an expression that represents that object whose lock must be held.
 *
 * <p>These work in concert with {@link HeldLock} objects that are instantiations
 * of {@link ModelLocks}s that represent locks that are currently held along
 * a particular control-flow path.  For analysis to be satisfied every 
 * NeededLock must be matched to a HeldLock.
 * 
 * <p>As these arise from actual region accesses in the code, they are associated
 * with a parse tree node that gives rise to the region access.
 */
public interface NeededLock {
  /**
   * Get the parse tree node of the region access that causes the need for 
   * the lock.
   */
  public IRNode getSource();
  
  /**
   * Is a write lock needed?
   */
  public boolean needsWrite();
}
