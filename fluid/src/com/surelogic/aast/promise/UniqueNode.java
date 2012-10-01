
package com.surelogic.aast.promise;


import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.INodeVisitor;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;

public class UniqueNode extends AbstractBooleanNode 
{ 
  /*public static final AbstractAASTNodeFactory factory = new Factory("Unique") {   
    @Override
    public AASTNode create(int _start) {
      return new UniqueNode (_start);
    }
  };*/

	private final boolean allowRead;
	
  // Constructors
  public UniqueNode(int offset, boolean allow) {
    super(offset);
    allowRead = allow;
  }
  
  @Override
  public String unparse(boolean debug, int indent) {
	if (!debug) {
		return unparseForPromise();
	}		
    // TODO allowRead?
    return unparse(debug, indent, "Unique");
  }

  @Override
  public String unparseForPromise() {
	  final IRNode node = getPromisedFor();
	  if (VariableDeclarator.prototype.includes(node)) {
		  return "Unique"+(allowRead ? "(allowRead=true)" : "");
	  }
	  return "Unique(\""+JavaNames.getFieldDecl(getPromisedFor())+"\""+
		     (allowRead ? ", allowRead=true)" : ")");
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new UniqueNode(offset,allowRead);
  }

  public boolean allowRead() {
	  return allowRead;
  }
}

