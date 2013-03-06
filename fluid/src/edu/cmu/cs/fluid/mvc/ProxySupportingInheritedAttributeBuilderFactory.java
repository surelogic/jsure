/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/ProxySupportingInheritedAttributeBuilderFactory.java,v 1.12 2006/03/30 19:47:20 chance Exp $ */
package edu.cmu.cs.fluid.mvc;

import java.util.Map;

import edu.cmu.cs.fluid.mvc.attributes.GuardedImmutableNodeAttribute;
import edu.cmu.cs.fluid.mvc.attributes.GuardedNodeAttribute;
import edu.cmu.cs.fluid.mvc.attributes.MutableLocalInheritedNodeAttribute;
import edu.cmu.cs.fluid.mvc.attributes.ProxySupportingNodeAttribute;
import edu.cmu.cs.fluid.ir.SimpleSlotFactory;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotInfo;

/**
 * Attribute builder factory that returns an attribute builder that
 * creates proxy-node-supporting attributes.
 * Behavior is the same as the attribute builder returned by 
 * {@link BasicInheritedAttributeBuilderFactory}
 * for model attributes.  For node attributes, the inheritance modes
 * {@link ProxyNodeSupport#MUTABLE_LOCAL_PROXY},
 * {@link ProxyNodeSupport#MUTABLE_SOURCE_PROXY}, and
 * {@link ProxyNodeSupport#IMMUTABLE_PROXY} are also supported.  For these
 * modes special SlotInfos are created <em>before</em> the normal attribute
 * wrapping process that support the storage of values for proxy-nodes.  The
 * proxy node attribute values are actually stored in a SlotInfo distinct
 * from the one used to store regular attributes.  These SlotInfos are not
 * accessible through normal means: instead, they are stored in a Map shared
 * among the builder and the attribute manager (and the model itself) that
 * maps from attribute name to the SlotInfo that stores attribute values.
 * This indirect way of doing things is to prevent clients of the model from
 * being able to change the attribute values of proxy nodes.
 *
 * @author Aaron Greenhouse
 */
public final class ProxySupportingInheritedAttributeBuilderFactory
implements BareAttributeInheritanceManager.InheritedAttributeBuilderFactory
{
  /**
   * Hashtable shared with the AttributeManager that maps attribute names
   * to the SlotInfo used to store proxy values for the attribute.
   */
  private final Map<String,SlotInfo> proxyAttributes;

  /**
   * Create a new attribute builder factory.
   * @param proxyMap The map in which the attribute&ndash;proxy-value-storage
   * associatations will be stored by the generated builder.
   */
  public ProxySupportingInheritedAttributeBuilderFactory( final Map<String,SlotInfo> proxyMap )
  {
    this.proxyAttributes = proxyMap;
  }
  
  // inherit javadoc.
  @Override
  public BareAttributeInheritanceManager.InheritedAttributeBuilder create()
  {
    return new ProxySupportingInheritedAttributeBuilder( proxyAttributes );
  }
}



/**
 * Attribute builder that creates proxy-node-supporting attributes.
 * For node attributes, the inheritance modes
 * {@link ProxyNodeSupport#MUTABLE_LOCAL_PROXY},
 * {@link ProxyNodeSupport#MUTABLE_SOURCE_PROXY}, and
 * {@link ProxyNodeSupport#IMMUTABLE_PROXY} are also supported.  For these
 * modes special SlotInfos are created <em>before</em> the normal attribute
 * wrapping process that support the storage of values for proxy-nodes.  The
 * proxy node attribute values are actually stored in a SlotInfo distinct
 * from the one used to store regular attributes.  These SlotInfos are not
 * accessible through normal means: instead, they are stored in a Map shared
 * among the builder and the attribute manager (and the model itself) that
 * maps from attribute name to the SlotInfo that stores attribute values.
 * This indirect way of doing things is to prevent clients of the model from
 * being able to change the attribute values of proxy nodes.
 *
 * @author Aaron Greenhouse
 */
