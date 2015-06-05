
package com.surelogic.aast.promise;


import com.surelogic.aast.*;

import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;

public final class BorrowedNode extends AbstractBooleanNode 
{ 
  // Constructors
  public BorrowedNode(int offset) {
    super(offset);
  }
  
  @Override
  public String unparse(boolean debug, int indent) {
	if (!debug) {
		return unparseForPromise();
	}		
    return unparse(debug, indent, "Borrowed");
  }

  @Override
  public String unparseForPromise() {
	  if (ParameterDeclaration.prototype.includes(getPromisedFor())) {
		  return "Borrowed";
	  }
	  return "Borrowed(\""+JavaNames.getFieldDecl(getPromisedFor())+"\")";
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

