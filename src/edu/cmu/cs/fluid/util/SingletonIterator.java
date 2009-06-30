package edu.cmu.cs.fluid.util;

import java.util.NoSuchElementException;

/**
 * An Iterator that contains a single element.
 */

public class SingletonIterator<T>
extends AbstractRemovelessIterator<T>
{
  /** element to wrap. */
  private final T object;
  private boolean hasNext;

  /**
   * Create a new iterator wrapped around a single element.
   * @param item The element to wrap.
   */
  public SingletonIterator( final T item )
  {
    super();
    object = item;
    hasNext = true;
  }

  public boolean hasNext()
  {
    return hasNext;
  }

  public T next()
  {
    if( hasNext ) {
      hasNext = false;
      return object;
    } else {
      throw new NoSuchElementException( "No more elements" );
    }
  }
}
