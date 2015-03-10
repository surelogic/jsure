package edu.cmu.cs.fluid.java.operator;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * A {@link Visitor} class that performs a tree walk. This is a simple
 * convenience subclass for client code.  The visitor enforces a {@link java.lang.Void}
 * return type on the visitation methods.  If your tree walk needs to aggregate
 * results from the visitation methods, consider subclassing
 * {@link TreeWalkVisitor} instead.
 * 
 * <p>{@link #visit(IRNode)} is reimplemented to visit the children of the node
 * instead of to do nothing.
 * 
 * <p>
 * This class is intended to be subclassed.
 * 
 * @see Visitor
 * @see TreeWalkVisitor
 */
public abstract class VoidTreeWalkVisitor extends Visitor<Void> {

  /**
   * Causes the class to visit an entire subtree.
   */
  @Override
  public final Void visit(final IRNode node) {
    handleNode(node);
    doAcceptForChildren(node);
    return null;
  }
  
  protected void handleNode(final IRNode node) {
    // do nothing
  }
}
