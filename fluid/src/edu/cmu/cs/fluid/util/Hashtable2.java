/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/Hashtable2.java,v 1.15 2007/07/05 18:15:14 aarong Exp $ */
package edu.cmu.cs.fluid.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

class Hashtable2Entry<K1,K2,V> implements Cloneable {
  K1 key1;
  K2 key2;
  V entry;
  Hashtable2Entry<K1,K2,V> next;
  
  Hashtable2Entry(K1 k1, K2 k2, V e, Hashtable2Entry<K1,K2,V> n) {
    key1 = k1;
    key2 = k2;
    entry = e;
    next = n;
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public Object clone() {
    try {
      Hashtable2Entry<K1,K2,V> cloned = (Hashtable2Entry<K1,K2,V>)super.clone();
      cloned.next = (Hashtable2Entry<K1,K2,V>)next.clone();
      return cloned;
    } catch (CloneNotSupportedException ex) {
      /*NOTREACHED*/
      return null;
    }
  }
}

/** A hastable with two keys returning one entry.
 * It can be used to cache the operation of a binary method.
 */
public class Hashtable2<K1,K2,V> implements Cloneable {
  private int capacity;
  private /* final */ float loadFactor;
  private int size;
  private Hashtable2Entry<K1,K2,V> contents[];
  
  @SuppressWarnings("unchecked")
  public Hashtable2(int initialCapacity, float loadFactor) {
    if (initialCapacity != 0 && initialCapacity % 2 == 0)
      ++initialCapacity;
    capacity = initialCapacity;
    if (loadFactor <= 0.0 || loadFactor >= 1.0) loadFactor = 0.75f;
    this.loadFactor = loadFactor;
    if (initialCapacity > 0) {
      contents = new Hashtable2Entry[initialCapacity];
      // stupid Java: contents = new Hashtable2Entry<K1,K2,V>[initialCapacity];
    }
  }
  
  public Hashtable2(int initialCapacity) {
    this(initialCapacity,0.75f);
  }
  
