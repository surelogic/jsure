/* $Header$ */
package edu.cmu.cs.fluid.util;

import java.util.NoSuchElementException;
import org.apache.commons.collections15.MapIterator;

/**
 * An iterator with no elements. 
 */
public class EmptyMapIterator<K,V> implements MapIterator<K,V>
{
  @SuppressWarnings("unchecked")
  public static final EmptyMapIterator prototype = new EmptyMapIterator();

  public EmptyMapIterator()
  {
  }

  public boolean hasNext()
  {
    return false;
  }
  
  @SuppressWarnings("unchecked")
  public static <K,V> MapIterator<K,V> prototype() { return prototype; }

  public K next() {
    throw new NoSuchElementException( "Iterator complete." );
  }

  public K getKey() {
    throw new NoSuchElementException( "Iterator complete." );
  }

  public V getValue() {
    throw new NoSuchElementException( "Iterator complete." );
  }

  public void remove() {
    throw new NoSuchElementException( "Iterator complete." );
  }

  public V setValue(V arg0) {
    throw new NoSuchElementException( "Iterator complete." );
  }  
}
