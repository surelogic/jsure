
package com.surelogic.aast.promise;

import com.surelogic.aast.*;

public class SelfProtectedNode extends AbstractModifiedBooleanNode 
{ 
  // Constructors
  public SelfProtectedNode(int offset, int mods) {
    super(offset, mods);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    return unparse(debug, indent, "ThreadSafe");
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new SelfProtectedNode(offset, mods);
  }
}

