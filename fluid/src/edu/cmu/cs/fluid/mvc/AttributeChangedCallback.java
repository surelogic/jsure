/* $Header*/
package edu.cmu.cs.fluid.mvc;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Interface for callbacks issued when an attribute has been modified.
 * These callbacks are used internally within the implementation of 
 * a model and are the mechanism by which a model would cause a 
 * ModelEvent to be sent due to breakage caused by a change to an
 * attribute.
 *
 * @author Aaron Greenhouse
 */
public interface AttributeChangedCallback
{
  /** 
   * Prototypical reference to an attribute changed callback that does
   * nothing.  This callback is used to terminate callback chains. 
   */
  public static final AttributeChangedCallback nullCallback =
    new NullCallback();

  /**
   * An attribute's value has changed.
   * @param attr The name of the attribute; interned String.
   * @param node If the attribute is a node-level attribute, then this
   * is the node whose attribute value changed.  If the attribute is
   * a model-level attribute then this is <code>null</code>.
   * @param value The new value of the attribute.
   */
  public void attributeChanged( String attr, IRNode node, Object value );
}



/**
 * An attribute changed callback that does nothing.
 * Used to implement a prototype pattern; the prototypical instance
 * is {@link AttributeChangedCallback#nullCallback}.
 */
final class NullCallback
implements AttributeChangedCallback
{
  /**
   * Constructer.  
   */
  protected NullCallback()
  {
  }

  /**
   * Handle an attribute changed event by doing nothing.
   */
  @Override
  public void attributeChanged( final String attr, final IRNode node,
                                final Object value )
  {
  }
}
