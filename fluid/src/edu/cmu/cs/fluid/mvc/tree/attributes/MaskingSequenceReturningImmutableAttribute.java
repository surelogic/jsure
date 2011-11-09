/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/attributes/MaskingSequenceReturningImmutableAttribute.java,v 1.4 2007/07/05 18:15:15 aarong Exp $
 *
 * MutableSequenceReturningImmutableAttribute.java
 * Created on March 29, 2002, 3:31 PM
 */

package edu.cmu.cs.fluid.mvc.tree.attributes;

import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRSequence;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotInfo;

/**
 * SlotInfo wrapper for handling sequence-valued immutable attributes for
 * which the sequence values are mutable.  Makes sure that mutations to the
 * sequence propogate through to the model and cause breakages to occur.
 * Doesn't worry about checking for IRNode presentness/attributability
 * because it will be wrapped yet again by something that does this check.
 * Same goes for synchronization on the model's structural lock.  Can also
 * be used to insure that the returned sequence values are immutable.
 *
 * @author Aaron Greenhouse
 */
public final class MaskingSequenceReturningImmutableAttribute
extends AbstractSequenceReturningImmutableAttribute<IRNode>
{
  /** Callback use to trap changes to the sequences. */
  private MutableSequenceAttributeValueWrapper.Callback callback;

  /** Whether the sequences should be immutable or not */
  private final boolean isMutable;
  
  /** The model used to mask out elements of the returned sequence */
  private final Model model;
  
  
  
  /** Creates a new instance of MutableSequenceReturningImmutableAttribute */
  public MaskingSequenceReturningImmutableAttribute(
    final Model mod, final Object mutex, final SlotInfo<IRSequence<IRNode>> si,
    final boolean mutable,
    final MutableSequenceAttributeValueWrapper.Callback cb )
  throws SlotAlreadyRegisteredException
  {
    super( mutex, si );    
    isMutable = mutable;
    callback = cb;
    model = mod;
  }

  @Override
  protected IRSequence<IRNode> createWrappedSequence(
    final IRNode node, final IRSequence<IRNode> seq )
  {
    return new MaskingSequenceAttributeValueWrapper(
                    structLock, node, seq, model, isMutable, callback );
  }
}
