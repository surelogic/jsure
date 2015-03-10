/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attributes/GuardedNodeAttribute.java,v 1.9 2007/07/05 18:15:17 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.attributes;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.*;

/**
 * Wrapper for SlotInfos intended for use by AttributeManagers that does
 * the following:
 * <ul>
 * <li>Rejects gets/sets for nodes that are not in the model.
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
 * @author Aaron Greenhosue
 */
public class GuardedNodeAttribute<T>
extends SlotInfoWrapper<T>
{
  /** The name of the attribute. */
  protected final String attrName;
  
  /** The model the attribute is a part of. */
  protected final Model partOf;
  
  /** The lock to use to protect the attributes values. */
  protected final Object mutex;
  
  /** The callback to use when the values change. */
  protected final AttributeChangedCallback callback;
  
  /** name must be interned. */
  public GuardedNodeAttribute( 
    final Model partOf, final Object mutex, 
    final SlotInfo<T> attribute, final String name,
    final AttributeChangedCallback cb )
  {
    super( attribute );
    this.partOf = partOf;
    this.mutex = mutex;
    this.attrName = name;
    this.callback = cb;
  }
  
  /**
   * Overriden to use the mutex and to check that the node&ndash;attribute
   * pair is attributable in the given model.
   */
  @Override
  protected final boolean valueExists( final IRNode node )
  {
    synchronized( mutex ) {
      final boolean present = partOf.isAttributable( node, attrName );
      return (present && super.valueExists( node ));
    }
  }
    
  /**
   * Overriden to use the mutex and to check the node&ndash;attribute
   * pair is attributable in the given model; also delegates to
   * the method {@link #setSlotValueImpl}.
   */
  @Override
  protected final void setSlotValue( final IRNode node, final T newValue )
  throws SlotImmutableException
  {
    boolean changed = false;
    synchronized( mutex ) {
      if( partOf.isAttributable( node, attrName ) ) {
        changed = setSlotValueImpl( node, newValue );
      } else {
        throw new SlotImmutableException(
            "Node " + node + " is not in model \""
           + partOf.getName() + "\"." );
      }
    }
    if( changed ) callback.attributeChanged( attrName, node, newValue );
  }

  /**
   * Delegate for {@link #setSlotValue} that actually implements the 
   * setting of the value.  The implementor is guaranteed that the 
   * mutex is held and that <tt>node</tt> is an attributable node of the
   * model.
   *
   * <p>This implementation of the method invokes the superclass's
   * setSlotValue method.
   *
   * @return <code>true</code> if the value was updated.
   */
  protected boolean setSlotValueImpl( final IRNode node, final T newValue )
  {
    super.setSlotValue( node, newValue );
    return true;
  }
  
  /**
   * Overriden to use the mutex and to make sure the node&ndash;attribute
   * combination is attributable in the assocaited model.
   */
  @Override
  protected final T getSlotValue( final IRNode node )
  throws SlotUndefinedException
  {
    synchronized( mutex ) {
      if( partOf.isAttributable( node, attrName ) ) {
        return super.getSlotValue( node );
      } else {
        throw new SlotUndefinedException(
            "Node " + node + " is not in model \""
          + partOf.getName() + "(" +partOf+ ")"+ "\"." );
      }
    }
  }
}
