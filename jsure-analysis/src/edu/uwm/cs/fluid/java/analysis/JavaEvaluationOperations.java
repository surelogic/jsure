package edu.uwm.cs.fluid.java.analysis;

import edu.uwm.cs.fluid.java.control.JavaEvaluationTransfer;
import edu.uwm.cs.fluid.java.control.LatticeDelegatingJavaEvaluationTransfer;

/**
 * Operations needed for {@link JavaEvaluationTransfer}.  Lattices
 * often need to implement these directly and have the transfer function
 * invoke the method on the lattice.  Analysis implementors can use 
 * {@link LatticeDelegatingJavaEvaluationTransfer} to directly delegate to
 * a lattice implementing this interface.
 * 
 * <p>This is meant to be a mixin interface for lattice implementations.
 * 
 * @param <T> The type of the value being pushed around the analysis.
 * @param <V> The type of the value being stored on the stack.
 */
public interface JavaEvaluationOperations<T, V> {
  /**
   * Is the given analysis value "normal"?  If it isn't then transfer
   * operations should do anything to it, but instead just pass it through
   * because it represents an analysis error.
   */
  public boolean isNormal(T v);
  
  /** Pop an element from the stack and discard it. */
  public T pop(T val);
  
  /** The value to push on the stack for calls to 
   * {@link JavaEvaluationTransfer#push}. */
  public V getAnonymousStackValue();
  
  /** Push a known element onto the stack. */
  public T push(T val, V v);

  /** Peek at the top element of the stack. */
  public V peek(T val);
  
  /**
   * Pop all pending arguments from the stack because of an exception being
   * raised.
   */
  public T popAllPending(T val, int floor);

  /** Pop the second from top element from stack */
  public T popSecond(T val);

  /** Duplicate the top element on the stack */
  public T dup(T val);
}
