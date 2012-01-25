package edu.cmu.cs.fluid.util;

import java.util.Iterator;
import static edu.cmu.cs.fluid.util.IteratorUtil.noElement;

/**
 * An Iterator wrapper that can both filter and expand objects
 * (for example, if they are Iterators) in the original Iterator.
 */

public abstract class ProcessIterator<T> extends SimpleRemovelessIterator<T>
{
  private final Iterator<T> iterator;  /** Iterator to wrap. */

  // null means we're done
  private Iterator<T> nestedIter = new EmptyIterator<T>(); 

  /**
   * Create a new iterator wrapped around an
   * existing iterator.
   * @param iter The iterator to wrap.
   */
  public ProcessIterator( final Iterator<T> iter ) {
    iterator = iter;
  }

  @Override
  protected Object computeNext() {
    while (nestedIter != null) {
      while(nestedIter.hasNext()) {
        Object o = select(nestedIter.next());
        if (o == notSelected) {
          // get more nodes to check 
          continue;
        }
        return o;
      }
      if (!iterator.hasNext()) {
        nestedIter = null;
        return noElement;
      }
      nestedIter = getNextIter(iterator.next());
    }
    return noElement;
  }

  @SuppressWarnings("unchecked")
  protected Iterator<T> getNextIter(Object o) { return (Iterator<T>) o; }

  protected Object select(Object o) { return o; }

  protected final Object notSelected = new Object();
}

