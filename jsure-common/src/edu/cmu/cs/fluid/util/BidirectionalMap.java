/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/BidirectionalMap.java,v 1.2 2007/07/10 22:16:30 aarong Exp $*/
package edu.cmu.cs.fluid.util;

import java.util.*;

/**
 * A map from keys to a value, and back.
 * Note that a value may map to multiple keys.
 * 
 * @author Edwin.Chan
 */
public interface BidirectionalMap<K,V> extends Map<K,V> {
  boolean containsKeyFor(V val);
  Collection<K> getCorrespondingKeys(V val);
  Collection<K> removeCorrespondingKeys(V val);
}
