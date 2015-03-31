
package com.surelogic.aast.promise;


import com.surelogic.aast.*;

import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.operator.VariableDeclaration;

public class ReadOnlyNode extends AbstractBooleanNode 
{ 
  // Constructors
  public ReadOnlyNode(int offset) {
    super(offset);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    return unparse(debug, indent, "ReadOnly");
  }

  @Override
  public String unparseForPromise() {
	  if (VariableDeclaration.prototype.includes(getPromisedFor())) {
		  return "ReadOnly";
	  }
	  return "ReadOnly(\""+JavaNames.getFieldDecl(getPromisedFor())+"\")";
  }
  
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new ReadOnlyNode(offset);
  }
}
