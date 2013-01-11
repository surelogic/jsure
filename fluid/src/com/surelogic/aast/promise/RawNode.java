package com.surelogic.aast.promise;

import com.surelogic.aast.*;
import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.annotation.rules.NonNullRules;

public class RawNode extends AbstractBooleanNode 
{ 
  private final String upTo;
  private final NamedTypeNode upToType;
	
  // Constructors
  public RawNode(int offset, String upTo, NamedTypeNode type) {
    super(offset);
    this.upTo = upTo == null ? "*" : upTo; 
    upToType = type;
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
  	return new RawNode(offset, upTo, upToType);
  }
  
  public String getUpTo() {
    return upTo;
  }
  
  public NamedTypeNode getUpToType() {
	return upToType;
  }
}

