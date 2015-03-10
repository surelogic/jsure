package edu.cmu.cs.fluid.mvc.sequence;

import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotFactory;

/**
 * Implements the minimum required functionality of the PureSequence Model. 
 *
 * @author Aaron Greenhouse
 */
final class PureSequenceImpl
extends AbstractSequenceModel
implements PureSequence
{
  //===========================================================
  //== Constructors
  //===========================================================

  protected PureSequenceImpl(
    final String name, final ModelCore.Factory mf,
    final SequenceModelCore.Factory sf, final SlotFactory slotf )
  throws SlotAlreadyRegisteredException
  {    
    super( name, mf, sf, slotf );
    finalizeInitialization();
  }
}
