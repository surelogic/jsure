/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/version/FVPAttributeInheritancePolicy.java,v 1.8 2008/05/15 16:24:11 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.version;

import edu.cmu.cs.fluid.mvc.*;

/**
 * Inheritance Policy that inherits all attributes
 * except for {@link Model#MODEL_NAME} and {@link Model#MODEL_NODE}
 * as immutable, preserving their kind.
 * 
 * <p>The class uses a singleton pattern, and thus has a private
 * constructor.  The one (and only) instance of the class is referred to 
 * by the field {@link #prototype}.
 */
public final class FVPAttributeInheritancePolicy
extends AbstractAttributeInheritancePolicy
implements AttributeInheritancePolicy
{
  /**
   * The singleton reference.
   */
  public static final AttributeInheritancePolicy prototype =
    new FVPAttributeInheritancePolicy();
  
  
  
  /**
   * Private constructor; use the singleton reference {@link #prototype}.
   */
  private FVPAttributeInheritancePolicy()
  {
  }
  
  
  /**
   * Filters out {@link Model#MODEL_NAME} and {@link Model#MODEL_NODE}.  All
   * other attributes are inherited as immutable preserving their kind
   * and name.
   */
  @Override
  protected HowToInherit filterCompAttr( final Model from, final String attr )
  {
    if( (attr != Model.MODEL_NAME) && (attr != Model.MODEL_NODE) ) {
      return new HowToInherit( attr, attr, AttributeInheritanceManager.IMMUTABLE,
                               from.getCompAttrKind( attr ) );
    } else {
      return DONT_INHERIT;
    }
  }
  
  /**
   * Inherits all attributes as immutable, preserving their kind and name.
   */
  @Override
  protected HowToInherit filterNodeAttr( final Model from, final String attr )
  {
    return new HowToInherit( attr, attr, AttributeInheritanceManager.IMMUTABLE,
                             from.getNodeAttrKind( attr ) );
  }
}