  public Hashtable2() {
    this(0);
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    boolean started = false;
    for (int i=0; i<capacity; ++i) {
      for (Hashtable2Entry<K1,K2,V> he = contents[i]; he != null; he = he.next) {
        if (started) sb.append(", ");
        else started = true;
        sb.append(he.key1);
        sb.append("*");
        sb.append(he.key2);
        sb.append("=");
        sb.append(he.entry);
      }
    }
    return sb.toString();
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public Object clone() {
    try {
      Hashtable2<K1,K2,V> other = (Hashtable2<K1,K2,V>)super.clone();
      other.contents = contents.clone();
      for (int i=0; i < capacity; ++i) {
        other.contents[i] = (Hashtable2Entry<K1,K2,V>)other.contents[i].clone();
      }
      return other;
    } catch (CloneNotSupportedException ex) {
      /*NOTREACHED*/
      return null;
    }
  }
  
  public int size() {
    return size;
  }
  
  public boolean isEmpty() {
    return size == 0;
  }
  
  private int locate(Object key1, Object key2) {
    int h1 = key1.hashCode();
    int h2 = key2.hashCode();
    int h = (h1 + (h2 << 16) + (h2 >> 16)) % capacity;
    if (h < 0) h += capacity;
    return h;
  }
  
  public V get(K1 key1, K2 key2) {
    if (size == 0) return null;
    int i = locate(key1,key2);
    for (Hashtable2Entry<K1,K2,V> he = contents[i];
    he != null;
    he = he.next) {
      // System.out.println("Looking at " + he.key1 + "*" + he.key2 + "=" + he.entry);
      if (key1.equals(he.key1) && key2.equals(he.key2))
        return he.entry;
    }
    return null;
  }
  
  public V put(K1 key1, K2 key2, V element)
  throws NullPointerException
  {
    if (element == null)
      throw new NullPointerException("cannot enter null in table");
    if (capacity == 0) rehash();
    int i = locate(key1,key2);
    for (Hashtable2Entry<K1,K2,V> he = contents[i];
    he != null;
    he = he.next) {
      if (key1.equals(he.key1) && key2.equals(he.key2)) {
        V old = he.entry;
        he.entry = element;
        return old;
      }
    }
    if ((size+1.0)/capacity > loadFactor) {
      rehash();
      return put(key1,key2,element);
    }
    // System.out.println("Adding to contents!");
    ++size;
    contents[i] = new Hashtable2Entry<K1,K2,V>(key1,key2,element,contents[i]);
    return null;
  }
  
  public V remove(K1 key1, K2 key2) {
    if (capacity == 0)
      return null;
    int i = locate(key1,key2);
    Hashtable2Entry<K1,K2,V> he = contents[i];
    if (he == null) return null;
    if (key1.equals(he.key1) &&
        key2.equals(he.key2)) {
      contents[i] = he.next;
      --size;
      return he.entry;
    }
    Hashtable2Entry<K1,K2,V> henew;
    for (; (henew=he.next) != null; he=henew) {
      if (key1.equals(henew.key1) && key2.equals(henew.key2)) {
        he.next = henew.next;
        --size;
        return henew.entry;
      }
    }
    return null;
  }
  
  private interface EntrySelector<Key1,Key2,Val,T> {
    public T select(Hashtable2Entry<Key1,Key2,Val> e);
  }
  
  class SharedIterator<T> extends AbstractRemovelessIterator<T> {
    private int i;
    private Hashtable2Entry<K1,K2,V> he;
    private final EntrySelector<K1,K2,V,T> selector;
    
    SharedIterator(int index, EntrySelector<K1,K2,V,T> s) {
      i = index;
      he = contents[index];
      selector = s;
    }
    
    public boolean hasNext() {
      return he != null;
    }
    
    public T next() {
      if (he == null) throw new NoSuchElementException("enumeration complete");
      T result = selector.select(he);
      he = he.next;
      if (he == null) {
        while (++i < contents.length) {
          if (contents[i] != null) {
            he = contents[i];
            break;
          }
        }
      }
      // System.out.println("Returning " + result);
      return result;
    }
  }
  
  private final EntrySelector<K1,K2,V,Pair<K1,K2>> keys_selector = new EntrySelector<K1,K2,V,Pair<K1,K2>>() {
	  public Pair<K1,K2> select(Hashtable2Entry<K1,K2,V> he) { return new Pair<K1,K2>(he.key1, he.key2); }
  };
  private final EntrySelector<K1,K2,V,K1> key1_selector = new EntrySelector<K1,K2,V,K1>() {
    public K1 select(Hashtable2Entry<K1,K2,V> he) { return he.key1; }
  };
  private final EntrySelector<K1,K2,V,K2> key2_selector = new EntrySelector<K1,K2,V,K2>() {
    public K2 select(Hashtable2Entry<K1,K2,V> he) { return he.key2; }
  };
  private final EntrySelector<K1,K2,V,V> value_selector = new EntrySelector<K1,K2,V,V>() {
    public V select(Hashtable2Entry<K1,K2,V> he) { return he.entry; }
  };
  
  public Iteratable<K1> keys1() {
    return enumerate(key1_selector);
  }
  public Iteratable<K2> keys2() {
    return enumerate(key2_selector);
  }
  public Iteratable<V> elements() {
    return enumerate(value_selector);
  }
  public Iteratable<Pair<K1,K2>> keys() {
	return enumerate(keys_selector);
  }
  private <T> Iteratable<T> enumerate(EntrySelector<K1,K2,V,T> selector) {
    if (size == 0)
      return new EmptyIterator<T>();
    for (int i=0; i<capacity; ++i) {
      if (contents[i] != null) {
        return new SharedIterator<T>(i,selector);
      }
    }
    /*NOTREACHED*/
    return new EmptyIterator<T>();
  }
  
  public boolean containsKey1(K1 key1) {
    return contains3(key1,null,null);
  }
  public boolean containsKey2(K2 key2) {
    return contains3(null,key2,null);
  }
  public boolean contains(V entry) {
    return contains3(null,null,entry);
  }
  
  private boolean contains3(K1 k1, K2 k2, V e) {
    for (int i=0; i < capacity; ++i) {
      for (Hashtable2Entry he = contents[i]; he != null; he=he.next) {
        if (he.key1 == k1 || he.key2 == k2 || he.entry == e)
          return true;
      }
    }
    return false;
  }
  
  public boolean containsKeys(K1 key1, K2 key2) {
    return get(key1,key2) != null;
  }
  
  @SuppressWarnings("unchecked")
  protected void rehash() {
    // System.out.print("Rehashing...");
    if (capacity == 0) {
      capacity = 11;
      contents = new Hashtable2Entry[11];
    } else {
      int newCapacity = capacity*2 + 1;
      Hashtable2<K1,K2,V> temp = new Hashtable2<K1,K2,V>(newCapacity,loadFactor);
      for (int i=0; i < capacity; ++i) {
        for (Hashtable2Entry<K1,K2,V> he = contents[i]; he != null; he=he.next) {
          temp.put(he.key1,he.key2,he.entry);
        }
      }
      capacity = newCapacity;
      contents = temp.contents;
    }
    // System.out.println(" done");
  }
  
  public void clear() {
    for (int i=0; i < capacity; ++i) {
      contents[i] = null;
    }
    size = 0;
  }

  public void putAll(Hashtable2<K1, K2, V> other) {
	  for(Pair<K1,K2> k : other.keys()) {
		  this.put(k.first(), k.second(), other.get(k.first(), k.second()));
	  }
  }
}

class TestHashtable2 {
  private final int value;
  
