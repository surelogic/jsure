package com.surelogic.aast.promise;

import com.surelogic.aast.*;

import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.annotation.rules.LockRules;

public final class ContainableNode extends AbstractModifiedBooleanNode 
{ 
  private final NamedTypeNode[] whenContainable;
	
  // Constructors
  public ContainableNode(int offset, int mods, NamedTypeNode[] when) {
    super(offset, mods);
    whenContainable = when;    
    setParents(when);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    return unparse(debug, indent, "Containable");
  }
  
  @Override
  protected void unparseExtra(boolean debug, int indent, StringBuilder sb) {
	  unparseTypes(debug, indent, sb, LockRules.WHEN_CONTAINABLE, whenContainable);
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new ContainableNode(offset, mods, whenContainable);
  }
  
  @Override
  protected boolean hasChildren() {
	  return whenContainable.length > 0;
  }
  
  public NamedTypeNode[] getWhenContainable() {
	  return whenContainable;
  }
}

