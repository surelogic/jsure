/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/MutableDelegatingSlotInfo.java,v 1.14 2007/07/10 22:16:31 aarong Exp $ */
package edu.cmu.cs.fluid.ir;

import java.util.*;

import com.surelogic.common.util.*;
/**
 * SlotInfoWrapper for "making changes" to a SlotInfo, 
 * without actually changing the original.
 * 
 * The original SlotInfo is used until the first time
 * the SlotInfo is set, at which point local storage
 * takes over.  Currently there is no way to "forget" a
 * local value.
 */
public class MutableDelegatingSlotInfo<T>
extends SlotInfo<T>
{   
  /** The first attribute. */
  private final SlotInfo<T> oldValues;

  /** The second attribute. */
  private final SlotInfo<T> localValues;
  
  private final SlotFactory slotFactory;

  /**
   * Create a new slotinfo.
   */
  public MutableDelegatingSlotInfo(final SlotInfo<T> attribute, final SlotFactory sf)
  {
    oldValues = attribute;
    
    SlotInfo<T> temp = null;
    try {
      temp = sf.newAttribute( attribute.name() + "-local-" + this.hashCode(), attribute.getType() );
    } catch( final SlotAlreadyRegisteredException e ) {
      // Shouldn't happen?
      System.err.println( "Got SlotAlreadyRegisteredException!" );
      e.printStackTrace( System.err );
      System.err.println( "Continuing..." );
    }
    localValues = temp;
    slotFactory = sf;
  }

  @Override
  public final IRType<T> type()
  {
    return oldValues.type();
  }

  @Override
  protected boolean valueExists( final IRNode node )
  {
    if( node.valueExists( localValues ) ) return true;
    else return node.valueExists( oldValues );
  }

  @SuppressWarnings("unchecked")
  @Override
  protected T getSlotValue( final IRNode node )
  throws SlotUndefinedException
  {
    // favor local over inherited
    if( node.valueExists( localValues ) ) {
      return node.getSlotValue( localValues );
    } else {
      if (oldValues.type() instanceof IRSequenceType) {
        IRSequence seq = (IRSequence) node.getSlotValue( oldValues );
        return (T) new Wrapper(seq, node);
      }
      // XXX need to wrap for composites
      return node.getSlotValue( oldValues );
    }
  }
  
  @Override
  protected void setSlotValue( final IRNode node, final T newValue )
  throws SlotImmutableException
  {
    node.setSlotValue( localValues, newValue ); 
    /*
    if (node.valueExists(localValues) && JJNode.tree.isNode(node)) {
      System.out.println("Set "+localValues.name()+" for "+DebugUnparser.toString(node));
    }
    */
  }
  
  @Override
  public final int size() {
    return oldValues.size() + localValues.size();
  }
  
  @SuppressWarnings("unchecked")
  private <V> IRSequence<V> copySequence(IRNode n, IRSequence<V> seq) {
    if (seq.isVariable()) {
      // variable, with initial size = 0
      IRSequence<V> copy  = slotFactory.newSequence(~0);
      Iterator<V> enm = seq.elements();
      for (int i=0; enm.hasNext(); i++) {
        copy.appendElement(enm.next());
      }
      return new MappingWrapper(n, seq, copy);
    }
    int size = seq.isVariable() ? ~seq.size() : seq.size();
    IRSequence<V> copy  = slotFactory.newSequence(size);
    Iterator<V> enm = seq.elements();
    for (int i=0; enm.hasNext(); i++) {
      copy.setElementAt(enm.next(), i);
    }
    return copy;
  }
  /*
  private void printLocations(IRSequence seq) {
    IRLocation loc = seq.firstLocation();
    for(int i=0; loc != null; i++) {
      System.out.println(i+": location "+loc.getID());
      loc = seq.nextLocation(loc);
    }
  }
  */
  
  /**
   * All mutators modified to implement copy-on-write
   */
  private class Wrapper<V> extends IRSequenceWrapper<V> {
    /**
     * Changes when the sequence is copied
     */
    protected IRSequence<V> sequence; 
    
    /**
     * Set to null when already copied
     */
    private IRNode n; 
    
    protected Wrapper(IRSequence<V> seq, IRNode n) {
      super(null);
      sequence = seq;
      // System.out.println("Creating wrapper sequence for "+DebugUnparser.toString(n));
      this.n = n;
    }
    
    @SuppressWarnings("unchecked")
    private IRSequence<V> copyOnWrite() {
      /*
      if (sequence.isVariable()) {
        System.out.println("Copying on write: variable sequence: "+sequence.size());
      } else {
        System.out.println("Copying on write: fixed sequence: "+sequence.size());
      }
      */
      IRSequence<V> copy = copySequence(n, sequence);
      setSlotValue(n, (T) copy);
      sequence = copy;
      n        = null;
      return copy;
    }

    @Override
    public int size() {
      return sequence.size();
    }

    @Override
    public boolean hasElements() {
      return sequence.hasElements();
    }

    @Override
    public Iteratable<V> elements() {
      return sequence.elements();
    }

    @Override
    public boolean validAt(IRLocation loc) {
      return sequence.validAt(loc);
    }

    @Override
    public V elementAt(IRLocation loc) {
      return sequence.elementAt(loc);
    }

    
    @Override
    public void setElementAt(V element, IRLocation loc) {
      copyOnWrite().setElementAt(element,loc);
    }

    @Override
    public IRLocation insertElementAt(V element, InsertionPoint ip) {
      return ip.insert(copyOnWrite(),element);
    }
    
    @Override
    public void removeElementAt(IRLocation i) {
      copyOnWrite().removeElementAt(i);
    }
      
    @Override
    public IRLocation location(int i) {
      return sequence.location(i);
    }

    @Override
    public int locationIndex(IRLocation loc) {
      return sequence.locationIndex(loc);
    }

    @Override
    public IRLocation firstLocation() {
      return sequence.firstLocation();
    }

    @Override
    public IRLocation lastLocation() {
      return sequence.lastLocation();
    }

    @Override
    public IRLocation nextLocation(IRLocation loc) {
      return sequence.nextLocation(loc);
    }
    
    @Override
    public IRLocation prevLocation(IRLocation loc) {
      return sequence.prevLocation(loc);
    }

    @Override
    public int compareLocations(IRLocation loc1, IRLocation loc2) {
      return sequence.compareLocations(loc1,loc2);
    }
    
    @Override
    public boolean isVariable() {
      return sequence.isVariable();
    }
  }

  /**
   * Intended to patch the difference between the original and copied locations
   * @author Edwin
   */
  private class MappingWrapper<V> extends IRSequenceWrapper<V> {
    /**
     * Used to remove itself when there's nothing left to map
     */
    private final IRNode n;     
    Map<IRLocation,IRLocation> locations = new HashMap<IRLocation,IRLocation>();
    
    protected MappingWrapper(IRNode n, IRSequence<V> orig, IRSequence<V> copy) {
      super(copy);
      this.n = n;
      initMappings(orig, copy);
    }

    private void initMappings(IRSequence<V> orig, IRSequence<V> copy) {
      final int size = orig.size();
      for(int i=0; i<size; i++) {
        locations.put(orig.location(i), copy.location(i));
      }
    }
    
    private IRLocation mapIn(IRLocation i) {
      IRLocation loc = locations.get(i);
      return (loc == null) ? i : loc;
    }
    
    @Override
    public boolean validAt(IRLocation loc) {
      return sequence.validAt(mapIn(loc));
    }

    @Override
    public V elementAt(IRLocation loc) {
      return sequence.elementAt(mapIn(loc));
    }

    @Override
    public void setElementAt(V element, IRLocation loc) {
      sequence.setElementAt(element, mapIn(loc));
    }

    @Override
    public IRLocation insertElementAt(V element, InsertionPoint ip) {
      // TODO
      return ip.insert(sequence, element);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void removeElementAt(IRLocation i) {
      IRLocation loc = mapIn(i);
      sequence.removeElementAt(loc);
      locations.remove(i); // Special handling
      if (locations.size() == 0) {
        System.out.println("Removing mapping wrapper");
        locations = Collections.emptyMap();
        setSlotValue(n, (T) sequence);
      }
    }
      
    @Override
    public IRLocation location(int i) {
      // TODO
      return sequence.location(i);
    }

    @Override
    public int locationIndex(IRLocation loc) {      
      return sequence.locationIndex(mapIn(loc));
    }

    @Override
    public IRLocation firstLocation() {
      // TODO
      return sequence.firstLocation();
    }

    @Override
    public IRLocation lastLocation() {
      // TODO
      return sequence.lastLocation();
    }

    @Override
    public IRLocation nextLocation(IRLocation loc) {
      // TODO
      return sequence.nextLocation(mapIn(loc));
    }
    
    @Override
    public IRLocation prevLocation(IRLocation loc) {
      // TODO
      return sequence.prevLocation(mapIn(loc));
    }

    @Override
    public int compareLocations(IRLocation loc1, IRLocation loc2) {
      return sequence.compareLocations(mapIn(loc1),mapIn(loc2));
    }
  }
}
