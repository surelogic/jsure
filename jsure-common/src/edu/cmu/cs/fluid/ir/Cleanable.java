/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/Cleanable.java,v 1.1 2007/04/13 03:11:33 boyland Exp $*/
package edu.cmu.cs.fluid.ir;

/**
 * A container that should be cleaned if nodes have been destroyed.
 * @author boyland
 */
public interface Cleanable {
  /**
   * Remove references to destroyed nodes and associated entries.
   */
  public int cleanup();
}
