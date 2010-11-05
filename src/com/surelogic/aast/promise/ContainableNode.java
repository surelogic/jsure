package com.surelogic.aast.promise;

import com.surelogic.aast.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class ContainableNode extends AbstractBooleanNode 
{ 
  public static final AbstractAASTNodeFactory factory = new Factory("Containable") {   
    @Override
    public AASTNode create(int _start) {
      return new ContainableNode (_start);
    }
  };

  // Constructors
  public ContainableNode(int offset) {
    super(offset);
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
  	return new ContainableNode(offset);
  }
}

