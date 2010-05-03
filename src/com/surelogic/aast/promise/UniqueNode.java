
package com.surelogic.aast.promise;


import com.surelogic.aast.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class UniqueNode extends AbstractBooleanNode 
{ 
  public static final AbstractAASTNodeFactory factory = new Factory("Unique") {   
    @Override
    public AASTNode create(int _start) {
      return new UniqueNode (_start);
    }
  };

  // Constructors
  public UniqueNode(int offset) {
    super(offset);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    return unparse(debug, indent, "Unique");
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new UniqueNode(offset);
  }
}

