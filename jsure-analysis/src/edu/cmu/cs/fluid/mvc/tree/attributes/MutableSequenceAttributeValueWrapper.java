/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/attributes/MutableSequenceAttributeValueWrapper.java,v 1.10 2007/07/05 18:15:15 aarong Exp $
 *
 * MutableSequenceAttributeValueWrapper.java
 * Created on March 29, 2002, 3:41 PM
 */

package edu.cmu.cs.fluid.mvc.tree.attributes;

import edu.cmu.cs.fluid.ir.*;

/**
 * IRSequence Wrapper for <em>mutable</em> IRSequences that are values 
 * of model attributes.  This wrapper insures that their mutation
 * triggers the appropriate model events in the model in which they 
 * belong.  Can also be used to insure that the sequence is immutable.
 *
 * @author Aaron Greenhouse
 */
public final class MutableSequenceAttributeValueWrapper<T>
extends AbstractSynchronizedMutableSequenceAttributeValue<T>
{
  /**
   * Create a new wrapper for a <em>mutable</em> sequence that is an attribute
   * value of a model.
   * @param mutex The model's structural lock.
   * @param node The node for which the sequence is an attribute value.
   * @param seq The sequence to wrap.
   * @param mutable Whether the wrapper should enforce immutability.
   * @param cb The callback to use when the sequence is changed.
   */
  public MutableSequenceAttributeValueWrapper(
    final Object mutex, final IRNode node, final IRSequence<T> seq,
    final boolean mutable, final Callback cb )
  {
    super( mutex, node, seq, mutable, cb );
  }

  @Override
  protected boolean validAtImpl(IRLocation loc) {
    return sequence.validAt(loc);
  }

  @Override
  protected T elementAtImpl(IRLocation loc) {
    return sequence.elementAt(loc);
  }

  
  
}
