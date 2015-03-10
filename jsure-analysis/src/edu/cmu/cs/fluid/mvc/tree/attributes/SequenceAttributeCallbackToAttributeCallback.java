package edu.cmu.cs.fluid.mvc.tree.attributes;

import edu.cmu.cs.fluid.mvc.AttributeChangedCallback;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRSequence;
import edu.cmu.cs.fluid.ir.InsertionPoint;

/**
 * Mutable sequence attribute Callback that translates
 * the callback to an attribute changed callback on a specific attribute.
 */
public final class SequenceAttributeCallbackToAttributeCallback
implements MutableSequenceAttributeValueWrapper.Callback
{
  final String attribute;
  final AttributeChangedCallback callback;

  public SequenceAttributeCallbackToAttributeCallback(
    final String attrName, final AttributeChangedCallback cb )
  {
    attribute = attrName;
    callback = cb;
  }

  @Override
  public void setElementAt(
    final IRSequence seq, final IRNode parent, final Object elt,
    final Object oldElt )
  {
    callback.attributeChanged( attribute, parent, seq );
  }

  @Override
  public void insertElementAt(
    final IRSequence seq, final IRNode parent, final Object elt,
    final InsertionPoint ip )
  {
    callback.attributeChanged( attribute, parent, seq );
  }

  @Override
  public void removeElementAt(
    final IRSequence seq, final IRNode parent, final Object oldElt )
  {
    callback.attributeChanged( attribute, parent, seq );
  }
}
