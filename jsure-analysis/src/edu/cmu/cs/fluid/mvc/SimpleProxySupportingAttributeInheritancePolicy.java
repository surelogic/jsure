/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/SimpleProxySupportingAttributeInheritancePolicy.java,v 1.8 2007/07/05 18:15:16 aarong Exp $
 *
 * SimpleProxySupportingAttributeInheritancePolicy.java
 * Created on March 25, 2002, 2:27 PM
 */

package edu.cmu.cs.fluid.mvc;



/**
 * A simple {@link AttributeInheritancePolicy} that inherits all the (and only the) 
 * {@link Model#INFORMATIONAL} attributes of the model 
 * as immutable proxy-supporting informational attributes without any sort of renaming.
 * This policy <em>should not</em> be used if there is a concern about
 * name clashes between attributes.
 * 
 * <p>The class uses a singleton pattern, and thus has a private
 * constructor.  The one (and only) instance of the class is referred to 
 * by the field {@link #prototype}.
 */
public class SimpleProxySupportingAttributeInheritancePolicy
extends AbstractAttributeInheritancePolicy
implements AttributeInheritancePolicy
{
  /**
   * The singleton reference.
   */
  public static final AttributeInheritancePolicy prototype =
    new SimpleProxySupportingAttributeInheritancePolicy();
  
  
  
  /**
   * Private constructor; the class implements the singleton pattern.
   * Use the field {@link #prototype} instead.
   */
  public SimpleProxySupportingAttributeInheritancePolicy()
  {
  }
  
  
  
  /**
   * Filters out non-informational attributes.
   */
  @Override
  protected HowToInherit filterCompAttr( final Model from, final String attr )
  {
    if( from.getCompAttrKind( attr ) == Model.INFORMATIONAL ) {
      return new HowToInherit( attr, attr, AttributeInheritanceManager.IMMUTABLE,
                               Model.INFORMATIONAL );
    } else if( from.getCompAttrKind( attr ) == Model.USER_DEFINED ) {
      return new HowToInherit( attr, attr, AttributeInheritanceManager.IMMUTABLE,
          Model.USER_DEFINED );
    } else {
      return DONT_INHERIT;
    }
  }
  
  /**
   * Filters out non-informational attributes.
   */
  @Override
  protected HowToInherit filterNodeAttr( final Model from, final String attr )
  {
    if( from.getNodeAttrKind( attr ) == Model.INFORMATIONAL ) {
      return new HowToInherit( attr, attr, ProxyNodeSupport.IMMUTABLE_PROXY,
                               Model.INFORMATIONAL );
    }
    else if( from.getNodeAttrKind( attr ) == Model.USER_DEFINED ) {
      return new HowToInherit( attr, attr, ProxyNodeSupport.IMMUTABLE_PROXY,
                               Model.USER_DEFINED );      
    } else {
      return DONT_INHERIT;
    }
  }
}
