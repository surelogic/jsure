package edu.cmu.cs.fluid.util;

import java.util.Iterator;
import static edu.cmu.cs.fluid.util.IteratorUtil.noElement;

/**
 * An Iterator wrapper that only filters out objects in the original Iterator
 */

public abstract class FilterIterator<T,T2> extends SimpleIterator<T2>
{
  /** Iterator to wrap. */
  private final Iterator<T> iterator;

  /**
   * Create a new iterator wrapped around an
   * existing iterator.
   * @param iter The iterator to wrap.
   */
  public FilterIterator( final Iterator<T> iter ) {
    iterator = iter;
  }

  public void remove() {
    iterator.remove();
  }

  @Override
  protected Object computeNext() {
    while(iterator.hasNext()) {
      Object o = select(iterator.next());
      if (o == noElement) {
        // get more nodes to check 
        continue;
      }
      return o;
    }
    return noElement;
  }

  /** 
   * Returns the transformed object, or noElement
   */
  protected abstract Object select(T o);
}
