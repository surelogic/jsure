
package com.surelogic.aast.promise;

import com.surelogic.aast.INodeVisitor;

public abstract class LockNameNode extends LockSpecificationNode { 
  // Fields
  private final String id;

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public LockNameNode(int offset,
                      String id) {
    super(offset);
    if (id == null) { throw new IllegalArgumentException("id is null"); }
    this.id = id;
  }

  /**
   * @return A non-null String
   */
  public String getId() {
    return id;
  }
  @Override
  public <T> T accept(INodeVisitor<T> visitor) { 
    return visitor.visit(this);
  }

  @Override
  public LockNameNode getLock() {
    return this;
  }
  
  @Override
  public LockType getType() {
    return LockType.RAW;
  }
}

