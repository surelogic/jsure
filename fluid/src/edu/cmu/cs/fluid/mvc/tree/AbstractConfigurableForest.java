// $Header:
// /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/AbstractConfigurableForest.java,v
// 1.15 2003/07/15 18:39:10 thallora Exp $

package edu.cmu.cs.fluid.mvc.tree;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.ir.IREnumeratedType.Element;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.visibility.VisibilityModel;
import edu.cmu.cs.fluid.util.*;
import static edu.cmu.cs.fluid.util.IteratorUtil.noElement;

/**
 * An abstract implementation of {@link ConfigurableForestView}. Provides the
 * minimal implementation of the interface, but is extensible. Building of the
 * exported model is abstracted into calls to {@link #setupNode(IRNode)}and
 * {@link #addSubtree(IRNode, IRNode)}to support the constraints of specialized Forests,
 * specifically SyntaxForests.
 * 
 * <p><em>For some reason there is no <code>ConfigurableForestViewCore</code>
 * class.  I don't know why this is.  Shouse probably take care of this
 * someday.  But not today (20 May 2004).</em>
 * 
 * <p>NOTE: Allows leaf nodes to be expande/collapse.  Not really a good
 * idea, but most renders won't offer this option anyway.
 * 
 * <p>
 * A non-abstract subclass must implement the {@link ConfigurableForestView}
 * interface.
 * 
 * @author Aaron Greenhouse
 */
