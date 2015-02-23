/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/CleanableMap.java,v 1.1 2007/04/13 03:11:33 boyland Exp $*/
package edu.cmu.cs.fluid.ir;

import java.util.Map;

/**
 * A Map that can be cleaned.
 * @author boyland
 */
public interface CleanableMap<K, V> extends Map<K, V>, Cleanable {

}
