/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/attributes/MaskingSequenceAttributeValueWrapper.java,v 1.5 2007/07/05 18:15:15 aarong Exp $
 */

package edu.cmu.cs.fluid.mvc.tree.attributes;

import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.ir.*;

/**
 * IRSequence Wrapper for <em>mutable</em> IRSequences that are values 
 * of model attributes but that can mask out elements of the sequence.  This 
 * is typically used for removing nodes that have been removed from the model
 * but that are still present in the underlying representation.  This wrapper
 * insures that mutation
 * triggers the appropriate model events in the model in which they 
 * belong.  Can also be used to insure that the sequence is immutable.
 *
 * @author Aaron Greenhouse
 */
public final class MaskingSequenceAttributeValueWrapper
extends AbstractSynchronizedMutableSequenceAttributeValue<IRNode>
{
  /** The model this sequence comes from. */
  private final Model model;

  /**
   * Create a new wrapper for a <em>mutable</em> sequence that is an attribute
   * value of a model.
   * @param mutex The model's structural lock.
   * @param node The node for which the sequence is an attribute value.
   * @param seq The sequence to wrap.
   * @param mutable Whether the wrapper should enforce immutability.
   * @param cb The callback to use when the sequence is changed.
   */
  public MaskingSequenceAttributeValueWrapper(
    final Object mutex, final IRNode node, final IRSequence<IRNode> seq,
    final Model mod, final boolean mutable, final Callback cb )
  {
    super( mutex, node, seq, mutable, cb );
    model = mod;
  }

  @Override
  protected boolean validAtImpl(final IRLocation loc) {
    /* peek at the element in the underlying sequence.  If it is
     * not a part of the model then the location is not valid.
     */
    if( sequence.validAt(loc) ) {
      final IRNode elt = sequence.elementAt(loc);
      return model.isPresent( elt );
    } else {
      return false;
    }
  }

  @Override
  protected IRNode elementAtImpl(final IRLocation loc) {
    /* peek at the element in the underlying sequence.  If it is not
     * part of the model then throw a SlotUndefinedException.  Peeking
     * may cause an SlotUndefinedException if the underlying element
     * is not defined.  This is okay, and we propogate the exception.
     */
    final IRNode elt = sequence.elementAt(loc);
    if( model.isPresent( elt ) ) {
      return elt;
    } else {
      throw new SlotUndefinedException("Element masked out of sequence");
    }
  }
}
