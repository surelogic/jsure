/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/set/PureSetImpl.java,v 1.6 2003/07/15 18:39:12 thallora Exp $ */
package edu.cmu.cs.fluid.mvc.set;

import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotFactory;

/**
 * An implementation of a {@link PureSet} model.  
 *
 * @author Aaron Greenhouse
 */
final class PureSetImpl
extends AbstractSetModel
implements PureSet
{
  //===========================================================
  //== Constructors
  //===========================================================

  protected PureSetImpl(
    final String name, final ModelCore.Factory mf,
    final SetModelCore.Factory sf, final SlotFactory slotf )
  throws SlotAlreadyRegisteredException
  {    
    super( name, mf, sf, slotf );
    finalizeInitialization();
  }
}
