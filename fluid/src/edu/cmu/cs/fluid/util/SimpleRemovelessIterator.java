// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/SimpleRemovelessIterator.java,v 1.6 2005/05/24 21:41:58 chance Exp $
package edu.cmu.cs.fluid.util;


/** An Iterator class with one way to get the next element.
 */
public abstract class SimpleRemovelessIterator<T> extends SimpleIterator<T> {
  public SimpleRemovelessIterator() { super(); }
  public SimpleRemovelessIterator(T initial) { super(initial); }
  @Override
  public final void remove()
  {
    throw new UnsupportedOperationException( "remove() not supported" );
  }
}
