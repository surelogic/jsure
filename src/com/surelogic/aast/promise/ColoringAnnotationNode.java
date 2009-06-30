
package com.surelogic.aast.promise;


import com.surelogic.aast.*;

public abstract class ColoringAnnotationNode extends AASTRootNode { 
  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ColoringAnnotationNode(int offset) {
    super(offset);
  }
}

