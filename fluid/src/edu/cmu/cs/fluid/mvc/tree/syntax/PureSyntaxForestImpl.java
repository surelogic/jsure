package edu.cmu.cs.fluid.mvc.tree.syntax;

import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.tree.ForestModelCore;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotFactory;

/**
 * Minimal implementation of {@link PureSyntaxForest} interface.
 */
final class PureSyntaxForestImpl
extends AbstractMutableSyntaxForestModel
implements PureSyntaxForest
{
  protected PureSyntaxForestImpl(
    final String name, final ModelCore.Factory mf,
    final ForestModelCore.Factory fmf, 
    final SyntaxForestModelCore.Factory sfmf, final SlotFactory sf )
  throws SlotAlreadyRegisteredException
  {    
    super( name, mf, fmf, sfmf, sf );
    finalizeInitialization();
  }
}
