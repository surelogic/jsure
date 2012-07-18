package com.surelogic.aast.promise;

import com.surelogic.aast.*;
import com.surelogic.annotation.rules.NonNullRules;

public class RawNode extends AbstractBooleanNode 
{ 
  private final String upTo, value;
	
  // Constructors
  public RawNode(int offset, String upTo, String value) {
    super(offset);
    this.upTo = upTo == null ? "*" : upTo;
    this.value = value == null ? "" : value;    
  }

  @Override
  public String unparse(boolean debug, int indent) {
	if ("*".equals(upTo)) {
		if (value.length() == 0) {
			return NonNullRules.RAW;
		}
		return NonNullRules.RAW+'('+value+')';
	}	
	return NonNullRules.RAW+"(upTo="+upTo+
		   (value.length() == 0 ? "" : ", value="+value)+')';
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new RawNode(offset, upTo, value);
  }
}

