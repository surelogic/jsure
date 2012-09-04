package com.surelogic.aast.promise;

import com.surelogic.aast.*;
import com.surelogic.annotation.rules.*;

public class RefObjectNode extends AbstractBooleanNode 
{ 
  // Constructors
  public RefObjectNode(int offset, int unusedMods) {
    super(offset);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    return unparse(debug, indent, EqualityRules.REF_OBJECT);
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new RefObjectNode(offset, 0);
  }
}

