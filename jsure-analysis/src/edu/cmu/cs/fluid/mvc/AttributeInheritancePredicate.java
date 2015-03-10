// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/AttributeInheritancePredicate.java,v 1.5 2003/07/15 18:39:10 thallora Exp $
package edu.cmu.cs.fluid.mvc;


/**
 * The interface for predicates used with CustomAttributeInheritancePolicy 
 */
public interface AttributeInheritancePredicate {
  /** This is returned by {@link #howToInherit} to signal that the
   * attribute should not be inherited
   */
  static final Object DONT_INHERIT = null;

  /** This is returned by {@link #howToInherit} to signal that the
   * predicate defers to the next one in the list
   */
  static final Object SKIP_ME      = new Object();

  /**
   * Returns the inheritance mode, or {@link #DONT_INHERIT} or {@link #SKIP_ME}
   * @param from The source model for the attribute in question
   * @param attr The attribute that is being considered for inheritance
   * @return The mode to inherit with, or one of the above
   */
  Object howToInherit(Model from, String attr);

  /**
   * Returns the name that the attribute should be mapped to in the 
   * inheriting View
   */
  String inheritAs(String attr);
}
