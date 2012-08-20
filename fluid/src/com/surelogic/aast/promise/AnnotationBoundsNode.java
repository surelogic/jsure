
package com.surelogic.aast.promise;

import com.surelogic.aast.*;

import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.annotation.rules.LockRules;

import edu.cmu.cs.fluid.java.JavaNode;

public final class AnnotationBoundsNode extends AbstractModifiedBooleanNode 
{ 
  private final NamedTypeNode[] threadSafe;	
  private final NamedTypeNode[] immutable;
  private final NamedTypeNode[] containable;  
	  
  // Constructors
  public AnnotationBoundsNode(int offset, NamedTypeNode[] threadSafe, 
		  NamedTypeNode[] immutable, NamedTypeNode[] containable) {
    super(offset, JavaNode.ALL_FALSE);
    this.threadSafe = threadSafe;
    this.immutable = immutable;
    this.containable = containable;
    setParents(threadSafe);
    setParents(immutable);
    setParents(containable);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    return unparse(debug, indent, "AnnotationBounds");
  }

  @Override
  protected void unparseExtra(boolean debug, int indent, StringBuilder sb) {
	  boolean unparsed = unparseTypes(debug, indent, sb, LockRules.THREAD_SAFE, threadSafe);
	  if (unparsed && immutable.length != 0) {
		  // Need a comma since something's coming up		  
		  sb.append(", ");
	  }
	  unparsed |= unparseTypes(debug, indent, sb, LockRules.IMMUTABLE, immutable);
	  if (unparsed && containable.length != 0) {
		  // Need a comma since something's coming up		  
		  sb.append(", ");
	  }
	  unparseTypes(debug, indent, sb, LockRules.CONTAINABLE, containable);
  }
  
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new AnnotationBoundsNode(offset, threadSafe, immutable, containable);
  }
  
  @Override
  protected boolean hasChildren() {
	  return (threadSafe.length + immutable.length + containable.length) > 0;
  }
  
  public NamedTypeNode[] getThreadSafe() {
	  return threadSafe;
  }
  
  public NamedTypeNode[] getImmutable() {
	  return immutable;
  }
  
  public NamedTypeNode[] getContainable() {
	  return containable;
  }  
  
  @Override
  public void visitAnnotationBounds(final WhenVisitor visitor) {
    for (final NamedTypeNode named : threadSafe) {
      visitor.visitWhenType(named);
    }
    for (final NamedTypeNode named : immutable) {
      visitor.visitWhenType(named);
    }
    for (final NamedTypeNode named : containable) {
      visitor.visitWhenType(named);
    }
  }
}

