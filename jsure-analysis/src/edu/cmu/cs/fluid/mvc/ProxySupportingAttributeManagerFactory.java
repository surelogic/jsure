/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/ProxySupportingAttributeManagerFactory.java,v 1.7 2007/07/05 18:15:16 aarong Exp $ */
package edu.cmu.cs.fluid.mvc;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.cs.fluid.ir.SlotInfo;

/**
 * Attribute Manager factory that produces attribute managers whose attributes
 * support proxy nodes.  A proxy node is a node that is not part of the normal
 * structure of the model.  It exists solely to give additional attribute
 * values to another node that it is associated with.  They are currently used
 * by configurable views to represent attribute values of ellided nodes.
 * An ellipsis node has an associated proxy node.  The purpose of this is to not
 * confuse the attribute values of the ellipsis node itself with those of the
 * nodes it is representing.
 *
 * <p>The returned attribute manager supports the property
 * {@link ProxyNodeSupport#PROXY_MAP} that has a {@link Map} value.  It
 * associates attribute names with the slot infos that are used to store the
 * proxy node attribute values.  This is needed because it is not possible
 * to set a proxy node attribute value using the attribute that is directly 
 * accesible from the attribute manager.
 *
 * @author Aaron Greenhouse
 */
public final class ProxySupportingAttributeManagerFactory
implements AttributeInheritanceManager.Factory
{
  /**
   * Singleton instance of the factory class.
   */
  public static final AttributeInheritanceManager.Factory prototype = 
    new ProxySupportingAttributeManagerFactory();
  
  /**
   * Private constructor to prevent the creation of factory objects.
   * Use {@link #prototype} instead.
   */
  private ProxySupportingAttributeManagerFactory()
  { 
  }

  @Override
  public AttributeInheritanceManager create(
    final Model model, final Object mutex, final AttributeManager attrManager )
  {
    return new ProxySupportingAttributeManager( model, mutex, attrManager );
  }
}



/**
 * Attribute Manager whose attributes
 * support proxy nodes.  A proxy node is a node that is not part of the normal
 * structure of the model.  It exists solely to give additional attribute
 * values to another node that it is associated with.  They are currently used
 * by configurable views to represent attribute values of ellided nodes.
 * An ellipsis node has an associated proxy node.  The purpose of this is to not
 * confuse the attribute values of the ellipsis node itself with those of the
 * nodes it is representing.
 *
 * <p>The attribute manager supports the property
 * {@link ProxyNodeSupport#PROXY_MAP} that has a {@link Map} value.  It
 * associates attribute names with the slot infos that are used to store the
 * proxy node attribute values.  This is needed because it is not possible
 * to set a proxy node attribute value using the attribute that is directly 
 * accesible from the attribute manager.
 *
 * @author Aaron Greenhouse
 */
final class ProxySupportingAttributeManager
extends BareAttributeInheritanceManager
{
  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin fields
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //===========================================================
  //== Fields for storing property values
  //===========================================================

  /**
   * The map from attribute name to the SlotInfo storing the proxy node
   * values for the attribute.
   */
  private final Map proxyAttributes;

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End fields
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  
  
  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Constructors
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  public ProxySupportingAttributeManager(
    final Model partOf, final Object mutex, final AttributeManager attrManager )
  {
    this( partOf, mutex, attrManager, new HashMap<String,SlotInfo>() );
  }

  // Hack so that the map can be shared by the attribute builder and the
  // implementation itself.
  private ProxySupportingAttributeManager(
    final Model partOf, final Object mutex, 
    final AttributeManager attrManager, final Map<String,SlotInfo> proxyMap )
  {
    super( partOf, mutex, attrManager,
      new ProxySupportingInheritedAttributeBuilderFactory( proxyMap ) );
    proxyAttributes = proxyMap;
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End Constructors
  //-----------------------------------------------------------
  //-----------------------------------------------------------



  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Property methods
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  /**
   * Implemenation understand the {@link ProxyNodeSupport#PROXY_MAP} property.
   */
  @Override
  public final Object getProperty( final String property )
  {
    if( property == ProxyNodeSupport.PROXY_MAP ) {
      return proxyAttributes;
    } else return super.getProperty( property );
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End Property methods
  //-----------------------------------------------------------
  //-----------------------------------------------------------
}


