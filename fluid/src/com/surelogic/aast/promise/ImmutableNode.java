
package com.surelogic.aast.promise;

import com.surelogic.aast.*;
import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.annotation.rules.LockRules;

public final class ImmutableNode extends AbstractModifiedBooleanNode 
{ 
  private final NamedTypeNode[] whenImmutable;
	
  // Constructors
  public ImmutableNode(int offset, int mods, NamedTypeNode[] when) {
    super(offset, mods);
    whenImmutable = when;
    setParents(when);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    return unparse(debug, indent, "Immutable");
  }
  
  @Override
  protected void unparseExtra(boolean debug, int indent, StringBuilder sb) {
	  unparseTypes(debug, indent, sb, LockRules.WHEN_IMMUTABLE, whenImmutable);
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new ImmutableNode(offset, mods, whenImmutable);
  }
  
  @Override
  protected boolean hasChildren() {
	  return whenImmutable.length > 0;
  }
  
  public NamedTypeNode[] getWhenImmutable() {
    return whenImmutable;
  }
  
  @Override
  public void visitAnnotationBounds(final WhenVisitor visitor) {
    for (final NamedTypeNode named : whenImmutable) {
      visitor.visitWhenType(named);
    }
  }
}

