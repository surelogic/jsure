/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/IsomorphicHashMap.java,v 1.2 2007/04/09 15:26:54 chance Exp $*/
package edu.cmu.cs.fluid.util;

import java.util.*;

/**
 * TODO eventually replace implementation
 * @author Edwin.Chan
 */
public class IsomorphicHashMap<K,V> extends HashMap<K,V> implements IsomorphicMap<K,V> {
  // Doubles the space
  Map<V,K> inverse = new HashMap<V,K>();
  
  @Override
  public V put(K key, V val) {
    if (containsKeyFor(val)) {
      K key2 = getCorrespondingKey(val);
      if (!key.equals(key2)) {
        throw new IllegalArgumentException("value '"+val+"' already has a mapping");
      }
    }
    inverse.put(val, key);
    return super.put(key, val);
  }
  
  @Override
  public boolean containsKeyFor(V val) {
    return inverse.containsKey(val);
  }

  @Override
  public K getCorrespondingKey(V val) {
    return inverse.get(val);
  }

  @Override
  public K removeCorrespondingKey(V val) {
    K key = getCorrespondingKey(val);
    inverse.remove(val);
    this.remove(key);
    return key;
  }
  
  /* Slow implementation
   * 
  private Entry<K,V> getEntryFor(V val) {
    if (val == null) {
      for (Entry<K,V> e : entrySet()) {
        if (e.getValue() == null) {
          return e;
        }
      }
    } else { // not-null
      for (Entry<K,V> e : entrySet()) {
        if (val.equals(e.getValue())) {
          return e;
        }
      }
    }
    return null;
  }
  
  public boolean containsKeyFor(V val) {
    return getEntryFor(val) != null;
  }
  
  public K getCorrespondingKey(V val) {
    return getEntryFor(val).getKey();
  }
  
  public V put(K key, V val) {
    if (containsKeyFor(val)) {
      throw new IllegalArgumentException("value already has a mapping");
    }
    return super.put(key, val);
  }
  */
}
