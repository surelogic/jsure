/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/set/AbstractSetModel.java,v 1.10 2005/08/02 13:54:37 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.set;

import java.util.Iterator;

import edu.cmu.cs.fluid.mvc.AVPair;
import edu.cmu.cs.fluid.mvc.AbstractModel;
import edu.cmu.cs.fluid.mvc.LocalAttributeManagerFactory;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotFactory;

/**
 * Abstract implementation of a set model. 
 *
 * @author Aaron Greenhouse
 */
public abstract class AbstractSetModel
extends AbstractModel
{
  /** Reference to the SetModelCore delegate */
  protected final SetModelCore setModCore;



  //===========================================================
  //== Constructors
  //===========================================================

  /**
   * Initialize the abstract set.
   * @param name The name of the model.
   * @param mf Factory for creating the ModelCore delegate.
   * @param sf Factory for creating the SetModelCore delegate.
   */
  protected AbstractSetModel( 
    final String name, final ModelCore.Factory mf,
    final SetModelCore.Factory sf, final SlotFactory slotf )
  throws SlotAlreadyRegisteredException
  {    
    super( name, mf, LocalAttributeManagerFactory.prototype, slotf );
    setModCore = sf.create( name, this, structLock, attrManager );
  }


  
  //===========================================================
  //== Node methods
  //===========================================================

  @Override
  public final Iterator<IRNode> getNodes()
  {
    synchronized( structLock ) {
      return setModCore.getNodes();
    }
  }

  @Override
  public final void addNode( final IRNode node, final AVPair[] vals )
  {
    synchronized( structLock ) {
      setModCore.addNode( node, vals );
    }
  }

  
  
  //===========================================================
  //== Set methods
  //===========================================================

  // Inherit javadoc
  public void addNode( final IRNode node )
  {
    boolean wasAdded = false;
    synchronized( structLock ) {
      wasAdded = setModCore.addNode( node );
    }
    if( wasAdded ) {
      modelCore.fireModelEvent(
        new SetModelEvent( this, SetModelEvent.NODE_ADDED, node ) ); 
    }
  }

  // Inherit javadoc
  @Override
  public void removeNode( final IRNode node )
  {
    boolean wasRemoved = false;
    synchronized( structLock ) {
      wasRemoved = setModCore.removeNode( node );
    }
    if( wasRemoved ) {
      modelCore.fireModelEvent(
        new SetModelEvent( this, SetModelEvent.NODE_REMOVED, node ) ); 
    }
  }

  // Inherit javadoc
  public int size()
  {
    synchronized( structLock ) {
      return setModCore.size();
    }
  }



  //===========================================================
  //== Attribute convienence methods
  //===========================================================

  // Inherit javadoc
  @Override
  public final boolean isPresent( final IRNode node )
  {
    synchronized( structLock ) {
      return setModCore.isPresent( node );
    }
  }
}

