package edu.cmu.cs.fluid.util;

import java.util.Enumeration;

/**
 * An Iterator created around an underlying 
 * {@link Enumeration}.  The {@link java.util.Iterator#remove()}
 * operation is not supported.
 */

public class EnumerationIterator<T>
extends AbstractRemovelessIterator<T>
{
  /** Enumeration to wrap. */
  private final Enumeration<T> enumeration;

  /**
   * Create a new iterator wrapped around an
   * existing enumeration.
   * @param enum The enumeration to wrap.
   */
  public EnumerationIterator( final Enumeration<T> enm )
  {
    enumeration = enm;
  }

  @Override
  public boolean hasNext()
  {
    return enumeration.hasMoreElements();
  }

  @Override
  public T next()
  {
    return enumeration.nextElement();
  }
}
