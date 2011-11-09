/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/ProxyAttributePolicy.java,v 1.7 2003/07/15 18:39:10 thallora Exp $ */
package edu.cmu.cs.fluid.mvc;

import java.util.Set;

/**
 * Interface for policies that determine how to set the attributes of
 * proxy nodes generated for ellipsis nodes in configurabel views.
 */
public interface ProxyAttributePolicy
{
  /**
   * Get the attributes to set for a proxy node that represents
   * the given set of nodes in the given model.
   */
  public AVPair[] attributesFor( Model model, Set skippedNodes );
}

