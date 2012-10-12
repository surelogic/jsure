
package com.surelogic.aast.promise;

import com.surelogic.aast.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class ThreadConfinedNode extends AbstractBooleanNode 
{ 
  public static final AbstractAASTNodeFactory factory = new Factory("ThreadConfined") {   
    @Override
    public AASTNode create(int _start) {
      return new ThreadConfinedNode (_start);
    }
  };

  // Constructors
  public ThreadConfinedNode(int offset) {
    super(offset);
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
  public IAASTNode cloneTree(){
  	return new ThreadConfinedNode(offset);
  }
}

