package com.surelogic.analysis.concurrency.model.instantiated;

import com.surelogic.aast.IAASTNode;
import com.surelogic.analysis.MethodCallUtils.EnclosingRefs;
import com.surelogic.analysis.concurrency.model.declared.ModelLock;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;

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
public interface NeededLock extends InstantiatedLock {
  public static enum Reason {
    FIELD_ACCESS(2000, 2010, 2001, 2011),
    INDIRECT_ACCESS(2002, 2012, 2003, 2013),
    METHOD_CALL(2004, 2014, 2005, 2015),
    LOCK_PRECONDITION(2006, 2016, 2007, 2017);
    
    private final int goodCategory;
    private final int goodMsg;
    
    private final int badCategory;
    private final int badMsg;
    
    Reason(final int goodCategory, final int goodMsg,
        final int badCategory, final int badMsg) {
      this.goodCategory = goodCategory;
      this.goodMsg = goodMsg;
      this.badCategory = badCategory;
      this.badMsg = badMsg;
    }
    
    public int getCategory(final boolean isGood) {
      return isGood ? goodCategory : badCategory;
    }
    
    public int getResultMessage(final boolean isGood) {
      return isGood ? goodMsg : badMsg;
    }
  }
  
  
  
  /**
   * Get the parse tree node of the region access that causes the need for 
   * the lock.
   */
  @Override
  public IRNode getSource();
  
  /**
   * Get the lock promise that controls why the lock is needed.  For 
   * field accesses this is the GuardedBy or RegionLock annotation on the
   * field declaration.  For method preconditions this is the RequiresLock
   * annotation on the method.
   */
  public PromiseDrop<? extends IAASTNode> getAssuredPromise();
  
  /**
   * Get the semantic reason for this lock's necessity.
   */
  public Reason getReason();
  
  /**
   * Is a write lock needed?
   */
  public boolean needsWrite();
  
  /**
   * The lock is passing through a context boundary from inside of the
   * initializer block of a nested class to the main flow of control of the
   * containing method body.  We need to convert the object reference expression
   * (if any) to make sense in the context of the method body.
   */
  public NeededLock replaceEnclosingInstanceReference(EnclosingRefs refs);
  
  /**
   * What lock, if any, the lock is aliased to/derived from.
   */
  public NeededLock getDerivedFrom();
  
  public String unparseForMessage();
  
  /**
   * Get the proposed RequiresLock annotation that would make this lock
   * satisfied, or null if there isn't one.
   */
  public ProposedPromiseDrop getProposedRequiresLock(IRNode mdecl, IRNode src);
}
