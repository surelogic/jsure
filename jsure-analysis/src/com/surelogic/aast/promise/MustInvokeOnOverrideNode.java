package com.surelogic.aast.promise;

import com.surelogic.aast.*;
import com.surelogic.annotation.rules.*;

public class MustInvokeOnOverrideNode extends AbstractBooleanNode 
{ 
  // Constructors
  public MustInvokeOnOverrideNode() {
    super();
  }

  @Override
  public String unparse(boolean debug, int indent) {
    return unparse(debug, indent, StructureRules.MUST_INVOKE_ON_OVERRIDE);
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  protected IAASTNode internalClone(final INodeModifier mod) {
  	return new MustInvokeOnOverrideNode();
  }
}