final class ProxySupportingInheritedAttributeBuilder
implements BareAttributeInheritanceManager.InheritedAttributeBuilder
{
  /**
   * Use "delegate inheritance" to exploit the functionality of
   * {@link BasicInheritedAttributeBuilder}.
   */
  private final BareAttributeInheritanceManager.InheritedAttributeBuilder
    basicBuilder;
  
  /**
   * Hashtable shared with the AttributeManager that maps attribute names
   * to the SlotInfo used to store proxy values for the attribute.
   */
  private final Map<String,SlotInfo> proxyAttributes;
  
  
  
  /**
   * Create a new attribute builder.
   * @param proxyMap The map in which the attribute&ndash;proxy-value-storage
   * associatations are stored.
   */
  public ProxySupportingInheritedAttributeBuilder( final Map<String,SlotInfo> proxyMap )
  {
    this.proxyAttributes = proxyMap;
    this.basicBuilder =
      BasicInheritedAttributeBuilderFactory.prototype.create(); 
  }
    
  /**
   * Generates an attribute wrapped by one of 
   * {@link edu.cmu.cs.fluid.mvc.attributes.MutableLocalInheritedModelAttribute},
   * {@link edu.cmu.cs.fluid.mvc.attributes.GuardedMutableModelAttribute}, or
   * {@link edu.cmu.cs.fluid.mvc.attributes.GuardedImmutableModelAttribute}.
   */ 
  @Override
  public <T> ComponentSlot<T> buildCompAttribute(
    final Model partOf, final Object mutex, final String attr,
    final Object mode, final ComponentSlot<T> ca,
    final AttributeChangedCallback cb )
  {
    return basicBuilder.buildCompAttribute( partOf, mutex, attr, mode, ca, cb );
  }
  
  /**
   * Generates an attribute wrapped by one of 
   * {@link edu.cmu.cs.fluid.mvc.attributes.MutableLocalInheritedNodeAttribute},
   * {@link edu.cmu.cs.fluid.mvc.attributes.GuardedNodeAttribute}, or
   * {@link edu.cmu.cs.fluid.mvc.attributes.GuardedImmutableNodeAttribute},
   * but that supports proxy nodes.
   */ 
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
      SlotInfo<T> wrapped = null;
      
      // new stuff
      try {
        if( mode == ProxyNodeSupport.MUTABLE_LOCAL_PROXY ) {
          wrapped = new MutableLocalInheritedNodeAttribute<T>(
                          partOf, mutex, attr,
                          createProxyStorage( partOf, attr, si ),
                          SimpleSlotFactory.prototype, cb );
        } else if( mode == ProxyNodeSupport.MUTABLE_SOURCE_PROXY ) {
          wrapped = new GuardedNodeAttribute<T>(
                          partOf, mutex,
                          createProxyStorage( partOf, attr, si ),
                          attr, cb );
        } else if( mode == ProxyNodeSupport.IMMUTABLE_PROXY ) {
          wrapped = new GuardedImmutableNodeAttribute<T>(
                          partOf, mutex,
                          createProxyStorage( partOf, attr, si ),
                          attr );
        } else {
          throw new IllegalArgumentException( "Unknown node inheritance mode." );
        }
      } catch( final SlotAlreadyRegisteredException e ) {
         // Cannot reasonably recover from this
         throw new RuntimeException( "Got SlotAlreadyRegisteredException" );
      }
    
      return wrapped;
    }
  }
  
  /**
   * Create storage for a proxy values associated with the given attribute,
   * create the wrapped the uses the new proxy storage and the provided 
   * inherited attribute storage, and adds the proxy storage to the hash table.
   * @return The SlotInfo that uses both the new proxy storage the provided
   * inherited storage.
   */
  private <T> SlotInfo<T> createProxyStorage(
    final Model partOf, final String attr, final SlotInfo<T> si )
  throws SlotAlreadyRegisteredException
  {
    // Create the new storage
    final SlotInfo<T> nsi = SimpleSlotFactory.prototype.newAttribute(
                           attr + "-" + partOf.hashCode() + "-proxy",
                           si.getType() );
    // Add the proxy storage to the map
    proxyAttributes.put( attr, nsi );
    
    // Create and return the proxy storing attribute
    final SlotInfo<T> proxyWrapped =
      new ProxySupportingNodeAttribute<T>( si, nsi, partOf );
    return proxyWrapped;
  }
  
  /**
   * Considers the modes {@link AttributeInheritanceManager#MUTABLE_LOCAL},
   * {@link AttributeInheritanceManager#MUTABLE_SOURCE},
   * {@link ProxyNodeSupport#MUTABLE_LOCAL_PROXY}, and
   * {@link ProxyNodeSupport#MUTABLE_SOURCE_PROXY}
   * to be mutable.
   */
  @Override
  public boolean isModeMutable( final Object mode )
  {
    return    (mode == AttributeInheritanceManager.MUTABLE_LOCAL)
           || (mode == AttributeInheritanceManager.MUTABLE_SOURCE)
           || (mode == ProxyNodeSupport.MUTABLE_LOCAL_PROXY)
           || (mode == ProxyNodeSupport.MUTABLE_SOURCE_PROXY);
  }
}
