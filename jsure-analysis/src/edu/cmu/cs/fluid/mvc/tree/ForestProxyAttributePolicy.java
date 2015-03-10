package edu.cmu.cs.fluid.mvc.tree;

import edu.cmu.cs.fluid.mvc.AVPair;
import edu.cmu.cs.fluid.mvc.ProxyAttributePolicy;
import edu.cmu.cs.fluid.ir.IRNode;

public interface ForestProxyAttributePolicy
extends ProxyAttributePolicy
{
  /**
   * Get thet attributes taht should be placed on the proxy node
   * representing the collapsed subtree <code>root</code>;
   * <i>NB</i>, the node <code>root</code> <em>is present</em>
   * in the model.
   */
  public AVPair[] attributesFor( ForestModel model, IRNode root );
}
