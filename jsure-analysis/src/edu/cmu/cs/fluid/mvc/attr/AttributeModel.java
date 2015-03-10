// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attr/AttributeModel.java,v 1.9 2003/07/15 21:47:19 aarong Exp $

package edu.cmu.cs.fluid.mvc.attr;

import edu.cmu.cs.fluid.mvc.set.SetModel;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRSequence;
import edu.cmu.cs.fluid.ir.IRType;

/**
 * A model of the attributes in another model.
 * The attributes are stored in a set.  Each attribute is represented by the
 * IRNode identified with it in the model from which it originates, as returned
 * by the {@link edu.cmu.cs.fluid.mvc.Model#getCompAttrNode} and
 * {@link edu.cmu.cs.fluid.mvc.Model#getNodeAttrNode} methods.
 *
 * <P>An implementation must support the
 * model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * <li>{@link SetModel#SIZE}
 * <li>{@link #ATTRIBUTES_OF}
 * </ul>
 *
 * <P>An implementation must support the node-level
 * attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#IS_ELLIPSIS}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#ELLIDED_NODES}
 * <li>{@link #ATTR_NAME}
 * <li>{@link #ATTR_LABEL}
 * <li>{@link #ATTR_KIND}
 * <li>{@link #ATTR_TYPE}
 * <li>{@link #DOMAIN}
 * <li>{@link #IS_MUTABLE}
 * <li>{@link #IS_NODE_ATTR}
 * </ul>
 *
 * @author Edwin Chan
 * @author Aaron Greenhouse
 */
public interface AttributeModel 
extends SetModel
{
  //===========================================================
  //== Names of standard model attributes
  //===========================================================

  /**
   * Model attribute indicating the model whose attributes
   * this model is modeling.  In a StatefulView, the value
   * of this attribute must be a member of the set
   * contained by {@link edu.cmu.cs.fluid.mvc.View#SRC_MODELS}.
   * This attribute is immutable and has type {@link edu.cmu.cs.fluid.mvc.ModelType}.
   */
  public static final String ATTRIBUTES_OF = "AttributeModel.ATTRIBUTES_OF";


  //===========================================================
  //== Names of standard node attributes
  //===========================================================

  /**
   * The name of the attribute represented by a node.
   * The value's type is {@link edu.cmu.cs.fluid.ir.IRStringType},
   * and is immutable.  The name of an attribute is fixed,
   * and is used to identify the attribute to the model.  It is 
   * a kind of unique ID. 
   */
  public static final String ATTR_NAME = "AttributeModel.attributeName";

  /**
   * The label of the attribute represented by a node.
   * The value's type is {@link edu.cmu.cs.fluid.ir.IRStringType},
   * and is mutable.  The label of an attribute is meant to be
   * displayed to humans.  The label is mutable, and a particular
   * attribute of a particular model can be differently labeled
   * in different AttributeModels, e.g. for i18n purposes.
   */
  public static final String ATTR_LABEL = "AttributeModel.attributeLabel";

  /**
   * The IR type of the attribute represented by a node.
   * The value's type is {@link edu.cmu.cs.fluid.ir.IRTypeType},
   * and is immutable.
   */
  public static final String ATTR_TYPE = "AttributeModel.attributeType";

  /**
   * The kind of an attribute.  This indicates information about 
   * how the attribute is used by the model.  Currently it has 
   * only two values: {@link edu.cmu.cs.fluid.mvc.Model#STRUCTURAL} and 
   * {@link edu.cmu.cs.fluid.mvc.Model#INFORMATIONAL}.  The value's type is 
   * {@link edu.cmu.cs.fluid.ir.IRIntegerType} and is immutable.  (This should
   * really be made an enumerated type, but I am to lazy to do this
   * right now.)
   */
   public static final String ATTR_KIND = "AttributeModel.attributeKind";

  /**
   * The models whose nodes form the domain of the attribute (only defined
   * for node-level attributes).  An immutable,
   * {@link edu.cmu.cs.fluid.ir.IRSequenceType}-valued attribute.  The sequence contains
   * the Models making up the union.  For 
   * {@link edu.cmu.cs.fluid.mvc.Model#MODEL_DOMAIN} attributes, the value is a sequence
   * of length one that contains the Model itself (i.e., the source model to
   * the attribute model).  The sequence itself is also immutable.
   */
  public static final String DOMAIN = "AttributeModel.domain";

  /**
   * Whether the attribute represented by a node is mutable or not.
   * The value's type is {@link edu.cmu.cs.fluid.ir.IRBooleanType},
   * and is mutable.
   */
  public static final String IS_MUTABLE = "AttributeModel.isMutable";

  /**
   * Whether the attribute represented by a node is defined on nodes or models
   * The value's type is {@link edu.cmu.cs.fluid.ir.IRBooleanType},
   * and is immutable.
   */
  public static final String IS_NODE_ATTR = "AttributeModel.isNodeAttr";



  //===========================================================
  //== Attribute Convienence methods  
  //===========================================================

  /**
   * Get the value of {@link #ATTR_NAME} attribute.
   */
  public String getName( IRNode node );

  /**
   * Get the value of {@link #ATTR_LABEL} attribute.
   */
  public String getLabel( IRNode node );

  /**
   * Get the value of {@link #ATTR_TYPE} attribute.
   */
  public IRType getType( IRNode node );

  /**
   * Get the value of {@link #ATTR_KIND} attribute.
   */
  public int getKind( IRNode node );

  /**
   * Get the value of the {@link #DOMAIN} attribute.
   */
  public IRSequence getDomain( IRNode node );
  
  /**
   * Get the value of {@link #IS_MUTABLE} attribute.
   */
  public boolean isMutable( IRNode node );

  /**
   * Get the value of {@link #IS_NODE_ATTR} attribute.
   */
  public boolean isNodeAttr( IRNode node );

  /**
   * Set the value of {@link #ATTR_LABEL} attribute.
   */
  public void setLabel( IRNode node, String label );



  //===========================================================
  //== Pickled State methods
  //===========================================================

  /**
   * Get pickled representation of the model's current state.
   */
  public PickledAttributeModelState getPickledState();

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
  public void setStateFromPickle( PickledAttributeModelState pickle );
}
