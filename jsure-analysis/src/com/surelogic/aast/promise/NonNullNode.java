package com.surelogic.aast.promise;

import com.surelogic.aast.*;
import com.surelogic.annotation.rules.NonNullRules;

public class NonNullNode extends AbstractBooleanNode 
{ 
  // Constructors
  public NonNullNode(int offset) {
    super(offset);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    return unparse(debug, indent, NonNullRules.NONNULL);
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  protected IAASTNode internalClone(final INodeModifier mod) {
  	return new NonNullNode(offset);
  }
}

