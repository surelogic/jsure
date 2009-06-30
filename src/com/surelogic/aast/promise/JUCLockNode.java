/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/JUCLockNode.java,v 1.3 2007/07/10 22:16:30 aarong Exp $*/
package com.surelogic.aast.promise;

import com.surelogic.aast.bind.AASTBinder;
import com.surelogic.aast.bind.ILockBinding;

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
	
  public boolean bindingExists() {
    return AASTBinder.getInstance().isResolvable(this);
  }

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
}
