
package com.surelogic.aast.promise;

import com.surelogic.Part;
import com.surelogic.aast.*;

import edu.cmu.cs.fluid.java.JavaNode;

public class NotThreadSafeNode extends AbstractModifiedBooleanNode 
{ 
  // Constructors
  public NotThreadSafeNode(Part state) {
    super("NotThreadSafe", JavaNode.ALL_FALSE, state);
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new NotThreadSafeNode(appliesTo);
  }
}

