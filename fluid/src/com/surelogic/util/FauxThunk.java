/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.util;

/**
 * An implementation of thunk that is already preevaluated.  Therefore, it 
 * isn't really a thunk, but it allows simple values to be provided to contexts
 * that require a thunk.  Essentially a wrapper class around <code>R</code>
 * values.
 */
public final class FauxThunk<R> implements IThunk<R> {
  private final R value;
  
  public FauxThunk(final R v) {
    value = v;
  }
  
  @Override
  public R getValue() {
    return value;
  }
  
  /**
   * Always returns <code>true</code>.
   */
  @Override
  public boolean isEvaluated() {
    return true;
  }
}
