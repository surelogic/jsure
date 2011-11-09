/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/AbstractMutableForestModel.java,v 1.8 2005/05/25 15:52:03 chance Exp $ */
package edu.cmu.cs.fluid.mvc.tree;

import edu.cmu.cs.fluid.mvc.AVPair;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.ModelEvent;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.tree.StructureException;

/**
 * An abstract implementation of a Forest Model allowing mutation of the model
 * using the forest model methods.  Mutability via the attributes still depends
 * on the ForestModelCore.Factory.  Can be either a tree
 * or a forest depending on the ForestModelCore delegate that is used.
 */
public abstract class AbstractMutableForestModel
extends AbstractForestModel
{
  //===========================================================
  //== Constructors
  //===========================================================

  protected AbstractMutableForestModel(
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
  public final void addNode( final IRNode node, final AVPair[] vals )
  {
    synchronized( structLock ) {
      forestModCore.addNode( node, vals );
    }
  }

  @Override
  public final void removeNode( final IRNode node )
  {
    synchronized( structLock ) {
      forestModCore.removeNode( node );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
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
  public final void initNode( final IRNode n )
  {
    synchronized( structLock ) {
      forestModCore.initNode( n );
    }
  }

  // This does not break the model!
  public final void initNode( final IRNode n, final int numChildren )
  {
    synchronized( structLock ) {
      forestModCore.initNode( n, numChildren );
    }
  }

  public final void setChild( final IRNode node, final int i, final IRNode newChild ) 
       throws StructureException
  {
    synchronized( structLock ) {
      forestModCore.setChild( node, i, newChild );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void setChild( final IRNode node, final IRLocation loc,
                        final IRNode newChild ) 
       throws StructureException
  {
    synchronized( structLock ) {
      forestModCore.setChild( node, loc, newChild );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void addChild( final IRNode node, final IRNode newChild )
       throws StructureException
  {
    synchronized( structLock ) {
      forestModCore.addChild( node, newChild );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void replaceChild( final IRNode node, final IRNode oldChild,
                            final IRNode newChild )
       throws StructureException
  {
    synchronized( structLock ) {
      forestModCore.replaceChild( node, oldChild, newChild );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final IRLocation insertChild( final IRNode node, final IRNode newChild,
                                 final InsertionPoint ip )
       throws StructureException    
  {
    IRLocation loc = null;
    synchronized( structLock ) {
      loc = forestModCore.insertChild( node, newChild, ip );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
    return loc;
  }

  public final void insertChild( final IRNode node, final IRNode newChild )
       throws StructureException
  {
    synchronized( structLock ) {
      forestModCore.insertChild( node, newChild );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void appendChild( final IRNode node, final IRNode newChild )
       throws StructureException
  {
    synchronized( structLock ) {
      forestModCore.appendChild( node, newChild );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void insertChildAfter( final IRNode node, final IRNode newChild,
                                final IRNode oldChild )
       throws StructureException
  {
    synchronized( structLock ) {
      forestModCore.insertChildAfter( node, newChild, oldChild );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void insertChildBefore( final IRNode node, final IRNode newChild,
                                 final IRNode oldChild )
       throws StructureException
  {
    synchronized( structLock ) {
      forestModCore.insertChildBefore( node, newChild, oldChild );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void removeChild( final IRNode node, final IRNode oldChild )
       throws StructureException
  {
    synchronized( structLock ) {
      forestModCore.removeChild( node, oldChild );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void removeChild( final IRNode node, final IRLocation loc )
  {
    synchronized( structLock ) {
      forestModCore.removeChild( node, loc );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void removeChildren( final IRNode node )
  {
    synchronized( structLock ) {
      forestModCore.removeChildren( node );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
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
  public final void initNode( final IRNode n, final int numParents,
                        final int numChildren )
  {
    synchronized( structLock ) {
      forestModCore.initNode( n, numParents, numChildren );
    }
  }

  public final void setParent( final IRNode node, final int i, final IRNode newParent )
  {
    synchronized( structLock ) {
      forestModCore.setParent( node, i, newParent );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void setParent( final IRNode node, final IRLocation loc,
                         final IRNode newParent )
  {
    synchronized( structLock ) {
      forestModCore.setParent( node, loc, newParent );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void addParent( final IRNode node, final IRNode newParent )
       throws StructureException
  {
    synchronized( structLock ) {
      forestModCore.addParent( node, newParent );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void removeParent( final IRNode node, final IRNode parent )
       throws StructureException
  {
    synchronized( structLock ) {
      forestModCore.removeParent( node, parent );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void replaceParent( final IRNode node, final IRNode oldParent,
                             final IRNode newParent )
       throws StructureException
  {
    synchronized( structLock ) {
      forestModCore.replaceParent( node, oldParent, newParent );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void removeParents( final IRNode node )
  {
    synchronized( structLock ) {
      forestModCore.removeParents( node );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
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

  public final void clearParent( final IRNode node )
  {
    synchronized( structLock ) {
      forestModCore.clearParent( node );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final IRNode getParentOrNull( final IRNode node )
  {
    synchronized( structLock ) {
      return forestModCore.getParentOrNull( node );
    }
  }

  public final void setSubtree( final IRNode parent, final int i,
                          final IRNode newChild )
  {
    synchronized( structLock ) {
      forestModCore.setSubtree( parent, i, newChild );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void setSubtree( final IRNode parent, final IRLocation loc,
                          final IRNode newChild )
  {
    synchronized( structLock ) {
      forestModCore.setSubtree( parent, loc, newChild );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void replaceSubtree( final IRNode oldChild, final IRNode newChild )
      throws StructureException
  {
    synchronized( structLock ) {
      forestModCore.replaceSubtree( oldChild, newChild );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void exchangeSubtree( final IRNode node1, final IRNode node2 )
      throws StructureException
  {
    synchronized( structLock ) {
      forestModCore.exchangeSubtree( node1, node2 );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final IRLocation insertSubtree( final IRNode node, final IRNode newChild,
                                   final InsertionPoint ip )
  {
    IRLocation loc = null;
    synchronized( structLock ) {
      loc = forestModCore.insertSubtree( node, newChild, ip );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
    return loc;
  }

  public final void insertSubtree( final IRNode parent, final IRNode newChild )
  {
    synchronized( structLock ) {
      forestModCore.insertSubtree( parent, newChild );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void appendSubtree( final IRNode parent, final IRNode newChild )
  {
    synchronized( structLock ) {
      forestModCore.appendSubtree( parent, newChild );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void removeSubtree( final IRNode node )
  {
    synchronized( structLock ) {
      forestModCore.removeSubtree( node );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void insertSubtreeAfter( final IRNode newChild, final IRNode oldChild )
  {
    synchronized( structLock ) {
      forestModCore.insertSubtreeAfter( newChild, oldChild );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void insertSubtreeBefore( final IRNode newChild, final IRNode oldChild )
  {
    synchronized( structLock ) {
      forestModCore.insertSubtreeBefore( newChild, oldChild );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  

  //===========================================================
  //== Methods for dealing with roots
  //===========================================================

  public final void removeRoot( final IRNode root )
  {
    synchronized( structLock ) {
      forestModCore.removeRoot( root );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void addRoot( final IRNode root )
  {
    synchronized( structLock ) {
      forestModCore.addRoot( root );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void insertRoot( final IRNode root )
  {
    synchronized( structLock ) {
      forestModCore.insertRoot( root );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void insertRootBefore( final IRNode newRoot, final IRNode root )
  {
    synchronized( structLock ) {
      forestModCore.insertRootBefore( newRoot, root );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void insertRootAfter( final IRNode newRoot, final IRNode root )
  {
    synchronized( structLock ) {
      forestModCore.insertRootAfter( newRoot, root );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void insertRootAt( final IRNode root, final IRLocation loc )
  {
    synchronized( structLock ) {
      forestModCore.insertRootAt( root, loc );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void insertRootAt( final IRNode root, final InsertionPoint ip )
  {
    synchronized( structLock ) {
      forestModCore.insertRootAt( root, ip );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End ForestModel Portion
  //-----------------------------------------------------------
  //-----------------------------------------------------------
}
