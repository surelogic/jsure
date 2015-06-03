
package com.surelogic.aast.promise;


import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.INodeVisitor;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.operator.VariableDeclaration;

public class UniqueNode extends AbstractBooleanNode 
{ 
  // Constructors
  public UniqueNode(int offset) {
    super(offset);
  }
  
  @Override
  public String unparse(boolean debug, int indent) {
	if (!debug) {
		return unparseForPromise();
	}		
    return unparse(debug, indent, "Unique");
  }

  @Override
  public String unparseForPromise() {
	  final IRNode node = getPromisedFor();
	  if (VariableDeclaration.prototype.includes(node)) {
		  return "Unique";
	  }
	  return "Unique(\""+JavaNames.getFieldDecl(getPromisedFor())+"\")";
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

