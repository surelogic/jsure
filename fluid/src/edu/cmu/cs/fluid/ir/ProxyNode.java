/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/ProxyNode.java,v 1.3 2005/06/10 20:55:55 chance Exp $ */
package edu.cmu.cs.fluid.ir;

import com.surelogic.ThreadSafe;

/** Concrete placeholders for other IRNodes.
 * The referred to node is stored within this proxy node.
 */
@ThreadSafe
public class ProxyNode extends AbstractProxyNode {
  private final IRNode node;
  public ProxyNode(IRNode n) {
    node = n;
  }
  @Override
  protected IRNode getIRNode() {
    return node;
  }
}
