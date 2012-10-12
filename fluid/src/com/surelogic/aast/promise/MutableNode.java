
package com.surelogic.aast.promise;

import com.surelogic.aast.*;

public class MutableNode extends AbstractBooleanNode 
{ 
  // Constructors
  public MutableNode() {
    super();
  }

  @Override
  public String unparse(boolean debug, int indent) {
    return unparse(debug, indent, "Mutable");
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new MutableNode();
  }
}

