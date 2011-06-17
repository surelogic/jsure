
package com.surelogic.aast.promise;


import com.surelogic.aast.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class ReadonlyNode extends AbstractBooleanNode 
{ 
  public static final AbstractAASTNodeFactory factory = new Factory("Readonly") {   
    @Override
    public AASTNode create(int _start) {
      return new ReadonlyNode (_start);
    }
  };

  // Constructors
  public ReadonlyNode(int offset) {
    super(offset);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    return unparse(debug, indent, "Readonly");
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new ReadonlyNode(offset);
  }
}

