package edu.cmu.cs.fluid.util;

import java.util.Iterator;

public abstract class IteratorWrapper<T>
extends AbstractRemovelessIterator<T>
{
  private final Iterator<T> iter;
  
  public IteratorWrapper( final Iterator<T> i ) {
    iter = i;
  }
  
  public final boolean hasNext() {
    return iter.hasNext();
  }
  
  public final T next() {
    final T o = iter.next();
    return processObject( o );
  }
  
  protected abstract T processObject( T o );
}
