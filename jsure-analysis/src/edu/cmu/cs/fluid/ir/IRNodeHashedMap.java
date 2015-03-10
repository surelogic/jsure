/*
 * Created on Aug 6, 2003
 *
 */
package edu.cmu.cs.fluid.ir;

import edu.cmu.cs.fluid.util.CustomizableHashCodeMap;

/**
 * A hashtable that maps IRnodes to various things.
 * Whenever it is rehash'ed, we remove any mappings for destroyed nodes.
 * @author chance
 * @author boyland
 */
public class IRNodeHashedMap<V> extends CustomizableHashCodeMap<IRNode,V> implements CleanableMap<IRNode,V> {
  public IRNodeHashedMap() {
    super(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_THRESHOLD);
  }
  
  public IRNodeHashedMap(int i) {
    super(i, DEFAULT_LOAD_FACTOR, 1);
  }
  
  @Override
  protected boolean isValidEntry(HashEntry<IRNode,V> e) {
    //boolean valid = !e.getKey().equals(IRNode.destroyedNode);
    boolean valid = (e.getKey().identity() != IRNode.destroyedNode);
    if (!valid) {
      return false;
    }
    return valid;
  }
  
  @Override
  public int cleanup() {
    return super.cleanup();
  }
  
  @Override
  public boolean compact() {
    return super.compact();
  }
}
