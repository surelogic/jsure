
package com.surelogic.aast.promise;

import com.surelogic.aast.*;

import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.annotation.rules.LockRules;

public class ThreadSafeNode extends AbstractModifiedBooleanNode 
{ 
  private final NamedTypeNode[] whenThreadSafe;	
  private final NamedTypeNode[] whenImmutable;
  private final NamedTypeNode[] whenContainable;  
	  
  // Constructors
  public ThreadSafeNode(int offset, int mods, NamedTypeNode[] whenThreadSafe, 
		  NamedTypeNode[] whenImmutable, NamedTypeNode[] whenContainable) {
    super(offset, mods);
    this.whenThreadSafe = whenThreadSafe;
    this.whenImmutable = whenImmutable;
    this.whenContainable = whenContainable;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    return unparse(debug, indent, "ThreadSafe");
  }

  @Override
  protected void unparseExtra(boolean debug, int indent, StringBuilder sb) {
	  boolean unparsed = unparseTypes(debug, indent, sb, LockRules.WHEN_THREAD_SAFE, whenThreadSafe);
	  if (unparsed && whenImmutable.length != 0) {
		  // Need a comma since something's coming up		  
		  sb.append(", ");
	  }
	  unparsed |= unparseTypes(debug, indent, sb, LockRules.WHEN_IMMUTABLE, whenImmutable);
	  if (unparsed && whenContainable.length != 0) {
		  // Need a comma since something's coming up		  
		  sb.append(", ");
	  }
	  unparseTypes(debug, indent, sb, LockRules.WHEN_CONTAINABLE, whenContainable);
  }
  
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new ThreadSafeNode(offset, mods, whenThreadSafe, whenImmutable, whenContainable);
  }
  
  @Override
  protected boolean hasChildren() {
	  return (whenThreadSafe.length + whenImmutable.length + whenContainable.length) > 0;
  }
  
  public NamedTypeNode[] getWhenThreadSafe() {
	  return whenThreadSafe;
  }
  
  public NamedTypeNode[] getWhenImmutable() {
	  return whenImmutable;
  }
  
  public NamedTypeNode[] getWhenContainable() {
	  return whenContainable;
  }  
}

