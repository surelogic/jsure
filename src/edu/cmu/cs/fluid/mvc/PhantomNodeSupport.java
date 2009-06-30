/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/PhantomNodeSupport.java,v 1.4 2003/07/15 18:39:10 thallora Exp $
 *
 * PhantomNodeSupport.java
 * Created on January 22, 2002, 2:27 PM
 */

package edu.cmu.cs.fluid.mvc;

/**
 * Constants needed to support phantom nodes.
 * @author Aaron Greenhouse
 */
public interface PhantomNodeSupport
{
  /**
   * Mode constant for AttributeInheritancePolicy indicating
   * that the attribute should be inherited as immutable
   * and that the attribute should be prepared to support phantom nodes.
   * @see PhantomSupportingAttributeManagerFactory
   */
  public static final Object IMMUTABLE_PHANTOM = new Object();
}

