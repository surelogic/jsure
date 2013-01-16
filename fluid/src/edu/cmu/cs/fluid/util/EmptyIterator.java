/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/EmptyIterator.java,v 1.11 2007/05/17 18:57:53 chance Exp $ */
package edu.cmu.cs.fluid.util;

import java.util.*;
import com.surelogic.*;

/**
 * An iterator with no elements. 
 */
public class EmptyIterator<T> extends AbstractRemovelessIterator<T> implements ListIterator<T>
{
  @Starts("nothing")
  @RegionEffects("reads Instance")  
  public EmptyIterator()
  {
    super();
  }

  @Override
  @Borrowed("this")
@RegionEffects("reads Instance")
public boolean hasNext()
  {
    return false;
  }

  @Override
  @Borrowed("this")
@RegionEffects("writes Instance")
public T next()
  {
    throw new NoSuchElementException( "Iterator complete." );
  }
  
  @Override
  @Borrowed("this")
@RegionEffects("reads Instance")
public boolean hasPrevious() {
    return false;
  }

  @Override
  @Borrowed("this")
@RegionEffects("writes Instance")
public T previous() {
    throw new NoSuchElementException( "Iterator complete." );
  }

  @Override
  @Borrowed("this")
@RegionEffects("reads Instance")
public int nextIndex() {
    return -1;
  }

  @Override
  @Borrowed("this")
@RegionEffects("reads Instance")
public int previousIndex() {
    return -1;
  }

  @Override
  public void set(final T arg0) {
    throw new UnsupportedOperationException( "set() not supported" );
  }

  @Override
  public void add(final T arg0) {
    throw new UnsupportedOperationException( "add() not supported" );
  }  

  private static final EmptyIterator<Object> prototype = new EmptyIterator<Object>();
  
  @SuppressWarnings("unchecked")
  public static <T> T prototype() {
	  return (T) prototype;
  }
}
