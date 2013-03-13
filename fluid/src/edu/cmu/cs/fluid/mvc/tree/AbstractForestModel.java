/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/AbstractForestModel.java,v 1.15 2007/05/30 20:35:17 chance Exp $ */
package edu.cmu.cs.fluid.mvc.tree;

import java.util.*;

import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotFactory;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.tree.DigraphListener;


/**
 * An abstract implementation of the getters of a Forest Model.  Can be either
 * a tree or a forest depending on the ForestModelCore delegate that is used.
 */

public abstract class AbstractForestModel
extends AbstractModel
{
  /** ForestModelCore delegate */
  protected final ForestModelCore forestModCore;



  //===========================================================
  //== Constructors
  //===========================================================

  protected AbstractForestModel(
    final String name, final ModelCore.Factory mf,
    final ForestModelCore.Factory fmf, final SlotFactory sf )
  throws SlotAlreadyRegisteredException
  {    
    super( name, mf, LocalAttributeManagerFactory.prototype, sf );
    forestModCore = fmf.create( name, this, structLock, attrManager,
                                new ForestAttrChangedCallback() );
  }

  
  
  //===========================================================
  //== Callback
  //===========================================================

  private class ForestAttrChangedCallback
  extends AbstractAttributeChangedCallback
  {
    @Override
    protected void attributeChangedImpl(
      final String attr, final IRNode node, final Object val )
    {
      modelCore.fireModelEvent(
        new AttributeValuesChangedEvent( AbstractForestModel.this, node, attr, val ) );
    }
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
  public final boolean isPresent( final IRNode node )
  {
    synchronized( structLock ) {
      return forestModCore.isPresent( node );
    }
  }

  @Override
  public final Iterator<IRNode> getNodes()
  {
    synchronized( structLock ) {
      return forestModCore.getNodes();
    }
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
  //== Digraph Methods 
  //===========================================================

  public final SlotInfo getAttribute( final String name ) 
  {
    synchronized( structLock ) {
      return forestModCore.getAttribute( name );
    }
  }

  public final boolean hasChildren( final IRNode node )
  {
    synchronized( structLock ) {
      return forestModCore.hasChildren( node );
    }
  }

  public final int numChildren( final IRNode node )
  {
    synchronized( structLock ) {
      return forestModCore.numChildren( node );
    }
  }

  public final IRLocation childLocation( final IRNode node, final int i )
  {
    synchronized( structLock ) {
      return forestModCore.childLocation( node, i );
    }
  }

  public final int childLocationIndex( final IRNode node, final IRLocation loc )
  {
    synchronized( structLock ) {
      return forestModCore.childLocationIndex( node, loc );
    }
  }

  public final IRLocation firstChildLocation( final IRNode node )
  {
    synchronized( structLock ) {
      return forestModCore.firstChildLocation( node );
    }
  }

  public final IRLocation lastChildLocation( final IRNode node )
  {
    synchronized( structLock ) {
      return forestModCore.lastChildLocation( node );
    }
  }

  public final IRLocation nextChildLocation( final IRNode node, final IRLocation loc )
  {
    synchronized( structLock ) {
      return forestModCore.nextChildLocation( node, loc );
    }
  }

  public final IRLocation prevChildLocation( final IRNode node, final IRLocation loc )
  {
    synchronized( structLock ) {
      return forestModCore.prevChildLocation( node, loc );
    }
  }

  public final int compareChildLocations( final IRNode node, final IRLocation loc1,
                                    final IRLocation loc2 )
  {
    synchronized( structLock ) {
      return forestModCore.compareChildLocations( node, loc1, loc2 );
    }
  }

  public final boolean hasChild( final IRNode node, final int i )
  {
    synchronized( structLock ) {
      return forestModCore.hasChild( node, i );
    }
  }

  public final boolean hasChild( final IRNode node, final IRLocation loc )
  {
    synchronized( structLock ) {
      return forestModCore.hasChild( node, loc );
    }
  }

  public final IRNode getChild( final IRNode node, final int i )
  {
    synchronized( structLock ) {
      return forestModCore.getChild( node, i );
    }
  }

  public final IRNode getChild( final IRNode node, final IRLocation loc ) 
  {
    synchronized( structLock ) {
      return forestModCore.getChild( node, loc );
    }
  }

  public final Iteratable<IRNode> children( final IRNode node )
  {
    synchronized( structLock ) {
      return forestModCore.children( node );
    }
  }
  
  public final List<IRNode> childList( final IRNode node )
  {
    synchronized( structLock ) {
      return forestModCore.childList( node );
    }
  }

  public final void addDigraphListener( final DigraphListener dl )
  {
    synchronized( structLock ) {
      forestModCore.addDigraphListener( dl );
    }
  }

  public final void removeDigraphListener( DigraphListener dl )
  {
    synchronized( structLock ) {
      forestModCore.removeDigraphListener( dl );
    }
  }

  public void addObserver(Observer o) {
    throw new UnsupportedOperationException( "Observers are obsolete here." );
  }

  //===========================================================
  //== Mutable Digraph Methods
  //===========================================================

  public final boolean isNode( final IRNode n )
  {
    synchronized( structLock ) {
      return forestModCore.isNode( n );
    }
  }

  public final Iteratable<IRNode> depthFirstSearch( final IRNode node )
  {
    synchronized( structLock ) {
      return forestModCore.depthFirstSearch( node );
    }
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
  //== Symmetric Digraph Methods 
  //===========================================================

  public final boolean hasParents( final IRNode node )
  {
    synchronized( structLock ) {
      return forestModCore.hasParents( node );
    }
  }
  
  public final int numParents( final IRNode node )
  {
    synchronized( structLock ) {
      return forestModCore.numParents( node );
    }
  }

  public final IRLocation parentLocation( final IRNode node, final int i )
  {
    synchronized( structLock ) {
      return forestModCore.parentLocation( node, i );
    }
  }

  public final int parentLocationIndex( final IRNode node, final IRLocation loc )
  {
    synchronized( structLock ) {
      return forestModCore.parentLocationIndex( node, loc );
    }
  }

  public final IRLocation firstParentLocation( final IRNode node )
  {
    synchronized( structLock ) {
      return forestModCore.firstParentLocation( node );
    }
  }

  public final IRLocation lastParentLocation( final IRNode node )
  {
    synchronized( structLock ) {
      return forestModCore.lastParentLocation( node );
    }
  }

  public final IRLocation nextParentLocation( final IRNode node, final IRLocation ploc )
  {
    synchronized( structLock ) {
      return forestModCore.nextParentLocation( node, ploc );
    }
  }
  
  public final IRLocation prevParentLocation( final IRNode node, final IRLocation ploc )
  {
    synchronized( structLock ) {
      return forestModCore.prevParentLocation( node, ploc );
    }
  }

  public final int compareParentLocations( final IRNode node, final IRLocation loc1,
                                     final IRLocation loc2 )
  {
    synchronized( structLock ) {
      return forestModCore.compareParentLocations( node, loc1, loc2 );
    }
  }

  public final IRNode getParent( final IRNode node, final int i )
  {
    synchronized( structLock ) {
      return forestModCore.getParent( node, i );
    }
  }

  public final IRNode getParent( final IRNode node, final IRLocation loc )
  {
    synchronized( structLock ) {
      return forestModCore.getParent( node, loc );
    }
  }

  public final Iteratable<IRNode> parents( final IRNode node )
  {
    synchronized( structLock ) {
      return forestModCore.parents( node );
    }
  }



  //===========================================================
  //== Mutable Symmetric Digraph Methods
  //===========================================================

  public final Iteratable<IRNode> connectedNodes( final IRNode root )
  {
    synchronized( structLock ) {
      return forestModCore.connectedNodes( root );
    }
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
  //== Tree Methods
  //===========================================================

  public final IRNode getParent( final IRNode node )
  {
    synchronized( structLock ) {
      return forestModCore.getParent( node );
    }
  }

  public final IRLocation getLocation( final IRNode node )
  {
    synchronized( structLock ) {
      return forestModCore.getLocation( node );
    }
  }

  public final IRNode getRoot( final IRNode subtree )
  {
    synchronized( structLock ) {
      return forestModCore.getRoot( subtree );
    }
  }

  public final Iteratable<IRNode> bottomUp( final IRNode subtree )
  {
    synchronized( structLock ) {
      return forestModCore.bottomUp( subtree );
    }
  }

  public final Iteratable<IRNode> topDown( final IRNode subtree )
  {
    synchronized( structLock ) {
      return forestModCore.topDown( subtree );
    }
  }



  //===========================================================
  //== Mutable Tree Methods
  //===========================================================

  public final Iteratable<IRNode> rootWalk( final IRNode node )
  {
    synchronized( structLock ) {
      return forestModCore.rootWalk( node );
    }
  }

  public final int comparePreorder( final IRNode node1, final IRNode node2 )
  {
    synchronized( structLock ) {
      return forestModCore.comparePreorder( node1, node2 );
    }
  }



  //===========================================================
  //== Methods for dealing with roots
  //===========================================================

  public final boolean isRoot( final IRNode node )
  {
    synchronized( structLock ) {
      return forestModCore.isRoot( node );
    }
  }

  public final Iteratable<IRNode> getRoots()
  {
    synchronized( structLock ) {
      return forestModCore.getRoots();
    }
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End ForestModel Portion
  //-----------------------------------------------------------
  //-----------------------------------------------------------
}
