// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/LocalAttributeManagerFactory.java,v 1.5 2003/07/15 18:39:10 thallora Exp $
package edu.cmu.cs.fluid.mvc;


/**
 * A attribute manager factory that creates an attribute manager suitable
 * for handling local attributes of mutable and immutable modes only, without
 * any support for inheriting attributes.  Such an attribute manager is 
 * appropriate for use with "pure" models.
 *
 * <p>This class uses the "Singleton" pattern, and thus has a private
 * constructor.  The (lone) instance of this class can be accessed through
 * the {@link #prototype} field.
 *
 * @author Aaron Greenhouse
 */
public final class LocalAttributeManagerFactory
implements AttributeManager.Factory
{
  /**
   * This class uses the "Singleton" pattern, and thus has a private
   * constructor.  The (lone) instance of this class can be accessed through
   * the {@link #prototype} field.
   */
  private LocalAttributeManagerFactory()
  {
  }
  
  // inherit the javadoc 
  @Override
  public AttributeManager create( final Model model, final Object structLock )
  {
    return new BareAttributeManager( model, structLock );
  }
  
  /** The prototype instance of this factory */
  public static final AttributeManager.Factory prototype =
    new LocalAttributeManagerFactory();
}

