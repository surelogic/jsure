
package com.surelogic.aast.promise;


import com.surelogic.aast.*;
import com.surelogic.parse.AbstractSingleNodeFactory;

public class BorrowedNode extends AbstractBooleanNode 
{ 
  public static final AbstractSingleNodeFactory factory = new Factory("Borrowed") {   
    @Override
    public AASTNode create(int _start) {
      return new BorrowedNode (_start);
    }
  };

  // Constructors
  public BorrowedNode(int offset) {
    super(offset);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    return unparse(debug, indent, "Borrowed");
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new BorrowedNode(offset);
  }
}

