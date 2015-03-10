/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/SymmetricDigraphInterface.java,v 1.9 2007/07/10 22:16:32 aarong Exp $ */
package edu.cmu.cs.fluid.tree;

import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;

/** Directed graphs that can be traversed in either direction.
 * @see SymmetricDigraph
 * @see SymmetricEdgeDigraphInterface
 */
public interface SymmetricDigraphInterface extends DigraphInterface {
  /** Return true if there is at least one parent location. */
  public boolean hasParents(IRNode node);
  
  /** Return the number of parents, defined or undefined, null or nodes. */
  public int numParents(IRNode node);

  /** Return the location for parent #i */
  public IRLocation parentLocation(IRNode node, int i);

  /** Return the numeric location for a location. */
  public int parentLocationIndex(IRNode node, IRLocation loc);

  /** Return the location of the first parent. */
  public IRLocation firstParentLocation(IRNode node);

  /** Return the location of the last parent. */
  public IRLocation lastParentLocation(IRNode node);

  /** Return next parent location or null. */
  public IRLocation nextParentLocation(IRNode node, IRLocation ploc);
  
  /** Return previous parent location or null. */
  public IRLocation prevParentLocation(IRNode node, IRLocation ploc);

  /** Return one of <dl>
   * <dt>&lt; 0<dd> if loc1 precedes loc2,
   * <dt>&gt; 0<dd> if loc1 follows loc2,
   * <dt>= 0<dd> if loc1 equals loc2.</dl>
   * These locations must be valid locations in the parents of the node.
   */
  public int compareParentLocations(IRNode node,
				    IRLocation loc1, IRLocation loc2);

  /** Return the i'th parent of a node. */
  public IRNode getParent(IRNode node, int i);

  /** Return the parent at location loc. */
  public IRNode getParent(IRNode node, IRLocation loc);

  /** Return the parents of a node in order. */
  public Iteratable<IRNode> parents(IRNode node);
}
