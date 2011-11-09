/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/predicate/SimplePredicateView.java,v 1.15 2003/07/15 21:47:19 aarong Exp $
 *
 * SimplePredicateView.java
 * Created on March 14, 2002, 1:47 PM
 */

package edu.cmu.cs.fluid.mvc.predicate;

import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * (Empty) Specialization of ModelToPredicateStatefulView.   
 *
 * <P>An implementation must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link Model#MODEL_NAME}
 * <li>{@link Model#MODEL_NODE}
 * <li>{@link edu.cmu.cs.fluid.mvc.set.SetModel#SIZE}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#VIEW_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#SRC_MODELS}
 * <li>{@link PredicateModel#PREDICATES_OF}
 * </ul>
 *
 * <p>The value of {@link PredicateModel#PREDICATES_OF} must be a member
 * of the set that is the value of {@link edu.cmu.cs.fluid.mvc.View#SRC_MODELS}.
 * The values of the <code>MODEL_NAME</code> and
 * <code>VIEW_NAME</code> attributes do not need to be the same.
 *
 * <P>An implementation  must support the node-level
 * attributes:
 * <ul>
 * <li>{@link Model#IS_ELLIPSIS}
 * <li>{@link Model#ELLIDED_NODES}
 * <li>{@link edu.cmu.cs.fluid.mvc.sequence.SequenceModel#LOCATION}
 * <li>{@link edu.cmu.cs.fluid.mvc.sequence.SequenceModel#INDEX}
 * <li>{@link PredicateModel#ATTR_NODE}
 * <li>{@link PredicateModel#PREDICATE}
 * <li>{@link PredicateModel#IS_VISIBLE}
 * <li>{@link PredicateModel#IS_STYLED}
 * <li>{@link PredicateModel#ATTRIBUTE}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface SimplePredicateView
extends ModelToPredicateStatefulView
{
  public static interface Factory
  {
    public SimplePredicateView create( String name, Model src )
    throws SlotAlreadyRegisteredException;
  }
}
