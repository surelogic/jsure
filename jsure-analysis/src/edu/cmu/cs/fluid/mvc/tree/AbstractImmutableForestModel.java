/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/AbstractImmutableForestModel.java,v 1.8 2005/05/25 15:52:03 chance Exp $ */
package edu.cmu.cs.fluid.mvc.tree;

import edu.cmu.cs.fluid.mvc.AVPair;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.tree.StructureException;

/**
 * An abstract implementation of a Forest Model disallowing mutation of the model
 * using the forest model methods&mdash;they all throw UnsupportedOperationExceptions.
 * Mutability via the attributes still depends
 * on the ForestModelCore.Factory.  Can be either a tree
 * or a forest depending on the ForestModelCore delegate that is used.
 */
public abstract class AbstractImmutableForestModel
extends AbstractForestModel
{
  //===========================================================
  //== Constructors
  //===========================================================

  protected AbstractImmutableForestModel(
    final String name, final ModelCore.Factory mf,
    final ForestModelCore.Factory fmf, final SlotFactory sf )
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
  public void removeNode( final IRNode node )
  {
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
  //-- (Move this to superclass later)
  //-----------------------------------------------------------
  //-----------------------------------------------------------

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



  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin Symmetric Digraph Model Portion
  //-- (Move this to super class later)
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //===========================================================
  //== Mutable Symmetric Digraph Methods
  //===========================================================

  // This does not break the model!
  public void initNode( final IRNode n, final int numParents,
                        final int numChildren )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void setParent( final IRNode node, final int i, final IRNode newParent )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void setParent( final IRNode node, final IRLocation loc,
                         final IRNode newParent )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void addParent( final IRNode node, final IRNode newParent )
       throws StructureException
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void removeParent( final IRNode node, final IRNode parent )
       throws StructureException
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void replaceParent( final IRNode node, final IRNode oldParent,
                             final IRNode newParent )
       throws StructureException
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void removeParents( final IRNode node )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End Symmetric Digraph
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  
  
  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin ForestModel Portion
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //===========================================================
  //== Mutable Tree Methods
  //===========================================================

  public void clearParent( final IRNode node )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public IRNode getParentOrNull( final IRNode node )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void setSubtree( final IRNode parent, final int i,
                          final IRNode newChild )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void setSubtree( final IRNode parent, final IRLocation loc,
                          final IRNode newChild )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void replaceSubtree( final IRNode oldChild, final IRNode newChild )
      throws StructureException
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void exchangeSubtree( final IRNode node1, final IRNode node2 )
      throws StructureException
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public IRLocation insertSubtree( final IRNode node, final IRNode newChild,
                                   final InsertionPoint ip )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void insertSubtree( final IRNode parent, final IRNode newChild )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void appendSubtree( final IRNode parent, final IRNode newChild )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void removeSubtree( final IRNode node )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void insertSubtreeAfter( final IRNode newChild, final IRNode oldChild )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void insertSubtreeBefore( final IRNode newChild, final IRNode oldChild )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }



  //===========================================================
  //== Methods for dealing with roots
  //===========================================================

  public void removeRoot( final IRNode root )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }
  
  public void addRoot( final IRNode root )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void insertRoot( final IRNode root )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void insertRootBefore( final IRNode newRoot, final IRNode root )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void insertRootAfter( final IRNode newRoot, final IRNode root )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void insertRootAt( final IRNode root, final IRLocation loc )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  public void insertRootAt( final IRNode root, final InsertionPoint ip )
  {
    throw new UnsupportedOperationException( "Mutation disallowed." );
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End ForestModel Portion
  //-----------------------------------------------------------
  //-----------------------------------------------------------
}
