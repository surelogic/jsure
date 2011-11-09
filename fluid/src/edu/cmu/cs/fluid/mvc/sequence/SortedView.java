/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/sequence/SortedView.java,v 1.8 2003/07/15 21:47:18 aarong Exp $
 *
 * SortedSetView.java
 * Created on March 6, 2002, 4:21 PM
 */

package edu.cmu.cs.fluid.mvc.sequence;

import edu.cmu.cs.fluid.mvc.AttributeInheritancePolicy;
import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * A specialization of {@link ModelToSequenceStatefulView} that has exactly
 * one source model that it converts into a sequence by sorting 
 * the nodes of the source model based on the values of a specific node
 * attribute.  The node
 * attribute relevant to sorting is controlled by the model attribute
 * {@link #SORT_ATTR}.  Whether the nodes are sorted in ascending or descending
 * order is controlled by the model attribute {@link #IS_ASCENDING}. 
 *
 * <p>An attribute may only be used as the value of the {@link #SORT_ATTR}
 * attribute if it is a node attribute inherited by the SortedSetView as
 * {@link edu.cmu.cs.fluid.mvc.AttributeInheritanceManager#IMMUTABLE} or
 * {@link edu.cmu.cs.fluid.mvc.AttributeInheritanceManager#MUTABLE_SOURCE}.
 * Attempting to set the attribute to a non-conforming attribute will result
 * in an IllegalArgumentException.
 *
 * <p>Sorting is performed using the String representations of 
 * node attribute values obtained from the {@link Model#nodeValueToString}
 * method of the source model.
 *
 * <P>An implementation of this interface must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link Model#MODEL_NAME}
 * <li>{@link Model#MODEL_NODE}
 * <li>{@link edu.cmu.cs.fluid.mvc.set.SetModel#SIZE}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#VIEW_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#SRC_MODELS}
 * <li>{@link #IS_ASCENDING}
 * <li>{@link #SORT_ATTR}
 * </ul>
 *
 * <p>The values of the MODEL_NAME and VIEW_NAME attributes do not
 * need to be the same.
 *
 * <P>An implementation must support the node-level
 * attributes:
 * <ul>
 * <li>{@link Model#IS_ELLIPSIS}
 * <li>{@link Model#ELLIDED_NODES}
 * <li>{@link SequenceModel#LOCATION}
 * <LI>{@link SequenceModel#INDEX}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface SortedView
extends ModelToSequenceStatefulView
{
  /**
   * Mutable, boolean-valued model attribute indicating whether or not the
   * nodes are sorted in ascending (<code>true</code>) or
   * descending (<code>false</code>) order.
   */
  public static final String IS_ASCENDING = "SortedSetView.IS_ASCENDING";

  /**
   * Mutable, String-valued model attribute naming the node-level
   * attribute whose values are used to order the nodes.  Setting the
   * attribute to a name that does non-conforming attribute name (see above)
   * will cause an IllegalArgumentException to be thrown.  This
   * can happen either using the convienence method, or when the attribute
   * is directly changed via its representative ComponentSlot.
   */
  public static final String SORT_ATTR = "SortedSetView.SORT_ATTRIBUTE";
  
  
  
  /**
   * Convienence method for setting the model-level attribute
   * {@link #IS_ASCENDING}.
   */
  public void setAscending( boolean isAscending );
  
  /**
   * Convienence method for getting the value of the model-level attribute
   * {@link #IS_ASCENDING}.
   */
  public boolean isAscending();
  
  
  
  /**
   * Convienence method for setting the model-level attribute
   * {@link #SORT_ATTR}. 
   * @exception IllegalArgumentException Thrown if the given
   *   attribute does not conform to the requirements of the sort attribute
   *   (see above).
   */
  public void setSortAttribute( String attrName );
  
  /**
   * Convienence method for getting the value of the model-level attribute
   * {@link #SORT_ATTR}.
   */
  public String getSortAttribute();
  
  
  /**
   * Factory for creating instances of SortedSetView.
   */
  public static interface Factory
  {
    /**
     * Create a new sorted view of a SetModel.
     * @param name The name of the new model.
     * @param srcModel The SetModel to be sorted.
     * @param attr The initial attribute whose values should be used to
     *    order the nodes.
     * @param isAscending Whether the nodes should be initially sorted in
     *    ascending order.
     * @param policy The policy controlling which attributes of the
     *    source model are inherited by the sorted view.
     * @exception IllegalArgumentException Thrown if <code>attr</code>
     *   does not name a node-level attribute of the source model that
     *   has been inherited by the sorted view.
     */
    public SortedView create(
      String name, Model srcModel, String attr, boolean isAscending,
      AttributeInheritancePolicy policy )
    throws SlotAlreadyRegisteredException;
  }
}
