package com.surelogic.aast.promise;

import com.surelogic.aast.*;

public abstract class AbstractBooleanNode extends AASTRootNode 
{ 
  // Constructors
  protected AbstractBooleanNode(int offset) {
    super(offset);
  }

  public String unparseForPromise() {
	  return unparse(false);
  }
  
  protected String unparse(boolean debug, int indent, String token) {
    if (debug) {
      StringBuilder sb = new StringBuilder();
      if (debug) { indent(sb, indent); }
      sb.append(token);
      sb.append('\n');
      return sb.toString();
    } else {
      return token;
    }
  }
    
  @Override
  public boolean isSameAs(IAASTRootNode other) {
	  return isSameClass(other);
  }
}

