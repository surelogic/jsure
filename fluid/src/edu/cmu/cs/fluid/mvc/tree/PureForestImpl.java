package edu.cmu.cs.fluid.mvc.tree;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotFactory;

/**
 * Minimal implementation of {@link PureForest}.
 */
final class PureForestImpl
extends AbstractMutableForestModel
implements PureForest
{
  //===========================================================
  //== Constructors
  //===========================================================

  public PureForestImpl(
    final String name, final ModelCore.Factory mf,
    final ForestModelCore.Factory fmf, final SlotFactory sf )
  throws SlotAlreadyRegisteredException
  {    
    super( name, mf, fmf, sf );
    finalizeInitialization();
  }
}
