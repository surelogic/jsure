/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/PhantomSupportingInheritedAttributeBuilderFactory.java,v 1.7 2006/03/30 19:47:20 chance Exp $ */
package edu.cmu.cs.fluid.mvc;

import edu.cmu.cs.fluid.mvc.attributes.PhantomSupportingNodeAttribute;
import edu.cmu.cs.fluid.ir.SlotInfo;

public final class PhantomSupportingInheritedAttributeBuilderFactory
implements BareAttributeInheritanceManager.InheritedAttributeBuilderFactory
{
  private final Model partOf;
  private final PhantomNodeIdentifier.Factory idFactory;
  
  public PhantomSupportingInheritedAttributeBuilderFactory( 
    final Model partOf, final PhantomNodeIdentifier.Factory idFactory )
  {
    this.partOf = partOf;
    this.idFactory = idFactory;
  }
  
  // inherit javadoc.
  @Override
  public BareAttributeInheritanceManager.InheritedAttributeBuilder create()
  {
    return new PhantomSupportingInheritedAttributeBuilder( partOf, idFactory );
  }
}



/**
 * Attribute builder that creates phantom-node-supporting attributes.
 * For node attributes, the inheritance mode
 * {@link PhantomNodeSupport#IMMUTABLE_PHANTOM} is also supported.  For this
 * mode special SlotInfos are created
 * that handle the mapping of phantom nodes.
 *
 * <p>Say more here...
 *
 * @author Aaron Greenhouse
 */
final class PhantomSupportingInheritedAttributeBuilder
implements BareAttributeInheritanceManager.InheritedAttributeBuilder
{
  /**
   * Use "delegate inheritance" to exploit the functionality of
   * {@link BasicInheritedAttributeBuilder}.
   */
  private final BareAttributeInheritanceManager.InheritedAttributeBuilder
    basicBuilder;
  
  /** Identifier of phantom nodes to use for the attributes. */
  private final PhantomNodeIdentifier identifier;
  
  
  
  
  public PhantomSupportingInheritedAttributeBuilder(
    final Model partOf, final PhantomNodeIdentifier.Factory idFactory )
  {
    identifier = idFactory.create( partOf );
    basicBuilder = BasicInheritedAttributeBuilderFactory.prototype.create();
  }
    
  
  
  @Override
  public <T> ComponentSlot<T> buildCompAttribute(
    final Model partOf, final Object mutex, final String attr,
    final Object mode, final ComponentSlot<T> ca,
    final AttributeChangedCallback cb ) 
  {
    return basicBuilder.buildCompAttribute( partOf, mutex, attr, mode, ca, cb );
  }

 
  
  
  @Override
  public <T> SlotInfo<T> buildNodeAttribute(
    final Model partOf, final Object mutex, final String attr,
    final Object mode, final SlotInfo<T> si, final AttributeChangedCallback cb )
  {
    if(    (mode == AttributeInheritanceManager.MUTABLE_LOCAL)
        || (mode == AttributeInheritanceManager.MUTABLE_SOURCE)
        || (mode == AttributeInheritanceManager.IMMUTABLE) ) {
      return basicBuilder.buildNodeAttribute( partOf, mutex, attr, mode, si, cb );
    } else {
      if( mode == PhantomNodeSupport.IMMUTABLE_PHANTOM ) {
        return new PhantomSupportingNodeAttribute<T>(
                     partOf, mutex, si, attr, identifier );
      } else {
        throw new IllegalArgumentException( "Unknown node inheritance mode." );
      }
    }
  }
  
  /**
   * Considers the modes {@link AttributeInheritanceManager#MUTABLE_LOCAL} and
   * {@link AttributeInheritanceManager#MUTABLE_SOURCE}
   * to be mutable.
   */
  @Override
  public boolean isModeMutable( final Object mode )
  {
    return    (mode == AttributeInheritanceManager.MUTABLE_LOCAL)
           || (mode == AttributeInheritanceManager.MUTABLE_SOURCE);
  }
}
