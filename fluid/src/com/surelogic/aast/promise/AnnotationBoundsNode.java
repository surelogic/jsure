
package com.surelogic.aast.promise;

import com.surelogic.aast.*;

import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.annotation.rules.LockRules;

import edu.cmu.cs.fluid.java.JavaNode;

public final class AnnotationBoundsNode extends AbstractModifiedBooleanNode { 
  private final NamedTypeNode[] containable;
  private final NamedTypeNode[] immutable;
  private final NamedTypeNode[] reference;
  private final NamedTypeNode[] threadSafe;
  private final NamedTypeNode[] value;
	  
  // Constructors
  public AnnotationBoundsNode(int offset, NamedTypeNode[] containable, 
		  NamedTypeNode[] immutable, NamedTypeNode[] reference,
		  NamedTypeNode[] threadSafe, NamedTypeNode value[]) {
    super(offset, JavaNode.ALL_FALSE);
    this.containable = containable;
    this.immutable = immutable;
    this.reference = reference;
    this.threadSafe = threadSafe;
    this.value = value;
    setParents(containable);
    setParents(immutable);
    setParents(reference);
    setParents(threadSafe);
    setParents(value);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    return unparse(debug, indent, "AnnotationBounds");
  }

  @Override
  protected void unparseExtra(boolean debug, int indent, StringBuilder sb) {
    if (unparseTypes(debug, indent, sb, LockRules.CONTAINABLE_PROP, containable)) {
      // Need a comma since something's coming up     
      sb.append(", ");
    }
    if (unparseTypes(debug, indent, sb, LockRules.IMMUTABLE_PROP, immutable)) {
      // Need a comma since something's coming up     
      sb.append(", ");
    }
    if (unparseTypes(debug, indent, sb, LockRules.REFERENCE_PROP, reference)) {
      // Need a comma since something's coming up     
      sb.append(", ");
    }
	  if (unparseTypes(debug, indent, sb, LockRules.THREAD_SAFE_PROP, threadSafe)) {
		  // Need a comma since something's coming up		  
		  sb.append(", ");
	  }
    if (unparseTypes(debug, indent, sb, LockRules.VALUE_PROP, value)) {
//      // Need a comma since something's coming up     
//      sb.append(", ");
    }
  }
  
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new AnnotationBoundsNode(offset, containable, immutable, reference, threadSafe, value);
  }
  
  @Override
  protected boolean hasChildren() {
	  return (threadSafe.length + immutable.length + containable.length + reference.length + value.length) > 0;
  }
  
  
  
  public NamedTypeNode[] getContainable() {
    return containable;
  }  

  public NamedTypeNode[] getImmutable() {
    return immutable;
  }
  
  public NamedTypeNode[] getReference() {
    return reference;
  }
  
  public NamedTypeNode[] getThreadSafe() {
	  return threadSafe;
  }
  
  public NamedTypeNode[] getValue() {
    return value;
  }
  
  public static interface BoundsVisitor {
    public void visitWhenType(NamedTypeNode namedType);
  }

  public void visitAnnotationBounds(final BoundsVisitor visitor) {
    visitContainableBounds(visitor);
    visitImmutableBounds(visitor);
    visitReferenceBounds(visitor);
    visitThreadSafeBounds(visitor);
    visitValueBounds(visitor);
  }
  
  public void visitContainableBounds(final BoundsVisitor visitor) {
    for (final NamedTypeNode named : containable) {
      visitor.visitWhenType(named);
    }
  }

  public void visitImmutableBounds(final BoundsVisitor visitor) {
    for (final NamedTypeNode named : immutable) {
      visitor.visitWhenType(named);
    }
  }
  
  public void visitReferenceBounds(final BoundsVisitor visitor) {
    for (final NamedTypeNode named : reference) {
      visitor.visitWhenType(named);
    }
  }

  public void visitThreadSafeBounds(final BoundsVisitor visitor) {
    for (final NamedTypeNode named : threadSafe) {
      visitor.visitWhenType(named);
    }
  }

  public void visitValueBounds(final BoundsVisitor visitor) {
    for (final NamedTypeNode named : value) {
      visitor.visitWhenType(named);
    }
  }
}

