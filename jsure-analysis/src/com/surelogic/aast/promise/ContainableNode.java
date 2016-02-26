package com.surelogic.aast.promise;

import com.surelogic.aast.*;

public final class ContainableNode extends AbstractModifiedBooleanNode 
{ 	
  // Constructors
  public ContainableNode(int mods) {
    super("Containable", mods, null);
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  protected IAASTNode internalClone(final INodeModifier mod) {
  	return new ContainableNode(mods);
  }
}

