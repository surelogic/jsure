
package com.surelogic.aast.promise;

import com.surelogic.aast.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class SelfProtectedNode extends AbstractBooleanNode 
{ 
  public static final AbstractAASTNodeFactory factory = new Factory("SelfProtected") {   
    @Override
    public AASTNode create(int _start) {
      return new SelfProtectedNode (_start);
    }
  };

  // Constructors
  public SelfProtectedNode(int offset) {
    super(offset);
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
  	return new SelfProtectedNode(offset);
  }
}

