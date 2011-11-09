package com.surelogic.aast.promise;

import com.surelogic.aast.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class ThreadRoleTransparentNode extends AbstractBooleanNode {
  // Fields

  public static final AbstractAASTNodeFactory factory = new Factory(
      "ThreadRoleTransparent") {
    @Override
    protected AASTNode create(int offset) {
      return new ThreadRoleTransparentNode(offset);
    }
  };

  // Constructors
  /**
   * Lists passed in as arguments must be
   * 
   * @unique
   */
  public ThreadRoleTransparentNode(int offset) {
    super(offset);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    return unparse(debug, indent, "ThreadRoleTransparent");
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {

    return visitor.visit(this);
  }

  @Override
  public IAASTNode cloneTree() {
    return new ThreadRoleTransparentNode(getOffset());
  }
}
