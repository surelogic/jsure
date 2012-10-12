
package com.surelogic.aast.promise;

import com.surelogic.aast.*;

public class NotThreadSafeNode extends AbstractBooleanNode 
{ 
  // Constructors
  public NotThreadSafeNode(int offset) {
    super(offset);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    return unparse(debug, indent, "NotThreadSafe");
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new NotThreadSafeNode(offset);
  }
}

