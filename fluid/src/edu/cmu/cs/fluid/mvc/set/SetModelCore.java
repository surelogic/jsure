// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/set/SetModelCore.java,v 1.19 2007/07/10 22:16:37 aarong Exp $

package edu.cmu.cs.fluid.mvc.set;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.util.EmptyIterator;
import edu.cmu.cs.fluid.util.ImmutableSet;
import edu.cmu.cs.fluid.util.IntegerTable;

/**
 * Generic implementation of the (new) core functionality of the
 * methods declared in {@link SetModel}.
 *
 * <P>Implements the model-level attribute {@link SetModel#SIZE}.
 *
 * @author Aaron Greenhouse
 */
public final class SetModelCore
extends AbstractCore
{
  //===========================================================
  //== Fields
  //===========================================================

  /** Storage for the {@link Model#isPresent} results. */
  private final SlotInfo<Boolean> isPresent;

  /** Storage for the {@link SetModel#SIZE} attribute */
  private final ComponentSlot<Integer> sizeAttr;



  //===========================================================
  //== Constructor
  //===========================================================

  /**
   * Create a new sequence core; only for use by factory objects.
   * To create a new instance use a factory object.
   */
  private SetModelCore(
    final String name, final SlotFactory sf, final ComponentSlot.Factory csf,
    final Model model, final Object lock, final AttributeManager manager )
  throws SlotAlreadyRegisteredException
  {
    super( model, lock, manager );

    // Init model attributes
    sizeAttr = csf.predefinedSlot(
                 IRIntegerType.prototype, IntegerTable.newInteger( 0 ) );
    attrManager.addCompAttribute( SetModel.SIZE, Model.STRUCTURAL, sizeAttr );

    isPresent = sf.newAttribute(
                  name + "-isPresent", IRBooleanType.prototype, Boolean.FALSE );
  }



  //===========================================================
  //== Methods
  //===========================================================

  /** 
   * Query if a node is in the model.
   * Caller must hold the model's structural lock.
   */
  public boolean isPresent( final IRNode node )
  {
    return ( node.getSlotValue( isPresent )).booleanValue();
  }

  /**
   * Insure that a node is not in the model.
   * Caller must hold the model's structural lock.
   */
  public boolean removeNode( final IRNode node )
  {
    if( (node.getSlotValue( isPresent )).booleanValue() ) {
      final int oldSize = size();
      node.setSlotValue( isPresent, Boolean.FALSE );
      sizeAttr.setValue( IntegerTable.newInteger( oldSize - 1 ) );
      return true;
    } else {
      return false;
    }
  }

  /**
   * Insure that a node is in the model.
   * Caller must hold the model's structural lock.
   * @return <code>true</code> if the node was added to the model;
   *   <code>false</code> if it was not added because it is already in the
   *   model.
   */
  public boolean addNode( final IRNode node )
  {
    if( !((node.getSlotValue( isPresent )).booleanValue()) ) {
      final int oldSize = size();
      node.setSlotValue( isPresent, Boolean.TRUE );
      sizeAttr.setValue( IntegerTable.newInteger( oldSize + 1 ) );
      return true;
    } else {
      return false;
    }
  }

  public void addNode(final IRNode node, final AVPair[] vals) {
    partOf.atomizeAction(new AddNode(node, vals)).execute();
  }
  
  /**
   * Add multiple nodes to the model.
   * Caller must hold the model's structural lock.
   * @param it An iterator returning the IRNodes to add to the set.
   * @return The new cardinality of the set.
   */
  public int addNodes( final Iterator it )
  {
    int size = size();
    while( it.hasNext() ) {
      final IRNode node = (IRNode) it.next();
      if( !((node.getSlotValue( isPresent )).booleanValue()) ) {
        node.setSlotValue( isPresent, Boolean.TRUE );
        size++;
      }
    }
    sizeAttr.setValue( IntegerTable.newInteger( size ) );
    return size;
  }

  /**
   * Get the number of elements in the set.
   * Caller must hold the model's structural lock.
   */
  public int size()
  {
    return (sizeAttr.getValue()).intValue();
  }

  /**
   * Get an iterator over the nodes in the set.  The nodes are not 
   * returned in any particular order.
   * Caller must hold the model's structural lock.
   */
  @SuppressWarnings("unchecked")
  public Iterator<IRNode> getNodes()
  {
    final ImmutableSet nodes = isPresent.index( Boolean.TRUE );
    if( (nodes != null) && !nodes.isInfinite() ) {
      return nodes.iterator();
    } else {
      return new EmptyIterator<IRNode>();
    }
  }

  /**
   * Action that implements the default {@link Model#addNode} behavior.
   */
  public class AddNode implements Model.AtomizedModelAction {
    private IRNode node;
    private AVPair[] pairs;

    public AddNode( final IRNode n, final AVPair[] p )
    {
      node = n;
      pairs = p;
    }

    @Override
    public List<ModelEvent> execute() {
      final SetModel sm = (SetModel) SetModelCore.this.partOf;
      sm.addNode( node );
      sm.setNodeAttributes( node, pairs );
      return Collections.<ModelEvent>singletonList(
          new SetModelEvent(
              sm, SetModelEvent.NODE_ADDED, node));
    }
  }



  //===========================================================
  //== Methods for assisting in the construction of Set Models
  //===========================================================

  /**
   * Remove all nodes from model, giving it a cardinality of zero.
   * Caller must hold the model's structural lock.
   */
  public void clearModel() {
    final Iterator nodes = isPresent.index( Boolean.TRUE ).iterator();
    while( nodes.hasNext() ) {
      final IRNode node = (IRNode) nodes.next();
      node.setSlotValue( isPresent, Boolean.FALSE );
    }
    sizeAttr.setValue( IntegerTable.newInteger( 0 ) );
  }

  

  //===========================================================
  //== ModelCore Factory Interfaces/Classes
  //===========================================================

  public static interface Factory 
  {
    public SetModelCore create(
      String name, Model model, Object structLock, AttributeManager manager )
    throws SlotAlreadyRegisteredException;
  }
  
  public static class StandardFactory
  implements Factory
  {
    private final SlotFactory slotFactory;
    private final ComponentSlot.Factory csFactory;
   
    public StandardFactory(
      final SlotFactory sf, final ComponentSlot.Factory csf )
    {
      slotFactory = sf;
      csFactory = csf;
    }

    @Override
    public SetModelCore create(
      final String name, final Model model, final Object structLock,
      final AttributeManager manager )
    throws SlotAlreadyRegisteredException
    {
      return new SetModelCore(
                   name, slotFactory, csFactory, model, structLock, manager );
    }
  }
}

