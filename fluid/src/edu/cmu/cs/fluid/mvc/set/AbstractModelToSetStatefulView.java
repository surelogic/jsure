/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/set/AbstractModelToSetStatefulView.java,v 1.14 2007/07/10 22:16:38 aarong Exp $ */

package edu.cmu.cs.fluid.mvc.set;

import java.util.Iterator;

import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 *
 * @author Aaron Greenhouse
 */
public abstract class AbstractModelToSetStatefulView
extends AbstractModelToModelStatefulView
implements ModelToSetStatefulView
{
  /** The SetModelCore delegate */
  protected final SetModelCore setModCore;



  //===========================================================
  //== Constructor
  //===========================================================

  // Subclass must init SRC_MODELS attribute!
  public AbstractModelToSetStatefulView(
    final String name, final ModelCore.Factory mf, final ViewCore.Factory vf,
    final SetModelCore.Factory smf, final AttributeManager.Factory attrFactory,
    final AttributeInheritanceManager.Factory inheritFactory )
  throws SlotAlreadyRegisteredException
  {
    super( name, mf, vf, mf.getFactory(), attrFactory, inheritFactory );
    setModCore = smf.create( name, this, structLock, attrManager );
  }


  
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin Model Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  @Override
  public void addNode( final IRNode node, final AVPair[] attrs )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  @Override
  public boolean isPresent( final IRNode node )
  {
    synchronized( structLock ) {
      return setModCore.isPresent( node );
    }
  }



  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End Model Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------



  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin SetModel Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

   //===========================================================
   //== Node methods
   //===========================================================

  /**
   * Returns an iterator over the nodes in the order they
   * appear in the sequence.
   */
  @Override
  public Iterator<IRNode> getNodes()
  {
    synchronized( structLock ) {
      return setModCore.getNodes();
    }
  }

  /** Insure that a node is not in the mdoel. */
  @Override
  public void removeNode( final IRNode node )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  /** Insure that a node is in the mode. */
  @Override
  public void addNode( final IRNode node )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  /** Get the number of elements in the set. */
  @Override
  public int size()
  {
    synchronized( structLock ) {
      return setModCore.size();
    }
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End SetModel portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
}

