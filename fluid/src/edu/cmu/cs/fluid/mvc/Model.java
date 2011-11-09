package edu.cmu.cs.fluid.mvc;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.*;

/**
 * <P>
 * Interface for models. A <code>Model</code> encapsulates a structure made
 * up of {@link edu.cmu.cs.fluid.ir.IRNode}s. A model may use versioned or
 * unversioned IR structures to build its representation. The model itself,
 * however, is oblivious to versioning. It is the responsiblity of the clients
 * (e.g., <code>StatefulViews</code>) and controllers to use the appropriate
 * version when accessing the Model. The method {@link #isPresent}is used to
 * determine whether a particular node is (currently) part of the model's
 * structure. (This used to be, but is no longer, represented by an
 * "Model.isPresent" attribute, but it was not very interesting because it is
 * always <code>true</code> for nodes that are part of the model.)
 * 
 * <P>
 * A model has component-level and node-level attributes. component-level
 * attributes are akin to class fields, they have one value per model.
 * Node-level attributes have one value per IRNode in the model. Every
 * attribute is uniquely identified by an IRNode associated with it on a per
 * model basis.
 * 
 * <p>
 * Attributes are grouped along various non-exclusive axes. An attribute has a
 * <em>kind</em>, either {@link #STRUCTURAL},{@link #INFORMATIONAL}, or
 * {@link #USER_DEFINED}, which reflects its role in the organization of the
 * model. A structural attribute is fundamentally related to the structure of
 * the abstraction that the model represents, e.g., containing the size of a
 * list, or the parents and children in a tree, etc. An informational attribute
 * is one that is fundamental to the nature of the model, but not related to
 * its structure, e.g., a label or other information that decorate the data
 * structure. A user-defined attribute is one that has been dynamically added
 * to the model using {@link #addCompAttribute},
 * {@link #addNodeAttribute(String, IRType, SlotFactory, boolean)}, or
 * {@link #addNodeAttribute(String, IRType, SlotFactory, boolean, Model[])}.
 * 
 * <p>
 * Node-level attributes are also grouped by their domain. The domain of a
 * node-level attribute is either {@link #MODEL_DOMAIN}, meaning the set of
 * nodes in the model (exported model in the case of a stateful view) itself,
 * or {@link #SRC_DOMAIN}, meaning the a set derived from the union of one or
 * more source models. Pure (non&ndash;stateful-view) models must only have
 * attributes with a model domain. An attribute with a source domain is further
 * annotated with the set of source models whose union makes up the domain.
 * 
 * <p>
 * The domain of an attribute (obviously) influences whether the model
 * considers itself responsible for storing values of that attribute for
 * particular nodes. The model can be queried to determine whether it (might)
 * hold a value a given node and attribute pair using the method
 * {@link #isAttributable(IRNode,String)}. A model may consider nodes that are
 * not strictly part of the model (those for which {@link #isPresent}is <code>true</code>)
 * to be attributable. Models that do this explain why and under what
 * circumstances this can happen in their documentation. In general, it is
 * expected that for any node that is actually part of the model (e.g.,
 * {@link #isPresent}in the model) there must exists at least attribute for
 * which {@link #isAttributable}returns <code>true</code>.
 * 
 * <p>
 * This meta-level information (kind, domain, type, etc.) about attributes
 * necessarily exists outside the attribute system itself, although may be
 * projected into the model&ndash;view regime by obtaining an
 * {@link edu.cmu.cs.fluid.mvc.attr.AttributeModel}of a model.
 * 
 * <P>
 * Models may contain special nodes that represent ellipsis. Views and
 * Renderers can choose to display these nodes specially. Ellipsis nodes have a
 * <code>true</code> value for the {@link #IS_ELLIPSIS}attribute.
 * 
 * <P>
 * Models send out {@link ModelEvent}s whenever they change. A model changes
 * whenever any of the structure it exposes is modified. A model (especially a
 * model that is a StatefulView) must be careful that it does not send events
 * that reveal inconsistent/intermediate states. <em>What about changes to attributes?</em>
 * 
 * <p>Models have a {@link #shutdown} method that should be called when the 
 * model is no longer needed.  This model should disconnect this model from
 * any other models.  At a minimum, the method clears out any listeners
 * that have been attached to it.  For stateful views, the method should
 * also remove the view's listeners from any models it is viewing and shutdown
 * the rebuild thread. 
 * 
 * <p>
 * Every <code>Model</code> has a unique <code>IRNode</code> identifying
 * it. In this future this node will be used to construct a meta-model of the
 * active model&ndash;view relationships.
 * 
 * <P>
 * An implementation must support the component-level attributes:
 * <ul>
 * <li>{@link #MODEL_NAME}
 * <li>{@link #MODEL_NODE}
 * </ul>
 * 
 * <P>
 * An implementation must support the node-level attributes:
 * <ul>
 * <li>{@link #IS_ELLIPSIS}
 * <li>{@link #ELLIDED_NODES}
 * </ul>
 * 
 * @author Aaron Greenhouse
 */
