
package com.surelogic.aast.promise;


import com.surelogic.aast.*;

public abstract class ThreadRoleAnnotationNode extends AASTRootNode { 
  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ThreadRoleAnnotationNode(int offset) {
    super(offset);
  }
  
  @Override
  public final String unparseForPromise() {
	  throw new UnsupportedOperationException();
  }
}

