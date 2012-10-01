
package com.surelogic.aast.promise;


import com.surelogic.aast.*;

public abstract class TargetedAnnotationNode extends AASTRootNode { 
  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public TargetedAnnotationNode(int offset) {
    super(offset);
  }
  
  public final String unparseForPromise() {
	  return unparse(false);
  }
}

