package com.surelogic.util;

/**
 * Interface for a thunk that returns a value of type <code>R</code>.
 */
public interface IThunk<R> {
  /**
   * Evaluate, if necessary, and return a value.
   */
  public R getValue();
  
  /**
   * Is the thunk evaluated?
   */
  public boolean isEvaluated();
}