public interface Model extends IRState {
  /**
	 * Canonical reference to the Category representing
	 * movel&ndash;view&ndash;controller related output.
	 */
  public static final Logger MV = SLLogger.getLogger("MV");

  //===========================================================
  //== Attribute kinds
  //===========================================================

  /**
	 * Attribute Kind indicating the attribute is related to the structure of the
	 * model. For example, the parents of a node in a tree, or the number of
	 * elements in sequence, or the location of an element in a sequence, etc.
	 */
  public static final int STRUCTURAL = 0;

  /**
	 * Attribute kind indicating the attribute is data "decorating" the structure
	 * of the model. For example, a node's label, a file's name, creation date,
	 * etc.
	 */
  public static final int INFORMATIONAL = 1;

  /**
	 * Attribute kind indicating the attribute was dynamically added after the
	 * model was instantiated.
	 */
  public static final int USER_DEFINED = 2;

  //===========================================================
  //== Attribute domain flags
  //===========================================================

  /**
	 * Constant indicating that a node-level attribute uses the nodes of the
	 * model itself as its domain.
	 */
  public static final int MODEL_DOMAIN = 0;

  /**
	 * Constant indicating that a node-level attribute uses the nodes of some
	 * union of its source models as its domain.
	 */
  public static final int SRC_DOMAIN = 1;

  //===========================================================
  //== Names of standard model attributes
  //===========================================================

  /**
	 * Model attribute containing the <code>IRNode</code> of the model. The
	 * value's type is {@link edu.cmu.cs.fluid.ir.IRNodeType}, and the is
	 * immutable.
	 */
  public static final String MODEL_NODE = "Model.NODE";

  /**
	 * Model attribute containing the name of the model. The value's type is
	 * {@link edu.cmu.cs.fluid.ir.IRStringType}, and the is immutable.
	 */
  public static final String MODEL_NAME = "Model.NAME";

  //===========================================================
  //== Names of standard node attributes
  //===========================================================

  /**
	 * Node attribute indicating whether a node represents an ellipsis. The
	 * value's type is {@link edu.cmu.cs.fluid.ir.IRBooleanType}. The value of
	 * this attribute for a given node cannot be directly set by users of this
	 * model, but does vary over time as the model changes. In the case of <code>StatefulView</code>s,
	 * this attribute is indexed using nodes of the exported model.
	 */
  public static final String IS_ELLIPSIS = "Model.isEllipsis";

  /**
	 * Node attribute giving the nodes an ellipsis node is replacing. The value
	 * is a {@link java.util.Set}of <code>IRNodes</code>. The source of any
	 * given <code>IRNode</code> in the set can be determined by quering the
	 * {@link #isPresent}attribute of the source models. In the case of <code>StatefulView</code>s,
	 * this attribute is indexed using nodes of the exported model.
	 */
  public static final String ELLIDED_NODES = "Model.ellidedNodes";

  

