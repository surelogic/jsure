package edu.cmu.cs.fluid.java.operator;

import java.util.List;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * A {@link Visitor} class that performs a tree walk. This is a simple
 * convenience subclass for client code.  This tree walker, unlike 
 * {@link VoidTreeWalkVisitor} supports visitation methods with return types.
 * The {@link #visit} method invokes {@link edu.cmu.cs.fluid.java.operator.Visitor#doAcceptForChildrenWithResults(IRNode)}
 * and then invokes the method {@link #mergeResults(List)} to convert the list 
 * of result values into a single result value.  The implementor of a subclass
 * must implement the {@link #mergeResults(List)} method as appropriate.  
 * 
 * <p>
 * This class is intended to be subclassed.
 * 
 * @see Visitor
 * @see VoidTreeWalkVisitor
 */
public abstract class TreeWalkVisitor<T> extends Visitor<T> {

  /**
   * Causes the class to visit an entire subtree.
   */
  @Override
  public final T visit(IRNode node) {
    return mergeResults(doAcceptForChildrenWithResults(node));
  }
  
  /**
   * Merge a list of values of type <code>T</code> into a 
   * single value of type <code>T</code>.
   */
  protected abstract T mergeResults(List<T> results);
}
