package com.surelogic.aast.promise;

import com.surelogic.aast.*;
import com.surelogic.annotation.rules.NonNullRules;

public class RawNode extends AbstractBooleanNode 
{ 
  private final String upTo;
	
  // Constructors
  public RawNode(int offset, String upTo) {
    super(offset);
    this.upTo = upTo == null ? "*" : upTo; 
  }

  @Override
  public String unparse(boolean debug, int indent) {
	if ("*".equals(upTo)) {
		return NonNullRules.RAW;
	}	
	return NonNullRules.RAW+"(upTo="+upTo+')';
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new RawNode(offset, upTo);
  }
  
  public String getUpTo() {
    return upTo;
  }
}

