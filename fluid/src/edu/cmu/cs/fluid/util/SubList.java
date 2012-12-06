/*$Header$*/
package edu.cmu.cs.fluid.util;

import java.util.*;
import com.surelogic.Starts;

public class SubList<T> implements List<T> {
  private final List<T> l;
  private final int offset, size;
  
  public SubList(List<T> a, int i1, int i2) {
    l = a;
    offset = i1;
    size = i2-i1;
    if (0 > i1 || i1 >= l.size()) {
      throw new IllegalArgumentException();
    }
    if (0 > i2 || i2 >= l.size()) {
      throw new IllegalArgumentException();
    }
    if (i2 < i1) {
      throw new IllegalArgumentException();
    }
  }

  private void checkBounds(int i) {
    if (0 > i || i >= size) {
      throw new IllegalArgumentException();
    }
  }
  
  @Starts("nothing")
public int size() {
    return size;
  }

  @Starts("nothing")
public boolean isEmpty() {
    return size == 0;
  }

  @Starts("nothing")
public boolean contains(Object o) {
    return indexOf(o) >= 0;
  }

  @Starts("nothing")
public Iterator<T> iterator() {
    return new SubListIterator<T>(l, offset, size);
  }

  @Starts("nothing")
public Object[] toArray() {
    Object[] a = new Object[size];
    for(int i=0; i<size; i++) {
      a[i] = get(i);
    }
    return a;
  }

  @SuppressWarnings("unchecked")
  public <E> E[] toArray(E[] a) {
    if (a.length < size) {
      a = (E[])java.lang.reflect.Array.
      newInstance(a.getClass().getComponentType(), size);
    }
    for(int i=0; i<size; i++) {
      a[i] = (E) get(i);
    }
    if (a.length > size)
      a[size] = null;
    return a;    
  }

  public boolean add(T arg0) {
    throw new UnsupportedOperationException("sublists cannot be modified");
  }

  @Starts("nothing")
public boolean remove(Object arg0) {
    throw new UnsupportedOperationException("sublists cannot be modified");
  }

  public boolean containsAll(Collection c) {
    for(Object o : c) {
      if (!contains(o)) {
        return false;
      }
    }
    return true;
  }

  public boolean addAll(Collection arg0) {
    throw new UnsupportedOperationException("sublists cannot be modified");
  }

  public boolean addAll(int arg0, Collection arg1) {
    throw new UnsupportedOperationException("sublists cannot be modified");
  }

  public boolean removeAll(Collection arg0) {
    throw new UnsupportedOperationException("sublists cannot be modified");
  }

  public boolean retainAll(Collection arg0) {
    throw new UnsupportedOperationException("sublists cannot be modified");
  }

  @Starts("nothing")
public void clear() {
    for(int i=offset, j=0; j<size; i++, j++) {
      l.set(i, null);
    }
  }

  @Starts("nothing")
public T get(int i) {
    checkBounds(i);
    return l.get(offset + i);
  }

  public T set(int i, T val) {
    checkBounds(i);
    return l.set(offset + i, val);
  }

  public void add(int i, T arg1) {
    throw new UnsupportedOperationException("sublists cannot be modified");
  }

  @Starts("nothing")
public T remove(int i) {
    throw new UnsupportedOperationException("sublists cannot be modified");
  }

  @Starts("nothing")
public int indexOf(Object o) {
    if (o == null) {
      for(int i=offset, j=0; j<size; i++, j++) {
        if (l.get(i) == null) {
          return j;
        }
      }
    } else {
      for(int i=offset, j=0; j<size; i++, j++) {
        if (o.equals(l.get(i))) {
          return j;
        }
      }
    }
    return -1;
  }

  @Starts("nothing")
public int lastIndexOf(Object o) {
    if (o == null) {
      for(int j=size-1, i=offset+j; j>=0; i--, j--) {
        if (l.get(i) == null) {
          return j;
        }
      }
    } else {
      for(int j=size-1, i=offset+j; j>=0; i--, j--) {
        if (o.equals(l.get(i))) {
          return j;
        }
      }
    }
    return -1;
  }

  @Starts("nothing")
public ListIterator<T> listIterator() {
    return new SubListIterator<T>(l, offset, size);
  }

  @Starts("nothing")
public ListIterator<T> listIterator(int i) {
    checkBounds(i);
    return new SubListIterator<T>(l, offset, size, i);
  }

  @Starts("nothing")
public List<T> subList(int i1, int i2) {
    return new SubList<T>(l, offset+i1, offset+i2);
  }
}
