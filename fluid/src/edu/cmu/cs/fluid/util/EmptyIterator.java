/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/EmptyIterator.java,v 1.11 2007/05/17 18:57:53 chance Exp $ */
package edu.cmu.cs.fluid.util;

import java.util.*;

/**
 * An iterator with no elements. 
 */
public class EmptyIterator<T> extends AbstractRemovelessIterator<T> implements ListIterator<T>
{
  public EmptyIterator()
  {
    super();
  }

  public boolean hasNext()
  {
    return false;
  }

  public T next()
  {
    throw new NoSuchElementException( "Iterator complete." );
  }
  
  public boolean hasPrevious() {
    return false;
  }

  public T previous() {
    throw new NoSuchElementException( "Iterator complete." );
  }

  public int nextIndex() {
    return -1;
  }

  public int previousIndex() {
    return -1;
  }

  public void set(final T arg0) {
    throw new UnsupportedOperationException( "set() not supported" );
  }

  public void add(final T arg0) {
    throw new UnsupportedOperationException( "add() not supported" );
  }  

  private static final EmptyIterator<Object> prototype = new EmptyIterator<Object>();
  
  @SuppressWarnings("unchecked")
  public static <T> T prototype() {
	  return (T) prototype;
  }
}