  //===========================================================
  //== Model lifecycle methods
  //===========================================================

  public void shutdown();
  
  
  
  //===========================================================
  //== Node-to-attribute methods
  //===========================================================

  /**
	 * Get the name of the attribute identified with the given IRNode.
	 * 
	 * @param node
	 *          The IRNode (representing an attribute) to query on.
	 * @return An interned String giving the name of the attribute.
	 * @exception UnknownAttributeException
	 *              Thrown if the IRNode is not identified with an attribute in
	 *              this manager.
	 */
  public String getAttributeName(IRNode node) throws UnknownAttributeException;

  /**
	 * Get the name-space of the attribute identified with the given IRNode.
	 * 
	 * @param node
	 *          The IRNode (representing an attribute) to query on.
	 * @return <code>true</code> iff the attribute identified with given node
	 *         is a node-level attribute.
	 * @exception UnknownAttributeException
	 *              Thrown if the IRNode is not identified with an attribute in
	 *              this manager.
	 */
  public boolean isNodeAttribute(IRNode node) throws UnknownAttributeException;

  //===========================================================
  //== Component attribute related methods
  //===========================================================

  /**
	 * Add a new user-defined model-level attribute.
	 * 
	 * @param name
	 *          The name of the attribute.
	 * @param type
	 *          The type of the attribute's value.
	 * @param csf
	 *          The factory to use to create the ComponentSlot that will store
	 *          the attribute's value.
	 * @param isMutable
	 *          Flag indicating whether the attribute is mutable or not.
	 * @return The component slot that will also be returned by calls to
	 *         {@link #getCompAttribute}; that is, it is appropriately wrapped,
	 *         etc.
	 * @exception AttributeAlreadyExistsException
	 *              Thrown if a model-level attribute of the same name already
	 *              exists in the model.
	 * @exception UnsupportedOperationException
	 *              Thrown if the model is in a state in which new attributes
	 *              cannot be created. Currently this occurs if the model has any
	 *              attached listeners (this may be removed in the future).
	 */
  public <T> ComponentSlot<T> addCompAttribute(
    String name,
    IRType<T> type,
    ComponentSlot.Factory csf,
    boolean isMutable);

  /**
	 * Get the names of the model-level attributes.
	 * 
	 * @return An <code>Iterator</code> over <code>String</code>s.
	 */
  public Iterator<String> getComponentAttributes();

  /**
	 * Query if a given model attribute is understood by the model.
	 */
  public boolean isComponentAttribute(String att);

  /**
	 * Query if a given model attribute is mutable.
	 * 
	 * @exception UnknownAttributeException
	 *              Thrown if the given attribute is not recognized.
	 */
  public boolean isCompAttrMutable(String att);

  /**
	 * Get the kind of a component attribute.
	 * 
	 * @exception UnknownAttributeException
	 *              Thrown if the given attribute is not recognized.
	 */
  public int getCompAttrKind(String att);

  /**
	 * Get the IRNode identified with a component attribute.
	 * 
	 * @exception UnknownAttributeException
	 *              Thrown if the given attribute is not recognized.
	 */
  public IRNode getCompAttrNode(String att);

  /**
	 * Get the storage for a component-level attribute.
	 * 
	 * @exception UnknownAttributeException
	 *              Thrown if the given attribute is not recognized.
	 */
  public ComponentSlot getCompAttribute(String att);

