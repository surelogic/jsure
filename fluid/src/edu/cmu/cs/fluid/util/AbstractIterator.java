package edu.cmu.cs.fluid.util;

import java.util.Iterator;

import com.surelogic.*;

public abstract class AbstractIterator<T>
implements Iteratable<T>
{
  /**
   * Create a new iterator
   */
  @Starts("nothing")
  @RegionEffects("reads Instance")
  public AbstractIterator()
  {
    super();
  }
  
  public final Iterator<T> iterator() {
    return this;
  }
}
