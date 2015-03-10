/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/EdgeDigraphInterface.java,v 1.6 2005/06/13 19:04:11 chance Exp $ */
package edu.cmu.cs.fluid.tree;

import java.util.Iterator;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;

/** Classes implementing this interface can be used
 * to traverse directed graphs in which both
 * nodes and edges are made up of IRNodes.  Information may thus
 * be stored on either nodes or edges.
 * @see EdgeDigraph
 */
public interface EdgeDigraphInterface extends DigraphInterface {
  /** Return the sink of an edge. */
  public IRNode getSink(IRNode edge);

  /** Return the i'th edge leaving a node. */
  public IRNode getChildEdge(IRNode node, int i);

  /** Return the outgoing edge at location loc. */
  public IRNode getChildEdge(IRNode node, IRLocation loc);

  /** Return an enumeration of the outgoing edges. */
  public Iterator<IRNode> childEdges(IRNode node);  
  
  /** Return an enumeration of all edges that connect node1
   * to node2 (which can be null).
   */
  public Iterator<IRNode> connections(IRNode n1, IRNode n2);
}
