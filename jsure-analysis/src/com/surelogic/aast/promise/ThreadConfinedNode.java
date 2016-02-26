
package com.surelogic.aast.promise;

import com.surelogic.aast.*;

public class ThreadConfinedNode extends AbstractBooleanNode 
{ 
  // Constructors
  public ThreadConfinedNode() {
    super();
  }

  @Override
  public String unparse(boolean debug, int indent) {
    return unparse(debug, indent, "ThreadConfined");
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  protected IAASTNode internalClone(final INodeModifier mod) {
  	return new ThreadConfinedNode();
  }
}