  /**
	 * Set the values of several component attributes atomically. The model may
	 * refuse to set the values if doing so would result in the model being in an
	 * inconsistent or illegal state.
	 * 
	 * @exception UnknownAttributeException
	 *              Thrown if any of the given attributes is unrecognized by the
	 *              model. When this is thrown it must appear that none of the
	 *              attributes have been modified by this call; <i>i</i>.<i>
	 *              e</i>., it must be as if all the attributes are checked for
	 *              validity first, and then all the attributes are set.
	 * @exception IllegalArgumentException
	 *              Thrown when some attribute values would result in the model
	 *              being in an illegal state. When this is thrown it must appear
	 *              that none of the attributes have been modified by this call;
	 *              <i>i</i>.<i>e</i>., it must be as if all the values
	 *              are checked for validity first, and then all the attributes
	 *              are set.
	 */
  public void setCompAttributes(AVPair[] pairs);

  //===========================================================
  //== Node attribute related methods
  //===========================================================

  /**
	 * Add a new user-defined node-level attribute whose domain is a union of one
	 * or more source models.
	 * 
	 * @param name
	 *          The name of the attribute.
	 * @param type
	 *          The type of the attribute's value.
	 * @param sf
	 *          The SlotFactory to use to create the attributes storage.
	 * @param isMutable
	 *          Flag indicating whether the attribute is mutable or not.
	 * @param srcs
	 *          An array of those models whose union is a (super-set) of the
	 *          domain of the attribute.
	 * @return The SlotInfo that will also be returned by calls to
	 *         {@link #getNodeAttribute}; that is, it is appropriately wrapped,
	 *         etc.
	 * @exception AttributeAlreadyExistsException
	 *              Thrown if a node-level attribute of the same name already
	 *              exists in the model.
	 * @exception UnsupportedOperationException
	 *              Thrown if the model is in a state in which new attributes
	 *              cannot be created. Currently this occurs if the model has any
	 *              attached listeners (this may be removed in the future).
	 */
  public <T> SlotInfo<T> addNodeAttribute(
    String name,
    IRType<T> type,
    SlotFactory sf,
    boolean isMutable,
    Model[] srcs);

  /**
	 * Add a new user-defined node-level attribute whose domain are the nodes of
	 * the model itself.
	 * 
	 * @param name
	 *          The name of the attribute.
	 * @param type
	 *          The type of the attribute's value.
	 * @param sf
	 *          The SlotFactory to use to create the attributes storage.
	 * @param isMutable
	 *          Flag indicating whether the attribute is mutable or not.
	 * @return The SlotInfo that will also be returned by calls to
	 *         {@link #getNodeAttribute}; that is, it is appropriately wrapped,
	 *         etc.
	 * @exception AttributeAlreadyExistsException
	 *              Thrown if a node-level attribute of the same name already
	 *              exists in the model.
	 */
  public <T> SlotInfo<T> addNodeAttribute(
    String name,
    IRType<T> type,
    SlotFactory sf,
    boolean isMutable);

  /**
	 * Get the names of the node-level attributes.
	 * 
	 * @return An <code>Iterator</code> over <code>String</code>s.
	 */
  public Iterator<String> getNodeAttributes();

  /**
	 * Query if a given attribute is understood by the model.
	 */
  public boolean isNodeAttribute(String att);

  /**
	 * Query if a given node attribute is mutable, that is whether clients can
	 * modify the attribute through <em>this</em> model. It is entirely likely
	 * that the attribute may be mutable when obtained from a different model,
	 * e.g., in the case of a model and a projection of that model.
	 * 
	 * @exception UnknownAttributeException
	 *              Thrown if the given attribute is not recognized.
	 */
  public boolean isNodeAttrMutable(String att);

  /**
	 * Get the kind of a node attribute.
	 * 
	 * @exception UnknownAttributeException
	 *              Thrown if the given attribute is not recognized.
	 */
  public int getNodeAttrKind(String att);

  /**
	 * Get the IRNode identified with a node attribute.
	 * 
	 * @exception UnknownAttributeException
	 *              Thrown if the given attribute is not recognized.
	 */
  public IRNode getNodeAttrNode(String att);

