
package com.surelogic.aast.promise;

import com.surelogic.aast.*;

public final class ImmutableNode extends AbstractModifiedBooleanNode 
{ 	
  // Constructors
  public ImmutableNode(int offset, int mods) {
    super(offset, mods);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    return unparse(debug, indent, "Immutable");
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new ImmutableNode(offset, mods);
  }
}

