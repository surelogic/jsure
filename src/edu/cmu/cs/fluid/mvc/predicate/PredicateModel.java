// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/predicate/PredicateModel.java,v 1.19 2005/05/20 15:48:08 chance Exp $

package edu.cmu.cs.fluid.mvc.predicate;

import edu.cmu.cs.fluid.mvc.sequence.SequenceModel;
import edu.cmu.cs.fluid.ir.IREnumeratedType;
import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;

/**
 * A model of the <em>node-level</em> attribute predicates for another model.
 * The predicates are stored in an ordered sequence.  The order
 * is significant.  <em>See description of styling/visibility
 * algorithm</em>.
 *
 * <P>An implementation must support the
 * model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * <li>{@link edu.cmu.cs.fluid.mvc.set.SetModel#SIZE}
 * <li>{@link #PREDICATES_OF}
 * </ul>
 *
 * <P>An implementation must support the node-level
 * attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#IS_ELLIPSIS}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#ELLIDED_NODES}
 * <li>{@link SequenceModel#LOCATION}
 * <li>{@link SequenceModel#INDEX}
 * <li>{@link #ATTR_NODE}
 * <li>{@link #PREDICATE}
 * <li>{@link #IS_VISIBLE}
 * <li>{@link #IS_STYLED}
 * <li>{@link #ATTRIBUTE}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface PredicateModel 
extends SequenceModel
{
  //===========================================================
  //== Strings for the enumeration used by IS_VISIBLE
  //===========================================================

  /** 
   * The name of the enumeration used by the IS_VISIBLE attribute.
   */
  public static final String VISIBLE_ENUM = "PredicateModel$VisibleEnumeration";

  /**
   * The name of the enumeration element indicating that nodes
   * with this attribute should be forced to be visible.  This
   * must be the first element of the enumeration.
   */
  public static final String PRED_VISIBLE = "Visible";

  /**
   * The name of the enumeration element indicating that nodes
   * with this attribute should be forced to be invisible.  This
   * must be the second element of the enumeration.
   */
  public static final String PRED_INVISIBLE = "Invisible";

  /**
   * The name of the enumeration element indicating the visibility of nodes
   * with this attribute should be left alone.  This
   * must be the third (and final) element of the enumeration.
   */
  public static final String LEAVE_ALONE = "Pass-through";



  //===========================================================
  //== Names of standard model attributes
  //===========================================================

  /**
   * Model attribute indicating the model whose attribute predicates
   * this model is modeling.  In a StatefulView, the value
   * of this attribute must be a member of the set
   * contained by {@link edu.cmu.cs.fluid.mvc.View#SRC_MODELS}.
   * This attribute is immutable and has type {@link edu.cmu.cs.fluid.mvc.ModelType}.
   */
  public static final String PREDICATES_OF = "PredicateModel.PREDICATES_OF";

  
  
  //===========================================================
  //== Names of standard node attributes
  //===========================================================

  /**
   * The IRNode identified with the attribute for which this node contains
   * a predicate.  The value's type is {@link edu.cmu.cs.fluid.ir.IRNodeType},
   * and is immutable.
   */
  public static final String ATTR_NODE = "PredicateModel.attributeNode";

  /**
   * The predicate used to select nodes from the attribute.
   * The value's type is {@link AttributePredicateType}.
   * The value is an {@link AttributePredicate} and is immutable.
   */
  public static final String PREDICATE = "PredicateModel.predicate";

  /**
   * Indicates whether the attribute is to be made visible or not.
   * The value's type is the {@link edu.cmu.cs.fluid.ir.IREnumeratedType} returned
   * by <code>IREnumeratedType.getIterator( PredicateModel.VISIBLE_ENUM )</code>.
   * and is mutable.
   */
  public static final String IS_VISIBLE = "PredicateModel.isVisible";

  /**
   * Indicates whether the attribute should be styled.
   * The value's type is {@link edu.cmu.cs.fluid.ir.IRBooleanType},
   * and is mutable.
   */
  public static final String IS_STYLED = "PredicateModel.isStyled";

  /**
   * The SlotInfo used to store values for the attribute; immutable.
   */
  public static final String ATTRIBUTE = "PredicateModel.attribute";

  /**
   * The SlotInfo used to store custom labels for the predicate; mutable
   */
  public static final String PRED_LABEL = "PredicateModel.customLabel";


  //===========================================================
  //== Attribute Convienence methods  
  //===========================================================

  /** Get the visibility Element corresponding to the String */
  public IREnumeratedType.Element getEnumElt( String name );

  /**
   * Get the value of {@link #ATTR_NODE} attribute.
   */
  public IRNode getAttributeNode( IRNode node );

  /**
   * Get the value of the {@link #PREDICATE} attribute.
   */
  public AttributePredicate getPredicate( IRNode node );
  
  /**
   * Get the value of {@link #IS_VISIBLE} attribute.
   */
  public IREnumeratedType.Element isVisible( IRNode node );

  /**
   * Get the value of {@link #IS_STYLED} attribute.
   */
  public boolean isStyled( IRNode node );
  
  /**
   * Set the value of {@link #IS_VISIBLE} attribute.
   */
  public void setVisible( IRNode node, IREnumeratedType.Element vis );


  /**
   * Set the value of {@link #IS_STYLED} attribute.
   */
  public void setStyled( IRNode node, boolean sty );

  /**
   * Get the value of {@link #ATTRIBUTE} attribute.
   */
  public SlotInfo getAttribute( IRNode node );

  /**
   * Get the value of {@link #PRED_LABEL} attribute.
   */
  public String getLabel( IRNode node );
  
  /**
   * Set the value of {@link #PRED_LABEL} attribute.
   */
  public void setLabel( IRNode node, String label );

  //===========================================================
  //== Methods to customize the predicate view
  //===========================================================

  public IRNode addPredicateBefore( IRLocation loc,
				    IRNode attrNode, 
				    AttributePredicate pred,
				    IREnumeratedType.Element visible,
				    boolean styled,
				    SlotInfo attribute );

  public IRNode addPredicateAfter( IRLocation loc,
				   IRNode attrNode, 
				   AttributePredicate pred,
				   IREnumeratedType.Element visible,
				   boolean styled,
				   SlotInfo attribute );

  /** Added at the end */
  public IRNode addPredicate( IRNode attrNode, 
			      AttributePredicate pred,
			      IREnumeratedType.Element visible,
			      boolean styled,
			      SlotInfo attribute );

  //===========================================================
  //== Pickled State methods
  //===========================================================

  /**
   * Get pickled representation of the model's current state.
   */
  public PickledPredicateModelState getPickledState();

  /**
   * State the state of the attribute model from a
   * pickled representation of the state.
   * Any attributes present in the pickle that are not currently present
   * in the model are ignored.  Any attributes in the model that are
   * not present in the pickle are moved to the end of the sequence,
   * with their relative order retained.
   * @exception IllegalArgumentException Thrown if the 
   * pickle did not come from this model.
   */
  public void setStateFromPickle( PickledPredicateModelState pickle );
}
