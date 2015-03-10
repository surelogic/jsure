// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/AttributeMatchingInheritancePredicate.java,v 1.6 2005/05/25 18:03:35 chance Exp $
package edu.cmu.cs.fluid.mvc;

import java.util.HashSet;
import java.util.Set;

/**
 * An implementation of AttributeInheritancePredicate that returns the
 * given mode if the attribute name matches one of the given ones.
 */
public class AttributeMatchingInheritancePredicate
  extends AbstractAttributeInheritancePredicate 
{
  private final Set<String> keys = new HashSet<String>();

  public AttributeMatchingInheritancePredicate(Object mode, String[] attrs) {
    super(mode);
    for(int i=0; i<attrs.length; i++) {
      addAttribute(attrs[i]);
    }
  }

  public AttributeMatchingInheritancePredicate(Object mode) {
    super(mode);
  }

  /** 
   * Returns the given mode if the attribute name matches the internal set
   */
  @Override
  public Object howToInherit( Model from, String attr ) {
    if( keys.contains(attr) ) {
      return mode;
    } 
    return SKIP_ME;
  }
  
  /** Adds an attribute to the those the predicate will match against */
  public void addAttribute(String attr) {
    keys.add(attr);
  }
}
