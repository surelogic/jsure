/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/SimpleAttributeInheritancePolicy.java,v 1.8 2007/07/05 18:15:16 aarong Exp $ */
package edu.cmu.cs.fluid.mvc;



/**
 * A simple {@link AttributeInheritancePolicy} that inherits all the 
 * {@link Model#INFORMATIONAL} and {@link Model#USER_DEFINED} attributes 
 * of the model as immutable attributes of the appropriate kind without 
 * any sort of renaming.
 * 
 * This policy <em>should not</em> be used if there is a concern about
 * name clashes between attributes.
 * 
 * <p>The class uses a singleton pattern, and thus has a private
 * constructor.  The one (and only) instance of the class is referred to 
 * by the field {@link #prototype}.
 */
public final class SimpleAttributeInheritancePolicy
extends AbstractAttributeInheritancePolicy
implements AttributeInheritancePolicy
{
  /**
   * The singleton reference.
   */
  public static final AttributeInheritancePolicy prototype =
    new SimpleAttributeInheritancePolicy(AttributeInheritanceManager.IMMUTABLE);

  public static final AttributeInheritancePolicy localMutablePrototype =
    new SimpleAttributeInheritancePolicy(AttributeInheritanceManager.MUTABLE_LOCAL);

  private Object mode;
  
  /**
   * Private constructor; the class implements the singleton pattern.
   * Use the field {@link #prototype} instead.
   */
  private SimpleAttributeInheritancePolicy(Object mode)
  {
    this.mode = mode;
  }
  
  /**
   * Filters out non-informational attributes.
   */
  @Override
  protected HowToInherit filterCompAttr( final Model from, final String attr )
  {
    if( from.getCompAttrKind( attr ) == Model.INFORMATIONAL  ) {
      return new HowToInherit( attr, attr, mode,
                               Model.INFORMATIONAL );
    } else if (from.getCompAttrKind( attr ) == Model.USER_DEFINED) {
      return new HowToInherit( attr, attr, mode,
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
      return new HowToInherit( attr, attr, mode,
                               Model.INFORMATIONAL );
    } else if (from.getNodeAttrKind( attr ) == Model.USER_DEFINED) {
      return new HowToInherit( attr, attr, mode,
                               Model.USER_DEFINED );
    } else {
      return DONT_INHERIT;
    }
  }
}
