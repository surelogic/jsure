/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/AbstractDigraphModel.java,v 1.11 2007/05/30 20:35:17 chance Exp $ */
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
 * An abstract implementation of the getters of a Digraph Model.
 */

public abstract class AbstractDigraphModel
extends AbstractModel
{
  /** DigraphModelCore delegate */
  protected final DigraphModelCore digraphModCore;



  //===========================================================
  //== Constructors
  //===========================================================

  protected AbstractDigraphModel(
    final String name, final ModelCore.Factory mf,
    final DigraphModelCore.Factory fmf, final SlotFactory sf )
  throws SlotAlreadyRegisteredException
  {    
    super( name, mf, LocalAttributeManagerFactory.prototype, sf );
    digraphModCore = fmf.create( name, this, structLock, attrManager,
                                new DigraphAttrChangedCallback() );
  }

  
  
  //===========================================================
  //== Callback
  //===========================================================

  private class DigraphAttrChangedCallback
  extends AbstractAttributeChangedCallback
  {
    @Override
    protected void attributeChangedImpl(
      final String attr, final IRNode node, final Object val )
    {
      modelCore.fireModelEvent(
        new AttributeValuesChangedEvent( AbstractDigraphModel.this, node, attr, val ) );
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
      return digraphModCore.isPresent( node );
    }
  }

  @Override
  public final Iterator<IRNode> getNodes()
  {
    synchronized( structLock ) {
      return digraphModCore.getNodes();
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
  //-----------------------------------------------------------
  //-----------------------------------------------------------

//  /**
//   * Disconnect the given node from all other nodes in the model.
//   * Removes all edges originating from or terminting in the
//   * given node.  This is probably a slow method!
//   */
//  public abstract void removeEdges( IRNode n );



  //===========================================================
  //== Digraph Methods 
  //===========================================================
  @SuppressWarnings("unchecked")  
  public final SlotInfo getAttribute( final String name ) 
  {
    synchronized( structLock ) {
      return digraphModCore.getAttribute( name );
    }
  }

  public final boolean hasChildren( final IRNode node )
  {
    synchronized( structLock ) {
      return digraphModCore.hasChildren( node );
    }
  }

  public final int numChildren( final IRNode node )
  {
    synchronized( structLock ) {
      return digraphModCore.numChildren( node );
    }
  }

  public final IRLocation childLocation( final IRNode node, final int i )
  {
    synchronized( structLock ) {
      return digraphModCore.childLocation( node, i );
    }
  }

  public final int childLocationIndex( final IRNode node, final IRLocation loc )
  {
    synchronized( structLock ) {
      return digraphModCore.childLocationIndex( node, loc );
    }
  }

  public final IRLocation firstChildLocation( final IRNode node )
  {
    synchronized( structLock ) {
      return digraphModCore.firstChildLocation( node );
    }
  }

  public final IRLocation lastChildLocation( final IRNode node )
  {
    synchronized( structLock ) {
      return digraphModCore.lastChildLocation( node );
    }
  }

  public final IRLocation nextChildLocation( final IRNode node, final IRLocation loc )
  {
    synchronized( structLock ) {
      return digraphModCore.nextChildLocation( node, loc );
    }
  }

  public final IRLocation prevChildLocation( final IRNode node, final IRLocation loc )
  {
    synchronized( structLock ) {
      return digraphModCore.prevChildLocation( node, loc );
    }
  }

  public final int compareChildLocations( final IRNode node, final IRLocation loc1,
                                    final IRLocation loc2 )
  {
    synchronized( structLock ) {
      return digraphModCore.compareChildLocations( node, loc1, loc2 );
    }
  }

  public final boolean hasChild( final IRNode node, final int i )
  {
    synchronized( structLock ) {
      return digraphModCore.hasChild( node, i );
    }
  }

  public final boolean hasChild( final IRNode node, final IRLocation loc )
  {
    synchronized( structLock ) {
      return digraphModCore.hasChild( node, loc );
    }
  }

  public final IRNode getChild( final IRNode node, final int i )
  {
    synchronized( structLock ) {
      return digraphModCore.getChild( node, i );
    }
  }

  public final IRNode getChild( final IRNode node, final IRLocation loc ) 
  {
    synchronized( structLock ) {
      return digraphModCore.getChild( node, loc );
    }
  }

  public final Iteratable<IRNode> children( final IRNode node )
  {
    synchronized( structLock ) {
      return digraphModCore.children( node );
    }
  }
  
  public final List<IRNode> childList( final IRNode node )
  {
    synchronized( structLock ) {
      return digraphModCore.childList( node );
    }
  }

  public final void addDigraphListener( final DigraphListener dl )
  {
    synchronized( structLock ) {
      digraphModCore.addDigraphListener( dl );
    }
  }

  public final void removeDigraphListener( DigraphListener dl )
  {
    synchronized( structLock ) {
      digraphModCore.removeDigraphListener( dl );
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
      return digraphModCore.isNode( n );
    }
  }

  public final Iteratable<IRNode>depthFirstSearch( final IRNode node )
  {
    synchronized( structLock ) {
      return digraphModCore.depthFirstSearch( node );
    }
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End DigraphModel Portion
  //-----------------------------------------------------------
  //-----------------------------------------------------------
}
