package com.surelogic.analysis.locks.locks;

import com.surelogic.aast.promise.*;

import edu.cmu.cs.fluid.sea.drops.promises.*;

public interface ILock {
  /** 
   * Get the IRNode of the lock declaration for the lock.
   */
  public AbstractLockDeclarationNode getLockDecl();

  /** 
   * Convenience method for getting the name of the lock.
   */  
  public String getName();
  
  /**
   * @return Returns the lockPromise.
   */
  public LockModel getLockPromise();
  
  /**
   * Must the the lock allow writes?
   */
  public boolean isWrite();
}