  TestHashtable2(int val) { value = val; }
  
  @Override
  public int hashCode() { return 1; }
  
  @Override
  public String toString() {
    return String.valueOf(value);
  }
  
  @Override
  public boolean equals(Object x) {
    if (x instanceof TestHashtable2) {
      return value == ((TestHashtable2)x).value;
    }
    return false;
  }
  
  public void reportError(String msg) {
    System.out.println("!!! "+msg);
  }
  
  public static void main(String[] args) {
    TestHashtable2 t = new TestHashtable2(0);
    t.test(args);
  }
  
  void test(String[] args) {
    test("nice", new Integer(1), new Integer(2), new Integer(3),
        new Integer(4), new Integer(5));
    test("nasty", new TestHashtable2(1), new TestHashtable2(2),
        new TestHashtable2(3), new TestHashtable2(4),
        new TestHashtable2(5));
    System.out.println("Test done.");
  }
  
  void test(String name, Object o1, Object o2,
      Object o3, Object o4, Object o5) {
    System.out.println("Testing Hashtable2 with " + name + " hash codes");
    Hashtable2<Object,Object,Object> h = new Hashtable2<Object, Object, Object>();
    h.put(o1,o1,o2);
    h.put(o1,o2,o3);
    h.put(o2,o3,o5);
    test(h,new Object[]{o1,o2,o3,o4,o5},
        new Object[]{o1,o1,o2, o1,o2,o3, o2,o3,o5});
    h.put(o1,o3,o4); h.put(o1,o4,o5); h.put(o2,o1,o3); h.put(o2,o2,o4);
    h.put(o3,o1,o4); h.put(o4,o1,o5); h.put(o3,o2,o5);
    test(h, new Object[]{o1,o2,o3,o4,o5},
        new Object[]{o1,o1,o2, o1,o2,o3, o2,o3,o5,
        o1,o3,o4, o1,o4,o5, o2,o1,o3, o2,o2,o4,
        o3,o1,o4, o4,o1,o5, o3,o2,o5});
    h.remove(o1,o2);
    test(h, new Object[]{o1,o2,o3,o4,o5},
        new Object[]{o1,o1,o2,           o2,o3,o5,
        o1,o3,o4, o1,o4,o5, o2,o1,o3, o2,o2,o4,
        o3,o1,o4, o4,o1,o5, o3,o2,o5});
    
    h.put(o1,o4,o2);
    test(h, new Object[]{o1,o2,o3,o4,o5},
        new Object[]{o1,o1,o2,           o2,o3,o5,
        o1,o3,o4, o1,o4,o2, o2,o1,o3, o2,o2,o4,
        o3,o1,o4, o4,o1,o5, o3,o2,o5});
    
    h.put(o1,o2,o3);
    h.put(o1,o4,o5);
    h.put(o1,o5,o1); h.put(o2,o4,o1); h.put(o3,o3,o1);
    h.put(o4,o2,o1); h.put(o5,o1,o1);
    h.put(o2,o5,o2); h.put(o3,o4,o2); h.put(o4,o3,o2); h.put(o5,o2,o2);
    h.put(o3,o5,o3); h.put(o4,o4,o3); h.put(o5,o3,o3);
    h.put(o4,o5,o4); h.put(o5,o4,o4); h.put(o5,o5,o5);
    
    test(h, new Object[]{o1,o2,o3,o4,o5},
        new Object[]{o1,o1,o2, o2,o1,o3, o3,o1,o4, o4,o1,o5, o5,o1,o1,
        o1,o2,o3, o2,o2,o4, o3,o2,o5, o4,o2,o1, o5,o2,o2,
        o1,o3,o4, o2,o3,o5, o3,o3,o1, o4,o3,o2, o5,o3,o3,
        o1,o4,o5, o2,o4,o1, o3,o4,o2, o4,o4,o3, o5,o4,o4,
        o1,o5,o1, o2,o5,o2, o3,o5,o3, o4,o5,o4, o5,o5,o5});
    
    h.clear();
    test(h, new Object[]{o1,o2,o3,o4,o5}, new Object[]{});
  }
  
