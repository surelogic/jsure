/*$Header$*/
package edu.cmu.cs.fluid.ir;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.surelogic.Unique;

import edu.cmu.cs.fluid.util.ListIteratable;
import edu.cmu.cs.fluid.util.SubList;

public abstract class IRAbstractSequence<S,T> extends IRAbstractState<T>
implements IRSequence<T>, List<T> {
  //===========================================================
  // Implementation of List
  //===========================================================

  @Unique("return")
  public IRAbstractSequence(IRState parent) {
    super(parent);
  }

  public IRAbstractSequence() {}

  public boolean isEmpty() {
    return size() == 0;
  }

  public Iterator<T> iterator() {
    return elements();
  }

  public boolean containsAll(Collection<?> c) {
    for(Object o : c) {
      if (!contains(o)) {
        return false;
      }
    }
    return true;
  }

  public boolean addAll(Collection<? extends T> c) {
    for(T t : c) {
      add(t);
    }
    return !c.isEmpty();
  }

  public boolean addAll(int i, Collection<? extends T> c) {
    IRLocation loc = location(i);
    for(T t : c) {
      insertElementBefore(t, loc);
    }
    return !c.isEmpty();
  }

  // FIX Not efficient
  public boolean removeAll(Collection<?> c) {
    boolean removed = false;
    for(Object o : c) {
      remove(o);
      removed = true;
    }
    return removed;
  }

  // FIX Not efficient
  public boolean retainAll(Collection<?> c) {
    boolean removed = false;
    for(Object o : this) {
      if (!c.contains(o)) {
        remove(o);  
        removed = true;
      }
    }
    return removed;
  }

  // FIX Not efficient
  public void clear() {    
    for(Object o : this) {
      remove(o);  
    }
  }

  public T get(int i) {
    return elementAt(i);
  }

  public T set(int i, T v) {
    T old = elementAt(i);
    setElementAt(v, i);
    return old;
  }

  public void add(int i, T val) {
    IRLocation loc = location(i);
    insertElementBefore(val, loc);
  }

  public T remove(int i) {
    IRLocation loc = location(i);
    T old = elementAt(loc);
    removeElementAt(loc);
    return old;
  }

  /*
  public int indexOf(Object o) {
    int size = size();
    if (o == null) {
      for(int i=0; i<size; i++) {
        if (elementAt(i) == null) {
          return i;
        }
      }
    } else {
      for(int i=0; i<size; i++) {
        if (o.equals(elementAt(i))) {
          return i;
        }
      }
    }
    return -1;
  }

  public int lastIndexOf(Object o) {
    int size = size();
    if (o == null) {
      for(int i=size-1; i>=0; i--) {
        if (elementAt(i) == null) {
          return i;
        }
      }
    } else {
      for(int i=size-1; i>=0; i--) {
        if (o.equals(elementAt(i))) {
          return i;
        }
      }
    }
    return -1;
  }
  */
  
  protected abstract ListIteratable<T> createListIterator(int i);
  
  public ListIterator<T> listIterator() {
    return getSlotFactory().newListIterator(createListIterator(0));
  }

  public ListIterator<T> listIterator(int i) {
    return getSlotFactory().newListIterator(createListIterator(i));
  }
  
  public List<T> subList(int i1, int i2) {
    return new SubList<T>(this, i1, i2);
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public final boolean equals(Object o) {
    if (o instanceof List) {
      List<T> l = (List<T>) o;
      Iterator<T> it = iterator();
      for (Object elt : l) {
        Object elt2 = it.next();
        if (elt == null && elt2 != null) {
          return false;
        } else if (elt == null) {
          // Should both be null
          continue;
        } else if (!elt.equals(elt2)) { // elt != null
          return false;
        }
      }
      return true;
    }
    return false; 
  }
  
  @Override
  public final String toString() {
    StringBuilder sb = new StringBuilder(super.toString());
    sb.append("[");
    
    boolean first = true;
    for (IRLocation loc = firstLocation(); loc != null; loc = nextLocation(loc)) {
      if (first) {
        first = false;
      } else {
        sb.append(", ");
      }
      try {
        sb.append(elementAt(loc));
      } catch (RuntimeException e) {
        sb.append("???");
      }
    }
    sb.append("]");
    return sb.toString();
  }
  
  //===========================================================
  // END Implementation of List
  //===========================================================
}
