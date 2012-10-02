
package com.surelogic.aast.promise;


import com.surelogic.aast.*;

import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;

public class ReadOnlyNode extends AbstractBooleanNode 
{ 
  public static final AbstractAASTNodeFactory factory = new Factory("ReadOnly") {   
    @Override
    public AASTNode create(int _start) {
      return new ReadOnlyNode (_start);
    }
  };

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
	  if (ParameterDeclaration.prototype.includes(getPromisedFor())) {
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

