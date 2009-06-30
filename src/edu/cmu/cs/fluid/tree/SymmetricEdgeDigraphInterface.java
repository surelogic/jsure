/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/SymmetricEdgeDigraphInterface.java,v 1.4 2005/05/20 15:48:06 chance Exp $ */
package edu.cmu.cs.fluid.tree;

import java.util.Iterator;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;

/** Classes implementing this interface enable traversal
 * of graphs with explicit edges in either direction.
 */
public interface SymmetricEdgeDigraphInterface 
     extends EdgeDigraphInterface, SymmetricDigraphInterface
{
  /** Return the source of an edge. */
  public IRNode getSource(IRNode edge);

  /** Return the i'th edge arriving at a node. */
  public IRNode getParentEdge(IRNode node, int i);
  /** Return the ingoing edge at location loc. */
  public IRNode getParentEdge(IRNode node, IRLocation loc);

  /** Return an enumeration of the ingoing edges. */
  public Iterator parentEdges(IRNode node);  
}
