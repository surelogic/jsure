
package com.surelogic.aast.promise;

import com.surelogic.aast.*;
import com.surelogic.parse.AbstractSingleNodeFactory;

public class SingleThreadedNode extends AbstractBooleanNode 
{ 
  public static final AbstractSingleNodeFactory factory = new Factory("SingleThreaded") {   
    @Override
    public AASTNode create(int _start) {
      return new SingleThreadedNode (_start);
    }
  };

  // Constructors
  public SingleThreadedNode(int offset) {
    super(offset);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    return unparse(debug, indent, "SingleThreaded");
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new SingleThreadedNode(offset);
  }
}

