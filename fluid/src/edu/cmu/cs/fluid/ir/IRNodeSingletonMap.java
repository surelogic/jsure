/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRNodeSingletonMap.java,v 1.1 2007/04/13 03:11:33 boyland Exp $*/
package edu.cmu.cs.fluid.ir;

import edu.cmu.cs.fluid.util.SingletonMap;

/**
 * A cleanable singleton map.
 * @author boyland
 */
public class IRNodeSingletonMap<V> extends SingletonMap<IRNode, V> implements CleanableMap<IRNode,V> {

  public IRNodeSingletonMap(IRNode k, V v) {
    super(k, v);
  }

  @Override
  public int cleanup() {
    IRNode key = getKey();
    if (key == null) return 0;
    if (key.identity() == IRNode.destroyedNode) {
      clear();
      return 1;
    }
    return 0;
  }
}
