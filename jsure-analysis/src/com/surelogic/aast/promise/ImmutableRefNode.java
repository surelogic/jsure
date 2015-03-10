
package com.surelogic.aast.promise;

import com.surelogic.aast.*;

public class ImmutableRefNode extends AbstractBooleanNode 
{ 
  // Constructors
  public ImmutableRefNode(int offset) {
    super(offset);
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
  	return new ImmutableRefNode(offset);
  }
}

