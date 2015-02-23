/*$Header$*/
package edu.cmu.cs.fluid.util;

import java.util.*;

public class SubListIterator<T> implements ListIterator<T> {
  private final List<T> l;
  private final int offset, size;
  private int index;
  
  public SubListIterator(List<T> l2, int offset2, int size2, int i) {
    l = l2;
    offset = offset2;
    size = size2;
    index = i;
  }
  
  public SubListIterator(List<T> l2, int offset2, int size2) {
    this(l2, offset2, size2, 0);
  }
  
  public boolean hasNext() {
    return index < size;
  }
  
  public T next() {
    if (hasNext()) {
      T val = l.get(offset+index);
      index++;
      return val;
    }
    throw new NoSuchElementException();
  }
  
  public boolean hasPrevious() {
    return index > 0;
  }
  
  public T previous() {
    if (hasPrevious()) {
      index--;
      T val = l.get(offset+index);
      return val;
    }
    throw new NoSuchElementException();
  }
  
  public int nextIndex() {
    return index;
  }
  
  public int previousIndex() {
    return index-1;
  }
  
  public void remove() {
    throw new UnsupportedOperationException("sublists cannot be modified");
  }
  
  public void set(T val) {
    l.set(offset+index, val);
  }
  
  public void add(T arg0) {
    throw new UnsupportedOperationException("sublists cannot be modified");
  }    
}
