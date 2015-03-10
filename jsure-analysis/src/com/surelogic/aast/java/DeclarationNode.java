package com.surelogic.aast.java;

import com.surelogic.aast.AASTRootNode;

public abstract class DeclarationNode extends AASTRootNode { 
  // Fields
  private final String id;

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public DeclarationNode(int offset,
                         String id) {
    super(offset);
    this.id = id;
  }

  public DeclarationNode getNode() { return this; }
  
  /**
   * @return A possibly-null String
   */
  public String getId() {
    return id;
  }
}

