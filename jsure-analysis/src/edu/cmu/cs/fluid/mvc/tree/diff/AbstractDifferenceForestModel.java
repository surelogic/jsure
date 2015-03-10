/*
 * $Header:
 * /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/diff/AbstractDifferenceForestModel.java,v
 * 1.16 2003/07/15 18:39:13 thallora Exp $
 */
package edu.cmu.cs.fluid.mvc.tree.diff;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidRuntimeException;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.diff.DifferenceModelCore;
import edu.cmu.cs.fluid.mvc.tree.AbstractForestToForestStatefulView;
import edu.cmu.cs.fluid.mvc.tree.ForestModel;
import edu.cmu.cs.fluid.mvc.tree.ForestModelCore;
/**
 * Abstract implementation of a ForestStatefulView that views two forests and
 * exports a forest that is their difference. Concrete implementations should
 * define attributes that contain the semantics of the difference.
 * 
 * Subclasses need to create the DIFF_LOCAL attribute and init the local change
 * enumeration.
 * 
 * @author Aaron Greenhouse
 */
@SuppressWarnings("unchecked")
public abstract class AbstractDifferenceForestModel
  extends AbstractForestToForestStatefulView {
  /**
	 * Logger for difference models.
	 */
  public static final Logger LOG = SLLogger.getLogger("MV.diff");

  //===========================================================
  //== Fields
  //===========================================================

  /**
	 * The attribute manager for merging attributes of source models.
	 */
  protected final AttributeMergingManager mergingManager;

  /**
	 * The source forest that is to be treated as the base. The difference
	 * describes how things have changed relative to this model.
	 */
  protected final ForestModel baseForest;

  /**
	 * The source forest that is to be treated as the delta. The difference
	 * describes how to convert the base into this model.
	 */
  protected final ForestModel deltaForest;

  /** The DifferenceModelCore delegate. */
  protected final DifferenceModelCore diffModCore;

  /** The DifferenceForestModelCore delegate. */
  protected final DifferenceForestModelCore diffForestModCore;

  /*
	 * Local copies of enumeration elements.
	 */

  /**
	 * Local reference to the enumeration elements of the position difference
	 * enumeration.
	 */
  private final IREnumeratedType.Element[] positionElts;

  /**
	 * Local reference to the enumeration elements of the subtree difference
	 * enumeration.
	 */
  private final IREnumeratedType.Element[] subtreeElts;

  /*
	 * State used by the difference algorithm.
	 */

  /** A list of all the nodes in the base model */
  protected final Collection<IRNode> basenodes = new Vector<IRNode>();

  /** A list of all the nodes in the delta model */
  protected final Collection<IRNode> deltanodes = new Vector<IRNode>();

  /** A list of all the nodes in both models */
  protected final Collection<IRNode> preserved = new HashSet<IRNode>(); // new Vector();

  /** A list of all the nodes in the delta model, but not the base model */
  protected final Collection<IRNode> added = new Vector<IRNode>();

  /** A list of all the nodes in the base model, but not the delta model */
  protected final Collection<IRNode> deleted = new Vector<IRNode>();

  /** A list of all the nodes with different parents (also preserved) */
  protected final Collection<IRNode> moved = new Vector<IRNode>();

  /** A list of all the nodes with different attributes (also preserved) */
  protected final Collection<IRNode> changed = new Vector<IRNode>();

  /** A list of all the phantom nodes */
  protected final Collection<IRNode> phantoms = new Vector<IRNode>();

  //===========================================================
  //== Constructor
  //===========================================================

  public AbstractDifferenceForestModel(
    final String name,
    final ForestModel base,
    final ForestModel delta,
    final ModelCore.Factory mf,
    final ViewCore.Factory vf,
    final ForestModelCore.Factory fmf,
    final DifferenceModelCore.Factory dmf,
    final DifferenceForestModelCore.Factory dfmf,
    final AttributeMergingManager.Factory mergFactory)
    throws SlotAlreadyRegisteredException {
    super(
      name,
      mf,
      vf,
      fmf,
      LocalAttributeManagerFactory.prototype,
      BasicAttributeInheritanceManagerFactory.prototype);
    mergingManager = mergFactory.create(this, structLock, attrManager);

    diffModCore = dmf.create(name, this, structLock, attrManager);
    diffForestModCore =
      dfmf.create(
        name,
        this,
        structLock,
        attrManager,
        new AttributeSrcChangedCallback());
    baseForest = base;
    deltaForest = delta;
    positionElts = DifferenceForestModelCore.getDiffPositionElts();
    subtreeElts = DifferenceForestModelCore.getDiffSubtreeElts();
    diffModCore.setSourceModels(viewCore, base, delta);

    // Connect model-view chain
    final ModelListener threadedSrcModelBreakageHandler =
      new ThreadedModelAdapter(srcModelBreakageHandler);
    base.addModelListener(threadedSrcModelBreakageHandler);
    delta.addModelListener(threadedSrcModelBreakageHandler);
  }

  /**
	 * This method should be called after the subclass has initialized the
	 * {@link DifferenceForestModel#NODE_ATTR_SRC}attribute. This method
	 * inherits/merges the INFORMATIONAL attributes of the two source models. (No
	 * attribute inheritance policy is used.)
	 */
  protected final void mergeAttributes(final AttributeChangedCallback cb) {
    /*
		 * Init the inherited and merged attributes. We enumerate through the
		 * INFORMATIONAL attributes of both source models and "inherit" all
		 * attributes that are only present in one of the source models and "merge"
		 * all attributes that are present in both. All attributes are inherited or
		 * merged as immutable (for now?).
		 * 
		 * XXX Clearly this will have problems when dynamically created attributes
		 * XXX are fixed to be properly propogated down the view chain.
		 */

    /*
		 * Arrays for the merging method calls. - "models" we never change. -
		 * "attrs" we will change the element values, but never realloc the array
		 * structure. Do this to prevent creation of too much garbage.
		 */
    final Model[] models = new Model[] { baseForest, deltaForest };
    final String[] attrs = new String[] { null, null };

    LOG.fine(
      "Start of attribute inheritance/merging for diff model \""
        + getName()
        + "\"");

    /*
		 * First we merge the model-level attributes.
		 */
    final Iterator<String> baseCompAttrs = baseForest.getComponentAttributes();
    while (baseCompAttrs.hasNext()) {
      final String attr = baseCompAttrs.next();
      if (baseForest.getCompAttrKind(attr) == Model.INFORMATIONAL) {
        if (deltaForest.isComponentAttribute(attr)) {
          if (deltaForest.getCompAttrKind(attr) == Model.INFORMATIONAL) {
            LOG.fine("Merging model-level attribute \"" + attr + "\"");
            attrs[0] = attrs[1] = attr;
            mergingManager.mergeCompAttributes(
              models,
              attrs,
              attr,
              AttributeMergingManager.IMMUTABLE_MERGED,
              Model.INFORMATIONAL,
              cb);
          }
        } else {
          LOG.fine(
            "Inheriting model-level attribute \""
              + attr
              + "\" from \""
              + baseForest.getName()
              + "\"");
          inheritManager.inheritCompAttribute(
            baseForest,
            attr,
            attr,
            AttributeInheritanceManager.IMMUTABLE,
            Model.INFORMATIONAL,
            cb);
        }
      }
    }

    final Iterator<String> deltaCompAttrs = deltaForest.getComponentAttributes();
    while (deltaCompAttrs.hasNext()) {
      final String attr = deltaCompAttrs.next();
      if (deltaForest.getCompAttrKind(attr) == Model.INFORMATIONAL) {
        /*
				 * If it's in the base model we will have merged it.
				 */
        if (!baseForest.isComponentAttribute(attr)) {
          LOG.fine(
            "Inheriting model-level attribute \""
              + attr
              + "\" from \""
              + deltaForest.getName());
          inheritManager.inheritCompAttribute(
            deltaForest,
            attr,
            attr,
            AttributeInheritanceManager.IMMUTABLE,
            Model.INFORMATIONAL,
            cb);
        }
      }
    }

    /*
		 * Now process the node-level attributes.
		 */
    final Iterator<String> baseNodeAttrs = baseForest.getNodeAttributes();
    while (baseNodeAttrs.hasNext()) {
      final String attr = baseNodeAttrs.next();
      if (baseForest.getNodeAttrKind(attr) == Model.INFORMATIONAL) {
        if (deltaForest.isNodeAttribute(attr)) {
          if (deltaForest.getNodeAttrKind(attr) == Model.INFORMATIONAL) {
            LOG.fine("Merging node-level attribute \"" + attr + "\"");
            attrs[0] = attrs[1] = attr;
            mergingManager.mergeNodeAttributes(
              models,
              attrs,
              attr,
              AttributeMergingManager.IMMUTABLE_MERGED,
              Model.INFORMATIONAL,
              cb);
          }
        } else {
          LOG.fine(
            "Inheriting node-level attribute \""
              + attr
              + "\" from \""
              + baseForest.getName()
              + "\"");
          inheritManager.inheritNodeAttribute(
            baseForest,
            attr,
            attr,
            AttributeInheritanceManager.IMMUTABLE,
            Model.INFORMATIONAL,
            cb);
        }
      }
    }

    final Iterator<String> deltaNodeAttrs = deltaForest.getNodeAttributes();
    while (deltaNodeAttrs.hasNext()) {
      final String attr = deltaNodeAttrs.next();
      if (deltaForest.getNodeAttrKind(attr) == Model.INFORMATIONAL) {
        /*
				 * If it's in the base model we will have merged it.
				 */
        if (!baseForest.isNodeAttribute(attr)) {
          LOG.fine(
            "Inheriting model-level attribute \""
              + attr
              + "\" from \""
              + deltaForest.getName()
              + "\"");
          inheritManager.inheritNodeAttribute(
            deltaForest,
            attr,
            attr,
            AttributeInheritanceManager.IMMUTABLE,
            Model.INFORMATIONAL,
            cb);
        }
      }
    }

    LOG.fine(
      "End of attribute inheritance/merging for diff model \""
        + getName()
        + "\"");
    /*
		 * End attribute inheritance.
		 */
  }

  /**
	 * Callback for changes to the {@link DifferenceForestModel#DEFAULT_ATTR_SRC}
	 * attribute.
	 */
  private class AttributeSrcChangedCallback
    extends AbstractAttributeChangedCallback {
    @Override
    protected void attributeChangedImpl(
      final String attr,
      final IRNode node,
      final Object val) {
      if (attr == DifferenceForestModel.DEFAULT_ATTR_SRC) {
        modelCore.fireModelEvent(
          new AttributeValuesChangedEvent(
            AbstractDifferenceForestModel.this,
            DifferenceForestModel.DEFAULT_ATTR_SRC,
            val));
      }
    }
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin DifferenceModel Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  public final Model getBaseModel() {
    /*
		 * We have local copies of the source models, so use them instead of
		 * calling the DifferenceModelCore delegate object.
		 */
    synchronized (structLock) {
      return baseForest;
    }
  }

  public final Model getDeltaModel() {
    /*
		 * We have local copies of the source models, so use them instead of
		 * calling the DifferenceModelCore delegate object.
		 */
    synchronized (structLock) {
      return deltaForest;
    }
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End DifferenceModel Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin DifferenceForestModel Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //===========================================================
  //== Type-safe get source methods.
  //===========================================================

  public final ForestModel getBaseModelAsForest() {
    /*
		 * We have local copies of the source models, so use them instead of
		 * calling the DifferenceModelCore delegate object.
		 */
    synchronized (structLock) {
      return baseForest;
    }
  }

  public final ForestModel getDeltaModelAsForest() {
    /*
		 * We have local copies of the source models, so use them instead of
		 * calling the DifferenceModelCore delegate object.
		 */
    synchronized (structLock) {
      return deltaForest;
    }
  }

  //===========================================================
  //== Attribute convienence methods
  //===========================================================

  public final IREnumeratedType.Element getDiffPosition(final IRNode node) {
    synchronized (structLock) {
      return diffForestModCore.getDiffPosition(node);
    }
  }

  public final IREnumeratedType.Element getDiffSubtree(IRNode node) {
    synchronized (structLock) {
      return diffForestModCore.getDiffSubtree(node);
    }
  }

  public final IRNode getDiffLabel(IRNode node) {
    synchronized (structLock) {
      return diffForestModCore.getDiffLabel(node);
    }
  }

  public final boolean getCompSelector() {
    synchronized (structLock) {
      return diffForestModCore.getCompSelector();
    }
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End DifferenceForestModel Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin ModelToModelStatefulView Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  //===========================================================
  //== The rebuild method
  //===========================================================

  /// Diff algorithm
  /** Re-run the diff algorithm and notify our views */
  @Override
  protected final void rebuildModel(final List l) throws InterruptedException {
    synchronized (structLock) {
      try {
        // Version.clampCurrent();

        // XXX: WHy is this synchronized?
        synchronized (this) {
          initDiff();
          final long start = System.currentTimeMillis();
          setupVectors();
          final long setup = System.currentTimeMillis();
          processVectors();
          final long process = System.currentTimeMillis();
          buildDiffTree();
          final long build = System.currentTimeMillis();
          LOG.info("Setup   = " + (setup - start) + " ms");
          LOG.info("Process = " + (process - setup) + " ms");
          LOG.info("Build   = " + (build - process) + " ms");
        }
      } catch (FluidRuntimeException e) {
        e.printStackTrace();
        return;
      } finally {
        // Version.unclampCurrent();
      }
    }
    // Break our views
    modelCore.fireModelEvent(new ModelEvent(this));
  }

  //===========================================================
  //== State clearing and setup methods
  //===========================================================

  /**
	 * Clear all the old difference state, and prepare to perform a new
	 * difference.
	 */
  // run from synchronized method
  private void initDiff() {
    // initialize vectors
    basenodes.clear();
    deltanodes.clear();
    added.clear();
    preserved.clear();
    deleted.clear();
    moved.clear();
    changed.clear();
    phantoms.clear();

    initLocalDiff();
  }

  /**
	 * Compute the raw node difference information: what nodes are added in the
	 * delta model, what nodes are removed from the delta model, and what nodes
	 * are preserved in the delta model. The following sets are initialized:
	 * <ul>
	 * <li>{@link #basenodes}with all the nodes in the base model.
	 * <li>{@link #deltanodes}with all the nodes in the delta model.
	 * <li>{@link #added}with all the nodes in the delta model that are not in
	 * the base model.
	 * <li>{@link #preserved}with all the nodes present in both models.
	 * <li>{@link #deleted}with all the nodes in the base but not the delta.
	 * </ul>
	 * <p>
	 * <em>Note that {@link #moved} is not initalized by this method, but
   * instead by {@link #computePositionDiffs}.</em>
	 */
  // run from synchronized method
  private void setupVectors() throws InterruptedException {
    LOG.fine("Getting a list of nodes in the base model");
    for (final Iterator<IRNode> bn = baseForest.getNodes(); bn.hasNext();) {
      basenodes.add(bn.next());
    }

    if (rebuildWorker.isInterrupted())
      throw cannedInterrupt;

    LOG.fine("Getting a list of nodes in the delta model");
    for (final Iterator<IRNode> dn = deltaForest.getNodes(); dn.hasNext();) {
      final IRNode node = dn.next();
      deltanodes.add(node);

      if (!basenodes.contains(node)) {
        if (LOG.isLoggable(Level.FINE))
          LOG.fine("Added: " + node.hashCode());
        added.add(node);
      } else {
        if (LOG.isLoggable(Level.FINE))
          LOG.fine("Preserved: " + node.hashCode());
        preserved.add(node);
      }
    }

    if (rebuildWorker.isInterrupted())
      throw cannedInterrupt;

    LOG.fine("Getting a list of deleted nodes in the base model");
    for (final Iterator<IRNode> bn = basenodes.iterator(); bn.hasNext();) {
      final IRNode node = bn.next();
      if (!preserved.contains(node)) {
        if (LOG.isLoggable(Level.FINE))
          LOG.fine("Deleted: " + node.hashCode());
        deleted.add(node);
      }
    }
  }

  //===========================================================
  //== Methods for setting computing and setting the needed
  //== difference attributes.
  //===========================================================

  /**
	 * Set the local, subtree, and positional difference attributes for the nodes
	 * based on the added, deleted, and preserved vectors.
	 */
  // run from synchronized method
  private void processVectors() throws InterruptedException {
    /*
		 * Compute the local, subtree, and positional difference values. (Ignores
		 * added nodes?)
		 */
    LOG.fine("Checking preserved nodes for attribute and position changes");
    final long start = System.currentTimeMillis();
    computeLocalSubtreeDiffs();
    final long local0 = System.currentTimeMillis();
    computePositionDiffs();
    final long pos = System.currentTimeMillis();

    /*
		 * Added nodes always have a local difference of 'added', and subtree and
		 * positional difference of 'not applicable'.
		 */
    LOG.fine("Marking added as added");
    for (final Iterator<IRNode> a = added.iterator(); a.hasNext();) {
      if (rebuildWorker.isInterrupted())
        throw cannedInterrupt;
      final IRNode addednode = a.next();
      setDiffLocal(addednode, DifferenceForestModel.ADDED);
      diffForestModCore.setDiffPosition(
        addednode,
        DifferenceForestModel.POS_NA);
      diffForestModCore.setDiffSubtree(addednode, DifferenceForestModel.SUB_NA);
    }

    /*
		 * Report computation times.
		 */
    final long add = System.currentTimeMillis();
    LOG.info("Local   = " + (local0 - start) + " ms");
    LOG.info("Position= " + (pos - local0) + " ms");
    LOG.info("Added   = " + (add - pos) + " ms");
  }

  /**
	 * Compute the local and subtree diffs, using a bottom-up traversal.
	 */
  private void computeLocalSubtreeDiffs() throws InterruptedException {
    for (final Iterator<IRNode> p = baseForest.getRoots(); p.hasNext();) {
      final IRNode root = p.next();
      LOG.fine("1 New ROOT " + root.hashCode());

      for (final Iterator<IRNode> e = baseForest.bottomUp(root);
        e.hasNext();
        ) {
        if (rebuildWorker.isInterrupted())
          throw cannedInterrupt;
        final IRNode n = e.next();

        // Processing for the local marker
        if (preserved.contains(n)) {
          /*
					 * See if the preserved node changed its local state.
					 */
          markLocalDiff(n);

          // Processing for the subtree marker
          if (isLeaf(n)) {
            /*
						 * Leaf nodes don't have subtress, so their subtree difference is
						 * "Not Applicable".
						 */
            LOG.fine("2 At a LEAF " + n.hashCode());
            diffForestModCore.setDiffSubtree(n, DifferenceForestModel.SUB_NA);
          } else {
            /*
						 * Internal (aka parent) node: figure out if the subtree changed.
						 */
            LOG.fine("3 At an INTERNAL node " + n.hashCode());
            diffForestModCore.setDiffSubtree(
              n,
              subtreeUnchangedP(n)
                ? DifferenceForestModel.SUB_SAME
                : DifferenceForestModel.SUB_DIFF);
          }
        } else if (deleted.contains(n)) {
          /*
					 * Deleted nodes have a local difference of deleted, and don't have a
					 * subtree difference.
					 */
          LOG.fine("Marking " + n.hashCode() + " as deleted");
          setDiffLocal(n, DifferenceForestModel.DELETED);
          diffForestModCore.setDiffSubtree(n, DifferenceForestModel.SUB_NA);
        } else {
          throw new edu.cmu.cs.fluid.FluidError(
            "Not preserved, added, or deleted?");
        }
      }
    }
  }

  /**
	 * Determine if the state of a subtree is unchanged or not. The local,
	 * positional, and subtree difference have been computed for all the nodes
	 * below the given node. Only the local and positiional differences have been
	 * computed from the given node.
	 */
  private boolean subtreeUnchangedP(final IRNode n) {
    return (
      nodeOrderedP(n)
        ? orderedSubtreeUnchangedP(n)
        : unorderedSubtreeUnchangedP(n));
  }

  /**
	 * Determine if the state of a subtree whose children are unordered is
	 * unchanged or not. The local, positional, and subtree difference have been
	 * computed for all the nodes below the given node. Only the local and
	 * positiional differences have been computed from the given node.
	 * 
	 * <p>
	 * The subtree has changed if the given node contains 'added' or 'removed'
	 * children in the delta model, or if a child of the node is otherwise
	 * determined to be structurally different by the method
	 * {@link #nodeMarkedSame}.
	 */
  private boolean unorderedSubtreeUnchangedP(final IRNode n) {
    /*
		 * Look at the children of 'n' in the base forest. If 'child' is a child of
		 * 'n' in the base forest but not the delta forest, or if 'child' is
		 * otherwise structurally different between the two models then the subtree
		 * is not unchanged.
		 */
    for (final Iterator<IRNode> eB = baseForest.children(n);
      eB.hasNext();
      ) {
      final IRNode child = eB.next();
      LOG.fine("\tChecking baseForest " + child);

      final IRNode p = deltaForest.getParentOrNull(child);
      if (!n.equals(p)) {
        LOG.fine("\tDIFF: re-parented from " + n + " to " + p);
        return false;
      }
      if (!nodeMarkedSame(child)) {
        return false;
      }
    }

    /*
		 * Look at the children of 'n' in the delta forest. If 'n' contains a child
		 * in the delta forest that is not in the base forest then the subtree is
		 * not unchanged.
		 */
    for (final Iterator<IRNode> eD = deltaForest.children(n);
      eD.hasNext();
      ) {
      final IRNode child = eD.next();
      LOG.fine("\tChecking deltaForest " + child);

      // new child for old parent
      if (added.contains(child)) {
        LOG.fine("\tDIFF: children changed");
        return false;
      }
    }

    /*
		 * If we made it over here then the subtree is unchanged.
		 */
    LOG.fine("\tSAME: structurally identical");
    return true;
  }

  /**
	 * Determine if the state of a subtree whose children are ordered is
	 * unchanged or not. The local, positional, and subtree difference have been
	 * computed for all the nodes below the given node. Only the local and
	 * positiional differences have been computed from the given node.
	 * 
	 * <p>
	 * The subtree has changed if the the children of the given node in the base
	 * and delta models are not in the same order, if one of the children has
	 * changed structurally as determined by the method {@link #nodeMarkedSame},
	 * or if the number of children in the two models is different.
	 */
  private boolean orderedSubtreeUnchangedP(final IRNode n) {
    final Iterator<IRNode> eB = baseForest.children(n);
    final Iterator<IRNode> eD = deltaForest.children(n);
    while (eB.hasNext() && eD.hasNext()) {
      final IRNode childB = eB.next();
      final IRNode childD = eD.next();
      LOG.fine("\tChecking baseForest " + childB + " deltaForest " + childD);

      // check if we have the same ordering of children
      if (childB != childD || (childB != null && !nodeMarkedSame(childB))) {
        return false;
      }
    }
    final boolean rv = (eB.hasNext() == eD.hasNext());
    LOG.fine("\tSAME: whole subtree structurally identical? " + rv);
    return rv;
  }

  /**
	 * Query whether a node is the same structurally in the base and delta
	 * models. A leaf node is the same if its local difference is not added or
	 * deleted. A parent node is the same if its subtree difference is SUB_SAME.
	 */
  private boolean nodeMarkedSame(final IRNode child) {
    final boolean isLeaf = isLeaf(child);
    final Object subMark = diffForestModCore.getDiffSubtree(child);
    final Object localMark = getDiffLocal(child);
    // check if the child is structurally the same
    if ((isLeaf
      && ((localMark == localElts(DifferenceForestModel.DELETED))
        || (localMark == localElts(DifferenceForestModel.ADDED))))
      || (!isLeaf && subMark != subtreeElts[DifferenceForestModel.SUB_SAME])) {
      LOG.fine(
        "\tDIFF: children changed structure "
          + isLeaf
          + " "
          + localMark
          + " "
          + subMark);
      return false;
    }
    return true;
  }

  /**
	 * Compute the positional difference attribute values for the entire forest
	 * using a top-down tree traversal.
	 */
  private void computePositionDiffs() throws InterruptedException {
    for (final Iterator<IRNode> roots = baseForest.getRoots(); roots.hasNext();) {
      final IRNode root = roots.next();
      LOG.fine("4 New ROOT " + root);
      computePositionDiffs(root);
    }
  }

  /**
	 * Compute the positional difference attribute for a node. (This is
	 * structural position, not semantic position). Also adds nodes to the
	 * {@link #moved}list.
	 * 
	 * <p>
	 * An unpreserved (added or deleted) node is never in the same place and
	 * never has the same subtree, and is always marked as having a 'not
	 * applicable' positional difference.
	 * 
	 * <p>
	 * A preserved node is in the same position if its path to a root in the
	 * forest is the same in both the base and delta models and it is in the same
	 * position relative to its siblings as determined by sameNodeOrderingP(). If
	 * a node is in the same position it is given a positional difference value
	 * of 'Same', otherwise 'Moved.' In addition, if a preserved node's subtree
	 * difference is 'Same', then the entire subtree is marked with the same
	 * positional difference as the node.
	 * 
	 * <p>
	 * If a node is moved, and has a difference parent then it is added to the
	 * list of nodes that will be given phantom limbs.
	 */
  private void computePositionDiffs(final IRNode n)
    throws InterruptedException {
    if (n == null)
      return;
    if (rebuildWorker.isInterrupted())
      throw cannedInterrupt;

    final boolean isLeaf = isLeaf(n);
    final Object subMark = diffForestModCore.getDiffSubtree(n);
    //    final Object localMark = getDiffLocal(n);
    getDiffLocal(n);

    LOG.fine("Looking at IRNode " + n);
    int mark;
    boolean samePlace;
    boolean sameSub;

    if (preserved.contains(n)) {
      LOG.fine("5 SAME node " + n.hashCode());
      samePlace = pathMatches(n) && sameNodeOrderingP(n);
      sameSub = subMark == subtreeElts[DifferenceForestModel.SUB_SAME];

      if (samePlace) {
        mark = DifferenceForestModel.POS_SAME;
        if (sameSub)
          markSubtree(n, DifferenceForestModel.POS_SAME);
      } else {
        mark = DifferenceForestModel.POS_MOVED;

        // Either the path to root or the ordering changed
        if (parentChanged(n)) {
          // phantom only if still in different directory
          moved.add(n);
        }
        if (sameSub)
          markSubtree(n, DifferenceForestModel.POS_ANC);
      }
    } else {
      samePlace = false;
      sameSub = false;
      mark = DifferenceForestModel.POS_NA;
    }

    diffForestModCore.setDiffPosition(n, mark);
    LOG.fine("\tSetting parent " + n.hashCode() + " to " + positionElts[mark]);

    /*
		 * Only recurse if then node is not a leaf and the subtree is known to be
		 * different.
		 */
    if (!isLeaf && !sameSub) {
      int i = 0;
      for (Iterator<IRNode> e = baseForest.children(n); e.hasNext();) {
        i++;
        LOG.fine("\tComputing diffs for child " + i);
        computePositionDiffs(e.next());
      }
    }
  }

  /**
	 * Determine if the path from a root to the given node is the same in both
	 * the base and delta models.
	 */
  private boolean pathMatches(final IRNode n) {
    if (n == null)
      return true;
    IRNode b, d, current = n;

    /*
		 * Loop as long as the parent of the current node is the same in each
		 * model.
		 */
    do {
      b = baseForest.getParentOrNull(current);
      d = deltaForest.getParentOrNull(current);
      current = b;
    } while (b == d && b != null && d != null);

    /*
		 * If the path is the same 'b' and 'd' will both be null. Otherwise they
		 * will be unequal nodes.
		 */
    return (b.equals(d));
  }

  /**
	 * For a preserved nodes only, is the node in the same order as was before
	 * with respect to its siblings? [I think this is what it does any
	 * way&mdash;Aaron]
	 */
  protected boolean sameNodeOrderingP(IRNode n) {
    // !nodeOrderedP(n)
    return true;
    // Ignoring added or deleted nodes -- only preserved nodes
  }

  /**
	 * Mark an entire subtree with the given positional difference element.
	 * 
	 * @param n
	 *          The root of the subtree to mark.
	 * @param mark
	 *          The index of the positional difference element.
	 */
  private void markSubtree(final IRNode n, final int mark) {
    for (int i = baseForest.numChildren(n) - 1; i >= 0; i--) {
      // Check that the child at each position actually exists
      final IRNode child = baseForest.getChild(n, i);
      if (child == null)
        continue;
      diffForestModCore.setDiffPosition(child, mark);
      LOG.fine(
        "\tSetting child  " + child.hashCode() + " to " + positionElts[mark]);
      markSubtree(child, mark);
    }
  }

  /**
	 * Checks if the parent of the node is different between the two models.
	 */
  private boolean parentChanged(final IRNode node) {
    boolean changed = false;
    final IRNode parent = baseForest.getParentOrNull(node);
    LOG.fine(
      "\tchecking if parent changed ("
        + parent
        + ", "
        + deltaForest.getParent(node)
        + ")");
    if (!parent.equals(deltaForest.getParentOrNull(node))) {
      changed = true;
    }
    return changed;
  }

  //===========================================================
  //== Methods for actually assembling the exported difference model.
  //===========================================================

  /**
	 * Build a tree, based on different node vectors. (basically the union of
	 * nodes in both models, plus phantom nodes)
	 */
  // run from synchronized method
  private void buildDiffTree() throws InterruptedException {
    /*
		 * Clear the exported forest and make sure all the nodes in the delta model
		 * are considered "isPresent" in the model.
		 */
    forestModCore.clearForest();
    forestModCore.cacheNodes(true, deltanodes.iterator());

    /*
		 * Build the exported model from the delta model, setting the "difference
		 * label" for each node to itself, and ordering children based on the delta
		 * model.
		 */
    final Iterator<IRNode> dn = deltanodes.iterator();
    while (dn.hasNext()) {
      if (rebuildWorker.isInterrupted())
        throw cannedInterrupt;
      final IRNode n = dn.next();
      setupNode(n);
      diffForestModCore.setDiffLabel(n, n);

      // append the node to the children list of its parent
      // or make it a root, as required
      final IRNode parent = deltaForest.getParentOrNull(n);
      if (parent == null || deltaForest.isRoot(n)) {
        LOG.fine("Adding a root " + n);
        forestModCore.addRoot(n);
      } else {
        LOG.fine("Adding a node");
        placeNode(deltaForest, parent, n, n);
      }
    }

    /*
		 * Add the deleted nodes to the exported model, giving them themselves as
		 * their difference label, and ordering children based on the base model.
		 * The parent of the deleted node is found from the base model.
		 */
    LOG.fine("next add the deleted nodes  to the diff tree");
    forestModCore.cacheNodes(false, deleted.iterator());
    final Iterator<IRNode> d = deleted.iterator();
    while (d.hasNext()) {
      final IRNode n = d.next();
      setupNode(n);
      diffForestModCore.setDiffLabel(n, n);
      LOG.fine("doing the manager.getparent in a loop");
      final IRNode parent = baseForest.getParentOrNull(n);
      if (parent != null) {
        updateModelSpecificVectors(n, n);
        LOG.fine("adding a child to a parent in the loop");
        placeNode(baseForest, parent, n, n);
      } else {
        throw new edu.cmu.cs.fluid.FluidError(
          "Error: could not find deleted node: " + n + "'s parent");
      }
    }

    /*
		 * Build a phantom limb for each moved subtree.
		 */
    LOG.fine("creating phantom limbs");
    final Iterator<IRNode> m = moved.iterator();
    while (m.hasNext()) {
      buildPhantomLimb(m.next());
    }
    forestModCore.cacheNodes(false, phantoms.iterator());
  }

  /**
	 * Build a phantom limb in the exported model for the given "moved" node.
	 */
  private void buildPhantomLimb(final IRNode node) {
    //get the original location of the tree from the base tree
    LOG.fine("building a phantom limb");
    final IRNode oldparent = baseForest.getParentOrNull(node);
    final IRNode phantomnode = new PlainIRNode();
    phantoms.add(phantomnode);

    // add phantom node to old parent
    forestModCore.initNode(phantomnode);
    if (oldparent == null) {
      forestModCore.addRoot(phantomnode);
    } else {
      placeNode(baseForest, oldparent, phantomnode, node);
    }
    setDiffLocal(phantomnode, DifferenceForestModel.PHANTOM);
    diffForestModCore.setDiffSubtree(phantomnode, DifferenceForestModel.SUB_NA);
    diffForestModCore.setDiffPosition(
      phantomnode,
      DifferenceForestModel.POS_NA);

    // update the label
    diffForestModCore.setDiffLabel(phantomnode, node);
    //SlotInfo label = getNodeAttribute(LabeledForest.LABEL);
    //phantomnode.setSlotValue(label, "Old '"+node.getSlotValue(label)+"'");
    updateModelSpecificVectors(node, phantomnode);
  }

  private void setShowBaseModelAttrs(final boolean showBase) {
    synchronized (structLock) {
      diffForestModCore.setCompSelector(showBase);
    }
    modelCore.fireModelEvent(
      new AttributeValuesChangedEvent(
        this,
        DifferenceForestModel.DEFAULT_ATTR_SRC,
        showBase ? Boolean.TRUE : Boolean.FALSE));
  }

  //=======================================================================
  //== Abstract parts of the difference algorithm
  //=======================================================================

  /**
	 * (Re)initializes the node for this model, so we can use it (again). This
	 * implementation should be good for generic trees. It is not good for
	 * SyntaxTrees.
	 */
  protected void setupNode(final IRNode n) {
    // add node to tree. Must only call initNode once!
    if (!forestModCore.isNode(n)) {
      forestModCore.initNode(n);
    } else {
      // clear the children, to get rid of cruft in exported model
      forestModCore.removeChildren(n);
    }
  }

  /**
	 * Clear the state needed for maintaining local difference information.
	 */
  // run from synchronized method
  protected abstract void initLocalDiff();

  // unused?
  //protected abstract int attributesChanged(IRNode n);

  /**
	 * Returns whether the model considers the given node to be a leaf node.
	 */
  protected abstract boolean isLeaf(IRNode n);

  /**
	 * Invoked on preserved nodes to determine how the local state of the node
	 * may have changed.
	 */
  protected abstract void markLocalDiff(IRNode n);

  /**
	 * Query if the children of a node are ordered.
	 */
  protected abstract boolean nodeOrderedP(IRNode n);

  /**
	 * Called by <code>buildDiffTree()</code> to update any local difference
   * state needed for building the exported model.
	 */
  protected abstract void updateModelSpecificVectors(IRNode n, IRNode add);

  /**
	 * Put a node in the exported model with a given difference label, and whose
	 * location under the given parent is determined by the label's position in
	 * the provided model.
	 * 
	 * @param order
	 *          The model, that together with <code>label</code> determines
	 *          <code>n</code>'s position with respect to its siblings.
	 * @param parent
	 *          The node to which to add <code>n</code>.
	 * @param n
	 *          The node to add to the exported model.
	 * @param label
	 *          The node that determines <code>n</code>'s position with
	 *          respect to its siblings.
	 */
  protected void placeNode(
    ForestModel order,
    IRNode parent,
    IRNode n,
    IRNode label) {
    // Remember to check for phantom nodes
    forestModCore.appendSubtree(parent, n);
  }

  /**
	 * Set the {@link DifferenceForestModel#DIFF_LOCAL}attribute for the given
	 * using the local difference enumeration item identified by the given index.
	 */
  protected abstract void setDiffLocal(IRNode node, int valIdx);

  /**
	 * Get the {@link DifferenceForestModel#DIFF_LOCAL}attribute value.
	 */
  protected abstract IREnumeratedType.Element getDiffLocal(IRNode node);

  /**
	 * Get the local difference enumeration element of the given index.
	 */
  protected abstract IREnumeratedType.Element localElts(int idx);
}
