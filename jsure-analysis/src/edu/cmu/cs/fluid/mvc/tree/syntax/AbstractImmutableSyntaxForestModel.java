/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/syntax/AbstractImmutableSyntaxForestModel.java,v 1.6 2003/07/15 18:39:13 thallora Exp $ */
package edu.cmu.cs.fluid.mvc.tree.syntax;

import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.tree.AbstractImmutableForestModel;
import edu.cmu.cs.fluid.mvc.tree.ForestModelCore;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotFactory;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * An abstract implementation of a Syntax Forest Model disallowing mutation
 * of the model using the forest model methods.  Mutability via the
 * attributes still depends on the ForestModelCore.Factory.  Can be either a
 * tree or a forest depending on the ForestModelCore delegate that is used.
 */
public abstract class AbstractImmutableSyntaxForestModel
extends AbstractImmutableForestModel
{
  protected final SyntaxForestModelCore synModCore;
  
  
  
  //===========================================================
  //== Constructors
  //===========================================================

  protected AbstractImmutableSyntaxForestModel(
    final String name, final ModelCore.Factory mf,
    final ForestModelCore.Factory fmf, final SyntaxForestModelCore.Factory sfmc,
    final SlotFactory sf )
  throws SlotAlreadyRegisteredException
  {    
    super( name, mf, fmf, sf );
    synModCore = sfmc.create( name, this, structLock, attrManager );
  }

  

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Start SyntaxForestModel Portion
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  public final Operator getOperator( final IRNode n )
  {
    synchronized( structLock ) {
      return synModCore.getOperator(n);
    }
  }

  public final boolean opExists( final IRNode n)
  {
    synchronized( structLock ) {
      return synModCore.opExists(n);
    }
  }

  public final void initNode( final IRNode n, final Operator op )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public final void initNode( final IRNode n, final Operator op, final int min )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public final void initNode(
    final IRNode n, final Operator op, final IRNode[] children )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }
 
  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End SyntaxForestModel Portion
  //-----------------------------------------------------------
  //-----------------------------------------------------------
}
