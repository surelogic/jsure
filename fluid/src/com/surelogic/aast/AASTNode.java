/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/AASTNode.java,v 1.9 2007/09/24 21:09:56 ethan Exp $*/
package com.surelogic.aast;

import java.util.List;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.tree.Operator;

public abstract class AASTNode implements IAASTNode {
  protected final int offset;
  protected AASTNode parent;

  protected AASTNode(int offset) {
    this.offset = offset;
  }

  @Override
  public AASTNode getParent() {
    return parent;
  }

  @Override
  public AASTRootNode getRoot() {
	  AASTNode here = this;	  
	  AASTNode parent = here.getParent();
	  while (parent != null) {
		  here = parent;
		  parent = here.getParent();
	  }
	  return (AASTRootNode) here;
  }
  
  /**
   * Only to be called by the parent AST node's constructor
   */
  public void setParent(AASTNode p) {
    parent = p;
  }

  public void setParents(AASTNode[] children) {
	  for(AASTNode n : children) {
		  n.setParent(this);
	  }
  }
  
  @Override
  public int getOffset() {
    return offset;
  }

  @Override
  public final String toString() {
    return unparse(false);
  }
  
  public final String unparse() {
    return unparse(true);
  }
  
  @Override
  public final String unparse(boolean debug) {
    return unparse(debug, 0);
  }

  protected final void indent(StringBuilder sb, int indent) {
    for(int i=0; i<indent; i++) { sb.append(' '); }
  }
  
  protected static <T> void unparseList(StringBuilder sb, List<T> l, String separator) {
    boolean first = true;
    for(T e : l) {
      if (first) {
        first = false;
      } else {
        sb.append(separator);
      }
      sb.append(e);
    }
  }
  protected static <T> void unparseList(StringBuilder sb, List<T> l) {
    unparseList(sb, l, ", ");
  }
  
  @Override
  public abstract String unparse(boolean debug, int indent);
  @Override
  public abstract <T> T accept(INodeVisitor<T> visitor);
  
  @Override
  public abstract IAASTNode cloneTree();

  @Override
  public IRNode getPromisedFor() {
    AASTNode n    = this;
    AASTNode last = n;
    while (n != null) {
      last = n;
      n = n.getParent();
    }
    IAASTRootNode root = (IAASTRootNode) last;
    return root.getPromisedFor(); 
  }
  
  @Override
  public IRNode getAnnoContext() {
    AASTNode n    = this;
    AASTNode last = n;
    while (n != null) {
      last = n;
      n = n.getParent();
    }
    IAASTRootNode root = (IAASTRootNode) last;
    return root.getAnnoContext(); 
  }
  
  public Operator getOp() {
    throw new UnsupportedOperationException();
  }
}

