package edu.cmu.cs.fluid.mvc.tree;

import edu.cmu.cs.fluid.mvc.AVPair;
import edu.cmu.cs.fluid.mvc.NullProxyAttributePolicy;
import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Proxy attribute policy for forests that never sets any attributes.
 */
public class NullForestProxyAttributePolicy
extends NullProxyAttributePolicy
implements ForestProxyAttributePolicy
{
  public static final NullForestProxyAttributePolicy prototype =
    new NullForestProxyAttributePolicy();

  private NullForestProxyAttributePolicy() { super(); }

  /**
   * Get thet attributes taht should be placed on the proxy node
   * representing the collapsed subtree <code>root</code>;
   * <i>NB</i>, the node <code>root</code> <em>is present</em>
   * in the model.
   */
  @Override
  public AVPair[] attributesFor( final ForestModel model, final IRNode root )
  {
    return empty;
  }
}
