
package com.surelogic.aast.promise;


import com.surelogic.aast.*;

public class AssumeFinalNode extends AbstractBooleanNode 
{ 
  public static final AbstractAASTNodeFactory factory = new Factory("AssumeFinal") {   
    @Override
    public AASTNode create(int _start) {
      return new AssumeFinalNode (_start);
    }
  };

  // Constructors
  public AssumeFinalNode(int offset) {
    super(offset);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    return unparse(debug, indent, "AssumeFinal");
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new AssumeFinalNode(offset);
  }
}