  /**
	 * Get the domain identified with a node attribute.
	 * 
	 * @exception UnknownAttributeException
	 *              Thrown if the given attribute is not recognized.
	 */
  public int getNodeAttrDomain(String att);

  /**
	 * Get the source models whose union forms the domain identified with a node
	 * attribute whose domain is {@link #SRC_DOMAIN}.
	 * 
	 * @exception UnknownAttributeException
	 *              Thrown if the given attribute is not recognized.
	 * @exception IllegalArgumentException
	 *              if the given attribute does not have a domain of
	 *              {@link #SRC_DOMAIN}.
	 */
  public Model[] getNodeAttrDomainSrcs(String att);

  /**
	 * Get the SlotInfo representing a node-level attribute.
	 * 
	 * @exception UnknownAttributeException
	 *              Thrown if the given attribute is not recognized.
	 */
  public SlotInfo getNodeAttribute(String att);

  /**
	 * Set the values of several node attributes atomically. The model may refuse
	 * to set the values if doing so would result in the model being in an
	 * inconsistent or illegal state.
	 * 
	 * @exception UnknownAttributeException
	 *              Thrown if any of the given attributes is unrecognized by the
	 *              model. When this is thrown it must appear that none of the
	 *              attributes have been modified by this call; <i>i</i>.<i>
	 *              e</i>., it must be as if all the attributes are checked for
	 *              validity first, and then all the attributes are set.
	 * @exception IllegalArgumentException
	 *              Thrown when some attribute values would result in the model
	 *              being in an illegal state. When this is thrown it must appear
	 *              that none of the attributes have been modified by this call;
	 *              <i>i</i>.<i>e</i>., it must be as if all the values
	 *              are checked for validity first, and then all the attributes
	 *              are set.
	 */
  public void setNodeAttributes(IRNode node, AVPair[] pairs);

  //===========================================================
  //== Node methods
  //===========================================================

  /**
	 * Get the nodes in a model-specific order. The iterator returned by this
	 * method must iterator over exactly those nodes for which the
	 * {@link #isPresent}attribute is <code>true</code>.
	 */
  public Iterator<IRNode> getNodes();

  /**
	 * Add a node to the model setting a subset of its attributes to the given
	 * values. The model may refuse to add the node if values for attributes are
	 * not provided, or if some provided attribute values would result in the
	 * model being in an inconsistent/illegal state.
	 * 
	 * @exception UnknownAttributeException
	 *              Thrown when the node is rejected because a given attribute is
	 *              unrecognized.
	 * @exception IllegalArgumentException
	 *              Thrown when the node is rejected because some attribute
	 *              values would result in the model being in an illegal state.
	 */
  public void addNode(IRNode n, AVPair[] vals);

  /**
	 * Remove a node from the model. The removal of the node may also result in
	 * other nodes being removed from the model, e.g., the children of the node
	 * in a tree-structured model.
	 * 
	 * @param n
	 *          The node to remove from the model
	 */
  public void removeNode(IRNode n);

  /**
	 * Query whether a node is part of the structure of a model. In particular,
	 * proxy nodes are not part of the model.
	 */
  public boolean isPresent(IRNode node);

  /**
	 * Query if a node that is not part of the model (e.g., for which
	 * {@link #isPresent}is <code>false</code>) has attribute values stored
	 * in the model anyway. This is primary for supporting proxy nodes in
	 * configurable views, but may have other uses in the future.
	 */
  public boolean isOtherwiseAttributable(IRNode node);

  /**
	 * Query whether the given node is in the domain of the given attribute.
	 * 
	 * @return <code>true</code> if the node is capable of having a value for
	 *         the given attribute in the model (although that value may be
	 *         currently undefined}; <code>false</code> if the node does not
	 *         currently and cannot ever have a value for the given attribute in
	 *         the model.
	 */
  public boolean isAttributable(IRNode node, String attr);

  //===========================================================
  //== Attribute Convienence Methods
  //===========================================================

