// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/AbstractAttributeInheritancePredicate.java,v 1.5 2003/07/15 18:39:10 thallora Exp $
package edu.cmu.cs.fluid.mvc;


/**
 * A partial implementation of AttributeInheritancePredicate.
 * Parameterized for the mode, but always defers to the next predicate.
 * Uses an identity mapping for the attribute name
 */
public abstract class AbstractAttributeInheritancePredicate
  implements AttributeInheritancePredicate 
{
  final Object mode;

  // Javadoc inherited
  public AbstractAttributeInheritancePredicate(Object mode) {
    this.mode = mode;
  }

  // Javadoc inherited
  @Override
  public Object howToInherit( Model from, String attr ) {
    return SKIP_ME;
  }

  // Javadoc inherited
  @Override
  public String inheritAs(final String attr) {
    return attr;
  }
}
