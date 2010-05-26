/*
 * Created on Jul 11, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package edu.cmu.cs.fluid.mvc.tree.attributes;

import java.io.IOException;
import java.io.PrintStream;
import edu.cmu.cs.fluid.ir.IRCompoundType;
import edu.cmu.cs.fluid.ir.IRInput;
import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IROutput;
import edu.cmu.cs.fluid.ir.IRSequence;
import edu.cmu.cs.fluid.ir.IRSequenceIterator;
import edu.cmu.cs.fluid.ir.IRSequenceException;
import edu.cmu.cs.fluid.ir.IRState;
import edu.cmu.cs.fluid.ir.InsertionPoint;
import edu.cmu.cs.fluid.util.Iteratable;

/**
 * Abstract wrapper for IRSequences that are values of model attributes. 
 * Provides synchronization needed to coordinate modification of the
 * sequence with modification of the model.  Also allows for modification
 * to be prevented.  Uses a callback mechanish to indicate when changes
 * to the sequence have been made. This is meant to be used by the model
 * implementation to catch changes so that the appropriate model events
 * can be sent out.
 * 
 * <P>This class does not subclass IRSequenceWrapper because there isn't
 * any implementation advantage to it, although it is very similar to it.
 * 
 * @see edu.cmu.cs.fluid.ir.IRSequenceWrapper
 */
