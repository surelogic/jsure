
package com.surelogic.aast.promise;

import com.surelogic.Part;
import com.surelogic.aast.*;

public final class ImmutableNode extends AbstractModifiedBooleanNode 
{ 	
  // Constructors
  public ImmutableNode(int mods, Part state) {
    super("Immutable", mods, state);
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  protected IAASTNode internalClone(final INodeModifier mod) {
  	return new ImmutableNode(mods, appliesTo);
  }
}

