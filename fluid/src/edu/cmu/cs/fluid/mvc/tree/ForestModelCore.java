package edu.cmu.cs.fluid.mvc.tree;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.*;
import edu
  .cmu
  .cs
  .fluid
  .mvc
  .tree
  .attributes
  .MutableSequenceReturningImmutableAttribute;
import edu
  .cmu
  .cs
  .fluid
  .mvc
  .tree
  .attributes
  .SequenceAttributeCallbackToAttributeCallback;
import edu.cmu.cs.fluid.tree.*;

/**
 * Abstract implemenation of core class for <code>ForestModel</code>.
 * <p>
 * Contains the machinery for adding the model-level attribute
 * {@link ForestModel#ROOTS}. The subclass must cause the attribute to be
 * added to the model by invoking the method {@link #initializeRoots}.
 * <p>
 * Adds the node-level attributes {@link DigraphModel#CHILDREN},
 * {@link ForestModel#IS_ROOT},{@link SymmetricDigraphModel#PARENTS}, and
 * {@link ForestModel#LOCATION}.
 * 
 * <p>
 * The callback provided to the constructor must handle changes to the
 * sequences in the {@link SymmetricDigraphModel#PARENTS}and
 * {@link DigraphModel#CHILDREN}attributes.
 * 
 * @author Aaron Greenhouse
 */
public abstract class ForestModelCore extends AbstractCore {
  public static final Logger CORE = SLLogger.getLogger("MV.core");

  //===========================================================
  //== Fields
  //===========================================================

  /**
	 * Whether the Attributes/Sequences in the attributes are allowed to be
	 * mutated by clients.
	 */
  protected final boolean isMutable;

  /** The structrue storing the forest */
  protected final MutableTreeInterface forest;

  /** The children attribute */
  protected final SlotInfo<IRSequence<IRNode>> children;

  /** The parents attribute */
  protected final SlotInfo<IRSequence<IRNode>> parents;

  /** The location attribute */
  protected final SlotInfo<IRLocation> location;

  /** The isRoot attribute */
  protected final SlotInfo<Boolean> isRoot;

  /*
	 * isPresent() related fields
	 */
  protected final Set<IRNode> isPresentCache = new HashSet<IRNode>();
  protected boolean precomputedNodes = false;

  protected final Bundle b = new Bundle();

  //===========================================================
  //== Constructor
  //===========================================================

  /**
	 * Subclass must: initialize ROOTS by calling {@link #initializeRoots}
	 */
  @SuppressWarnings("unchecked")
  protected ForestModelCore(
    final String name,
    final MutableTreeInterface tree,
    final SlotFactory sf,
    final boolean mutable,
    final Model model,
    final Object lock,
    final AttributeManager manager,
    final AttributeChangedCallback cb)
    throws SlotAlreadyRegisteredException {
    // Init the tree delegate
    super(model, lock, manager);
    forest = tree;
    isMutable = mutable;

    // Init node attributes
    isRoot =
      sf.newAttribute(
        name + "-" + ForestModel.IS_ROOT,
        IRBooleanType.prototype,
        Boolean.FALSE);
    attrManager.addNodeAttribute(ForestModel.IS_ROOT, Model.STRUCTURAL, isRoot);

    parents = forest.getAttribute(Tree.PARENTS);
    attrManager.addNodeAttribute(
      SymmetricDigraphModel.PARENTS,
      Model.STRUCTURAL,
      new MutableSequenceReturningImmutableAttribute<IRNode>(
        structLock,
        parents,
        isMutable,
        new SequenceAttributeCallbackToAttributeCallback(
          SymmetricDigraphModel.PARENTS,
          cb)));

    location = forest.getAttribute(Tree.LOCATION);
    attrManager.addNodeAttribute(
      ForestModel.LOCATION,
      Model.STRUCTURAL,
      location);

    children = forest.getAttribute(Digraph.CHILDREN);
    attrManager.addNodeAttribute(
      DigraphModel.CHILDREN,
      Model.STRUCTURAL,
      new MutableSequenceReturningImmutableAttribute<IRNode>(
        structLock,
        children,
        isMutable,
        new SequenceAttributeCallbackToAttributeCallback(
          DigraphModel.CHILDREN,
          cb)));

    /*
		 * Roots attribute initialized via initializeRoots(), invoked from a
		 * subclass.
		 */

    b.saveAttribute(isRoot);
  }

