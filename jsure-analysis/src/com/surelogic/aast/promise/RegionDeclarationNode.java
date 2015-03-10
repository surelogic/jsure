
package com.surelogic.aast.promise;

public abstract class RegionDeclarationNode extends PromiseDeclarationNode { 
  // Fields

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public RegionDeclarationNode(int offset,
                               String id) {
    super(offset, id);
  }
}