@SuppressWarnings("unchecked")
public abstract class AbstractSynchronizedMutableSequenceAttributeValue<T>
  implements IRSequence<T> {
  /** The wrapped sequence.*/
  protected final IRSequence<T> sequence;

  /** The lock of the model the sequence is a part of. */
  protected final Object structLock;

  /** The IRNode for which this sequence is an attribute value. */
  protected final IRNode belongsTo;

  /** The callback to use when the sequence is mutated. */
  protected final Callback callback;

  /** Whether the sequence should be immutable. */
  protected final boolean isMutable;



  //----------- Inner Classes ---------------------------------
  
  /**
   * Interface for callbacks issued when a wrapped sequence is mutated.
   */
  public static interface Callback {
    /**
     * Callback method invoked when an element of the sequence is set.
     */
	public void setElementAt(
      IRSequence seq,
      IRNode seqNode,
      Object elt,
      Object oldElt);

    /**
     * Callback method invoked when an element is inserted into the sequence.
     */
    public void insertElementAt(
      IRSequence seq,
      IRNode seqNode,
      Object elt,
      InsertionPoint loc);

    /**
     * callback method invoked when an element is remved from the sequence.
     */
    public void removeElementAt(IRSequence seq, IRNode seqNode, Object oldElt);
  }



  /**
   * A null callback.
   */
  private static final class NullCallback implements Callback {
    public void insertElementAt(
      final IRSequence seq,
      final IRNode seqNode,
      final Object elt,
      final InsertionPoint loc) {
    }

    public void removeElementAt(
      final IRSequence seq,
      final IRNode seqNode,
      final Object oldElt) {
    }

    public void setElementAt(
      final IRSequence seq,
      final IRNode seqNode,
      final Object elt,
      final Object oldElt) {
    }
  }



  // ----------- Static fields --------------------------
  
  /** A prototypical null callback. */
  public static final Callback nullCallback = new NullCallback();



  // ---------- Constructor --------------------------------
  
  /**
   * Create a new wrapper for a <em>mutable</em> sequence that is an attribute
   * value of a model.
   * @param mutex The model's structural lock.
   * @param node The node for which the sequence is an attribute value.
   * @param seq The sequence to wrap.
   * @param mutable Whether the wrapper should enforce immutability.
   * @param cb The callback to use when the sequence is changed.
   */
  public AbstractSynchronizedMutableSequenceAttributeValue(
    final Object mutex,
    final IRNode node,
    final IRSequence<T> seq,
    final boolean mutable,
    final Callback cb) {
    sequence = seq;
    isMutable = mutable;
    structLock = mutex;
    belongsTo = node;
    callback = cb;
  }



  // ---- Methods ----------
    
  public final int size() {
    synchronized (structLock) {
      return sequence.size();
    }
  }

  public final boolean isVariable() {
    synchronized (structLock) {
      return sequence.isVariable();
    }
  }

  public final boolean hasElements() {
    synchronized (structLock) {
      return sequence.hasElements();
    }
  }

  public final Iteratable<T> elements() {
    /* Create a new enumeration based on the wrapped presentation.
     * This way the enumeration picks up any changes made by the
     * wrapper.
     */
    return new IRSequenceIterator<T>(this);
  }

  public final boolean validAt(final int i) {
    synchronized (structLock) {
      final IRLocation loc = sequence.location(i);
      if (loc == null)
        throw new IRSequenceException("index out of bounds");
      return validAtImpl(loc);
    }
  }

  public final boolean validAt(final IRLocation loc) {
    synchronized (structLock) {
      return validAtImpl(loc);
    }
  }

  /**
   * @-requiresLock structLock
   */
  protected abstract boolean validAtImpl(IRLocation loc);

  public final T elementAt(int i) {
    synchronized (structLock) {
      final IRLocation loc = sequence.location(i);
      if (loc == null)
        throw new IRSequenceException("index out of bounds");
      return elementAtImpl(loc);
    }
  }

  public final T elementAt(final IRLocation loc) {
    synchronized (structLock) {
      return elementAtImpl(loc);
    }
  }

  /**
   * @requiresLock structLock
   */
  protected abstract T elementAtImpl(IRLocation loc);

  public final void setElementAt(final T element, final int i) {
    Object oldElt = null;
    synchronized (structLock) {
      final IRLocation loc = sequence.location(i);
      if (loc == null)
        throw new IRSequenceException("index out of bounds");
      oldElt = setElementAtImpl(element,loc);
    }
    callback.setElementAt( sequence, belongsTo, element, oldElt );
  }

  public final void setElementAt(final T element, final IRLocation loc) {
    Object oldElt = null;
    synchronized (structLock) {
      oldElt = setElementAtImpl(element, loc);
    }
    callback.setElementAt( sequence, belongsTo, element, oldElt );
  }

  /**
   * @requiresLock structLock
   */
  protected T setElementAtImpl(final T element, final IRLocation loc) {
    if( !isMutable ) {
      throw new UnsupportedOperationException( "Sequence is immutable." );
    }
    final T oldElt = sequence.elementAt( loc );
    sequence.setElementAt( element, loc );
    return oldElt;
  }

  public final IRLocation insertElement(final T element) {
    return insertElementAt(element,InsertionPoint.first);
  }

  public final IRLocation appendElement(final T element) {
    return insertElementAt(element,InsertionPoint.last);
  }

  public final IRLocation insertElementBefore(
    final T element,
    final IRLocation i) {
      return insertElementAt(element,InsertionPoint.createBefore(i));
  }

  public final IRLocation insertElementAfter(
    final T element,
    final IRLocation i) {
      return insertElementAt(element,InsertionPoint.createAfter(i));
  }

  protected IRLocation insertElementAt(final T element, final InsertionPoint ip) {
    IRLocation newLoc = null;
    synchronized( structLock ) {
      if( !isMutable ) {
        throw new UnsupportedOperationException( "Sequence is immutable." );
      }
      newLoc = ip.insert(sequence, element);
    }
    callback.insertElementAt( sequence, belongsTo, element, ip );
    return newLoc;
  }

  public final void removeElementAt(final IRLocation i) {
    T oldElt = null;
    synchronized( structLock ) {
      if( !isMutable ) {
        throw new UnsupportedOperationException( "Sequence is immutable." );
      }
      oldElt = sequence.elementAt( i );
      sequence.removeElementAt( i );
    }
    callback.removeElementAt( sequence, belongsTo, oldElt );
  }

  public final IRLocation location(final int i) {
    synchronized (structLock) {
      return sequence.location(i);
    }
  }

  public final int locationIndex(final IRLocation loc) {
    synchronized (structLock) {
      return sequence.locationIndex(loc);
    }
  }

  public final IRLocation firstLocation() {
    synchronized (structLock) {
      return sequence.firstLocation();
    }
  }

  public final IRLocation lastLocation() {
    synchronized (structLock) {
      return sequence.lastLocation();
    }
  }

  public final IRLocation nextLocation(final IRLocation loc) {
    synchronized (structLock) {
      return sequence.nextLocation(loc);
    }
  }

  public final IRLocation prevLocation(final IRLocation loc) {
    synchronized (structLock) {
      return sequence.prevLocation(loc);
    }
  }

  public final int compareLocations(
    final IRLocation loc1,
    final IRLocation loc2) {
    synchronized (structLock) {
      return sequence.compareLocations(loc1, loc2);
    }
  }

  public void writeValue(IROutput out) throws IOException {
  }

  public void describe(PrintStream out) {
  }

  public void writeContents(IRCompoundType t, IROutput out)
    throws IOException {
  }

  public void readContents(IRCompoundType t, IRInput in) throws IOException {
  }

  public boolean isChanged() {
    return false;
  }

  public void writeChangedContents(IRCompoundType t, IROutput out)
    throws IOException {
  }

  public void readChangedContents(IRCompoundType t, IRInput in)
    throws IOException {
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IRState#getParent()
   */
  public IRState getParent() {
    synchronized (structLock) {
      return sequence.getParent();
    }
  }
}
