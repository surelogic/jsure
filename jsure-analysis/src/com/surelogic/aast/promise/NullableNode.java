
package com.surelogic.aast.promise;

import com.surelogic.aast.*;
import com.surelogic.annotation.rules.NonNullRules;

public class NullableNode extends AbstractBooleanNode 
{ 
  // Constructors
  public NullableNode(int offset) {
    super(offset);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    return unparse(debug, indent, NonNullRules.NULLABLE);
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new NullableNode(offset);
  }
}

