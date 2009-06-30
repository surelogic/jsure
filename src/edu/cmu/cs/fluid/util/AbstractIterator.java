package edu.cmu.cs.fluid.util;

import java.util.Iterator;

public abstract class AbstractIterator<T>
implements Iteratable<T>
{
  /**
   * Create a new iterator
   */
  public AbstractIterator()
  {
    super();
  }
  
  public final Iterator<T> iterator() {
    return this;
  }
}
