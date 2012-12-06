/*
 * Created on Feb 15, 2005
 *
 */
package edu.cmu.cs.fluid.util;

import java.util.*;

import edu.cmu.cs.fluid.NotImplemented;
import com.surelogic.Starts;

/**
 * @author Edwin
 *
 */
@SuppressWarnings("unchecked")
public class SetMultiMap<K,V> extends CustomHashMap2 implements IMultiMap<K,V> {
  private static final HashEntryFactory prototype = new SetEntryFactory();
  
  public SetMultiMap() {
    super(prototype);
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.util.IMultiMap#map(java.lang.Object, java.lang.Object)
   */
  public void map(K key, V val) {
    Set<V> s = (Set<V>) getEntryAlways(key);
    s.add(val);
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.util.IMultiMap#remove(java.lang.Object, java.lang.Object)
   */
  public void remove(K key, V val) {
    Collection<V> s = find(key);
    if (s != null) {
      s.remove(val);
    }
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.util.IMultiMap#removeAll(java.lang.Object)
   */
  public Collection<V> removeAll(K key) {
    return (Collection<V>) remove(key); 
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.util.IMultiMap#find(java.lang.Object)
   */
  public Collection<V> find(K key) {
    Set<V> s = (Set<V>) get(key);
    return s;
  }
  
  static class SetEntry extends HashSet implements IHashEntry, IMultiMap.Entry {
    final int hash;
    final Object key;
    IHashEntry next;
    
    SetEntry(int h, Object k) {
      hash = h;
      key = k;
    }
    
    @Starts("nothing")
	public Object getKey() {
      return key;  
    }

    @Starts("nothing")
	public Object getValue() {
      return this;
    }

    public Object setValue(Object value) {
      throw new NotImplemented("The Set cannot be replaced");
    }

    public int getHash() {
      return hash;
    }

    public IHashEntry getNext() {
      return next;
    }

    public void setNext(IHashEntry newNext) {
      next = newNext;
    }

    public Collection getValues() {
      return this;
    }    
  }
  
  static class SetEntryFactory implements HashEntryFactory {
    void checkValue(Object value) {
      if (value != null) {
        System.err.println("Ignoring attempt to create SetEntry with value "+value);
      }
    }
    
    public IHashEntry create(Object key, Object value, int hash) {
      checkValue(value);
      return new SetEntry(hash, key);
    }

    public Map.Entry create(Object key, Object value) {
      checkValue(value);
      return new SetEntry(CopiedHashMap2.hash(key), key);
    }

    public boolean isValid(Map.Entry entry) {
      return (entry instanceof SetEntry);
    }

    /**
     * Copy value/contents
     */
    public void copy(Map.Entry from, Map.Entry to) {
      SetEntry f = (SetEntry) from;
      SetEntry t = (SetEntry) to;      
      t.clear();
      t.addAll(f);
    } 
  }
}


