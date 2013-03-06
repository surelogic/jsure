// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/sequence/SequenceModelCore.java,v 1.38 2007/07/10 22:16:30 aarong Exp $

package edu.cmu.cs.fluid.mvc.sequence;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.set.SetModel;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.util.ImmutableSet;
import edu.cmu.cs.fluid.util.IntegerTable;
import edu.cmu.cs.fluid.util.Iteratable;

/**
 * Generic implementation of the (new) functionality
 * declared in {@link SequenceModel}.  This core also tackles the SetModel
 * functionality because it is eaiser to incorporate it here then to try
 * and coordinate with a peer SetModelCore.
 *
 * <P>Adds the model-level attribute {@link SequenceModel#SIZE}.
 * 
 * <P>Adds the node-level attributes {@link SequenceModel#LOCATION},
 * {@link SequenceModel#INDEX}, {@link SequenceModel#NEXT}, and
 * {@link SequenceModel#PREVIOUS}.
 *
 * <P>The attributes {@link SequenceModel#LOCATION} and {@link
 * SequenceModel#INDEX} can be set up to be allow the model to be changed by
 * mutated the value of the attribute; i.e., setting the location or the index
 * of a node will move the node within the sequence.  The
 * callback given to the core's constructor must cause the model to send
 * break events when these attributes are changed.
 *
 * <p>Supports stateful views that require sorting via the method
 * {@link #insertSorted} which can be used to implement an insertion
 * sort.  A useful implementation of an insertion sort is provided by
 * {@link #buildSorted}.
 *
 * @author Aaron Greenhouse
 */
