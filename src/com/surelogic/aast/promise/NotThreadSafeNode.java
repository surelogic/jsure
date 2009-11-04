
package com.surelogic.aast.promise;

import com.surelogic.aast.*;
import com.surelogic.parse.AbstractSingleNodeFactory;

public class NotThreadSafeNode extends AbstractBooleanNode 
{ 
  public static final AbstractSingleNodeFactory factory = new Factory("NotThreadSafe") {   
    @Override
    public AASTNode create(int _start) {
      return new NotThreadSafeNode (_start);
    }
  };

  // Constructors
  public NotThreadSafeNode(int offset) {
    super(offset);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    return unparse(debug, indent, "NotThreadSafe");
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new NotThreadSafeNode(offset);
  }
}

