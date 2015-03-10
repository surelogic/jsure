/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/ProxyNodeSupport.java,v 1.5 2003/07/15 18:39:10 thallora Exp $
 *
 * ProxySupport.java
 *
 * Created on January 17, 2002, 10:32 AM
 */

package edu.cmu.cs.fluid.mvc;

/**
 * Constants for supporting ProxyNodes in attribute inheritance policies.
 *
 * @author Aaron Greenhouse
 */
public interface ProxyNodeSupport
{
  /**
   * Mode constant for AttributeInheritancePolicy indicating
   * that the attribute should be inherited as locally mutable
   * and support the use of proxy nodes, the values of which
   * are always immutable.  This mode is only applicable to node attributes.
   */
  public static final Object MUTABLE_LOCAL_PROXY = new String("mutableLocalProxy");

  /**
   * Mode constant for AttributeInheritancePolicy indicating
   * that the attribute should be inherited as source mutable
   * and support the use of proxy nodes, the values of which
   * are always immutable.  This mode is only applicable to node attributes.
   */
  public static final Object MUTABLE_SOURCE_PROXY = new String("mutableSourceProxy");

  /**
   * Mode constant for AttributeInheritancePolicy indicating
   * that the attribute should be inherited as immutable
   * and support the use of proxy nodes (which are also immutable).
   * This mode is only applicable to node attributes.
   */
  public static final Object IMMUTABLE_PROXY = new String("immutableProxy");
  
  
  
  /**
   * Attribute Manager property name for getting the attribute to
   * proxy value storage map from the attribute manager.
   */
  public static final String PROXY_MAP = "ProxyNodeSupport.PROXY_MAP";
}