  @SuppressWarnings("unchecked")
  void test(Hashtable2<Object,Object,Object> h, Object[] objects, Object[] entries) {
    int n = objects.length;
    //ImmutableHashOrderSet all = new ImmutableHashOrderSet(objects);
    ImmutableHashOrderSet[] entrySet = new ImmutableHashOrderSet[]{ImmutableHashOrderSet.empty,ImmutableHashOrderSet.empty,ImmutableHashOrderSet.empty};
    int size = entries.length;
    
    // System.out.println("Hashtable = " + h);
    
    // initialize entry sets:
    for (int e=0; e < size; ++e) {
      int e3 = e % 3;
      entrySet[e3] = entrySet[e3].addElement(entries[e]);
    }
    
    // test get:
    for (int i=0; i < n; ++i) {
      Object oi = objects[i];
      for (int j=0; j < n; ++j) {
        Object oj = objects[j];
        // determine an entry from table
        Object entry = null;
        for (int e = 0; e < size; e += 3) {
          if (entries[e] == oi &&
              entries[e+1] == oj)
            entry = entries[e+2];
        }
        // also look in table:
        Object result = h.get(oi,oj);
        if (result != entry) {
          if (entry == null) {
            reportError("h.get(" + oi + "," + oj + ") = " + result +
            " but should not be defined.");
          } else if (result == null) {
            reportError("h.get(" + oi + "," + oj + ") is undefined " +
                " but should be " + entry);
          } else {
            reportError("h.get(" + oi + "," + oj + ") = " + result +
                " but should be " + entry);
          }
        }
        if (h.containsKeys(oi,oj) != (entry != null)) {
          if (entry == null) {
            reportError("h.get(" + oi + "," + oj + ") is defined " +
            " but should not be defined.");
          } else {
            reportError("h.get(" + oi + "," + oj + ") is undefined " +
            " but should be defined.");
          }
        }
      }
    }
    
    test(h,objects,0,h.keys1(),entrySet[0]);
    test(h,objects,1,h.keys2(),entrySet[1]);
    test(h,objects,2,h.elements(),entrySet[2]);
  }
  
  @SuppressWarnings("unchecked")
  void test(Hashtable2<Object,Object,Object> h, Object[] objects, int kind,
      Iterator enm, ImmutableHashOrderSet set) {
    int n = objects.length;
    for (int i=0; i < n; ++i) {
      Object oi = objects[i];
      boolean result = false;
      switch (kind) {
      case 0: result = h.containsKey1(oi); break;
      case 1: result = h.containsKey2(oi); break;
      case 2: result = h.contains(oi); break;
      }
      if (result != set.contains(oi)) {
        if (result) {
          reportError("h.contains" +
              ((kind==0)?"Key1":(kind==1)?"Key2":"") +
              "(" + oi + ") is true but should be false.");
        } else {
          reportError("h.contains" +
              ((kind==0)?"Key1":(kind==1)?"Key2":"") +
              "(" + oi + ") is false but should be true.");
        }
      }
    }
    ImmutableHashOrderSet s = ImmutableHashOrderSet.empty;
    while (enm.hasNext()) {
      s = s.addElement(enm.next());
    }
    if (!s.equals(set)) {
      reportError("h." +
          ((kind==0)?"keys1":(kind==1)?"keys2":"elements") +
          "() = " + s + " but should be " + set);
    }
  }
}
