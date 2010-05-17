package com.surelogic.util;

/**
 * A true thunk implementation.  The {@link #evaluation} method is called
 * to generate the value the first time {@link #getValue} is called.  After that,
 * the value is cached.  So if <code>getValue()</code> is never called,
 * <code>evaluate</code> is never called.  Otherwise, <code>evaluate</code> is
 * called exactly once.
 */
public abstract class Thunk<R> implements IThunk<R> {
  private boolean evaluated = false;
  private R value = null;
  
  protected Thunk() {
    super();
  }
  
  /**
   * Override to define the value of the thunk.
   * @return
   */
  protected abstract R evaluate();
  
  public final R getValue() {
    if (!evaluated) {
      value = evaluate();
      evaluated = true;
    }
    return value;
  }
  
  public final boolean isEvaluated() {
    return evaluated;
  }
}
