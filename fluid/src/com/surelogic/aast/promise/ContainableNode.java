package com.surelogic.aast.promise;

import com.surelogic.aast.*;

public final class ContainableNode extends AbstractModifiedBooleanNode 
{ 	
  // Constructors
  public ContainableNode(int mods) {
    super(mods);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    return unparse(debug, indent, "Containable");
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new ContainableNode(mods);
  }
}

