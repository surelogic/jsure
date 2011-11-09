
package com.surelogic.aast.promise;


import com.surelogic.aast.*;

public final class BorrowedNode extends AbstractBooleanNode 
{ 
  private final boolean allowReturn;

  // Constructors
  public BorrowedNode(int offset, boolean allow) {
    super(offset);
    allowReturn = allow;
  }

  public final boolean allowReturn() {
      return allowReturn;
  }
  
  @Override
  public String unparse(boolean debug, int indent) {
    // TODO allowReturn?
    return unparse(debug, indent, "Borrowed");
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new BorrowedNode(offset, allowReturn);
  }
}

