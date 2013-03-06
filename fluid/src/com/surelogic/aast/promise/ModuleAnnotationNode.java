/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/ModuleAnnotationNode.java,v 1.1 2007/10/27 17:11:10 dfsuther Exp $*/
package com.surelogic.aast.promise;

import com.surelogic.aast.*;

public abstract class ModuleAnnotationNode extends AASTRootNode {

  // Constructors
  public ModuleAnnotationNode(int offset) {
    super(offset);
  }
  
  @Override
  public final String unparseForPromise() {
	  throw new UnsupportedOperationException();
  }
}
