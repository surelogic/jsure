/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/AbstractForestToForestStatefulView.java,v 1.13 2003/07/15 18:39:10 thallora Exp $ */
package edu.cmu.cs.fluid.mvc.tree;

import edu.cmu.cs.fluid.mvc.AttributeInheritanceManager;
import edu.cmu.cs.fluid.mvc.AttributeManager;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.ViewCore;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * @author Aaron Greenhouse
 */
public abstract class AbstractForestToForestStatefulView
extends AbstractModelToForestStatefulView
{
  //===========================================================
  //== Constructor
  //===========================================================

  // Does *not* init source models attribute!
  // Does init EllipisPolicy
  public AbstractForestToForestStatefulView(
    final String name, final ModelCore.Factory mf, final ViewCore.Factory vf,
    final ForestModelCore.Factory fmf, 
    final AttributeManager.Factory attrFactory,
    final AttributeInheritanceManager.Factory inheritFactory )
  throws SlotAlreadyRegisteredException
  {
    super( name, mf, vf, fmf, attrFactory, inheritFactory );
  }
}

