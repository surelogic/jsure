package com.surelogic.aast.promise;

import com.surelogic.aast.*;
import com.surelogic.annotation.rules.*;

public class RefObjectNode extends AbstractBooleanNode 
{ 
  // Constructors
  public RefObjectNode() {
    super();
  }

  @Override
  public String unparse(boolean debug, int indent) {
    return unparse(debug, indent, EqualityRules.REF_OBJECT);
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  protected IAASTNode internalClone(final INodeModifier mod) {
  	return new RefObjectNode();
  }
}

