/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/AbstractForestToModelStatefulView.java,v 1.9 2003/07/15 18:39:10 thallora Exp $ */
package edu.cmu.cs.fluid.mvc.tree;

import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * @author Aaron Greenhouse
 */
public abstract class AbstractForestToModelStatefulView
extends AbstractModelToModelStatefulView
{
  //===========================================================
  //== Constructor
  //===========================================================

  // Does *not* init source models attribute!
  // Does init EllipisPolicy
  public AbstractForestToModelStatefulView(
    final String name, final ModelCore.Factory mf, final ViewCore.Factory vf,
    final AttributeManager.Factory attrFactory,
    final AttributeInheritanceManager.Factory inheritFactory )
  throws SlotAlreadyRegisteredException
  {
    super( name, mf, vf, mf.getFactory(), attrFactory, inheritFactory );
  }
}

