/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/EdgeDigraphConnections.java,v 1.5 2005/06/13 19:04:11 chance Exp $ */
package edu.cmu.cs.fluid.tree;

import java.util.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotUndefinedException;
import edu.cmu.cs.fluid.util.AbstractRemovelessIterator;

/** An enumeration of connections between two nodes.
 * The first node must be a node in an edge digraph,
 * the second node may be null.
 */
public class EdgeDigraphConnections extends AbstractRemovelessIterator<IRNode> {
  private final EdgeDigraphInterface digraph;
  //private final IRNode node1;
  private final IRNode node2;
  private final Iterator<IRNode> childEdges;
  
  public EdgeDigraphConnections(EdgeDigraphInterface ed, IRNode n1, IRNode n2) {
    digraph = ed;
    //node1 = n1;
    node2 = n2;
    childEdges = digraph.childEdges(n1);
  }
  
  private boolean nextIsValid = false;
  private IRNode nextEdge = null;
  
  @Override
  public boolean hasNext() {
    if (!nextIsValid) {
      try {	
        nextEdge = next();
      } catch (NoSuchElementException e) {
        nextEdge = null;
        nextIsValid = false;
      }
    }
    nextIsValid = true;
    return nextEdge != null;
  }
  
  @Override
  public IRNode next() throws NoSuchElementException {
    if (nextIsValid) {
      if (nextEdge == null)
        throw new NoSuchElementException("no more children");
      nextIsValid = false;
      return nextEdge;
    }
    for (;;) {
      IRNode edge = childEdges.next();
      try {
        IRNode sink = digraph.getSink(edge);
        if (node2 == null ? null == sink : node2.equals(sink)) {
          return edge;
        }
      } catch (SlotUndefinedException e) {
      }
    }
  }
}
