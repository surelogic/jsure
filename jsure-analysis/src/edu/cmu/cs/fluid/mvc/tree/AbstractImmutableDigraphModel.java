/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/AbstractImmutableDigraphModel.java,v 1.4 2005/05/25 15:52:03 chance Exp $ */
package edu.cmu.cs.fluid.mvc.tree;

import edu.cmu.cs.fluid.mvc.AVPair;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.tree.StructureException;

/**
 * An abstract implementation of a Digraph Model disallowing mutation of the model
 * using the digraph model methods&mdash;they all throw UnsupportedOperationExceptions.
 * Mutability via the attributes still depends
 * on the DigraphModelCore.Factory.
 */
public abstract class AbstractImmutableDigraphModel
extends AbstractDigraphModel
{
  //===========================================================
  //== Constructors
  //===========================================================

  protected AbstractImmutableDigraphModel(
    final String name, final ModelCore.Factory mf,
    final DigraphModelCore.Factory fmf, final SlotFactory sf )
  throws SlotAlreadyRegisteredException
  {    
    super( name, mf, fmf, sf );
  }

  
  
  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin Model Portion
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //===========================================================
  //== Node methods
  //===========================================================

  @Override
  public void addNode( final IRNode node, final AVPair[] attrs )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  @Override
  public void removeNode( final IRNode node ) {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End Model Portion
  //-----------------------------------------------------------
  //-----------------------------------------------------------



  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin DigraphModelPortion
  //-----------------------------------------------------------
  //-----------------------------------------------------------

//  public final void removeEdges( final IRNode n )
//  {
//    throw new UnsupportedOperationException( "Mutation disallowed." );
//  }



  //===========================================================
  //== Mutable Digraph Methods
  //===========================================================

  // This does not break the model!
  public void initNode( final IRNode n )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  // This does not break the model!
  public void initNode( final IRNode n, final int numChildren )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void setChild( final IRNode node, final int i, final IRNode newChild ) 
       throws StructureException
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void setChild( final IRNode node, final IRLocation loc,
                        final IRNode newChild ) 
       throws StructureException
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void addChild( final IRNode node, final IRNode newChild )
       throws StructureException
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void replaceChild( final IRNode node, final IRNode oldChild,
                            final IRNode newChild )
       throws StructureException
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public IRLocation insertChild( final IRNode node, final IRNode newChild,
                                 final InsertionPoint ip )
       throws StructureException    
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void insertChild( final IRNode node, final IRNode newChild )
       throws StructureException
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void appendChild( final IRNode node, final IRNode newChild )
       throws StructureException
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void insertChildAfter( final IRNode node, final IRNode newChild,
                                final IRNode oldChild )
       throws StructureException
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void insertChildBefore( final IRNode node, final IRNode newChild,
                                 final IRNode oldChild )
       throws StructureException
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void removeChild( final IRNode node, final IRNode oldChild )
       throws StructureException
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void removeChild( final IRNode node, final IRLocation loc )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void removeChildren( final IRNode node )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End DigraphModel Portion
  //-----------------------------------------------------------
  //-----------------------------------------------------------
}
