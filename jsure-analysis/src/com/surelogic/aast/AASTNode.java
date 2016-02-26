/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/AASTNode.java,v 1.9 2007/09/24 21:09:56 ethan Exp $*/
package com.surelogic.aast;

import java.util.List;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.tree.Operator;

import com.surelogic.aast.INodeModifier.Status;
import com.surelogic.aast.visitor.DescendingVisitor;

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
  public final IAASTNode cloneTree() {
	return cloneOrModifyTree(INodeModifier.CLONE, Status.CLONE);
  }

  @Override
  public final IAASTNode modifyTree(final INodeModifier mod) {
	final Status status = mod.createNewAAST(this);
	if (status == Status.KEEP) {
	  // Need to check the rest of the AAST
	  final boolean changing = this.accept(new DescendingVisitor<Boolean>(Boolean.FALSE) {
		@Override	  
		public Boolean doAccept(AASTNode node) {
		  if (node != null && mod.createNewAAST(node) != Status.KEEP) {
			return Boolean.TRUE;
		  }
		  return super.doAccept(node);
		}
		protected Boolean combineResults(Boolean before, Boolean next) {
		  return before || next;
		}
	  });
	  if (!changing) {
		return this;
	  }
	}
	return cloneOrModifyTree(mod, status);
  }
  
  public final IAASTNode cloneOrModifyTree(final INodeModifier mod) {
	return cloneOrModifyTree(mod, mod.createNewAAST(this));
  }
  
  /**
   * If mod says to KEEP, then we'll clone it
   */
  protected final IAASTNode cloneOrModifyTree(final INodeModifier mod, final Status status) {
	switch (status) {
	case CLONE:
	case KEEP:
		return internalClone(mod);
	case MODIFY:
		return mod.modify(this);
	default:
		throw new IllegalStateException("Unknown: "+status);
	}
  }
  
  /**
   * Clone this node, but call cloneOrModifyTree() on children
   */
  protected abstract IAASTNode internalClone(final INodeModifier mod);
  
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