  /**
	 * Subclasses must call this from their constructor so that the
	 * {@link ForestModel#ROOTS}attribute is initializec.
	 * 
	 * @param rootsSeq
	 *          The sequence used to represent the sequence of roots of the
	 *          forest.
	 */
  protected final void initializeRoots(final IRSequence<IRNode> rootsSeq) {
    final ComponentSlot<IRSequence<IRNode>> rootsAttr =
      new SimpleComponentSlot<IRSequence<IRNode>>(
        IRSequenceType.nodeSequenceType,
        ConstantExplicitSlotFactory.prototype,
        rootsSeq);
    attrManager.addCompAttribute(
      ForestModel.ROOTS,
      Model.STRUCTURAL,
      rootsAttr);
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Stuff for managing isPresent()
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  /**
	 * @precondition <code>inNode</code> is not <code>null</code>
	 */
  protected final boolean computeIsPresent(final IRNode inNode) {
    if (isPresentCache.contains(inNode)) {
      return true;
    }
    boolean rv = computeIsPresentLocal(inNode);
    if (rv)
      isPresentCache.add(inNode);
    return rv;
  }

  /**
	 * @precondition <code>inNode</code> is not <code>null</code>
	 */
  private boolean computeIsPresentLocal(final IRNode inNode) {
    IRNode node = inNode;
    boolean isPresent = false;
    while ((node != null) && !isPresent) {
      try {
        if (node.getSlotValue(isRoot).equals(Boolean.TRUE)) {
          isPresent = true;
        } else {
          node = forest.getParentOrNull(node);
        }
      } catch (SlotUndefinedException e) {
        /*
				 * If IS_ROOT is undefined the node is definately not part of the
				 * model.
				 * 
				 * Insure termination of loop by setting both isPresent and node.
				 */
        isPresent = false;
        node = null;
      }
    }
    return isPresent;
  }

  /**
	 * Make sure that IRNodes given by the iterator are present in the cache of
	 * nodes that are part of the forest.
	 */
  public final void cacheNodes(final boolean clear, final Iterator<IRNode> it) {
    if (clear) {
      isPresentCache.clear();
      CORE.fine("IsPresent cleared");
    }
    precomputedNodes = true;

    CORE.fine("Caching isPresent for " + this +" = " + isPresentCache.size());
    while (it.hasNext()) {
      isPresentCache.add(it.next());
    }
    CORE.info("isPresent = " + isPresentCache.size());
  }

  public final void precomputeNodes() {
    CORE.fine("Pre-computing isPresent");
    cacheNodes(true, getNodes());
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End of Inner Classes
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin Digraph
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //===========================================================
  //== Digraph Methods -- Later, move this to DigraphModelCore
  //===========================================================

  public SlotInfo getAttribute(String name) {
    if (name.equals(Digraph.CHILDREN)) {
      return attrManager.getNodeAttribute(DigraphModel.CHILDREN);
    } else if (name.equals(Tree.PARENTS)) {
      return attrManager.getNodeAttribute(SymmetricDigraphModel.PARENTS);
    } else if (name.equals(Tree.LOCATION)) {
      return attrManager.getNodeAttribute(ForestModel.LOCATION);
    }
    return null;
  }

  /**
	 * Return true when the node has at first one child location.
	 */
  public final boolean hasChildren(final IRNode node) {
    return forest.hasChildren(node);
  }

  /**
	 * Return the number of children, defined or undefined, null or nodes.
	 */
  public final int numChildren(final IRNode node) {
    return forest.numChildren(node);
  }

  /** Return the location for child #i */
  public final IRLocation childLocation(final IRNode node, final int i) {
    return forest.childLocation(node, i);
  }

  /** Return the numeric location of a location */
  public final int childLocationIndex(
    final IRNode node,
    final IRLocation loc) {
    return forest.childLocationIndex(node, loc);
  }

  /** Return the location of the first child. */
  public final IRLocation firstChildLocation(final IRNode node) {
    return forest.firstChildLocation(node);
  }

  /** Return the location of the last child. */
  public final IRLocation lastChildLocation(final IRNode node) {
    return forest.lastChildLocation(node);
  }

  /** Return the location of the next child (or null). */
  public final IRLocation nextChildLocation(
    final IRNode node,
    final IRLocation loc) {
    return forest.nextChildLocation(node, loc);
  }

  /** Return the location of the previous child (or null). */
  public final IRLocation prevChildLocation(
    final IRNode node,
    final IRLocation loc) {
    return forest.prevChildLocation(node, loc);
  }

  /**
	 * Return one of
	 * <dl>
	 * <dt>&lt; 0
	 * <dd>if loc1 precedes loc2,
	 * <dt>&gt; 0
	 * <dd>if loc1 follows loc2,
	 * <dt>= 0
	 * <dd>if loc1 equals loc2.
	 * </dl>
	 * These locations must be valid locations in the children of the node.
	 */
  public final int compareChildLocations(
    final IRNode node,
    final IRLocation loc1,
    final IRLocation loc2) {
    return forest.compareChildLocations(node, loc1, loc2);
  }

  /**
	 * Return true if the child is defined.
	 * 
	 * @exception IRSequenceException
	 *              if the index is out of range
	 */
  public final boolean hasChild(final IRNode node, final int i) {
    return forest.hasChild(node, i);
  }

  /**
	 * Return true if the child is defined.
	 * 
	 * @exception IRSequenceException
	 *              if the location is invalid (or null).
	 */
  public final boolean hasChild(final IRNode node, final IRLocation loc) {
    return forest.hasChild(node, loc);
  }

  /** Return the i'th child of a node. */
  public final IRNode getChild(final IRNode node, final int i) {
    if (!isPresent(node)) {
      return null;
    }
    return forest.getChild(node, i);
  }

  /** Return the child at location loc. */
  public final IRNode getChild(final IRNode node, final IRLocation loc) {
    if (!isPresent(node)) {
      return null;
    }
    return forest.getChild(node, loc);
  }

  /** Return the children of a node in order. */
  public final Iteratable<IRNode> children(final IRNode node) {
    if (!isPresent(node)) {
      return new EmptyIterator<IRNode>();
    }
    return forest.children(node);
  }
  
  /** Return the children of a node in order. */
  public final List<IRNode> childList(final IRNode node) {
    if (!isPresent(node)) {
      return Collections.emptyList();
    }
    return forest.childList(node);
  }

  /**
	 * Attach a listener to the digraph. If the digraph is mutated, the listener
	 * should be called.
	 */
  public final void addDigraphListener(final DigraphListener dl) {
    forest.addDigraphListener(dl);
  }

  /** Detach a listener from the digraph. */
  public final void removeDigraphListener(DigraphListener dl) {
    forest.removeDigraphListener(dl);
  }

  //===========================================================
  //== Mutable Digraph Methods -- Later, move this to DigraphModelCore
  //===========================================================

  /**
	 * Add a node to the directed graph. Notify define observers and inform
	 * listeners of this new node.
	 * 
	 * @param n
	 *          a new node to add to the graph
	 * @throws SlotImmutableException
	 *           if node already in graph
	 */
  public final void initNode(final IRNode n) {
    forest.initNode(n);
  }

  /**
	 * Add a node to the directed graph. Notify define observers and inform
	 * listeners of this new node.
	 * 
	 * @param n
	 *          a new node to add to the graph
	 * @param numChildren
	 *          if &gt;= 0 then the number of children for a fixed arity node. If
	 *          &lt; 0, then <tt>~numChildren</tt> is the number of initial
	 *          children for a variable arity node.
	 * @throws SlotImmutableException
	 *           if node already in graph
	 */
  public final void initNode(final IRNode n, final int numChildren) {
    forest.initNode(n, numChildren);
  }

  /** Return true if the node is part of this direction graph. */
  public final boolean isNode(final IRNode n) {
    return forest.isNode(n);
  }

  /**
	 * Set the i'th child of the node to be newChild.
	 * 
	 * @exception StructureException
	 *              if the child is not suitable
	 */
  public final void setChild(
    final IRNode node,
    final int i,
    final IRNode newChild)
    throws StructureException {
    forest.setChild(node, i, newChild);
  }

  /**
	 * Set the child at location loc of the node to be newChild.
	 * 
	 * @exception StructureException
	 *              if the child is not suitable
	 */
  public final void setChild(
    final IRNode node,
    final IRLocation loc,
    final IRNode newChild)
    throws StructureException {
    forest.setChild(node, loc, newChild);
  }

  /**
	 * Adopt a new child to the children without disturbing existing children. If
	 * the children are fixed in size, we look for an undefined or null child
	 * location. If the children are variable in size, we append to the end.
	 * 
	 * @exception StructureException
	 *              if there is no space to add
	 */
  public final void addChild(final IRNode node, final IRNode newChild)
    throws StructureException {
    forest.addChild(node, newChild);
  }

  /**
	 * Replace the node's oldChild with newChild.
	 * 
	 * @exception StructureException
	 *              if oldChild is not a child, or newChild is not suitable.
	 */
  public final void replaceChild(
    final IRNode node,
    final IRNode oldChild,
    final IRNode newChild)
    throws StructureException {
    forest.replaceChild(node, oldChild, newChild);
  }

  /**
	 * Add new child as a new child of node at the given insertion point.
	 * 
	 * @exception StructureException
	 *              if newChild is not suitable or the parent cannot accept new
	 *              children.
	 * @return location of new child
	 */
  public final IRLocation insertChild(
    final IRNode node,
    final IRNode newChild,
    final InsertionPoint ip)
    throws StructureException {
    return forest.insertChild(node, newChild, ip);
  }

  /**
	 * Add newChild as a new first child of node.
	 * 
	 * @exception StructureException
	 *              if newChild is not suitable or the parent cannot accept new
	 *              children.
	 */
  public final void insertChild(final IRNode node, final IRNode newChild)
    throws StructureException {
    forest.insertChild(node, newChild);
  }

  /**
	 * Add newChild as a new last child of node.
	 * 
	 * @exception StructureException
	 *              if newChild is not suitable or the parent cannot accept new
	 *              children.
	 */
  public final void appendChild(final IRNode node, final IRNode newChild)
    throws StructureException {
    forest.appendChild(node, newChild);
  }

  /**
	 * Add newChild as a new child after the given child of node.
	 * 
	 * @exception StructureException
	 *              if oldChild is not a child, newChild is not suitable, or the
	 *              parent cannot accept new children.
	 */
  public final void insertChildAfter(
    final IRNode node,
    final IRNode newChild,
    final IRNode oldChild)
    throws StructureException {
    forest.insertChildAfter(node, newChild, oldChild);
  }

  /**
	 * Add newChild as a new child before the given child of node.
	 * 
	 * @exception StructureException
	 *              if if oldChild is not a child, newChild is not suitable, or
	 *              the parent cannot accept new children.
	 */
  public final void insertChildBefore(
    final IRNode node,
    final IRNode newChild,
    final IRNode oldChild)
    throws StructureException {
    forest.insertChildBefore(node, newChild, oldChild);
  }

  /**
	 * Remove oldChild from the sequence of children of a node. If the sequence
	 * is variable, we get rid of its location too, otherwise, we substitute
	 * null.
	 * 
	 * @exception StructureException
	 *              if oldChild is not a child,
	 */
  public final void removeChild(final IRNode node, final IRNode oldChild)
    throws StructureException {
    forest.removeChild(node, oldChild);
  }

  /**
	 * Remove the child (if any) at the given location. Replace with null if
	 * sequence is fixed.
	 */
  public final void removeChild(final IRNode node, final IRLocation loc) {
    forest.removeChild(node, loc);
  }

  /** Remove all the children of a node. */
  public final void removeChildren(final IRNode node) {
    forest.removeChildren(node);
  }

  /**
	 * Return an enumeration of the nodes in the graph. First we return the root
	 * given and then recursively the enumerations of each of its children.
	 */
  public final Iteratable<IRNode> depthFirstSearch(final IRNode node) {
    return forest.depthFirstSearch(node);
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End Digraph
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin Symmetric Digraph
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //===========================================================
  //== Symmetric Digraph Methods -- Later, move this to
  // SymmetricDigraphModelCore
  //===========================================================

  /** Return true if there is at least one parent location. */
  public final boolean hasParents(final IRNode node) {
    return forest.hasParents(node);
  }

  /** Return the number of parents, defined or undefined, null or nodes. */
  public final int numParents(final IRNode node) {
    return forest.numParents(node);
  }

  /** Return the location for parent #i */
  public final IRLocation parentLocation(final IRNode node, final int i) {
    return forest.parentLocation(node, i);
  }

  /** Return the numeric location for a location. */
  public final int parentLocationIndex(
    final IRNode node,
    final IRLocation loc) {
    return forest.parentLocationIndex(node, loc);
  }

  /** Return the location of the first parent. */
  public final IRLocation firstParentLocation(final IRNode node) {
    return forest.firstParentLocation(node);
  }

  /** Return the location of the last parent. */
  public final IRLocation lastParentLocation(final IRNode node) {
    return forest.lastParentLocation(node);
  }

  /** Return next parent location or null. */
  public final IRLocation nextParentLocation(
    final IRNode node,
    final IRLocation ploc) {
    return forest.nextParentLocation(node, ploc);
  }

  /** Return previous parent location or null. */
  public final IRLocation prevParentLocation(
    final IRNode node,
    final IRLocation ploc) {
    return forest.prevParentLocation(node, ploc);
  }

  /**
	 * Return one of
	 * <dl>
	 * <dt>&lt; 0
	 * <dd>if loc1 precedes loc2,
	 * <dt>&gt; 0
	 * <dd>if loc1 follows loc2,
	 * <dt>= 0
	 * <dd>if loc1 equals loc2.
	 * </dl>
	 * These locations must be valid locations in the parents of the node.
	 */
  public final int compareParentLocations(
    final IRNode node,
    final IRLocation loc1,
    final IRLocation loc2) {
    return forest.compareParentLocations(node, loc1, loc2);
  }

  /** Return the i'th parent of a node. */
  public final IRNode getParent(final IRNode node, final int i) {
    return forest.getParent(node, i);
  }

  /** Return the parent at location loc. */
  public final IRNode getParent(final IRNode node, final IRLocation loc) {
    return forest.getParent(node, loc);
  }

  /** Return the parents of a node in order. */
  public final Iteratable<IRNode> parents(final IRNode node) {
    return forest.parents(node);
  }

  //===========================================================
  //== Mutable Symmetric Digraph Methods -- Later, move this to
  // SymmetricDigraphModelCore
  //===========================================================

  /**
	 * Create a new node in the graph and ready space for parents and children.
	 * 
	 * @param n
	 *          node to add to graph
	 * @param numParents
	 *          fixed number of parents if >= 0, ~initial number of variable
	 *          parents if < 0. (defaults to -1 if omitted, see
	 *          {@link #initNode(IRNode,int)})
	 * @param numChildren
	 *          fixed number of children if >= 0, ~initial number of variable
	 *          children if < 0.
	 * @exception SlotImmutableException
	 *              if node already in graph.
	 */
  public final void initNode(
    final IRNode n,
    final int numParents,
    final int numChildren) {
    forest.initNode(n, numParents, numChildren);
  }

  /**
	 * Set the specified parent. <strong>Warning: if the list of parents is
	 * variable, it may be reordered.</strong>
	 * 
	 * @param i
	 *          0-based indicator of parent to change
	 * @param newParent
	 *          new node to be parent, may be null.
	 */
  public final void setParent(
    final IRNode node,
    final int i,
    final IRNode newParent) {
    forest.setParent(node, i, newParent);
  }

  /**
	 * Set the specified parent. <strong>Warning: if the list of parents is
	 * variable, it may be reordered.</strong>
	 * 
	 * @param loc
	 *          location to change parent.
	 * @param newParent
	 *          new node to be parent, may be null.
	 */
  public final void setParent(
    final IRNode node,
    final IRLocation loc,
    final IRNode newParent) {
    forest.setParent(node, loc, newParent);
  }

  /**
	 * Add a new parent to the parents list without disturbing existing parents.
	 * If the parents are fixed in size, we look for a null or undefined parent
	 * to replace. If the parents are variable in size, we append to the end.
	 * 
	 * @exception StructureException
	 *              if there is no space to add
	 */
  public final void addParent(final IRNode node, final IRNode newParent)
    throws StructureException {
    forest.addParent(node, newParent);
  }

  /**
	 * Remove the link between a parent and a node. Neither may be null.
	 * 
	 * @exception StructureException
	 *              if parent is not a parent of node
	 */
  public final void removeParent(final IRNode node, final IRNode parent)
    throws StructureException {
    forest.removeParent(node, parent);
  }

  /**
	 * Replace the node's oldParent with newParent.
	 * 
	 * @exception StructureException
	 *              if oldParent is not a parent, or newParent is not suitable.
	 */
  public final void replaceParent(
    final IRNode node,
    final IRNode oldParent,
    final IRNode newParent)
    throws StructureException {
    forest.replaceParent(node, oldParent, newParent);
  }

  /** Remove all the parents of a node. */
  public final void removeParents(final IRNode node) {
    forest.removeParents(node);
  }

  /** Remove a node from the graph. */
  public final void removeNode(final IRNode node) {
    forest.removeNode(node);
  }

  /** Return all the nodes connected with a root. */
  public final Iteratable<IRNode> connectedNodes(final IRNode root) {
    return forest.connectedNodes(root);
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End Symmetric Digraph
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin Tree
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //===========================================================
  //== Tree Methods
  //===========================================================

  /**
	 * Return parent of a tree node. @precondition nonNull(node)
	 */
  public final IRNode getParent(final IRNode node) {
    return forest.getParent(node);
  }

  /**
	 * The location is a value used by an IRSequence to locate an element. For
	 * IRArray, it is an integer. @precondition nonNull(node)
	 */
  public final IRLocation getLocation(final IRNode node) {
    return forest.getLocation(node);
  }

  /**
	 * Return the root of a subtree.
	 */
  public final IRNode getRoot(final IRNode subtree) {
    return forest.getRoot(subtree);
  }

  /**
	 * Return an enumeration of nodes in the subtree starting with leaves and
	 * working toward the node given. A postorder traversal.
	 */
  public final Iteratable<IRNode> bottomUp(final IRNode subtree) {
    return forest.bottomUp(subtree);
  }

  /**
	 * Return an enumeration of nodes in the subtree starting with this node and
	 * working toward the leaves. A preorder traversal.
	 */
  public final Iteratable<IRNode> topDown(final IRNode subtree) {
    return forest.topDown(subtree);
  }

  //===========================================================
  //== Mutable Tree Methods
  //===========================================================

  /**
	 * Set the parent of a node to null. If it currently has a parent, the node
	 * is first removed from this parent's children.
	 */
  public final void clearParent(final IRNode node) {
    forest.clearParent(node);
  }

  /**
	 * Return the parent of a tree node (or null if null). If the parent was
	 * undefined, it is made null (and null is returned).
	 * 
	 * @see #getParent(IRNode)
	 */
  public final IRNode getParentOrNull(final IRNode node) {
    return forest.getParentOrNull(node);
  }

  /**
	 * Place a subtree as the child of another node. The subtree is first removed
	 * from where it currently is stored, if necessary.
	 */
  public final void setSubtree(
    final IRNode parent,
    final int i,
    final IRNode newChild) {
    forest.setSubtree(parent, i, newChild);
  }

  /**
	 * Place a subtree as the child of another node. The subtree is first removed
	 * from where it currently is stored, if necessary.
	 */
  public final void setSubtree(
    final IRNode parent,
    final IRLocation loc,
    final IRNode newChild) {
    forest.setSubtree(parent, loc, newChild);
  }

  /**
	 * Replace one subtree in the tree by another subtree. Each subtree is
	 * represented by its root. The new subtree is removed from its current
	 * location (if any), the old subtree must have a parent. These properties
	 * are changed after the routine returns. @precondition nonNull(oldChild)
	 * 
	 * @exception StructureException
	 *              If oldChild does currently have a parent or newChild is not
	 *              legal in the place of oldChild.
	 */
  public final void replaceSubtree(
    final IRNode oldChild,
    final IRNode newChild)
    throws StructureException {
    forest.replaceSubtree(oldChild, newChild);
  }

  /**
	 * Exchange where two subtrees are located. If one does not have a parent,
	 * then the operation is identical to a replaceSubtree operation. If both do
	 * not have parents, this operation has no effect. @precondition
	 * nonNull(node1) && nonNull(node2)
	 * 
	 * @exception StructureException
	 *              If either node is not legal in the place of the other.
	 */
  public final void exchangeSubtree(final IRNode node1, final IRNode node2)
    throws StructureException {
    forest.exchangeSubtree(node1, node2);
  }

  /**
	 * Insert a subtree at the given point in a node's children sequence. The
	 * subtree is first removed from wherever it currently resides. @precondition
	 * nonNull(parent) && nonNull(newChild) && nonNull(ip)
	 */
  public final IRLocation insertSubtree(
    final IRNode node,
    final IRNode newChild,
    final InsertionPoint ip) {
    return forest.insertSubtree(node, newChild, ip);
  }

  /**
	 * Insert a subtree at the beginning of the child sequence. The subtree is
	 * first removed from wherever it currently resides. @precondition
	 * nonNull(parent) && nonNull(newChild)
	 */
  public final void insertSubtree(final IRNode parent, final IRNode newChild) {
    forest.insertSubtree(parent, newChild);
  }

  /**
	 * Insert a subtree at the end of the children sequence. The subtree is first
	 * removed from wherever it currently resides. @precondition nonNull(parent) &&
	 * nonNull(newChild)
	 */
  public final void appendSubtree(final IRNode parent, final IRNode newChild) {
    forest.appendSubtree(parent, newChild);
  }

  /**
	 * Remove a subtree from a variable size sequence. The same function as
	 * {@link #clearParent}. @precondition nonNull(node)
	 */
  public final void removeSubtree(final IRNode node) {
    forest.removeSubtree(node);
  }

  /**
	 * Insert a subtree after another in a sequence. The subtree is first removed
	 * from wherever it currently resides. @precondition nonNull(oldChild) &&
	 * nonNull(newChild)
	 * 
	 * @param oldChild
	 *          a subtree in a sequence
	 * @param newChild
	 *          a subtree to be put in the sequence after oldChild.
	 */
  public final void insertSubtreeAfter(
    final IRNode newChild,
    final IRNode oldChild) {
    forest.insertSubtreeAfter(newChild, oldChild);
  }

  /**
	 * Insert a subtree before another in a sequence. The subtree is first
	 * removed from wherever it currently resides. @precondition
	 * nonNull(oldChild) && nonNull(newChild)
	 * 
	 * @param oldChild
	 *          a subtree in a sequence
	 * @param newChild
	 *          a subtree to be put in sequence before oldChild.
	 */
  public final void insertSubtreeBefore(
    final IRNode newChild,
    final IRNode oldChild) {
    forest.insertSubtreeBefore(newChild, oldChild);
  }

  /**
	 * Return an enumeration of nodes from this one to a root.
	 */
  public final Iteratable<IRNode> rootWalk(final IRNode node) {
    return forest.rootWalk(node);
  }

  /**
	 * Compare the ordering of two nodes in a preorder traversal of a tree.
	 * 
	 * @throws IllegalArgumentException
	 *           if they are not in the same tree,
	 * @throws NullPointerException
	 *           if either node is null.
	 * @return a value
	 *         <dl>
	 *         <dt>-2
	 *         <dd>if node1 precedes node2 in postorder or preorder
	 *         <dt>-1
	 *         <dd>if node1 is an ancestor of node2
	 *         <dt>0
	 *         <dd>if node1 equals node2
	 *         <dt>1
	 *         <dd>if node1 is a descendant of node2
	 *         <dt>2
	 *         <dd>if node1 follows node2 in postorder or preorder
	 *         </dl>
	 *         In particular, < 0 means precedes in preorder
	 */
  public final int comparePreorder(final IRNode node1, final IRNode node2) {
    return forest.comparePreorder(node1, node2);
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End Tree
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin Model Portion
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //===========================================================
  //== Node Methods
  //===========================================================

  public abstract Iterator<IRNode> getNodes();

  public abstract void addNode(IRNode node, AVPair[] vals);

  public final boolean isPresent(final IRNode node) {
    return computeIsPresent(node);
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End Model Portion
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin ForestModel Portion
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //===========================================================
  //== Digraph Model Methods
  //===========================================================

  // none

  //===========================================================
  //== Symmetric Digraph Model Methods
  //===========================================================

  // none

  //===========================================================
  //== Forest Model Methods
  //===========================================================

  /** Clear the tree */
  public abstract void clearForest();

  /** Test if a node is a root in the forest. */
  public abstract boolean isRoot(IRNode node);

  /**
	 * Remove a root from the roots.
	 * 
	 * @exception IllegalArgumentException
	 *              Thrown if the node is not a member of the forest
	 */
  public abstract void removeRoot(IRNode root);

  /**
	 * Append a root to the forest.
	 * 
	 * @exception IllegalArgumentException
	 *              Thrown if the node is already a member of the forest.
	 */
  public abstract void addRoot(IRNode root);

  /**
	 * Insert a root at the start of the roots.
	 * 
	 * @exception IllegalArgumentException
	 *              Thrown if the node is already a member of the forest.
	 */
  public abstract void insertRoot(IRNode root);

  /**
	 * Insert a new root before another root.
	 * 
	 * @exception IllegalArgumentException
	 *              Thrown if the new root is already a member of the forest.
	 */
  public abstract void insertRootBefore(IRNode newRoot, IRNode root);

  /**
	 * Insert a new root after another root.
	 * 
	 * @exception IllegalArgumentException
	 *              Thrown if the new root is already a member of the forest.
	 */
  public abstract void insertRootAfter(IRNode newRoot, IRNode root);

  /**
	 * Insert a new root at a given location. The root is inserted using an
	 * insertion point before the given location.
	 */
  public abstract void insertRootAt(IRNode root, IRLocation loc);

  /**
	 * Insert a new root with a given insertion point.
	 */
  public abstract void insertRootAt(IRNode newRoot, InsertionPoint ip);

  /**
	 * Get the roots of the forest in order.
	 */
  public abstract Iteratable<IRNode> getRoots();

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End ForestModel Portion
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin Factories
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  public static interface Factory {
    public ForestModelCore create(
      String name,
      Model model,
      Object structLock,
      AttributeManager manager,
      AttributeChangedCallback cb)
      throws SlotAlreadyRegisteredException;
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End Factories
  //-----------------------------------------------------------
  //-----------------------------------------------------------
}
