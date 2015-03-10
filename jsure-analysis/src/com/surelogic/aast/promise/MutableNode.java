
package com.surelogic.aast.promise;

import com.surelogic.Part;
import com.surelogic.aast.*;

public class MutableNode extends AbstractModifiedBooleanNode 
{ 
  // Constructors
  public MutableNode(int mods, Part state) {
	super("Mutable", mods, state);
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new MutableNode(mods, appliesTo);
  }
}

