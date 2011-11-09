/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/BidirectionalHashMap.java,v 1.6 2008/06/30 22:16:29 chance Exp $*/
package edu.cmu.cs.fluid.util;

import java.util.*;

/**
 * TODO eventually replace implementation to reduce space
 * @author Edwin.Chan
 */
public class BidirectionalHashMap<K,V> extends HashMap<K,V> implements BidirectionalMap<K,V> {
  // Doubles the space
  IMultiMap<V,K> inverse = new SetMultiMap<V,K>();
  
  @Override
  public V put(K key, V val) {    
    V oldVal = super.put(key, val);
    if (val == oldVal) {
      return oldVal;
    }
    inverse.remove(oldVal, key);
    inverse.map(val, key);
    return oldVal;
  }
  
  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
	  if (m == null) {
		  return;
	  }
	  for(Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
		  this.put(e.getKey(), e.getValue());
	  }
  }
  
  public boolean containsKeyFor(V val) {
    return !inverse.find(val).isEmpty();
  }

  public Collection<K> getCorrespondingKeys(V val) {
    return inverse.find(val);
  }

  public Collection<K> removeCorrespondingKeys(V val) {
    Collection<K> keys = getCorrespondingKeys(val);
    if (keys != null) {
      inverse.removeAll(val);
      for (K key : keys) {
        this.remove(key);
      }
    }
    return keys;
  }
}
