package com.surelogic.aast.promise;

import com.surelogic.aast.*;
import com.surelogic.annotation.rules.*;

public class ValueObjectNode extends AbstractBooleanNode 
{ 
  // Constructors
  public ValueObjectNode() {
    super();
  }

  @Override
  public String unparse(boolean debug, int indent) {
    return unparse(debug, indent, EqualityRules.VALUE_OBJECT);
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new ValueObjectNode();
  }
}

