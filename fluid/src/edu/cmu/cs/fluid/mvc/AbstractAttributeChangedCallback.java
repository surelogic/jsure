/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/AbstractAttributeChangedCallback.java,v 1.6 2004/03/07 18:42:43 thallora Exp $ */
package edu.cmu.cs.fluid.mvc;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * An abstract implementation of the attributed changed callback that
 * automatically handles the chaining of callbacks.  Chaining is caused
 * by the construction of model&ndash;view chains as an attribute is
 * inheritited by stateful views.
 *
 * <p>Implementations should implemented the method
 * {@link #attributeChangedImpl}, which is invoked by the 
 * implementation of {@link #attributeChanged} provided by this class.
 *
 * @author Aaron Greenhouse
 */
public abstract class AbstractAttributeChangedCallback
implements AttributeChangedCallback
{
  /** 
   * The next callback in the chain.  A list is terminated by a 
   * reference to the null callback: {@link AttributeChangedCallback}.
   */
  private final AttributeChangedCallback next;

  /**
   * Initialize the class.
   * @param n The next callback in the chain; if there is no next callback,
   * a reference to {@link AttributeChangedCallback#nullCallback} should be 
   * provided.
   */
  public AbstractAttributeChangedCallback( final AttributeChangedCallback n )
  {
    next = n;
  }

  /**
   * Initialize the class using the null callback.
   */
  public AbstractAttributeChangedCallback()
  {
    this( AttributeChangedCallback.nullCallback );
  }

  /**
   * An attribute's value has changed.  The implementation provided by
   * this class invokes <code>attributeChangedImpl()</code> and then 
   * invokes the next callback in the chain.
   * 
   * @param attr The name of the attribute; interned String.
   * @param node If the attribute is a node-level attribute, then this
   * is the node whose attribute value changed.  If the attribute is
   * a model-level attribute then this is <code>null</code>.
   * @param value The new value of the attribute.
   */
  @Override
  public final void attributeChanged(
    final String attr, final IRNode node, final Object value )
  {
    attributeChangedImpl( attr, node, value );
    next.attributeChanged( attr, node, value );
  }

  /**
   * An attribute's value has changed.  This method is called by
   * {@link #attributeChanged} to deal with the change.
   * @param attr The name of the attribute; interned String.
   * @param node If the attribute is a node-level attribute, then this
   * is the node whose attribute value changed.  If the attribute is
   * a model-level attribute then this is <code>null</code>.
   * @param value The new value of the attribute.
   */
  protected abstract void attributeChangedImpl(
    String attr, IRNode node, Object value );
 }

