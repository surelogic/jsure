/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attributes/GuardedImmutableNodeAttribute.java,v 1.8 2007/07/05 18:15:17 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.attributes;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.*;

/**
 * Wrapper for SlotInfos intended for use by AttributeManagers that does
 * the following:
 * <li>Enforces immutability of attributes.
 * <li>Rejects gets for nodes that are not in the model.
 * <li>Protects the data in the attribute using the Model's structLock
 * </ul>
 *
 * <p><em>Membership in the model need only hold at the start
 * of execution of any of the <code>valueExists</code>,
 * <code>getSlotValue</code> and </code>setSlotValue</code>.</em>
 * This is okay because if membership changes from member to non-,
 * then any subsequent attempts to read or set the value will fail,
 * so it doesn't matter that the value was allowed to change.  If
 * membership changes from non- to member, then caller should
 * have been better about protecting the seqeunce of actions at the
 * call site.
 *
 * @author Aaron Greenhouse
 */
public final class GuardedImmutableNodeAttribute<T>
extends GuardedNodeAttribute<T>
{
  /** name must be interned. */
  public GuardedImmutableNodeAttribute(
    final Model partOf, final Object mutex,
    final SlotInfo<T> attribute, final String name )
  {
    super( partOf, mutex, attribute, name, AttributeChangedCallback.nullCallback );
  }
 
  @Override
  protected boolean setSlotValueImpl( final IRNode node, final T newValue )
  throws SlotImmutableException
  {
    throw new SlotImmutableException(
        "Attribute \"" + attrName + "\" in model \""
      + partOf.getName() + "\" is Immutable." );
  }
}

