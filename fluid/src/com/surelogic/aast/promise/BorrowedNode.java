
package com.surelogic.aast.promise;


import com.surelogic.aast.*;

import edu.cmu.cs.fluid.java.JavaNames;

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
	if (!debug) {
		return unparseForPromise();
	}		
    // TODO allowReturn?
    return unparse(debug, indent, "Borrowed");
  }

  @Override
  public String unparseForPromise() {
	  return "Borrowed(\""+JavaNames.getFieldDecl(getPromisedFor())+"\""+
		     (allowReturn ? ", allowReturn=true)" : ")");
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

