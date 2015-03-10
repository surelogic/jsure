
package com.surelogic.aast.promise;

import com.surelogic.aast.*;

public class NotContainableNode extends AbstractBooleanNode 
{ 
  // Constructors
  public NotContainableNode() {
    super();
  }

  @Override
  public String unparse(boolean debug, int indent) {
    return unparse(debug, indent, "NotContainable");
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new NotContainableNode();
  }
}

