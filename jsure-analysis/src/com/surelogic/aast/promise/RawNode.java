package com.surelogic.aast.promise;

import com.surelogic.aast.*;
import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.annotation.parse.AnnotationVisitor;
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
    type.setParent(this);
  }

  @Override
  public String unparse(boolean debug, int indent) {
	if ("*".equals(upTo)) {
		return NonNullRules.RAW;
	}	
	return NonNullRules.RAW+"("+AnnotationVisitor.THROUGH+"="+upTo+')';
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  protected IAASTNode internalClone(final INodeModifier mod) {
  	return new RawNode(offset, upTo, (NamedTypeNode) upToType.cloneOrModifyTree(mod));
  }
  
  public String getUpTo() {
    return upTo;
  }
  
  public NamedTypeNode getUpToType() {
	return upToType;
  }
}

