
package com.surelogic.aast.promise;


import com.surelogic.aast.java.*;

public abstract class PromiseDeclarationNode extends DeclarationNode 
{ 
  // Fields

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public PromiseDeclarationNode(int offset,
                                String id) {
    super(offset, id);
  }
  
  public String unparseForPromise() {
	  return unparse(false);
  }
}

