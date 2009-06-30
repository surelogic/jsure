package edu.cmu.cs.fluid.mvc.tree.syntax;

import edu.cmu.cs.fluid.mvc.tree.ForestModel;
import edu.cmu.cs.fluid.tree.SyntaxTreeInterface;

/**
 * The syntax forest model interface.
 * 
 * <P>An implementation must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * <li>{@link edu.cmu.cs.fluid.mvc.tree.ForestModel#ROOTS}
 * </ul>
 *
 * <P>An implementation must support the node-level
 * attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#IS_ELLIPSIS}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#ELLIDED_NODES}
 * <li>{@link edu.cmu.cs.fluid.mvc.tree.DigraphModel#CHILDREN}
 * <li>{@link edu.cmu.cs.fluid.mvc.tree.SymmetricDigraphModel#PARENTS}
 * <li>{@link edu.cmu.cs.fluid.mvc.tree.ForestModel#LOCATION}
 * <li>{@link #OPERATOR}
 * </ul>
 *
 * @author Edwin Chan
 */
public interface SyntaxForestModel
extends ForestModel, SyntaxTreeInterface
{
  //===========================================================
  //== Names of standard node attributes
  //===========================================================
  
  /**
   * The operator for a syntax node {@link edu.cmu.cs.fluid.tree.Operator};
   * immutable structural attribute.
   */
  public static final String OPERATOR = "SyntaxForestModel.operator";

  
  
  //===========================================================
  //== Convienence methods for attributes
  //===========================================================
  
  /*
   * getOperator() and opExists() come from SyntaxTreeInferface
   */
}
