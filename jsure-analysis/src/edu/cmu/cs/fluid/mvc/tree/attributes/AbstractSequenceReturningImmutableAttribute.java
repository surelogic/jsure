/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/attributes/AbstractSequenceReturningImmutableAttribute.java,v 1.5 2006/03/29 19:54:51 chance Exp $
 *
 * MutableSequenceReturningImmutableAttribute.java
 * Created on March 29, 2002, 3:31 PM
 */

package edu.cmu.cs.fluid.mvc.tree.attributes;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.util.ImmutableSet;

/**
 * Abstract SlotInfo wrapper for handling sequence-valued immutable attributes.
 * Doesn't worry about checking for IRNode presentness/attributability
 * because it will be wrapped yet again by something that does this check.
 * Same goes for synchronization on the model's structural lock.  Subclasses
 * implement method {@link #createWrappedSequence} to control how the
 * sequence values in the attribute are wrapped.
 *
 * @author Aaron Greenhouse
 */
public abstract class AbstractSequenceReturningImmutableAttribute<T>
extends DerivedSlotInfo<IRSequence<T>>
{
  /**
   * Counter used to make sure that all the attribute wrappers
   * have unique names.
   */
  private static long wrapperCount = 0L;


  
  /** SlotInfo to wrap.  Must be immutable but return mutable sequences. */
  private final SlotInfo<IRSequence<T>> slotInfo;

  /**
   * Cache of the Wrappers, so that we always return the same wrapper
   * for any given sequence.  Probably ought to be smart about doing some
   * garbage collecting in the future.
   */
  private final Map<IRSequence<T>,IRSequence<T>> sequenceWrappers;
  
  /** The structural lock of the model.  Only used to pass to other objects. */
  protected final Object structLock;
  
  
  
  /** Creates a new instance of MutableSequenceReturningImmutableAttribute */
  public AbstractSequenceReturningImmutableAttribute(
    final Object mutex, final SlotInfo<IRSequence<T>> si )
  throws SlotAlreadyRegisteredException
  {
    super( si.name() + "-wrapper" + wrapperCount, si.getType() );
    wrapperCount += 1;
    
    structLock = mutex;
    slotInfo = si;
    sequenceWrappers = new HashMap<IRSequence<T>,IRSequence<T>>();
  }

  @Override
  protected final IRSequence<T> getSlotValue( final IRNode node )
  {
    final IRSequence<T> seq = node.getSlotValue( slotInfo );
    if( seq != null ) {
      IRSequence<T> wrapped = sequenceWrappers.get( seq );
      if( wrapped == null ) {
        wrapped = createWrappedSequence( node, seq );
        sequenceWrappers.put( seq, wrapped );
      } 
      return wrapped;
    } else {
      return null;
    }
  }

  /**
   * This method actually creates the wrapper for a given sequence.
   * 
   * @param node The node the sequence is associated with in the SlotInfo
   * @param seq The sequence to wrap.
   * @return A wrapper around <code>seq</code> that implements some
   *    specific functionality.
   */
  protected abstract IRSequence<T> createWrappedSequence(
    IRNode node, IRSequence<T> seq );

  @Override
  protected final boolean valueExists( final IRNode node )
  {
    return node.valueExists( slotInfo );
  }

  /**
   * Returns null.  Should fix this in the future,
   * but these attributes are unlikedly to be indexed.
   */
  @Override
  public final ImmutableSet<IRNode> index( final IRSequence<T> val )
  {
    return null;
  }
}
