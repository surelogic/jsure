// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/SimpleAttributeInheritancePredicate.java,v 1.6 2007/07/05 18:15:16 aarong Exp $
package edu.cmu.cs.fluid.mvc;


/**
 * A simple implementation of AttributeInheritancePredicate.
 * Uses an identity mapping for the attribute name
 */
public class SimpleAttributeInheritancePredicate
  extends AbstractAttributeInheritancePredicate 
{
  /** A node-level predicate that returns IMMUTABLE if INFORMATIONAL */
  public static final AttributeInheritancePredicate prototype = 
    new SimpleAttributeInheritancePredicate(AttributeInheritanceManager.IMMUTABLE, true);

  /** A node-level predicate that returns IMMUTABLE_PROXY if INFORMATIONAL */
  public static final AttributeInheritancePredicate proxyPrototype = 
    new SimpleAttributeInheritancePredicate(ProxyNodeSupport.IMMUTABLE_PROXY, true);

  /** A component-level predicate that returns IMMUTABLE if INFORMATIONAL */
  public static final AttributeInheritancePredicate compPrototype = 
    new SimpleAttributeInheritancePredicate(AttributeInheritanceManager.IMMUTABLE, false);

  /** 
   * Specifies whether it should be operating on node- or
   * component-level attributes
   */
  final boolean nodeP;

  /** 
   * @param mode The mode to return if the attribute is INFORMATIONAL
   * @param node True if operating on node-level attributes, or on
   * component-level attributes
   */
  public SimpleAttributeInheritancePredicate(Object mode, boolean node) {
    super(mode);
    nodeP = node;
  }

  /** Returns the mode parameter if the attribute is INFORMATIONAL */
  @Override
  public Object howToInherit( Model from, String attr ) {
    int kind = nodeP ? from.getNodeAttrKind( attr ) : from.getCompAttrKind( attr ); 

    if( kind == Model.INFORMATIONAL ) {
      return mode;
    } 
    return SKIP_ME;
  }
}