public final class SequenceModelCore
extends AbstractCore
implements IRSequence<IRNode>
{
  //===========================================================
  //== Fields
  //===========================================================

  /** The Sequence */
  private final IRSequence<IRNode> sequence;

  /** Mapping from nodes in the sequence to their positions. */
  private final SlotInfo<IRLocation> locations;

  /** Mapping from nodes to indexes. */
  private final SlotInfo<Integer> indexes;

  /** Storage for the {@link Model#isPresent} method */
  private final SlotInfo<Boolean> isPresent;

  /** Map from nodes to the next node. */
  private final SlotInfo<IRNode> next;

  /** Map from nodes to the previous node. */
  private final SlotInfo<IRNode> previous;



  //===========================================================
  //== Constructor
  //===========================================================

  /**
   * Create a new sequence core; only for use by factory objects.
   * To create a new instance use a factory object.
   * @param isMutable Whether or not the
   *    {@link SequenceModel#LOCATION}
   *    and {@link SequenceModel#INDEX} attributes are mutable.
   */
  protected SequenceModelCore(
    final Model model, final Object lock, final AttributeManager manager,
    final String name, final IRSequence<IRNode> s, final SlotFactory sf,
    final boolean isMutable, final AttributeChangedCallback cb )
  throws SlotAlreadyRegisteredException
  {
    super( model, lock, manager );
    sequence = s;

    // Init model-level attributes
    attrManager.addCompAttribute(
      SetModel.SIZE, Model.STRUCTURAL, new SeqSizeComponentSlot() );
    attrManager.addCompAttribute(
      SequenceModel.FIRST, Model.STRUCTURAL, new FirstEltComponentSlot() );

    // Init node-level attributes
    isPresent = sf.newAttribute(
      name + "-isPresent", IRBooleanType.prototype, Boolean.FALSE );
    locations = sf.newAttribute(
      name + "-" + SequenceModel.LOCATION, IRLocationType.prototype, null );
    indexes = new LocationsToIndexesSlotInfo( locations );
    next = new NextNodeSlotInfo( name + "-" + SequenceModel.NEXT );
    previous = new PreviousNodeSlotInfo( name + "-" + SequenceModel.PREVIOUS );

    attrManager.addNodeAttribute( SequenceModel.NEXT, Model.STRUCTURAL, next );
    attrManager.addNodeAttribute(
      SequenceModel.PREVIOUS, Model.STRUCTURAL, previous );
    attrManager.addNodeAttribute(
      SequenceModel.LOCATION, Model.STRUCTURAL, isMutable,
      new SettableLocationsSlotInfo( locations ), cb );
    attrManager.addNodeAttribute(
      SequenceModel.INDEX, Model.STRUCTURAL, isMutable, indexes, cb );
  }



  //===========================================================
  //== Inner Classes
  //===========================================================

  /**
   * SlotInfo wrapper that is used to cause changes to the values of
   * {@link SequenceModel#LOCATION} to cause a move of the node in the
   * underlying IRSequence.
   */
  private class SettableLocationsSlotInfo
  extends SlotInfoWrapper<IRLocation>
  {
    public SettableLocationsSlotInfo( final SlotInfo<IRLocation> locs )
    {
      super( locs );
    }

    @Override
    protected void setSlotValue( final IRNode node, final IRLocation newLoc )
    throws SlotImmutableException
    {
      final IRLocation oldLoc = node.getSlotValue( wrapped );
      final int relationship = compareLocations( newLoc, oldLoc );
      if( relationship != 0 ) {
        removeElementAt( oldLoc );
        if( relationship < 0 ) {
          insertElementBefore( node, newLoc );
        } else {
          insertElementAfter( node, newLoc );
        }
      }
    }
  }


  
  
  /**
   * SlotInfo wrapper that is used to derive
   * the {@link SequenceModel#INDEX} Attribute from the
   * {@link SequenceModel#LOCATION} attribute.
   */
  private class LocationsToIndexesSlotInfo
  extends SlotInfoWrapper
  {
    public LocationsToIndexesSlotInfo( final SlotInfo locs )
    {
      super( locs );
    }

    @Override
    protected Object getSlotValue( final IRNode node )
    {
      if( isPresent( node ) ) {
        final IRLocation loc = (IRLocation)super.getSlotValue( node );
        return IntegerTable.newInteger( locationIndex( loc ) );
      } else {
        throw new SlotUndefinedException(
          "Node " + node + " is not in model \"" + partOf.getName() + "\"." );
      }
    }

    @Override
    protected void setSlotValue( final IRNode node, final Object val )
    throws SlotImmutableException
    {
      final int newIdx = ((Integer)val).intValue();
      final IRLocation oldLoc = (IRLocation)node.getSlotValue( wrapped );
      final int oldIdx = locationIndex( oldLoc );

      if( newIdx != oldIdx ) {
        removeElementAt( oldLoc );
        if( newIdx < oldIdx ) {
          insertElementBefore( node, location( newIdx ) );
        } else {
          insertElementAfter( node, location( newIdx-1 ) );
        }
      }
    }

    @Override
    public ImmutableSet<IRNode> index( final Object value )
    {
      try {
        final int index = ((Number)value).intValue();
        final IRLocation loc = location( index );
        return super.index( loc );
      } catch( final ClassCastException e ) {
        throw new ClassCastException( "Indexes Attribute must be " +
				      "indexed with an Integer" );
      }
    }
  }

  
  
  /**
   * SlotInfo wrapper that is used to derive
   * the {@link SequenceModel#NEXT} Attribute from the
   * underlying IRSequence.
   */
  private class NextNodeSlotInfo
  extends DerivedSlotInfo<IRNode>
  {
    public NextNodeSlotInfo( final String name )
    throws SlotAlreadyRegisteredException
    {
      super( name, IRNodeType.prototype );
    }

    @Override
    protected IRNode getSlotValue( final IRNode node )
    {
      if( isPresent( node ) ) {
        final IRLocation current = node.getSlotValue( locations );
        final IRLocation next = sequence.nextLocation( current );
        return ((next == null) ? null : sequence.elementAt( next ));
      } else {
        throw new SlotUndefinedException(
          "Node " + node + " is not in model \"" + partOf.getName() + "\"." );
      }
    }

    @Override
    protected boolean valueExists( final IRNode node )
    {
      return (node.getSlotValue( isPresent )).booleanValue();
    }
  }

  
  
  /**
   * SlotInfo wrapper that is used to derive
   * the {@link SequenceModel#PREVIOUS} Attribute from the
   * underlying IRSequence.
   */
  private class PreviousNodeSlotInfo
  extends DerivedSlotInfo<IRNode>
  {
    public PreviousNodeSlotInfo( final String name )
    throws SlotAlreadyRegisteredException
    {
      super( name, IRNodeType.prototype );
    }

    @Override
    protected IRNode getSlotValue( final IRNode node )
    {
      if( isPresent( node ) ) {
        final IRLocation current = node.getSlotValue( locations );
        final IRLocation previous = sequence.prevLocation( current );
        return ((previous == null) ? null : sequence.elementAt( previous ));
      } else {
        throw new SlotUndefinedException(
          "Node " + node + " is not in model \"" + partOf.getName() + "\"." );
      }
    }

    @Override
    protected boolean valueExists( final IRNode node )
    {
      return (node.getSlotValue( isPresent )).booleanValue();
    }
  }

  
  
  /**
   * ComponentSlot used for the {@link SequenceModel#SIZE} attribute.
   * Derives its value from the underlying IRSequence.
   * Synchronization is not required, because it is only used
   * via the attribute manager which will wrap it in a 
   * synchronizer.
   */
  private class SeqSizeComponentSlot extends DefaultDescribe
  implements ComponentSlot<Integer>
  {
    @Override
    public IRType<Integer> getType() { return IRIntegerType.prototype; }

    @Override
    public Integer getValue()
    {
      return IntegerTable.newInteger( sequence.size() );
    }

    @Override
    public boolean isValid() { return true; }
    @Override
    public boolean isChanged() { return true; }
  
    @Override
    public Slot<Integer> setValue( final Integer newValue ) 
      throws SlotImmutableException
    {
      throw new SlotImmutableException( "Cannot set attribute " +
					"\"SetModel.SIZE\"" );
    }

    @Override
    public void writeValue( final IRType ty, final IROutput out )
      throws IOException
    {
      throw new UnsupportedOperationException( "Not yet implemented" ); 
    }

    @Override
    public Slot<Integer> readValue( final IRType ty, final IRInput in )
      throws IOException
    {
      throw new UnsupportedOperationException( "Not yet implemented" ); 
    }
  }


  /**
   * ComponentSlot used for the {@link SequenceModel#FIRST} attribute.
   * Derives its value from the underlying IRSequence.
   * Synchronization is not required, because it is only used
   * via the attribute manager which will wrap it in a 
   * synchronizer.
   */
  private class FirstEltComponentSlot extends DefaultDescribe
  implements ComponentSlot<IRNode>
  {
    @Override
    public IRType<IRNode> getType() { return IRNodeType.prototype; }

    @Override
    public IRNode getValue()
    {
      return
	(sequence.size() == 0)
	? null 
	: sequence.elementAt( 0 );
    }

    @Override
    public boolean isValid() { return true; }
    @Override
    public boolean isChanged() { return true; }
  
    @Override
    public Slot<IRNode> setValue( final IRNode newValue ) 
      throws SlotImmutableException
    {
      throw new SlotImmutableException( "Cannot set attribute " +
					"\"SequenceModel.FIRST\"" );
    }

    @Override
    public void writeValue( final IRType ty, final IROutput out )
      throws IOException
    {
      throw new UnsupportedOperationException( "Not yet implemented" ); 
    }

    @Override
    public Slot<IRNode> readValue( final IRType ty, final IRInput in )
      throws IOException
    {
      throw new UnsupportedOperationException( "Not yet implemented" ); 
    }
  }



  //===========================================================
  //== Methods from IRSequenceWrapper
  //===========================================================

  @Override
  public IRState getParent() {
    return null; // TODO
  }
  
  // Inherit java doc for IRSequence 
  @Override
  public int size() {
    return sequence.size();
  }

  // Inherit java doc for IRSequence 
  @Override
  public boolean isVariable() {
    return sequence.isVariable();
  }

  // Inherit java doc for IRSequence 
  @Override
  public boolean hasElements() {
    return sequence.hasElements(); 
  }

  // Inherit java doc for IRSequence 
  @Override
  @SuppressWarnings("unchecked")
  public Iteratable<IRNode> elements() {
    return sequence.elements(); 
  }

  // Inherit java doc for IRSequence 
  @Override
  public boolean validAt( final int i ) {
    return sequence.validAt( i );
  }

  // Inherit java doc for IRSequence 
  @Override
  public boolean validAt(IRLocation loc) {
    return sequence.validAt(loc);
  }

  // Inherit java doc for IRSequence 
  @Override
  public IRNode elementAt( final int i ) {
    return sequence.elementAt( i );
  }

  // Inherit java doc for IRSequence 
  @Override
  public IRNode elementAt( final IRLocation loc ) {
    return sequence.elementAt(loc);
  }

  @Override
  public void setElementAt( final IRNode element, final int i ) {
    IRLocation loc = location(i);
    if (loc == null)
      throw new IRSequenceException("index out of bounds");
    setElementAt( element, loc );
  }

  // Inherit java doc for IRSequence 
  // Override to update the locations attribute
  @Override
  public void setElementAt( final IRNode element, final IRLocation loc )
  {
    /*
    if( !(element instanceof IRNode) ) {
      throw new ClassCastException( "Can only add IRNodes to SequenceModels." );
    }
    */
    final IRNode old = elementAt( loc );
    old.setSlotValue( isPresent, Boolean.FALSE );
    old.setSlotValue( locations, null );
    sequence.setElementAt(element,loc);
    final IRNode newNode = element;
    newNode.setSlotValue( isPresent, Boolean.TRUE );
    newNode.setSlotValue( locations, loc );
  }

  @Override
  public IRLocation insertElement( final IRNode element ) {
    return insertElementAt(element,InsertionPoint.first);
  }

  @Override
  public IRLocation appendElement( final IRNode element ) {
    return insertElementAt(element,InsertionPoint.last);
  }
  
  @Override
  public IRLocation insertElementBefore( final IRNode element, final IRLocation i ) {
    return insertElementAt(element,InsertionPoint.createBefore(i));
  }

  @Override
  public IRLocation insertElementAfter( final IRNode element, final IRLocation i ) {
    return insertElementAt(element,InsertionPoint.createAfter(i));
  }

  // Inherit java doc for IRSequence 
  // Override to update the locations attribute
  public IRLocation insertElementAt( final IRNode element, final InsertionPoint ip )
  { 
    /*
    if( !(element instanceof IRNode) ) {
      throw new ClassCastException( "Can only add IRNodes to SequenceModels." );
    }
    */
    final IRLocation loc = ip.insert( sequence, element );
    final IRNode newNode = element;
    newNode.setSlotValue( isPresent, Boolean.TRUE );
    newNode.setSlotValue( locations, loc );
    return loc;
  }
  
  // Inherit java doc for IRSequence 
  // Override to update the locations attribute
  @Override
  public void removeElementAt( final IRLocation i ) {
    final IRNode old = elementAt( i );
    sequence.removeElementAt( i );
    old.setSlotValue( isPresent, Boolean.FALSE );
    old.setSlotValue( locations, null );
  }
    
  // Inherit java doc for IRSequence 
  @Override
  public IRLocation location( final int i ) {
    return sequence.location(i);
  }

  // Inherit java doc for IRSequence 
  @Override
  public int locationIndex( final IRLocation loc ) {
    return sequence.locationIndex(loc);
  }

  // Inherit java doc for IRSequence 
  @Override
  public IRLocation firstLocation() {
    return sequence.firstLocation();
  }

  // Inherit java doc for IRSequence 
  @Override
  public IRLocation lastLocation() {
    return sequence.lastLocation();
  }

  // Inherit java doc for IRSequence 
  @Override
  public IRLocation nextLocation( final IRLocation loc ) {
    return sequence.nextLocation(loc);
  }
  
  // Inherit java doc for IRSequence 
  @Override
  public IRLocation prevLocation( final IRLocation loc ) {
    return sequence.prevLocation(loc);
  }

  // Inherit java doc for IRSequence 
  @Override
  public int compareLocations( final IRLocation loc1, final IRLocation loc2 ) {
    return sequence.compareLocations(loc1,loc2);
  }

  // I/O currently NOPs (sequences are "derived", not in storable form.

  @Override
  public void writeValue( final IROutput out)
  throws IOException
  {
    sequence.writeValue( out );
  }

  @Override
  public void writeContents( final IRCompoundType t, final IROutput out)
  throws IOException
  {
    sequence.writeContents( t, out );
  }

  @Override
  public void readContents( final IRCompoundType t, final IRInput in )
  throws IOException
  {
    sequence.readContents( t, in );
  }

  @Override
  public boolean isChanged()
  {
    return sequence.isChanged();
  }

  @Override
  public void writeChangedContents( final IRCompoundType t, final IROutput out )
  throws IOException
  {
    sequence.writeChangedContents( t, out );
  }

  @Override
  public void readChangedContents( final IRCompoundType t, final IRInput in )
  throws IOException
  {
    sequence.readChangedContents( t, in );
  }



  //===========================================================
  //== Node methods
  //===========================================================

  /**
   * Returns an iterator over the nodes in the order they
   * appear in the sequence.
   */
  public Iteratable<IRNode> getNodes()
  {
    return elements();
  }

  /**
   * Add a node to the sequence.
   * If no value for the {@link SequenceModel#LOCATION} attribute is
   * specified, then the node is appended to the sequence.  Otherwise the
   * node is inserted <em>before</em> the given position.  An exception
   * is thrown if the location is not valid for the sequence.
   *
   * @param node The node to add to the sequence.
   * @param vals The attributes and their initial values.
   * @exception UnknownAttributeException 
   * Thrown when the node is rejected because a given attribute is
   * unrecognized.
   * @exception IllegalArgumentException
   * Thrown when the node is rejected because some attribute values would
   * result in the model being in an illegal state.
   */
  public void addNode( final IRNode node, final AVPair[] vals )
  {
    /* Delegate to an action because changing the values of the properties
     * causes model events to be generated.  By using an action, these
     * events are captured and fired out as one composite event once all the
     * property values have been changed.
     */
    partOf.atomizeAction(new AddNode(node, vals)).execute();
  }

  /**
   * Action used to implement {@link #addNode}
   */
  private class AddNode implements Model.AtomizedModelAction {
    private final IRNode node;

    private final AVPair[] pairs;

    public AddNode(final IRNode n, final AVPair[] p) {
      node = n;
      pairs = p;
    }

    @Override
    public List<ModelEvent> execute() {
      final Model model = SequenceModelCore.this.partOf;
      final AVPair locAtt = AVPair.findAttribute(pairs, SequenceModel.LOCATION);
      boolean added = false;
      IRLocation insertedAt = null;

      // Check that all the attributes are understood.
      // Check before we add the node to the model so that
      // we don't add the node and then have an error.
      for (int i = 0; i < pairs.length; i++) {
        if (!model.isNodeAttribute(pairs[i].getAttribute())) {
          throw new UnknownAttributeException(pairs[i].getAttribute(), model);
        }
      }

      // add the node
      try {
        final SequenceModel seq = (SequenceModel) model;

        // No location specified, append the node
        if (locAtt == null) {
          insertedAt = seq.appendElement(node);
          added = true;
        } else {
          // insert node at given location
          insertedAt = seq.insertElementBefore(node, (IRLocation) locAtt
              .getValue());
          added = true;
        }
      } catch (ClassCastException e) {
        throw new IllegalArgumentException("Value for LOCATION attribute "
            + "is not an IRLocation");
      } catch (IRSequenceException e) {
        throw new IllegalArgumentException("Couldn't insert node: "
            + e.getMessage());
      } catch (Exception e) {
        throw new IllegalArgumentException("Couldn't insert node: "
            + e.getMessage());
      }

      // set attribute values
      if (added) {
        for (int i = 0; i < pairs.length; i++) {
          if (locAtt != pairs[i]) {
            final SlotInfo attr = model.getNodeAttribute(pairs[i]
                .getAttribute());
            node.setSlotValue(attr, pairs[i].getValue());
          }
        }
      }

      return Collections.<ModelEvent>singletonList(
          new SequenceModelEvent(
              model, SequenceModelEvent.NODE_INSERTED, node, insertedAt));
    }
  }



  // ===========================================================
  //== Attribute Setter Method
  //===========================================================

  /**
   * Implementation of {@link Model#setNodeAttributes} for SequenceModels.
   * @exception IllegalArgumentException Thrown if values for both
   * the {@link SequenceModel#INDEX} and {@link SequenceModel#LOCATION}
   * attributes are provided.
   */
  public void setNodeAttributes( final IRNode node, final AVPair[] pairs,
                                 final ModelCore modelCore )
  {
    boolean index = false;
    boolean location = false;
    
    for( int i = 0; (i < pairs.length) && !(index && location); i++ ) {
      final String attr = pairs[i].getAttribute();
      index = (attr == SequenceModel.INDEX);
      location = (attr == SequenceModel.LOCATION);
    }
    if( index && location ) {
      throw new IllegalArgumentException(
        "Cannot set both " + SequenceModel.INDEX + " and " + SequenceModel.LOCATION );
    }
    modelCore.setNodeAttributes( node, pairs );
  }



  //===========================================================
  //== Attribute convienence methods
  //===========================================================

  /**
   * Query if a node is part of the model.
   */
  public boolean isPresent( final IRNode node )
  {
    final Boolean val = node.getSlotValue( isPresent );
    return val.booleanValue();
  }

  /**
   * Get the value of the {@link SequenceModel#LOCATION} attribute.
   */
  public IRLocation getLocation( final IRNode node )
  {
    return node.getSlotValue( locations );
  }
  
  /**
   * Get the value of the {@link SequenceModel#INDEX} attribute.
   */
  public int getIndex( final IRNode node )
  {
    final Integer i = node.getSlotValue( indexes );
    return i.intValue();
  }

  /**
   * Implementation of {@link Model#compValueToString}.  Understands
   * that attribute {@link SequenceModel#FIRST} refers to a node within
   * the model, and prints the node's identity instead of using the 
   * default IRNode printing.
   */
  public String compValueToString(
    final ModelCore modelCore, final String attr )
  throws UnknownAttributeException
  {
    if( attr == SequenceModel.FIRST ) {
      return partOf.idNode( (IRNode) partOf.getCompAttribute(
                                       SequenceModel.FIRST ).getValue() );
    } else {
      return modelCore.compValueToString( attr );
    }
  }
  

  
  //===========================================================
  //== Methods for assisting in the building of models
  //===========================================================

  /**
   * Clear the model, producing a sequence of size 0.
   * Caller must hold the structural lock.
   */
  public void clearModel()
  {
    final int oldSize = size();
    if( oldSize > 0 ) {
      for( int i = 0; i < oldSize; i++ ) {
        removeElementAt( location( 0 ) );
      }
    }
  }

  /**
   * Insert a node into the sequence based on the order defined
   * by the given {@link Comparator} object.  It is assumed that the
   * sequence is currently sorted according to this order.  This 
   * method is made public in case it is not convienent for the nodes
   * to be sorted to be placed in a single Iterator as required by
   * {@link #buildSorted}.
   *
   * @param node The node to be inserted into the sequence.
   * @param order The total order used to sort the nodes.
   * @param seqSize The current size of the sequence.  (Used instead of 
   *   {@link #size()} as a bit of optimization.)
   */
  public void insertSorted(
    final IRNode node, final Comparator<IRNode> order, final int seqSize )
  {
    int insertBeforeIdx = seqSize;
    for( int i = 0; (i < seqSize) && (insertBeforeIdx == seqSize); i++ ) {
      final IRNode n2 = elementAt( i );
      if( order.compare( node, n2 ) < 0 ) insertBeforeIdx = i;
    }
    if( insertBeforeIdx == seqSize ) {
      appendElement( node );
    } else {
      insertElementBefore( node, location( insertBeforeIdx ) );
    }
  }

  /**
   * Given an Iterator over IRNodes, build a sequence that contains all the
   * nodes in the iterator sorted according to the provided total order.
   * <em>Assumes the sequence is initiallly empty (with size 0)</em>.
   * @param nodes Iterator over the IRNodes that should be placed into 
   *   the sequence.
   * @param order The total ordering used to sort the nodes.
   */
  public void buildSorted( final Iterator<IRNode> nodes, final Comparator<IRNode> order )
  {
    /* special case for first node */
    if( nodes.hasNext() ) {
      appendElement( nodes.next() );

      /* continue with rest of nodes */
      int seqSize = 1;
      while( nodes.hasNext() ) {        
        insertSorted( nodes.next(), order, seqSize );
        seqSize += 1;
      }
    }
  }
  
  
  
  //===========================================================
  //== ModelCore Factory Interfaces/Classes
  //===========================================================

  public static interface Factory 
  {
    public SequenceModelCore create(
      Model model, Object lock, AttributeManager manager, String name,
      AttributeChangedCallback cb )
    throws SlotAlreadyRegisteredException;
  }

  public static class StandardFactory
  implements Factory
  {
    private final IRSequence<IRNode> seq;
    private final SlotFactory slotFactory;
    private final boolean isMutable;
    
    public StandardFactory(
      final IRSequence<IRNode> s, final SlotFactory sf, final boolean isMut )
    {
      seq = s;
      slotFactory = sf;
      isMutable = isMut;
    }

    @Override
    public SequenceModelCore create(
      final Model model, final Object lock, final AttributeManager manager,
      final String name, final AttributeChangedCallback cb )
    throws SlotAlreadyRegisteredException
    {
      return new SequenceModelCore(
                   model, lock, manager, name, seq, slotFactory,
                   isMutable, cb );
    }
  }
}

