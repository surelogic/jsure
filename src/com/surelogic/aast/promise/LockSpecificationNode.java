package com.surelogic.aast.promise;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.IHasLockBinding;

public abstract class LockSpecificationNode extends AASTNode 
implements IHasLockBinding { 
  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public LockSpecificationNode(int offset) {
    super(offset);
  }
  
  public abstract LockNameNode getLock();
  
  public abstract LockType getType();
}

