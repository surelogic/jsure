/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/PhantomSupportingAttributeManagerFactory.java,v 1.5 2003/07/15 18:39:10 thallora Exp $ */
package edu.cmu.cs.fluid.mvc;


/**
 * <p>Say stuff here...
 *
 * @author Aaron Greenhouse
 */
public final class PhantomSupportingAttributeManagerFactory
implements AttributeInheritanceManager.Factory
{
  private final PhantomNodeIdentifier.Factory idFactory;

  public PhantomSupportingAttributeManagerFactory(
    final PhantomNodeIdentifier.Factory idf )
  { 
    this.idFactory = idf;
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
                 new PhantomSupportingInheritedAttributeBuilderFactory( 
                       model, idFactory ) );
  }
  
}