  /**
	 * Convienence method for getting the value of the model attribute
	 * {@link #MODEL_NODE}.
	 */
  public IRNode getNode();

  /**
	 * Convienence method for getting the value of the model attribute
	 * {@link #MODEL_NAME}.
	 */
  public String getName();

  /**
	 * Get the value of the {@link #ELLIDED_NODES}attribute.
	 */
  public Set<IRNode> getEllidedNodes(IRNode node);

  /**
	 * Get the value of the {@link #IS_ELLIPSIS}attribute.
	 */
  public boolean isEllipsis(IRNode node);

  //===========================================================
  //== Atomic Actions
  //===========================================================
  
  /**
   * Create a new action that executes atomically within the model. The lock on
   * the model will be held during the entire action, and a single composite
   * model event is sent at the end of the action. If the action throws an
   * exception, then no event will be sent. Any events generated by the model
   * during the execution of the action are buffered, and included in the
   * composite event. The action itself can append events to the composite event
   * by returning a non-<code>null</code> non-empty List of model events.
   * 
   * <p>
   * The idea here is that the action-writer writes the action as an
   * implementation of {@link AtomizedModelAction}, this method returns a
   * wrapped version that does all the locking, etc. We return a new
   * AtomizedModelAction so that it be wrapped again. This important for
   * managing model&ndash;view chains.
   * <em>In general, you should not use this method directly, but should instead
   * use {@link ModelUtils#wrapAction(Model, Model.AtomizedModelAction)}.</em>
   * 
   * <p>
   * <em>If the action uses other models then care should be 
   * taken to a void a cycle, which could cause deadlock.</em>
   */
  public AtomizedModelAction atomizeAction(AtomizedModelAction action);

  /**
   * Interface for model actions.  See {@link Model#atomizeAction(AtomizedModelAction)}.
   */
  public interface AtomizedModelAction {
    /**
     * Execute an action on the given model.
     * 
     * @returns A list of events summarizing the effects of the action, if
     *          necessary. These events always follow any events that are
     *          generated during the action via manipulation of the model.
     *          If the action does not need to report additional events, 
     *          this should return {@link java.util.Collections#emptyList() an empty list}.
     */
    public List<ModelEvent> execute();
  }

  
  
  //===========================================================
  //== Model reflection methods
  //===========================================================

  /**
	 * Return a string representation of a node. This is model-specific, but it
	 * should approximate an attribute list, givin string representations of the
	 * all the attributes for which this node has a value.
	 * 
	 * @exception IllegalArgumentException
	 *              Thrown if <code>node</code> is not part of the model.
	 */
  public String toString(IRNode node);

  /**
	 * Return a string identifying the given node. This differs from
	 * {@link #toString(IRNode)}in that it is only meant to provide a name for a
	 * node, derived, for example, from an attribute value.
	 */
  public String idNode(IRNode node);

  /**
	 * Return a string representation of the value stored in the given attribute
	 * for the given node.
	 * 
	 * @exception UnknownAttributeException
	 *              Thrown if the attribute is not part of the model.
	 */
  public String nodeValueToString(IRNode node, String attr)
    throws UnknownAttributeException;

  /**
	 * Return a string representaiton of the value of a given component-level
	 * attribute.
	 * 
	 * @exception UnknownAttributeException
	 *              Thrown if the attribute is not part of the model.
	 */
  public String compValueToString(String attr)
    throws UnknownAttributeException;

  //===========================================================
  //== Model Listener Methods
  //===========================================================

  /**
	 * Adds a listener that is notified whenever the model is altered.
	 */
  public void addModelListener(ModelListener l);

  /**
	 * Removes a model listener.
	 */
  public void removeModelListener(ModelListener l);

  //===========================================================
  //== Query about the relationship between models
  //===========================================================

  /**
	 * Query if the model is above a view in a model&ndash;view chain.
	 */
  public boolean upChainFrom(View v);
}
