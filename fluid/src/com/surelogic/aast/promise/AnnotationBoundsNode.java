
package com.surelogic.aast.promise;

import com.surelogic.aast.*;

import com.surelogic.aast.java.NamedTypeNode;

import edu.cmu.cs.fluid.java.JavaNode;

public final class AnnotationBoundsNode extends AbstractModifiedBooleanNode { 
  private final NamedTypeNode[] containable;
  private final NamedTypeNode[] immutable;
  private final NamedTypeNode[] reference;
  private final NamedTypeNode[] threadSafe;
  private final NamedTypeNode[] value;
	  
  // Constructors
  public AnnotationBoundsNode(NamedTypeNode[] containable, 
		  NamedTypeNode[] immutable, NamedTypeNode[] reference,
		  NamedTypeNode[] threadSafe, NamedTypeNode value[]) {
    super("AnnotationBounds", JavaNode.ALL_FALSE, null);
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
  protected void unparseExtra(boolean debug, int indent, StringBuilder sb) {
	  final String[] attrs = new String[] { 
			  getNameList("containable", getContainable()),
			  getNameList("immutable", getImmutable()), 
			  getNameList("referenceObject", getReference()),
			  getNameList("threadSafe", getThreadSafe()), 
			  getNameList("valueObject", getValue()) 
	  };
	  boolean first = true;
	  for (final String at : attrs) {
		  if (at != null) {
			  if (!first) {
				  sb.append(", ");
			  } else {
				  first = false;
			  }
			  sb.append(at);
		  }
	  }	
	/*	    
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
    */
  }
  
  private String getNameList(final String attribute, final NamedTypeNode[] list) {
    if (list.length > 0) {
      final StringBuilder sb = new StringBuilder();
      sb.append(attribute);
      sb.append(" = {");
      boolean first = true;
      for (final NamedTypeNode namedType : list) {
        if (!first) {
          sb.append("\", \"");
        } else {
          first = false;
          sb.append('\"');
        }
        sb.append(namedType.getType());
      }
      sb.append("\"}");
      return sb.toString();
    } else {
      return null;
    }
  }
  
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new AnnotationBoundsNode(containable, immutable, reference, threadSafe, value);
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

