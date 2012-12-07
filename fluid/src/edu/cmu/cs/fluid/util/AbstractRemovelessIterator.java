package edu.cmu.cs.fluid.util;

import java.util.Iterator;

import com.surelogic.RegionEffects;
import com.surelogic.Starts;

/**
 * An Iterator that never supports the 
 * {@link Iterator#remove()} operation.
 */

public abstract class AbstractRemovelessIterator<T> extends AbstractIterator<T>
{
  /**
   * Create a new iterator
   */
  @Starts("nothing")
  @RegionEffects("reads Instance")
  public AbstractRemovelessIterator()
  {
    super();
  }

  /**
   * This operation is not supported.
   * @throws UnsupportedOperationException Always thrown.
   */
  public final void remove()
  {
    throw new UnsupportedOperationException( "remove() not supported" );
  }
}
