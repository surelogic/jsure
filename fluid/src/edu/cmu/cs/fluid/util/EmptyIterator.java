/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/EmptyIterator.java,v 1.11 2007/05/17 18:57:53 chance Exp $ */
package edu.cmu.cs.fluid.util;

import java.util.*;

/**
 * An iterator with no elements. 
 */
public class EmptyIterator<T> extends AbstractRemovelessIterator<T> implements ListIterator<T>
{
  @SuppressWarnings("unchecked")
  public static final EmptyIterator prototype = new EmptyIterator();

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
  
  @SuppressWarnings("unchecked")
  public static <T> Iteratable<T> prototype() { return prototype; }
  
  @SuppressWarnings("unchecked")
  public static <T> ListIterator<T> listIterator() { return prototype; }

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

  public void set(T arg0) {
    throw new UnsupportedOperationException( "set() not supported" );
  }

  public void add(T arg0) {
    throw new UnsupportedOperationException( "add() not supported" );
  }  
}
