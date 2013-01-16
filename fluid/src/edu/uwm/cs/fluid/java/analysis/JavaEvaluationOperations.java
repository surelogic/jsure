package edu.uwm.cs.fluid.java.analysis;

import edu.uwm.cs.fluid.java.control.JavaEvaluationTransfer;

/**
 * Operations needed for {@link JavaEvaluationTransfer}.  Lattices
 * often need to implement these directly and have the transfer function
 * invoke the method on the lattice.  Should probably retroactively
 * update {@link JavaEvaluationTransfer} to implement this method too, but
 * right now the classes are in the wrong packages for that.  (Also,
 * {@link JavaEvaluationOperations#popAllPending(Object)} has a different
 * signature here to capture the fact that the transfer function has a floor
 * on the stack height to deal with sub analysis issues.)
 * 
 * @param <T> The type of the value being pushed around the analysis.
 * @param <V> The type of the value being stored on the stack.
 */
public interface JavaEvaluationOperations<T, V> {
  /** Pop an element from the stack and discard it. */
  public T pop(T val);
  
  /** Push an unknown element onto the stack. */
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
