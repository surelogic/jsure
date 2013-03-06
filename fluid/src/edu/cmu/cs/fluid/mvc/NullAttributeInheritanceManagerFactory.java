/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/NullAttributeInheritanceManagerFactory.java,v 1.4 2003/07/15 18:39:10 thallora Exp $
 *
 * NullAttributeInheritanceManagerFactory.java
 * Created on March 6, 2002, 1:46 PM
 */

package edu.cmu.cs.fluid.mvc;

/**
 * Factory that always returns AttributeInheritanceManagers that never 
 * actually allow an attribute to be inherited.  That is, the methods,
 * {@link AttributeInheritanceManager#inheritNodeAttribute} and
 * {@link AttributeInheritanceManager#inheritCompAttribute} always
 * return <code>false</code>.
 * 
 * <p>The class uses a singleton pattern, and thus has a private
 * constructor.  The one (and only) instance of the class is referred to 
 * by the field {@link #prototype}.
 *
 * @author Aaron Greenhouse
 */
public final class NullAttributeInheritanceManagerFactory
implements AttributeInheritanceManager.Factory
{
  /** Prototype instance of factory. */
  public final static AttributeInheritanceManager.Factory prototype =
    new NullAttributeInheritanceManagerFactory();
  
  /** Prototype instance of the null attribute inheritance manager. */
  private final static AttributeInheritanceManager nullPrototype =
    new NullAttributeInheritanceManager();
  
  /**
   * Use the prototype instance {@link #prototype}.
   */
  private NullAttributeInheritanceManagerFactory()
  {
  }
  
  // inherit javadoc
  @Override
  public AttributeInheritanceManager create(
    final Model model, final Object mutex, final AttributeManager attrManager )
  {
    return nullPrototype;
  }  
}



/**
 * AttributeInheritanceManagers that never 
 * actually allow an attribute to be inherited.  That is, the methods,
 * {@link AttributeInheritanceManager#inheritNodeAttribute} and
 * {@link AttributeInheritanceManager#inheritCompAttribute} always
 * return <code>false</code>.
 *
 * @author Aaron Greenhouse
 */
final class NullAttributeInheritanceManager
implements AttributeInheritanceManager
{
  protected NullAttributeInheritanceManager()
  {
  }
  
  /**
   * Get the value of a property.  This default implementation doesn't 
   * understand any properties and always throws an exception.
   */
  @Override
  public Object getProperty( final String property )
  throws IllegalArgumentException
  {
    throw new IllegalArgumentException(
        "Attribute managers of class " + this.getClass().getName()
      + " do not understand the property \"" + property + "\"." );
  }
  
  /**
   * Set the value of a property.  This default implementation doesn't 
   * understand any properties and always throws an exception.
   */
  @Override
  public void setProperty( final String property, final Object value )
  throws IllegalArgumentException
  {
    throw new IllegalArgumentException(
        "Attribute managers of class " + this.getClass().getName()
      + " do not understand the property \"" + property + "\"." );
  }
  
  /**
   * This implementation does nothing.
   */
  @Override
  public void inheritAttributesFromModel(
    final Model srcModel, final AttributeInheritancePolicy policy,
    final AttributeChangedCallback cb )
  {
    // noop
  }
  
  /**
   * Always refuses to inherit the attribute; always returns <code>false</code>.
   */
  @Override
  public boolean inheritCompAttribute(
    final Model model, final String srcAttr, final String attr,
    final Object mode, final int kind, final AttributeChangedCallback cb )
  {
    return false;
  }

  /**
   * Always refuses to inherit the attribute; always returns <code>false</code>.
   */
  @Override
  public boolean inheritNodeAttribute(
    final Model model, final String srcAttr, final String attr,
    final Object mode, final int kind, final AttributeChangedCallback cb )
  {
    return false;
  }
}
