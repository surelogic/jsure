/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/JUCLockNode.java,v 1.3 2007/07/10 22:16:30 aarong Exp $*/
package com.surelogic.aast.promise;

import java.util.Map;

import com.surelogic.aast.bind.AASTBinder;
import com.surelogic.aast.bind.ILockBinding;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Represents an AAST node for the .fooLock() annotation
 * 
 * @author edwin
 */
public abstract class JUCLockNode extends LockSpecificationNode {
	private final LockNameNode lock;
	
	public JUCLockNode(int offset, LockNameNode lock){
		super(offset);
		if(lock == null){
			throw new IllegalArgumentException("lock is null");
		}
		lock.setParent(this);
		this.lock = lock;
	}
	
  @Override
  public boolean bindingExists() {
    return AASTBinder.getInstance().isResolvable(this);
  }

  @Override
  public ILockBinding resolveBinding() {
    return null; // FIX AASTBinder.getInstance().resolve(this);
  }
  
	/**
	 * Return a non-null LockNameNode for the lock that is associated with this
	 * @return
	 */
	@Override
  public final LockNameNode getLock(){
		return lock;
	}

  
  
  @Override
  public final boolean satisfiesSpecfication(
      final LockSpecificationNode ancestor,
      final Map<IRNode, Integer> positionMap,
      final How how) {
    return ancestor.jucLockSatistiesSpecification(this, positionMap, how);
  }

  @Override
  final boolean jucLockSatistiesSpecification(
      final JUCLockNode overriding, final Map<IRNode, Integer> positionMap,
      final How how) {
    return overriding.namesSameLockAs(this, positionMap, how);
  }
  
  @Override
  final boolean lockNameSatisfiesSpecification(
      final LockNameNode overriding, final Map<IRNode, Integer> positionMap,
      final How how) {
    // Never the same as a lock name
    return false;
  }

  
  
  /**
   * Compare two JUC Locks from two declarations of the same method to see if
   * they refer to same lock. This is complicated by the fact that the formal
   * arguments of the two declarations, while the same in number, can have
   * different names. The <code>positionMap</code> is used to map formal
   * arguments of both declarations to their position in the argument list. Both
   * methods use the same map because the keys, the
   * <code>VariableUseExpressionNode</code> objects, are globally unique.
   * 
   * <p>
   * This is the same as
   * {@link #satisfiesSpecfication(LockSpecificationNode, Map)}, but the
   * implementation is specific to JUCLockNodes.
   */
  public abstract boolean namesSameLockAs(
      JUCLockNode ancestor, Map<IRNode, Integer> positionMap, How how);
  
  abstract boolean namesSameLockAsReadLock(
      ReadLockNode overriding, Map<IRNode, Integer> positionMap, How how);
  
  abstract boolean namesSameLockAsWriteLock(
      WriteLockNode overriding, Map<IRNode, Integer> positionMap, How how);
}
