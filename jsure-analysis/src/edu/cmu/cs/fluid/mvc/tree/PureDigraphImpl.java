package edu.cmu.cs.fluid.mvc.tree;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotFactory;

/**
 * Minimal implementation of {@link PureForest}.
 */
final class PureDigraphImpl
extends AbstractMutableDigraphModel
implements PureDigraph
{
  //===========================================================
  //== Constructors
  //===========================================================

  public PureDigraphImpl(
    final String name, final ModelCore.Factory mf,
    final DigraphModelCore.Factory dmf, final SlotFactory sf )
  throws SlotAlreadyRegisteredException
  {    
    super( name, mf, dmf, sf );
    finalizeInitialization();
  }
}
