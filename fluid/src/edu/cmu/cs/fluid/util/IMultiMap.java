/*
 * Created on Feb 15, 2005
 *
 */
package edu.cmu.cs.fluid.util;

import java.util.*;

/**
 * A map from a key to a List of elements
 * 
 * @author Edwin
 *
 */
public interface IMultiMap<K,V> {
  /**
   * Does not create another mapping if val already is mapped
   * 
   * @param key
   * @param val
   */
  void map(K key, V val);
  void remove(K key, V val);  
  
  /**
   * Removes all mappings for the given key
   */
  Collection<V> removeAll(K key);
  
  void clear();
  
  /**
   * @return The mutable collection of mappings (not a copy)
   */
  Collection<V> find(K key);
  
  Set<Entry<K,V>> entrySet();
  
  public interface Entry<K,V> {
    K getKey();
    Collection<V> getValues();
  }
}
