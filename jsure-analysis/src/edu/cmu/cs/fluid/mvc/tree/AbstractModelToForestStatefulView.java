/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/AbstractModelToForestStatefulView.java,v 1.24 2007/05/30 20:35:17 chance Exp $ */
package edu.cmu.cs.fluid.mvc.tree;

import java.util.*;

import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.InsertionPoint;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.tree.DigraphListener;
import edu.cmu.cs.fluid.tree.StructureException;

/**
 *
 * @author Aaron Greenhouse
 */
public abstract class AbstractModelToForestStatefulView
extends AbstractModelToModelStatefulView
{
  /** The ForestModelCore delegate */
  protected final ForestModelCore forestModCore;



  //===========================================================
  //== Constructor
  //===========================================================

  // Subclass must init SRC_MODELS attribute!
  // To prevent changes to the attributes or the sequences contained
  // in the attributes, pass "false" to the isMutable parameter
  // when creating the ForestModelCore.Factory
  public AbstractModelToForestStatefulView(
    final String name, final ModelCore.Factory mf, final ViewCore.Factory vf,
    final ForestModelCore.Factory fmf,
    final AttributeManager.Factory attrFactory,
    final AttributeInheritanceManager.Factory inheritFactory )
  throws SlotAlreadyRegisteredException
  {
    super( name, mf, vf, mf.getFactory(), attrFactory, inheritFactory );
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
      // should be smarter about this in the future
      // for now, just break 
      modelCore.fireModelEvent(
        new AttributeValuesChangedEvent( 
              AbstractModelToForestStatefulView.this, node, attr, val ) );
    }
  }



  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin Model Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  @Override
  public boolean isPresent( final IRNode node )
  {
    synchronized( structLock ) {
      return forestModCore.isPresent( node );
    }
  }

  @Override
  public Iterator<IRNode> getNodes()
  {
    synchronized( this ) {
      return forestModCore.getNodes();
    }
  }

  @Override
  public void addNode( final IRNode node, final AVPair[] attrs )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End Model Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------



  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin DigraphModelPortion
  //-- (Move this to superclass later)
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //===========================================================
  //== Digraph Methods 
  //===========================================================

  public final SlotInfo getAttribute(String name) {
    synchronized( structLock ) {
      return forestModCore.getAttribute(name); 
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
    throw new UnsupportedOperationException( "Observers are obsolete here" );
  }


  //===========================================================
  //== Mutable Digraph Methods
  //===========================================================

  // This does not break the model!
  public void initNode( final IRNode n )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  // This does not break the model!
  public void initNode( final IRNode n, final int numChildren )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public boolean isNode( final IRNode n )
  {
    return forestModCore.isNode( n );
  }

  public void setChild( final IRNode node, final int i, final IRNode newChild ) 
       throws StructureException
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void setChild( final IRNode node, final IRLocation loc,
                        final IRNode newChild ) 
       throws StructureException
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void addChild( final IRNode node, final IRNode newChild )
       throws StructureException
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void replaceChild( final IRNode node, final IRNode oldChild,
                            final IRNode newChild )
       throws StructureException
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public IRLocation insertChild( final IRNode node, final IRNode newChild,
                                 final InsertionPoint ip )
       throws StructureException    
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void insertChild( final IRNode node, final IRNode newChild )
       throws StructureException
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void appendChild( final IRNode node, final IRNode newChild )
       throws StructureException
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void insertChildAfter( final IRNode node, final IRNode newChild,
                                final IRNode oldChild )
       throws StructureException
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void insertChildBefore( final IRNode node, final IRNode newChild,
                                 final IRNode oldChild )
       throws StructureException
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void removeChild( final IRNode node, final IRNode oldChild )
       throws StructureException
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void removeChild( final IRNode node, final IRLocation loc )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void removeChildren( final IRNode node )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public Iteratable<IRNode>depthFirstSearch( final IRNode node )
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

  public final Iteratable<IRNode>parents( final IRNode node )
  {
    synchronized( structLock ) {
      return forestModCore.parents( node );
    }
  }




  //===========================================================
  //== Mutable Symmetric Digraph Methods
  //===========================================================

  // This does not break the model!
  public void initNode( final IRNode n, final int numParents,
                        final int numChildren )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void setParent( final IRNode node, final int i, final IRNode newParent )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void setParent( final IRNode node, final IRLocation loc,
                         final IRNode newParent )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void addParent( final IRNode node, final IRNode newParent )
       throws StructureException
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void removeParent( final IRNode node, final IRNode parent )
       throws StructureException
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void replaceParent( final IRNode node, final IRNode oldParent,
                             final IRNode newParent )
       throws StructureException
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void removeParents( final IRNode node )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  @Override
  public void removeNode( final IRNode node )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }
  
  public Iteratable<IRNode>connectedNodes( final IRNode root )
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

  public void clearParent( final IRNode node )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public IRNode getParentOrNull( final IRNode node )
  {
    synchronized( structLock ) {
      return forestModCore.getParentOrNull( node );
    }
  }

  public void setSubtree( final IRNode parent, final int i,
                          final IRNode newChild )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void setSubtree( final IRNode parent, final IRLocation loc,
                          final IRNode newChild )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void replaceSubtree( final IRNode oldChild, final IRNode newChild )
      throws StructureException
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void exchangeSubtree( final IRNode node1, final IRNode node2 )
      throws StructureException
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public IRLocation insertSubtree( final IRNode node, final IRNode newChild,
                                   final InsertionPoint ip )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void insertSubtree( final IRNode parent, final IRNode newChild )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void appendSubtree( final IRNode parent, final IRNode newChild )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void removeSubtree( final IRNode node )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void insertSubtreeAfter( final IRNode newChild, final IRNode oldChild )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void insertSubtreeBefore( final IRNode newChild, final IRNode oldChild )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final Iteratable<IRNode>rootWalk( final IRNode node )
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

  public boolean isRoot( final IRNode node )
  {
    synchronized( structLock ) {
      return forestModCore.isRoot( node );
    }
  }

  public void removeRoot( final IRNode root )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void addRoot( final IRNode root )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void insertRoot( final IRNode root )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void insertRootBefore( final IRNode newRoot, final IRNode root )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void insertRootAfter( final IRNode newRoot, final IRNode root )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void insertRootAt( final IRNode root, final IRLocation loc )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public void insertRootAt( final IRNode root, final InsertionPoint ip )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public Iteratable<IRNode> getRoots()
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

