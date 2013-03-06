package edu.cmu.cs.fluid.util;

import java.util.Iterator;

/**
 * An Iterator wrapper that never supports the 
 * {@link Iterator#remove()} operation.
 */

public class RemovelessIterator<T>
extends AbstractRemovelessIterator<T>
{
  /** Iterator to wrap. */
  private final Iterator<T> iterator;

  /**
   * Create a new iterator wrapped around an
   * existing iterator.
   * @param iter The iterator to wrap.
   */
  public RemovelessIterator( final Iterator<T> iter )
  {
    super();
    iterator = iter;
  }

  @Override
  public boolean hasNext()
  {
    return iterator.hasNext();
  }

  @Override
  public T next()
  {
    return iterator.next();
  }
}
