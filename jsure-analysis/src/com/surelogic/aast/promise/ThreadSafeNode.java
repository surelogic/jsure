
package com.surelogic.aast.promise;

import com.surelogic.Part;
import com.surelogic.aast.*;

public final class ThreadSafeNode extends AbstractModifiedBooleanNode 
{   
  // Constructors
  public ThreadSafeNode(int mods, Part state) {
    super("ThreadSafe", mods, state);
  }
  
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  protected IAASTNode internalClone(final INodeModifier mod) {
  	return new ThreadSafeNode(mods, appliesTo);
  }
}

