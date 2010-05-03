package com.surelogic.aast.promise;

import com.surelogic.aast.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class TransparentNode extends AbstractBooleanNode {
  // Fields

  public static final AbstractAASTNodeFactory factory = new Factory(
      "Transparent") {
    @Override
    protected AASTNode create(int offset) {
      return new TransparentNode(offset);
    }
  };

  // Constructors
  /**
   * Lists passed in as arguments must be
   * 
   * @unique
   */
  public TransparentNode(int offset) {
    super(offset);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    return unparse(debug, indent, "Transparent");
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {

    return visitor.visit(this);
  }

  @Override
  public IAASTNode cloneTree() {
    return new TransparentNode(getOffset());
  }
}
