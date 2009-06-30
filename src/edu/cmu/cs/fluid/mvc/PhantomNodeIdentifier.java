/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/PhantomNodeIdentifier.java,v 1.7 2003/07/15 21:47:18 aarong Exp $
 *
 * PhantomNodeIdentifier.java
 * Created on January 22, 2002, 2:46 PM
 */

package edu.cmu.cs.fluid.mvc;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Defines interface for identifying and mapping phantom nodes.
 *
 * @see edu.cmu.cs.fluid.mvc.attributes.PhantomSupportingNodeAttribute
 * @author Aaron Greenhouse
 */
public interface PhantomNodeIdentifier
{
  /**
   * Query if a node is a phantom node.
   */
  public boolean isPhantomNode( IRNode node );
    
  /**
   * Map a phantom node to the node that represents it's current position
   * within the model structure.
   */
  public IRNode mapPhantomNode( IRNode node );



  /**
   * Interface defining factories for phantom node identifiers.  This is
   * necessary because identifiers usually need to have access to model
   * attributes to determine if a node is a phantom node or not, but the
   * identifier needs to be "specified" by the model builder as part of the
   * specification of the attribute manager, which is done before the model
   * is created&mdash;so it would be impossible to get a reference to the model to 
   * get the necessary attributes.  By using a factory, the reference is 
   * available because when the model object is actually (finally) constructed,
   * the reference to the model object will be given to the attribute manager
   * factory, which can give it to the attribute manager, which can ultimately
   * give it to the phantom node identifier via the phantom node factory.
   *
   * @author Aaron Greenhouse
   */
  public static interface Factory
  {
    /**
     * Create an new phantom node identifier object.
     * @param model The model the identifier is to be associated with.
     */
    public PhantomNodeIdentifier create( Model model );
  }
}