@SuppressWarnings("unchecked")
public abstract class AbstractConfigurableForest
  extends AbstractForestToForestStatefulView {
  
  Object temp = DebugUnparser.class;
  
  //===========================================================
  //== Fields
  //===========================================================
  
  /**
   * Logger for this class
   */
  static final Logger LOG = SLLogger.getLogger("MV.tree.config");

  // What the heck is a marker?? -- Aaron, 20 May 2004
  /** Marker for ellipsis nodes */
  private static final String ellipsisMarker =
    "AbstractConfigurableForest.ellipsis";


  
  
  /** Reference to the enumeration type of the view mode attribute */
  private static final IREnumeratedType viewModeEnum =
    AbstractCore.newEnumType( ConfigurableForestView.VIEW_MODE_ENUM,
                 new String[] {
                       ConfigurableForestView.VIEW_FLATTENED,
                       ConfigurableForestView.VIEW_PATH_TO_ROOT,
                       ConfigurableForestView.VIEW_VERTICAL_ELLIPSIS } );
  
  private static final int FLATTENED_IDX = 0;
  private static final int PATH_TO_ROOT_IDX = 1;
  private static final int VERTICAL_ELLIPSIS_IDX = 2;
  
  
  
  /** The ConfigurableViewCore delegate object. */
  protected final ConfigurableViewCore configViewCore;

  /** The source visibility model. */
  protected final VisibilityModel srcVizModel;

  /** The source forest. */
  protected final ForestModel srcModel;

  /**
	 * Slot info for recording the expanded state of nodes when the tree is
	 * flattened.
	 */
  private final SlotInfo<Boolean> expandedFlat;

  /**
	 * Slot info for recording the expanded state of nodes when the tree is not
	 * flattened.
	 */
  private final SlotInfo<Boolean> expandedPath;

  /**
	 * SlotInfo that multi-plexes between <code>expandedFlat</code> and <code>expandedPath</code>.
	 */
  private final SlotInfo<Boolean> isExpanded;

  /**
	 * SlotInfo for storing the "renderAsParent" attribute values.
	 */
  private final SlotInfo<Boolean> renderAsParent;

  /** The attribute inheritance policy. */
  private final AttributeInheritancePolicy attrPolicy;

  //---------------- State used for renderering

  /** <code>true</code> iff the view mode is FLATTENED */
  private boolean isFlattened;

  /** the Attribute policy for ellided nodes */
  protected final ForestProxyAttributePolicy proxyPolicy;

  /** Stores if a node has a visible descendent in non-list view. */
  private final SlotInfo<Boolean> visibleDescendents;

  /** Count used when building model */
  private int numTrees = 0;

  /** The ellipsis policy attribute */
  private final ComponentSlot<ForestEllipsisPolicy> ellipsisAttr;
  
  /** The view mode attribute */
  private final ComponentSlot<IREnumeratedType.Element> viewModeAttr;

  /** The set of nodes currently decorating with proxies. */
  private final Set<IRNode> proxyWearing;

  //===========================================================
  //== Constructor
  //===========================================================

  protected AbstractConfigurableForest(
    final String name,
    final ForestModel src,
    final VisibilityModel vizModel,
    final ModelCore.Factory mf,
    final ViewCore.Factory vf,
    final ForestModelCore.Factory fmf,
    final ConfigurableViewCore.Factory cvf,
    final AttributeInheritancePolicy aip,
    final ForestProxyAttributePolicy pp,
    final ForestEllipsisPolicy ePolicy,
    final boolean ef,
    final boolean ep)
    throws SlotAlreadyRegisteredException {
    // Init model parts
    super(
      name,
      mf,
      vf,
      fmf,
      LocalAttributeManagerFactory.prototype,
      ProxySupportingAttributeManagerFactory.prototype);
    proxyWearing = new HashSet<IRNode>();

    srcModel = src;
    srcVizModel = vizModel;
    final AttributeChangedCallback nodeCB = new NodeAttrChangedCallback();
    configViewCore =
      cvf.create(
        name,
        this,
        structLock,
        srcModel,
        attrManager,
        inheritManager,
        nodeCB);
    attrPolicy = aip;
    proxyPolicy = pp;

    // Init model attributes
    final AttributeChangedCallback cb2 = new ForestViewChangedCallback();
    ellipsisAttr =
      new SimpleComponentSlot<ForestEllipsisPolicy>(
        ForestEllipsisPolicyType.prototype,
        SimpleExplicitSlotFactory.prototype,
        ePolicy);
    attrManager.addCompAttribute(
      ConfigurableForestView.ELLIPSIS_POLICY,
      Model.STRUCTURAL,
      ellipsisAttr,
      cb2);

    final Element initViewMode = viewModeEnum.getElement(PATH_TO_ROOT_IDX);
    viewModeAttr =
      new SimpleComponentSlot<IREnumeratedType.Element>(
        viewModeEnum,
        SimpleExplicitSlotFactory.prototype,
        initViewMode);
    attrManager.addCompAttribute(
      ConfigurableForestView.VIEW_MODE,
      Model.STRUCTURAL,
      viewModeAttr,
      cb2);
    updateShadowViewState(initViewMode);
    
    configViewCore.setSourceModels(srcModel, viewCore, srcVizModel);

    // Init the Node attributes
    final SlotFactory ssf = SimpleSlotFactory.prototype;
    expandedFlat =
      ssf.newAttribute(
        name + "-expandedFlattened",
        IRBooleanType.prototype,
        (ef ? Boolean.TRUE : Boolean.FALSE));
    expandedPath =
      ssf.newAttribute(
        name + "-expandedPathToRoot",
        IRBooleanType.prototype,
        (ep ? Boolean.TRUE : Boolean.FALSE));
    isExpanded =
      new IsExpandedSlotInfo(name + "-" + ConfigurableForestView.IS_EXPANDED);
    attrManager.addNodeAttribute(
      ConfigurableForestView.IS_EXPANDED,
      Model.STRUCTURAL,
      isExpanded,
      nodeCB);

    renderAsParent =
      ssf.newAttribute(
        name + "-" + ConfigurableForestView.RENDER_AS_PARENT,
        IRBooleanType.prototype,
        Boolean.FALSE);
    attrManager.addNodeAttribute(
      ConfigurableForestView.RENDER_AS_PARENT,
      Model.STRUCTURAL,
      renderAsParent);

    inheritManager.inheritAttributesFromModel(
      srcModel,
      attrPolicy,
      AttributeChangedCallback.nullCallback);

    // Initialize the model contents
    visibleDescendents = ssf.newAttribute(Boolean.FALSE);

    // Connect model-view chain
    // Do not add listener to the regular srcModel because
    // the visibility model also breaks when the srcModel does
    // so if we just listen to the visibility model things will
    // work. If we listen to both we get double rebuilds which are
    // annoying. CAVEAT: VisibilityModel must always listen to
    // the source model.
    // srcModel.addModelListener( ml );
    srcVizModel.addModelListener(srcModelBreakageHandler);
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin Inner Classes
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  /**
	 * DerivedSlotInfo that multiplexex between {@link #expandedFlat} and
	 * {@link #expandedPath} based on the value of {@link #isFlattened}.
	 */
  private class IsExpandedSlotInfo extends DerivedSlotInfo<Boolean> {
    public IsExpandedSlotInfo(final String name)
      throws SlotAlreadyRegisteredException {
      super(name, IRBooleanType.prototype);
    }

    @Override
    protected Boolean getSlotValue(final IRNode node) {
      return node.getSlotValue(isFlattened ? expandedFlat : expandedPath);
    }

    @Override
    protected void setSlotValue(final IRNode node, final Boolean val) {
      node.setSlotValue((isFlattened ? expandedFlat : expandedPath), val);
    }

    @Override
    protected boolean valueExists(final IRNode node) {
      return node.valueExists(isFlattened ? expandedFlat : expandedPath);
    }

    @Override
    public ImmutableSet<IRNode> index(final Boolean val) {
      return (isFlattened ? expandedFlat : expandedPath).index(val);
    }
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End Inner Classes
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin Local State
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //===========================================================
  //== Callbacks
  //===========================================================

  private class ForestViewChangedCallback
    extends AbstractAttributeChangedCallback {
    @Override
    protected void attributeChangedImpl(
      final String attr,
      final IRNode node,
      final Object val) {
      boolean broken = false;

      if (attr == ConfigurableForestView.VIEW_MODE) {
        /*
				 * XXX Problem, this update of the local state does not occur in the
				 * XXX same critical section as the attribute value update.
				 */
        updateShadowViewState((IREnumeratedType.Element) val);
        broken = true;
      } else if (attr == ConfigurableForestView.ELLIPSIS_POLICY) {
        broken = true;
      }
      if (broken) {
        signalBreak(
          new AttributeValuesChangedEvent(
            AbstractConfigurableForest.this,
            node,
            attr,
            val));
      }
    }
  }

  private class NodeAttrChangedCallback
    extends AbstractAttributeChangedCallback {
    @Override
    protected void attributeChangedImpl(
      final String attr,
      final IRNode node,
      final Object val) {
      if (attr == ConfigurableView.IS_HIDDEN
        || attr == ConfigurableForestView.IS_EXPANDED) {
        signalBreak(
          new AttributeValuesChangedEvent(
            AbstractConfigurableForest.this,
            node,
            attr,
            val));
      }
    }
  }

  //===========================================================
  //== Tree Presentation Parameters
  //===========================================================

  /**
   * Update the local shadow view state.
   */
  private void updateShadowViewState(final IREnumeratedType.Element mode) {
    isFlattened = mode.equals(viewModeEnum.getElement(FLATTENED_IDX));
  }
  
  /**
	 * Toggle the flattened state of the tree. 
	 */
  public final void setViewFlattened() {
    setViewMode(viewModeEnum.getElement(FLATTENED_IDX));
  }

  /**
	 * Toggle the path to root state of the tree. A tree in path to root mode
	 * makes sure that all the ancestors of a visible node are visible.
	 */
  public final void setViewPathToRoot() {
    setViewMode(viewModeEnum.getElement(PATH_TO_ROOT_IDX));
  }

  /**
   * Set the view mode to be vertical ellipsis.
   */
  public final void setViewVerticalEllipsis() {
    setViewMode(viewModeEnum.getElement(VERTICAL_ELLIPSIS_IDX));
  }
  
  /**
   * </code>true</code> iff the view mode is flattened.
   */
  public boolean isViewFlattened() {
    return viewModeEnum.getElement(FLATTENED_IDX).equals(getViewMode());
  }

  /**
   * </code>true</code> iff the view mode is path to root.
   */
  public boolean isViewPathToRoot() {
    return viewModeEnum.getElement(PATH_TO_ROOT_IDX).equals(getViewMode());
  }

  /**
   * </code>true</code> iff the view mode is vertical ellipsis.
   */
  public boolean isViewVerticalEllipsis() {
    return viewModeEnum.getElement(VERTICAL_ELLIPSIS_IDX).equals(getViewMode());
  }

  /** Get the visibility Element corresponding to the String */
  public final IREnumeratedType.Element getEnumElt(final String name)
  {
    final int idx = viewModeEnum.getIndex(name);
    return (idx == -1) ? null : viewModeEnum.getElement(idx);
  }

  /**
   * Set the view mode.
   * @param mode The view mode.
   */
  public final void setViewMode(final IREnumeratedType.Element mode) {
    synchronized (structLock) {
      viewModeAttr.setValue(mode);
      updateShadowViewState(mode);
    }
    signalBreak(
      new AttributeValuesChangedEvent(
        this,
        ConfigurableForestView.VIEW_FLATTENED,
        mode));
  }
  
  /** Get the view mode. */
  public IREnumeratedType.Element getViewMode() {
    synchronized (structLock) {
      return viewModeAttr.getValue();
    }
  }
  
  //===========================================================
  //== Ellipsis Control
  //===========================================================

  /**
	 * Set the ellipsis policy.
	 */
  public final void setForestEllipsisPolicy(final ForestEllipsisPolicy p) {
    synchronized (structLock) {
      ellipsisAttr.setValue(p);
    }
    signalBreak(
      new AttributeValuesChangedEvent(
        this,
        ConfigurableForestView.ELLIPSIS_POLICY,
        p));
  }

  /**
	 * Get the ellipsis policy
	 */
  public final ForestEllipsisPolicy getForestEllipsisPolicy() {
    synchronized (structLock) {
      return ellipsisAttr.getValue();
    }
  }

  //===========================================================
  //== Node Hiding methods
  //===========================================================

  /**
	 * Interface for factories that generator iterators over a given ForestModel
	 * based on a particular root node.
	 */
  protected static interface GetIter {
    /**
		 * Return an iterator over nodes "related" to the given root node in the
		 * given ForestModel.
		 */
    public Iterator<IRNode> get(ForestModel fm, IRNode root);
  }

  /**
	 * Iterator factory prototype that returns a top-down iteration of the
	 * subtree rooted at the given root node.
	 */
  private static final GetIter getSubtree = new GetIter() {
    @Override
    public final Iterator<IRNode> get(final ForestModel fm, final IRNode root) {
      return fm.topDown(root);
    }
  };

  /**
	 * Iterator factory prototype that returns an in-order iteration of the
	 * children of the given node.
	 */
  private static final GetIter getChildren = new GetIter() {
    @Override
    public final Iterator<IRNode> get(final ForestModel fm, final IRNode root) {
      return fm.children(root);
    }
  };

  /**
	 * Iterator factory prototype that returns an in-order iteration of the
	 * siblings of the given node.
	 */
  private static final GetIter getSiblings = new GetIter() {
    @Override
    public final Iterator<IRNode> get(final ForestModel fm, final IRNode root) {
      final IRNode parent = fm.getParent(root);
      if (parent == null) {
        return new EmptyIterator<IRNode>();
      } else {
        return new FilterIterator<IRNode,IRNode>(fm.children(parent)) {
          final boolean debug = LOG.isLoggable(Level.FINE);
          @Override
          protected final Object select(final IRNode o) {            
            if (o.equals(root)) {
              if (debug) {
                LOG.fine("Skipping " + o);
              }
              return noElement;
            }
            return o;
          }
        };
      }
    }
  };

  /**
	 * Iterator factory prototype that returns a top-down in-order iteration of
	 * the sub-trees rooted at the siblings of the given node.
	 */
  private static final GetIter getSiblingSubs = new GetIter() {
    @Override
    public final Iterator<IRNode> get(final ForestModel fm, final IRNode root) {
      return new ProcessIterator<IRNode>(getSiblings.get(fm, root)) {
        final boolean debug = LOG.isLoggable(Level.FINE);
        @Override
        protected final Iterator<IRNode> getNextIter(final Object o) {
          if (debug) {
            LOG.fine("Looking at subtree for " + o);
          }
          return getSubtree.get(fm, (IRNode) o);
        }
      };
    }
  };

  private void setHiddenForEnum(
    final GetIter ge,
    final IRNode root,
    final boolean hidden) {
    synchronized (structLock) {
      final Iterator<IRNode> nodes = ge.get(srcModel, root);
      while (nodes.hasNext()) {
        final IRNode node = nodes.next();
        configViewCore.setHidden(node, hidden);
      }
    }
    signalBreak(new ModelEvent(this));
  }

  /**
	 * Set the hidden status for an entire sub-tree.
	 * 
	 * @param root
	 *          The root of the sub-tree
	 * @param hidden
	 *          <code>true</code> if the sub-tree should be hidden; <code>false</code>
	 *          if it should be shown.
	 */
  public final void setHiddenForSubtree(
    final IRNode root,
    final boolean hidden) {
    setHiddenForEnum(getSubtree, root, hidden);
  }

  /**
	 * Set the hidden status for an a node's children
	 * 
	 * @param root
	 *          The root of the sub-tree
	 * @param hidden
	 *          <code>true</code> if the sub-tree should be hidden; <code>false</code>
	 *          if it should be shown.
	 */
  public final void setHiddenForChildren(
    final IRNode root,
    final boolean hidden) {
    setHiddenForEnum(getChildren, root, hidden);
  }

  /**
	 * Set the hidden status for an a node's siblings
	 * 
	 * @param root
	 *          The root of the sub-tree
	 * @param hidden
	 *          <code>true</code> if the sub-tree should be hidden; <code>false</code>
	 *          if it should be shown.
	 */
  public final void setHiddenForSiblings(
    final IRNode root,
    final boolean hidden) {
    setHiddenForEnum(getSiblings, root, hidden);
  }

  /**
	 * Set the hidden status for an a node's siblings' subtrees
	 * 
	 * @param root
	 *          The root of the sub-tree
	 * @param hidden
	 *          <code>true</code> if the sub-tree should be hidden; <code>false</code>
	 *          if it should be shown.
	 */
  public final void setHiddenForSiblingSubtrees(
    final IRNode root,
    final boolean hidden) {
    setHiddenForEnum(getSiblingSubs, root, hidden);
  }

  private boolean areSomeHiddenForEnum(
    final GetIter ge,
    final IRNode root,
    final boolean hidden) {
    synchronized (structLock) {
      final Iterator<IRNode> nodes = ge.get(srcModel, root);
      while (nodes.hasNext()) {
        final IRNode node = nodes.next();
        if (configViewCore.isHidden(node) == hidden) {
          return true;
        }
      }
    }
    return false;
  }

  /**
	 * Check the hidden status for an entire sub-tree.
	 * 
	 * @param root
	 *          The root of the sub-tree
	 * @param hidden
	 *          <code>true</code> if looking for hidden nodes ;<code>false</code>
	 *          if looking for visible nodes
	 */
  public final boolean areSomeHiddenForSubtree(
    final IRNode root,
    final boolean hidden) {
    return areSomeHiddenForEnum(getSubtree, root, hidden);
  }

  /**
	 * Check the hidden status for a node's children
	 * 
	 * @param root
	 *          The root of the sub-tree
	 * @param hidden
	 *          <code>true</code> if looking for hidden nodes ;<code>false</code>
	 *          if looking for visible nodes
	 */
  public final boolean areSomeHiddenForChildren(
    final IRNode root,
    final boolean hidden) {
    return areSomeHiddenForEnum(getChildren, root, hidden);
  }

  /**
	 * Check the hidden status for a node's siblings.
	 * 
	 * @param root
	 *          The root of the sub-tree
	 * @param hidden
	 *          <code>true</code> if looking for hidden nodes ;<code>false</code>
	 *          if looking for visible nodes
	 */
  public final boolean areSomeHiddenForSiblings(
    final IRNode root,
    final boolean hidden) {
    return areSomeHiddenForEnum(getSiblings, root, hidden);
  }

  /**
	 * Check the hidden status for the subtrees rooted by a node's siblings.
	 * 
	 * @param root
	 *          The root of the sub-tree
	 * @param hidden
	 *          <code>true</code> if looking for hidden nodes ;<code>false</code>
	 *          if looking for visible nodes
	 */
  public final boolean areSomeHiddenForSiblingSubtrees(
    final IRNode root,
    final boolean hidden) {
    return areSomeHiddenForEnum(getSiblingSubs, root, hidden);
  }

  //===========================================================
  //== Node Expansion methods
  //===========================================================

  /**
	 * Set the expanded state of a node.
	 * 
	 * @param flag
	 *          <code>true</code> if the node should be presented expanded;
	 *          <code>false</code> if it should be presented collapsed.
	 */
  public final void setExpanded(final IRNode node, final boolean flag) {
    final Boolean val = flag ? Boolean.TRUE : Boolean.FALSE;
    synchronized (structLock) {
      node.setSlotValue(isExpanded, val);
    }
    signalBreak(
      new AttributeValuesChangedEvent(
        this,
        node,
        ConfigurableForestView.IS_EXPANDED,
        val));
  }

  /**
	 * Returns <code>true</code> iff the node is being presented expanded. This
	 * must also return the opposide of <code>isCollapsed</code>.
	 */
  public final boolean isExpanded(final IRNode node) {
    synchronized (structLock) {
      final Boolean b = node.getSlotValue(isExpanded);
      return b.booleanValue();
    }
  }

  /**
	 * Returns <code>true</code> iff the node is being presented collapsed.
	 * This must also return the opposide of <code>isExpanded</code>.
	 */
  public final boolean isCollapsed(final IRNode node) {
    return !isExpanded(node);
  }

  /**
	 * Toggle the expanded state of a node. Makes an expanded node collapsed; a
	 * collapsed node becomes expanded.
	 */
  public final boolean toggleExpanded(final IRNode node) {
    Boolean newState;
    synchronized (structLock) {
      newState = !isExpanded(node) ? Boolean.TRUE : Boolean.FALSE;
      node.setSlotValue(isExpanded, newState);
    }
    signalBreak(
      new AttributeValuesChangedEvent(
        this,
        node,
        ConfigurableForestView.IS_EXPANDED,
        newState));
    return newState.booleanValue();
  }

  /**
	 * Set the expanded state of an entire sub-tree.
	 * 
	 * @param root
	 *          The root of the sub-tree to operate on.
	 * @param flag
	 *          The state to set all the nodes in the given sub-tree.
	 */
  public final void setExpandedForSubtree(final IRNode root, boolean flag) {
    synchronized (structLock) {
      final Boolean bFlag = flag ? Boolean.TRUE : Boolean.FALSE;
      final Iterator<IRNode> nodes = srcModel.topDown(root);
      while (nodes.hasNext()) {
        final IRNode node = nodes.next();
        node.setSlotValue(isExpanded, bFlag);
      }
    }
    signalBreak(new ModelEvent(this));
  }

  //===========================================================
  //== Convienence Methods for the rendering attributes
  //===========================================================

  public final boolean shouldRenderAsParent(final IRNode node) {
    synchronized (structLock) {
      final Boolean b = node.getSlotValue(renderAsParent);
      return b.booleanValue();
    }
  }

  /** Caller must hold structural lock. */
  private void setRenderAsParent(final IRNode node, final boolean b) {
    node.setSlotValue(renderAsParent, b ? Boolean.TRUE : Boolean.FALSE);
  }

  //===========================================================
  //== Local State: Does a node have a descendent that must
  //== be displayed?
  //===========================================================

  private boolean isParentInSrcModel(final IRNode node) {
    return (srcModel.numChildren(node) != 0);
  }

  /**
	 * Return if a node has a descendent that must be displayed.
	 */
  private boolean hasVisibleDesc(final IRNode node) {
    if (node == null)
      return false;
    final Boolean b = node.getSlotValue(visibleDescendents);
    return b.booleanValue();
  }

  /**
	 * Set whether a node has a descendent that must be displayed.
	 */
  private void setVisibleDesc(final IRNode node, final boolean b) {
    node.setSlotValue(visibleDescendents, b ? Boolean.TRUE : Boolean.FALSE);
  }

  /**
	 * Walk the src model using a post-fix traversal starting and the given node
	 * and compute whether each node has a descendent that must be displayed.
	 */
  private boolean computeVisibleDescendents(final IRNode node)
    throws InterruptedException {
    if (rebuildWorker.isInterrupted())
      throw cannedInterrupt;

    if (node == null)
      return false;
    else {
      if (srcModel.hasChildren(node)) {
        final Iterator<IRNode> nodes = srcModel.children(node);
        boolean vd = false;
        try {
          while (nodes.hasNext()) {
            final IRNode c = nodes.next();
            vd |= computeVisibleDescendents(c);
          }
        } catch (SlotUndefinedException e) {
          // e.printStackTrace();
        }
        setVisibleDesc(node, vd);
        return vd || isNodeVisible(node);
      } else {
        setVisibleDesc(node, false);
        return isNodeVisible(node);
      }
    }
  }

  //===========================================================
  //== Rebuild methods
  //===========================================================

  /**
	 * This causes the source model to be traversed and the sub-model to be
	 * built.
	 */
  @Override
  protected final void rebuildModel(final List events)
    throws InterruptedException {
    final boolean debug = LOG.isLoggable(Level.FINE);
    
    LOG.info("Rebuilding " + this.getName());
    synchronized (structLock) {
      // Clear the existing tree model...
      forestModCore.clearForest();

      // Remove proxy cruft
      final Iterator<IRNode> proxyIter = proxyWearing.iterator();
      while (proxyIter.hasNext()) {
        final IRNode n = proxyIter.next();
        configViewCore.removeProxyFrom(n);
        proxyIter.remove();
      }

      // Reset the ellipsis policy
      final ForestEllipsisPolicy ellipsisPolicy = getForestEllipsisPolicy();
      if (ellipsisPolicy != null)
        ellipsisPolicy.resetPolicy();
      
      // Build the new model
      numTrees = 0;
      final Iterator<IRNode> roots = srcModel.getRoots();
      while (roots.hasNext()) {
        final IRNode root = roots.next();

        if (isViewPathToRoot()) {
          if (debug) {
            LOG.fine("Computing path to " + root);
          }
          computeVisibleDescendents(root);
          buildPathToRoot(ellipsisPolicy, root, null, 0, 0);
        } else if (isViewFlattened()){
          if (debug) {
            LOG.fine("Computing flat CFV for " + root);
          }
          buildFlattened(ellipsisPolicy, root, true, null, true, 0, 0);
        } else { // vertical-ellipsis
          if (debug) {
            LOG.fine("Computing vertical ellipsis for " + root);
          }
          final ForestVerticalEllipsisPolicy ep = getVerticalEllipsisPolicy();
          ep.resetPolicy();
          buildVerticalEllipsis(ep, root, null, true, 0, 0);
          ep.applyPolicy();
        }
      }

      // Insert ellipses
      if (rebuildWorker.isInterrupted())
        throw cannedInterrupt;
      if (ellipsisPolicy != null)
        ellipsisPolicy.applyPolicy();

      forestModCore.precomputeNodes();
    }

    LOG.info("Finished rebuilding CFV");

    // Break our views
    modelCore.fireModelEvent(new ModelEvent(this));
  }

  protected ForestVerticalEllipsisPolicy getVerticalEllipsisPolicy() {
//  final ForestVerticalEllipsisPolicy ep = new SingleEllipsisVerticalEllipsisPolicy((ConfigurableForestView) this, false, true);
    final ForestVerticalEllipsisPolicy ep = new MultipleEllipsisVerticalEllipsisPolicy((ConfigurableForestView) this, true);
//    final ForestVerticalEllipsisPolicy ep = NoEllipsisVerticalEllipsisPolicy.prototype;
    return ep;
  }

  /**
   * Builds a sub-tree of the sub-model in veritical ellipsis mode.
   * 
   * @param node
   *          The node to consider for adding to the sub-model.
   * @param parent
   *          The node that should be <code>node</code>'s parent in the
   *          sub-model if <code>node</code> is added.
   * @param sameParent 
   *          Whether <code>parent</code> is the same node that is 
   *          <code>node</code>'s parent in the source model.
   * @param newPos
   *          The position <code>node</code> will have in its parent's
   *          children list if it is added.
   * @param oldPos
   *          The position <code>node</code> had in its parent's
   *          children list in the source model.
   * @return <code>true</code> iff <code>node</code> was added to the
   *         exported model.
   */
  private boolean buildVerticalEllipsis(
      final ForestVerticalEllipsisPolicy ellipsisPolicy, final IRNode node,
      final IRNode parent, final boolean sameParent, final int newPos, final int oldPos)
      throws InterruptedException {
    /* Abort if node is null. Restart rebuild if we've been interrupted */
    if (node == null) return false;
    if (rebuildWorker.isInterrupted())
      throw cannedInterrupt;
    
    final boolean added;
    final IRNode nextParent;
    
    /*
     * Add a node if its ancestors are expanded (the fact that we get here) and
     * the node is visible (isNodeVisible(node) == true).
     */
    if (isNodeVisible(node)) {
      addNodeToExportedModel(node, parent, sameParent, oldPos);
      // We added the node, it becomes the next parent
      added = true;
      nextParent = node;
    } else {
      // node not added
      added = false;
      
      if (srcVizModel.isVisible(node)) {
        // only add ellipsis if node is locally hidden
        if (ellipsisPolicy != null) {
          // Ellipsis policy determines the next parent
          nextParent = ellipsisPolicy.nodeSkipped(node, parent,
              (parent == null) ? numTrees : newPos, oldPos);
        } else {
          // no ellipsis policy, use incoming parent as next parent
          nextParent = parent;
        }
      } else {
        // Don't add ellipsis if node is hidden in the visibility model
        // Use incoming parent as next parent
        nextParent = parent;
      }
    }

    if (isExpanded(node)) {
      final Iterator children = srcModel.children(node);
      int childPos = 0;
      for(int idx = 0; children.hasNext(); idx++) {
        final IRNode c = (IRNode) children.next();
        if (c == null)
          continue;
        final boolean addedChild = buildVerticalEllipsis(ellipsisPolicy, c,
            nextParent, (nextParent.equals(node)), childPos, idx);
        // Is this right?
        if (addedChild && (nextParent != null)) childPos += 1;
      }
    }
    if (added) {
      /* This works only because we only add proxy nodes to collapsed
       * nodes, and we don't recurse down collapsed nodes.  If we did,
       * we would have a problem because the ellipsis nodes have not
       * yet been attached to the rest of the tree so the proxy 
       * information would not be accurate.
       */
      updateRendererAndProxyAttributes(node);
    }

    return added;
  }

  /**
	 * Builds a sub-tree of the sub-model in path-to-root mode.
	 * 
	 * @param node
	 *          The node to consider for adding to the sub-model.
	 * @param parent
	 *          The node that should be <code>node</code>'s parent in the
	 *          sub-model if <code>node</code> is added.
   * @param newPos
   *          The position <code>node</code> will have in its parent's
   *          children list if it is added.
   * @param oldPos
   *          The position <code>node</code> had in its parent's
   *          children list in the source model.
	 * @return <code>true</code> iff <code>node</code> was added to the
	 *         exported model.
	 */
  private boolean buildPathToRoot(
    final ForestEllipsisPolicy ellipsisPolicy,
    final IRNode node,
    final IRNode parent,
    final int newPos, final int oldPos)
    throws InterruptedException {
    if (node == null) return false;
    
    if (rebuildWorker.isInterrupted())
      throw cannedInterrupt;
    boolean added = false;

    /*
		 * Add a node only if its ancestors are expanded (unhidden == true). This
		 * effectively ends the traversal down the current branch when unhidden ==
		 * false. If all the ancestors are expanded, and the node is visible in its
		 * own right, or it is forced to be visible by having a visible descendent,
		 * then it is added to the exported model.
		 */
    if ((hasVisibleDesc(node) || isNodeVisible(node))) {
      addNodeToExportedModel(node, parent, true, oldPos);
      added = true;
    } else if (ellipsisPolicy != null) {
      if (srcVizModel.isVisible(node)) {
        ellipsisPolicy.nodeSkipped(
          node,
          parent,
          (parent == null) ? numTrees : newPos);
      }
    }

    /* 
     * We can only skip a node if the node has no visible descendents.  Thus,
     * if we skipped a node, we will also skip its descendents, so don't 
     * bother with the recursion.
     * (Added this short circuit 24 June 2004)
     */
    if (added) {
      if (isExpanded(node)) {
        final Iterator children = srcModel.children(node);
        int childPos = 0;
        for(int idx = 0; children.hasNext(); idx++) {
          final IRNode c = (IRNode) children.next();
          if (c == null)
            continue;
          final boolean addedChild =
            buildPathToRoot(ellipsisPolicy, c, node, childPos, idx);
          if (addedChild) childPos += 1;
        }
      }
      if (added) updateRendererAndProxyAttributes(node);
    }
    
    return added;
  }

  /**
	 * Builds a sub-tree of the sub-model in flattened mode.
	 * 
	 * @param node
	 *          The node to consider for adding to the sub-model.
	 * @param unhidden
	 *          <code>true</code> iff <code>node</code> has an ancester that
	 *          was added to the sub-model and all ancestors between that node
	 *          and <code>node</code> are expanded.
	 * @param parent
	 *          The node that should be <code>node</code>'s parent in the
	 *          sub-model if <code>node</code> is added.
   * @param sameParent 
   *          Whether <code>parent</code> is the same node that is 
   *          <code>node</code>'s parent in the source model.
   * @param newPos
   *          The position <code>node</code> will have in its parent's
   *          children list if it is added.
   * @param oldPos
   *          The position <code>node</code> had in its parent's
   *          children list in the source model.
	 * @return <code>true</code> iff <code>node</code> was added to the
	 *         exported model.
	 */
  private boolean buildFlattened(
    final ForestEllipsisPolicy ellipsisPolicy,
    final IRNode node,
    final boolean unhidden,
    final IRNode parent, final boolean sameParent,
    final int newPos, final int oldPos)
    throws InterruptedException {
    if (rebuildWorker.isInterrupted())
      throw cannedInterrupt;
    boolean added = false;

    /*
		 * Add the node if it is visible in its own right, or it is has expanded,
		 * visible ancestors. This will force otherwise invisible nodes to be shown
		 * if they have an expanded ancestor in the exported model.
		 */
    if ((node != null) && (isNodeVisible(node) || unhidden)) {
      addNodeToExportedModel(node, parent, sameParent, oldPos);
      added = true;
    } else if (ellipsisPolicy != null) {
      if (srcVizModel.isVisible(node)) {
        ellipsisPolicy.nodeSkipped(
          node,
          parent,
          ((parent == null) ? numTrees : newPos));
      }
    }

    /*
		 * Unhidden becomes false once we hit an unexpanded node, and then it stays
		 * false.
		 */
    //    final boolean newUnhidden = isExpanded( node ) && unhidden;
    final boolean newUnhidden = added ? isExpanded(node) : unhidden;

    /*
		 * If we added the node and it is expanded, then we will put its children
		 * underneath it. Otherwise (we didn't at the node, or it is not expanded)
		 * the children become roots in the exported model.
		 */
    final IRNode newParent = (added && isExpanded(node)) ? node : null;

    final Iterator<IRNode> children = srcModel.children(node);
    int childPos = 0;
    for (int idx = 0; children.hasNext(); idx++) {
      final IRNode c = children.next();
      if (c == null)
        continue;
      final boolean addedChild =
        buildFlattened(ellipsisPolicy, c, newUnhidden, newParent, 
            (newParent.equals(node)), childPos, idx);
      if (addedChild && (newParent != null))
        childPos += 1;
    }
    if (added)
      updateRendererAndProxyAttributes(node);

    return added;
  }

  /**
   * Add a node to the exported model.
   * 
   * @param node
   *          The node to add
   * @param parent
   *          The parent of <code>node</code>
   * @param sameParent
   *          Whether <code>parent</code> is <code>node</code>'s parent
   *          in the source model.
   * @param idx
   *          The position <code>node</code> had in the children list of of
   *          it's parent in the original model (this is not necessarily the
   *          same node as <code>parent</code>).
   */
  private void addNodeToExportedModel(
      final IRNode node, final IRNode parent,
      final boolean sameParent, final int idx) {
    if (!forestModCore.isNode(node)) {
      // LOG.debug("Setting up "+node+" for exported model");
      setupNode(node);
    } else {
      //      if (LOG.isDebugEnabled()) {
      //        LOG.debug("Clearing the children/cruft for "+node);
      //      }

      // clear the children, to get rid of cruft in exported model
      // forestModCore.clearParent( node );
      forestModCore.removeChildren(node);
    }
    // append the node to the children list of its parent
    // or make it a root, as required
    if (parent == null) {
      //    	if (LOG.isDebugEnabled()) {
      //        LOG.debug("Adding as root: "+node);
      //      }

      forestModCore.addRoot(node);
      numTrees += 1;
    } else {
      //    	if (LOG.isDebugEnabled()) {
      //        LOG.debug("Adding as node: "+node);
      //      }

      addSubtree(parent, node, sameParent, idx);
    }
  }

  private void updateRendererAndProxyAttributes(final IRNode node) {
    // make sure the node is rendered as a parent node
    final boolean isParent = isParentInSrcModel(node);
    setRenderAsParent(node, isParent);

    // if the node is a collapsed parent node, add a proxy
    if (isParent && isCollapsed(node)) {
      proxyWearing.add(node);
      configViewCore.setProxyNodeAttributes(
        configViewCore.generateProxyFor(node),
        proxyPolicy.attributesFor((ForestModel) this, node));
    }
  }

  //===========================================================
  //== Ellipsis Insertion
  //===========================================================

  /**
   * Create an ellipsis node.  Only to be used ellipsis policies!
   */
  public final IRNode createEllipsisNode() {
    final IRNode ellipsis = new MarkedIRNode(ellipsisMarker);
    setupEllipsisNode(ellipsis);
    modelCore.setEllipsis(ellipsis, true);
    // Ellipsis should not be rendered as a parent
    setRenderAsParent(ellipsis, false);
    return ellipsis;
  }
  
  // Model will/must already be locked when this is called
  public final void insertEllipsisAt(
      final IRNode ellipsis,
    final IRNode parent,
    final InsertionPoint ip,
    final Set<IRNode> nodes) {
//    final IRNode ellipsis = createEllipsisNode();
    if (parent == null)
      forestModCore.insertRootAt(ellipsis, ip);
    else
      forestModCore.insertChild(parent, ellipsis, ip);
    finalizeEllipsis(parent, ellipsis, nodes);
  }

  // Model will/must already be locked when this is called
  public final void insertEllipsis(final IRNode ellipsis, final IRNode parent, final Set<IRNode> nodes) {
//    final IRNode ellipsis = createEllipsisNode();
    if (parent == null)
      forestModCore.insertRoot(ellipsis);
    else
      forestModCore.insertSubtree(parent, ellipsis);
    finalizeEllipsis(parent, ellipsis, nodes);
  }

  // Model will/must already be locked when this is called
  public final void appendEllipsis(final IRNode ellipsis, final IRNode parent, final Set<IRNode> nodes) {
//    final IRNode ellipsis = createEllipsisNode();
    if (parent == null)
      forestModCore.addRoot(ellipsis);
    else
      addSubtree(parent, ellipsis, false, -1);
    finalizeEllipsis(parent, ellipsis, nodes);
  }

  protected final void finalizeEllipsis(
    final IRNode parent,
    final IRNode ellipsis,
    final Set<IRNode> nodes) {
    modelCore.setEllidedNodes(ellipsis, nodes);

    // update renderer attributes
    // Parent should now be rendered as a parent (if it wasn't already)
    if (parent != null)
      setRenderAsParent(parent, true);

    // Init the ellipsis's proxy node
    proxyWearing.add(ellipsis);
    configViewCore.setProxyNodeAttributes(
      configViewCore.generateProxyFor(ellipsis),
      proxyPolicy.attributesFor(this, nodes));
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End Local State
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin ConfigurableView Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  public final void setHiddenForAllNodes(final boolean notVisible) {
    synchronized (structLock) {
      configViewCore.setHiddenForAllNodes(srcModel, notVisible);
    }
    signalBreak(new ModelEvent(this));
  }

  public final boolean isProxyNode(final IRNode node) {
    synchronized (structLock) {
      return configViewCore.isProxyNode(node);
    }
  }

  public final IRNode getProxyNode(final IRNode node) {
    synchronized (structLock) {
      return configViewCore.getProxyNode(node);
    }
  }

  public final boolean isHidden(final IRNode node) {
    synchronized (structLock) {
      return configViewCore.isHidden(node);
    }
  }

  public final boolean isShown(final IRNode node) {
    synchronized (structLock) {
      return configViewCore.isShown(node);
    }
  }

  public final void setHidden(final IRNode node, final boolean isHidden) {
    synchronized (structLock) {
      configViewCore.setHidden(node, isHidden);
    }
    signalBreak(
      new AttributeValuesChangedEvent(
        this,
        node,
        ConfigurableView.IS_HIDDEN,
        (isHidden ? Boolean.TRUE : Boolean.FALSE)));
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End ConfigurableView Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin Model Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  /**
	 * Override implementation to return <code>true</code> if the node is a
	 * proxy node.
	 */
  @Override
  public final boolean isOtherwiseAttributable(final IRNode node) {
    synchronized (structLock) {
      return configViewCore.isProxyNode(node);
    }
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End Model Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  private boolean isNodeVisible(final IRNode node) {
    return !isHidden(node) && srcVizModel.isVisible(node);
  }

  protected abstract void setupNode(IRNode n);

  protected abstract void setupEllipsisNode(IRNode n);

  protected abstract void addSubtree(
      IRNode parent, IRNode n, boolean sameParent, int idx);
}
