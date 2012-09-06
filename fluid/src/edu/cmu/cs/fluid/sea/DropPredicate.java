package edu.cmu.cs.fluid.sea;

/**
 * Defines a predicate used to filter a set of drops.
 */
public interface DropPredicate {

  /**
   * Decides if a specific drop should be an element of the resulting set.
   * <p>
   * If checking that the passed drop is of a specific type use
   * {@link IDropInfo#instanceOf(Class)} rather than the <tt>instatnceof</tt>
   * operator. An exact type check should use the {@link IDropInfo#getTypeName()}
   * 
   * @param d
   *          the drop to evaluate.
   * @return <code>true</code> if the drop should be included,
   *         <code>false</code> if the drop should be omitted.
   */
  boolean match(IDropInfo d);
}
