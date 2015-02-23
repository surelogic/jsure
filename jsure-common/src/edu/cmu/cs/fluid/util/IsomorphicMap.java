/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/IsomorphicMap.java,v 1.3 2007/07/10 22:16:30 aarong Exp $*/
package edu.cmu.cs.fluid.util;

import java.util.Map;

/**
 * A one-to-one map
 * 
 * @author Edwin.Chan
 */
public interface IsomorphicMap<K,V> extends Map<K,V> {
  boolean containsKeyFor(V val);
  K getCorrespondingKey(V val);
  K removeCorrespondingKey(V val);
}
