/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/BasicAttributeInheritanceManagerFactory.java,v 1.5 2003/07/15 18:39:10 thallora Exp $ */
package edu.cmu.cs.fluid.mvc;


/**
 * Factory that produces attribute inheritance managers that support the basic
 * attribute inheritance modes:
 * {@link AttributeInheritanceManager#MUTABLE_LOCAL},
 * {@link AttributeInheritanceManager#MUTABLE_SOURCE}, and
 * {@link AttributeInheritanceManager#IMMUTABLE}.  
 * 
 * <p>The class uses a singleton pattern, and thus has a private
 * constructor.  The one (and only) instance of the class is referred to 
 * by the field {@link #prototype}. 
 *
 * @author Aaron Greenhouse
 */
public final class BasicAttributeInheritanceManagerFactory
implements AttributeInheritanceManager.Factory
{
  /** Reference to the one and only instance of the class. */
  public static final AttributeInheritanceManager.Factory prototype =
    new BasicAttributeInheritanceManagerFactory();

  
  
  /**
   * Use the prototype: {@link #prototype}.
   */
  private BasicAttributeInheritanceManagerFactory()
  {
  }
  
  
  
  /**
   * Create a new attribute inheritance manager for a particular model.
   * @param model The model whose attribute inheritance is to be managed
   *             by the new instance.
   * @param mutex The lock used to protect the state of the model.
   * @param attrManager The attribute manager of the model.
   */
  @Override
  public AttributeInheritanceManager create(
    final Model model, final Object mutex, final AttributeManager attrManager )
  {
    return new BareAttributeInheritanceManager(
                 model, mutex, attrManager,
                 BasicInheritedAttributeBuilderFactory.prototype );
  }
}
 