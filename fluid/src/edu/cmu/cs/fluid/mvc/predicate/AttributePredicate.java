/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/predicate/AttributePredicate.java,v 1.8 2003/07/15 18:39:11 thallora Exp $ */

package edu.cmu.cs.fluid.mvc.predicate;


/**
 * Interface for predicates that are used to select nodes based on 
 * the value the node has for a particular attribute.  Used by 
 * {@link PredicateModel}s.  
 */
public interface AttributePredicate
{
  /** Get the <em>human readable</em> label describing the predicate. */
  public String getLabel();
  
  /** Query if the predicate includes the given value. */
  public boolean includesValue( Object value );

  /**
   * Query if the predicate includes the given tuple of values.
   */
  public boolean includesValues( Object[] values );
}

