/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/ConnectedNodes.java,v 1.9 2007/07/05 18:15:16 aarong Exp $ */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;

/** Return all the nodes that can be reached from
 * a root by traversing a symmetric digraph in either direction.
 */
public class ConnectedNodes extends DepthFirstSearch {
  public ConnectedNodes(SymmetricDigraphInterface dig, IRNode root) {
    super(dig,root);
  }
  private IRLocation ploc;

  @Override
  @SuppressWarnings("unchecked")
  protected void pushState() {
    super.pushState();
    stack.push(ploc);
  }

  @Override
  protected void popState() {
    ploc = (IRLocation)stack.pop();
    super.popState();
  }
  
  @Override
  protected void visit(IRNode n) {
    super.visit(n);
    ploc = ((SymmetricDigraphInterface)digraph).firstParentLocation(n);
  }

  @Override
  protected boolean additionalChildren(IRNode node) {
    if (ploc == null) return false;
    IRNode newNode = ((SymmetricDigraphInterface)digraph).getParent(node,ploc);
    ploc = ((SymmetricDigraphInterface)digraph).nextParentLocation(node,ploc);
    if (mark(newNode)) {
      visit(newNode);
    }
    return true;
  }
}
