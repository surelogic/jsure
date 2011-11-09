/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/AbstractMutableDigraphModel.java,v 1.4 2005/05/25 15:52:03 chance Exp $ */
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
public abstract class AbstractMutableDigraphModel
extends AbstractDigraphModel
{
  //===========================================================
  //== Constructors
  //===========================================================

  protected AbstractMutableDigraphModel(
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

  /**
   * Caveat: Ignores the <code>vals</code> argument.
   */
  @Override
  public final void addNode( final IRNode node, final AVPair[] vals )
  {
    synchronized( structLock ) {
      digraphModCore.addNode( node );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  @Override
  public final void removeNode( final IRNode node ) {
    synchronized( structLock ) {
      digraphModCore.removeNode( node );
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
  //-----------------------------------------------------------
  //-----------------------------------------------------------

//  public final void removeEdges( final IRNode n )
//  {
//    synchronized( structLock ) {
//      digraphModCore.removeEdges(n);
//    }
//    // Later, change to send a more specific event 
//    modelCore.fireModelEvent( new ModelEvent( this ) );
//  }
  
  

  //===========================================================
  //== Mutable Digraph Methods
  //===========================================================

  // This does not break the model!
  public final void initNode( final IRNode n )
  {
    synchronized( structLock ) {
      digraphModCore.initNode( n );
    }
  }

  // This does not break the model!
  public final void initNode( final IRNode n, final int numChildren )
  {
    synchronized( structLock ) {
      digraphModCore.initNode( n, numChildren );
    }
  }

  public final void setChild( final IRNode node, final int i, final IRNode newChild ) 
       throws StructureException
  {
    synchronized( structLock ) {
      digraphModCore.setChild( node, i, newChild );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void setChild( final IRNode node, final IRLocation loc,
                        final IRNode newChild ) 
       throws StructureException
  {
    synchronized( structLock ) {
      digraphModCore.setChild( node, loc, newChild );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void addChild( final IRNode node, final IRNode newChild )
       throws StructureException
  {
    synchronized( structLock ) {
      digraphModCore.addChild( node, newChild );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void replaceChild( final IRNode node, final IRNode oldChild,
                            final IRNode newChild )
       throws StructureException
  {
    synchronized( structLock ) {
      digraphModCore.replaceChild( node, oldChild, newChild );
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
      loc = digraphModCore.insertChild( node, newChild, ip );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
    return loc;
  }

  public final void insertChild( final IRNode node, final IRNode newChild )
       throws StructureException
  {
    synchronized( structLock ) {
      digraphModCore.insertChild( node, newChild );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void appendChild( final IRNode node, final IRNode newChild )
       throws StructureException
  {
    synchronized( structLock ) {
      digraphModCore.appendChild( node, newChild );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void insertChildAfter( final IRNode node, final IRNode newChild,
                                final IRNode oldChild )
       throws StructureException
  {
    synchronized( structLock ) {
      digraphModCore.insertChildAfter( node, newChild, oldChild );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void insertChildBefore( final IRNode node, final IRNode newChild,
                                 final IRNode oldChild )
       throws StructureException
  {
    synchronized( structLock ) {
      digraphModCore.insertChildBefore( node, newChild, oldChild );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void removeChild( final IRNode node, final IRNode oldChild )
       throws StructureException
  {
    synchronized( structLock ) {
      digraphModCore.removeChild( node, oldChild );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void removeChild( final IRNode node, final IRLocation loc )
  {
    synchronized( structLock ) {
      digraphModCore.removeChild( node, loc );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  public final void removeChildren( final IRNode node )
  {
    synchronized( structLock ) {
      digraphModCore.removeChildren( node );
    }
    // Later, change to send a more specific event 
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End DigraphModel Portion
  //-----------------------------------------------------------
  //-----------------------------------------------------------
}
