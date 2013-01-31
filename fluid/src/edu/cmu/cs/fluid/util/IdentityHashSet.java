/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/IdentityHashSet.java,v 1.3 2007/10/15 19:55:17 chengt Exp $*/
package edu.cmu.cs.fluid.util;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Set;
import com.surelogic.Starts;

public class IdentityHashSet<E> extends AbstractSet<E> {

  private static final Object nullProxy = new Object();
  private static final Object removedProxy = new Object();
  
  private static final double maxFill = 0.75;
  private static final int initialCapacity = 200;
  
  private E[] values;
  private int size;
  private int used;
  private int version;
  
  @SuppressWarnings("unchecked")
  private E[] makeArray(int n) {
    if (n < 1) throw new IllegalArgumentException("array too small.");
    used = 0;
    return (E[])new Object[n];
  }
  
  public IdentityHashSet() {
    clear();
  }
  
  public IdentityHashSet(Set<E> other) {
    int size2 = other.size();
    resizeArray(size2);
    addAll(other);
  }

  private void resizeArray(int size2) {
    values = makeArray((int)(2 * size2 / maxFill)+initialCapacity);
  }
  
  @Starts("nothing")
@Override
  public Iterator<E> iterator() {
    return new Iterator<E>() {
      int v = version;
      int old = -1;
      int i = -1;
      {
        move();
      }
      private void move() {
        old = i++;
        while (i < values.length && ((values[i] == null) || (values[i] == removedProxy))) {
          ++i;
        }
      }
      
      @Override
      public boolean hasNext() {
        if (v != version) throw new ConcurrentModificationException("set modified during iteration");
        return i < values.length;
      }

      @Override
      public E next() {
        if (v != version) throw new ConcurrentModificationException("set modified during iteration");
        E result = values[i];
        if (result == nullProxy) result = null;
        move();
        return result;
      }

      @Override
      @SuppressWarnings("unchecked")
      public void remove() {
        if (v != version) throw new ConcurrentModificationException("set modified during iteration");
        if (old == -1) throw new IllegalStateException("remove called at wrong time");
        values[old] = (E)removedProxy;
        --size;
        old = -1;
        v = ++version;
      } 
    };
  }

  @Starts("nothing")
@Override
  public int size() {
    return size;
  }

   private int hash(Object elem, boolean skipRemoved) {
	int i = (elem.hashCode() & 0x7FFFFFFF) % values.length;
    if (i < 0) i += values.length;
    for (;;) {
      E other = values[i];
      if (other == elem) return i;
      if (other == null) return i;
      if (!skipRemoved && other == removedProxy) return i;
      ++i;
      if (i == values.length) i = 0;
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T mapNull(T elem) {
    if (elem == null) elem = (T)nullProxy;
    return elem;
  }
  
  @Override
  public boolean add(E o) {
    if (contains(o)) return false;
    if (values.length*maxFill <= used) rehash();
    o = mapNull(o);
    int i = hash(o,false);
    ++version;
    ++used;
    ++size;
    values[i] = o;
    return true;
  }

  @Starts("nothing")
@Override
  public boolean contains(Object o) {
    o = mapNull(o);
    return values[hash(o,true)] == o;
  }

  @Starts("nothing")
@SuppressWarnings("unchecked")
  @Override
  public boolean remove(Object o) {
    o = mapNull(o);
    int i = hash(o,true);
    if (values[i] == o) {
      values[i] = (E)removedProxy;
      --size;
      ++version;
      return true;
    }
    return false;
  }

  @Starts("nothing")
@Override
  public void clear() {
    size = 0;
    values = makeArray(initialCapacity);
    ++version;
  }
  
  protected void rehash() {
    E[] old = values;
    resizeArray(size);
    ++version;
    for (E value : old) {
      if (value == null || value == removedProxy) continue;
      values[hash(value,false)] = value;
    }
    used = size;
  }
}

class TestIdentityHashSet {
  public static void main(String[] args) {
    Set<Integer> s = new IdentityHashSet<Integer>();
    for (int i=0; i < 5; ++i) {
      if (s.size() != i) {
        System.out.println("!!! size is wrong (expected " + i + "), size = " + s.size());
      }
      boolean b = s.add(IntegerTable.newInteger(i));
      if (!b) {
        System.out.println("!!! add found element already there!");
      }
    }
    for (int i=0; i < 5; ++i) {
      if (s.size() != 5) {
        System.out.println("!!! size is wrong (expected " + 5 + "), size = " + s.size());
      }
      boolean b = s.add(IntegerTable.newInteger(i));
      if (b) {
        System.out.println("!!! add already there added again!");
      }
    } 
    for (int i=0; i < 5; ++i) {
      if (s.size() != 5-i) {
        System.out.println("!!! size is wrong (expected " + (5-i) + "), size = " + s.size());
      }
      boolean b = s.remove(IntegerTable.newInteger(i));
      if (!b) {
        System.out.println("!!! remove didn't find it!");
      }
    }
    for (int i=0; i < 10; ++i) {
      if (s.size() != i) {
        System.out.println("!!! size is wrong (expected " + i + "), size = " + s.size());
      }
      boolean b = s.add(IntegerTable.newInteger(i));
      if (!b) {
        System.out.println("!!! add found element already there!");
      }
    }
    for (int i=10; i < 25; ++i) {
      if (s.size() != i) {
        System.out.println("!!! size is wrong (expected " + i + "), size = " + s.size());
      }
      boolean b = s.add(IntegerTable.newInteger(i));
      if (!b) {
        System.out.println("!!! add found element already there!");
      }
    }
    Set<Integer> other = new IdentityHashSet<Integer>();
    boolean toggle = true;
    for (Iterator<Integer> it = s.iterator(); it.hasNext(); ) {
      Integer n = it.next();
      if (toggle) {
        it.remove();
        other.add(n);
        toggle = false;
      } else {
        toggle = true;
      }
    }
    if (s.size() != 12 || other.size() != 13) {
      System.out.println("!!! Toggle didn't? " + s.size() + ", " + other.size());
    }
    s.addAll(other);
    if (s.size() != 25) {
      System.out.println("!!! Lost some elements? now " + s.size());
    }
  }
}