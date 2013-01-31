/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/Tree.java,v 1.44
 * 2003/10/06 18:45:21 chance Exp $
 */
package edu.cmu.cs.fluid.tree;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.ThreadSafe;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.util.*;

/**
 * This class contains methods for accessing (untyped) tree nodes. Each node
 * has a parent, location and a vector of children. We have basic parent/child
 * commands. A number of XXChild methods in Digraph have be rewritten for trees
 * and called XXSubtree, for example {@link #setChild(IRNode,int,IRNode)}has
 * been supplemented by {@link #setSubtree(IRNode,int,IRNode)}. The subtree
 * version (as opposed to ones inherited from Digraph) first remove the subtree
 * from its former location before attaching it in the new location. The
 * inherited methods are not changed, except to signal an error if a node would
 * get two or more parents.
 * <P>
 * Known bugs:
 * <ul>
 * <li>As with {@link Digraph}, listeners and observers are never informed of
 * changes when this tree is a derived Tree.
 * <p>
 * A fix would require keeping conditional listeners/observers through the
 * wrapper slots.
 * </p>
 * </ul>
 */
@ThreadSafe
public class Tree extends Digraph implements MutableTreeInterface {
  public static final Logger LOG = SLLogger.getLogger("TREE");
  public static final String PARENTS = "parents";
  public static final String LOCATION = "location";

  protected interface Mutator extends Digraph.Mutator {
    public void clearParent(IRNode node);
  }

  @Override
  protected Digraph.Mutator createStoredMutator(SlotFactory sf) {
    return new StoredMutator(sf);
  }
  @Override
  protected Digraph.Mutator createDelegatingMutator() {
    return new DelegatingMutator();
  }

  /* final */
  //SlotInfo<IRSequence<IRNode>> parentsSlotInfo;
  SlotInfo<IRNode> parentSlotInfo;
  
  static IRType<IRSequence<IRNode>> parentsType = new IRSequenceType<IRNode>(IRNodeType.prototype);
  static IRType<IRNode> parentType = IRNodeType.prototype;
  
  /**
	 * Return parent of a tree node. @precondition nonNull(node)
	 * 
	 * @throws SlotUndefinedException
	 *           if no parent defined
	 * @throws NullPointerException
	 *           if node is null
	 * @see #getParentOrNull(IRNode)
	 */
  @Override
  public IRNode getParent(IRNode node) {
	/*
    final IRSequence<IRNode> seq = getParents(node);
    return seq.elementAt(0);
    */
    /*
	Object o = node.getSlotValue(parentSlotInfo);
	if (o instanceof IRNode || o == null) {	
		return (IRNode) o;		
	} else {
		System.out.println(o);
		node.getSlotValue(parentSlotInfo);
		return null;
	}
    */
    return node.getSlotValue(parentSlotInfo);
  }
  
  /*
  protected IRSequence<IRNode> getParents(IRNode node) {
    return node.getSlotValue(parentsSlotInfo);
  }
  */

  protected void setParent(IRNode node, IRNode parent) {
	/*
    final IRSequence<IRNode> seq = node.getSlotValue(parentsSlotInfo);
    seq.setElementAt(parent, 0);
    */
    node.setSlotValue(parentSlotInfo,parent);
  }

  /**
	 * Set the parent of a node to null. If it currently has a parent, the node
	 * is first removed from this parent's children.
	 */
  @Override
  public void clearParent(IRNode node) {
    ((Mutator) mutator).clearParent(node);
  }

  protected boolean parentExists(IRNode node) {
//    try {
      if (node != null) {
        //return node.valueExists(parentsSlotInfo) && getParents(node).validAt(0);
        return node.valueExists(parentSlotInfo);
      } else {
        return false;
      }
//    } catch (NullPointerException e) {
//      LOG.log(Level.FINE, "Got null ", e);
//      return false;
//    }
  }

  /**
	 * Return the parent of a tree node (or null if null). If the parent was
	 * undefined, null is returned.
	 * 
	 * @see #getParent(IRNode)
	 */
  @Override
  public IRNode getParentOrNull(IRNode node) {
    if (node == null) {
      return null;
    }
    /*
		 * else if (parentExists(node)) { return getParent(node); } else if
		 * (isNode(node) && node.valueExists(parentsSlotInfo)) { // only if defined
		 * and fully in the tree // (observers looking at a newly created node
		 * before // it has been tree-initialized might/do call this function)
		 * clearParent(node); }
		 */
    // Optimized via inlining and reordering parentExists() and getParent()
    /*
    if (node.valueExists(parentsSlotInfo)) {
      final IRSequence<IRNode> seq = getParents(node);

      // if there's really a parent
      if (seq.validAt(0)) {
        return seq.elementAt(0);
      }
    }
    */
    if (node.valueExists(parentSlotInfo)) {
      return node.getSlotValue(parentSlotInfo);
    }
    return null;
  }

  /* final */
  SlotInfo<IRLocation> locationSlotInfo;

  /**
	 * The location is a value used by an IRSequence to locate an element. For
	 * IRArray, it is an integer. @precondition nonNull(node)
	 */
  @Override
  public IRLocation getLocation(IRNode node) {
    return node.getSlotValue(locationSlotInfo);
  }
  protected void setLocation(IRNode node, IRLocation loc) {
    node.setSlotValue(locationSlotInfo, loc);
  }

  /**
	 * Return the location of a child within the children of a node. In this
	 * case, it is easy to compute because of the stored information, (compare
	 * with Digraph.findChild).
	 * 
	 * @see Digraph#findChild
	 */
  @Override
  protected IRLocation findChild(IRNode node, IRNode child)
    throws IllegalChildException {
    if (child == null)
      return super.findChild(node, child);
    if (node.equals(getParentOrNull(child)))
      return getLocation(child);
    throw new IllegalChildException("not a child of node");
  }

  public Tree(String name, SlotFactory sf)
    throws SlotAlreadyRegisteredException {
    super(name, sf);
    if (name == null) {
      parentSlotInfo = sf.newAttribute();
      locationSlotInfo = sf.newAttribute();
    } else {
      parentSlotInfo = sf.newAttribute(name + ".Tree.parents", parentType);
      locationSlotInfo =
        sf.newAttribute(name + ".Tree.location", IRLocationType.prototype);
    }
  }

  /**
	 * Create a Tree that delegates to a Tree presumed to lie behind the given
	 * attributes.
	 */
  public Tree(
    SlotInfo<IRSequence<IRNode>> childrenAttribute,
    SlotInfo<IRNode> parentAttribute,
    SlotInfo<IRLocation> locationAttribute) {
    super(childrenAttribute);
    parentSlotInfo = parentAttribute;
    locationSlotInfo = locationAttribute;
  }
  
  public Tree(SlotFactory sf, Tree orig)
    throws SlotAlreadyRegisteredException 
  {
    super(sf, orig);

    parentSlotInfo = new MutableDelegatingSlotInfo<IRNode>(orig.parentSlotInfo, sf);
    locationSlotInfo = new MutableDelegatingSlotInfo<IRLocation>(orig.locationSlotInfo, sf);
}

  @Override
  public Iteratable<IRNode> connectedNodes(final IRNode node) {
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override
  public void initNode(final IRNode node, final int numP, final int numC) {
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override
  public void removeNode(final IRNode node) {
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override
  public void replaceParent(
    final IRNode node,
    final IRNode oldParent,
    final IRNode newParent) {
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override
  public void removeParent(final IRNode node, final IRNode oldParent) {
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override
  public void removeParents(final IRNode node) {
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override
  public void addParent(final IRNode node, final IRNode newParent) {
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override
  public void setParent(
    final IRNode node,
    final int i,
    final IRNode newParent) {
    throw new UnsupportedOperationException("not yet implemented");
  }

  @Override
  public void setParent(
    final IRNode node,
    final IRLocation loc,
    final IRNode newParent) {
    throw new UnsupportedOperationException("not yet implemented");
  }

  /**
	 * Place a subtree as the child of another node. The subtree is first removed
	 * from where it currently is stored, if necessary.
	 */
  @Override
  public void setSubtree(IRNode parent, int i, IRNode newChild) {
    if (parentExists(newChild))
      clearParent(newChild);
    setChild(parent, i, newChild);
  }

  /**
	 * Place a subtree as the child of another node. The subtree is first removed
	 * from where it currently is stored, if necessary.
	 */
  @Override
  public void setSubtree(IRNode parent, IRLocation loc, IRNode newChild) {
    if (parentExists(newChild))
      clearParent(newChild);
    setChild(parent, loc, newChild);
  }

  /**
	 * Replace one subtree in the tree by another subtree. Each subtree is
	 * represented by its root. The new subtree is removed from its current
	 * location (if any), the old subtree must have a parent. These properties
	 * are changed after the routine returns. @precondition nonNull(oldChild)
	 * 
	 * @exception IllegalChildException
	 *              If oldChild does currently have a parent or newChild is not
	 *              legal in the place of oldChild.
	 */
  @Override
  public void replaceSubtree(IRNode oldChild, IRNode newChild)
    throws IllegalChildException {
    IRNode parent = getParent(oldChild);
    if (parent == null) {
      throw new IllegalChildException("replaced subtree has no parent");
    }
    removeSubtree(newChild);
    if (newChild != null) { 
      setChild(parent, getLocation(oldChild), newChild);
    }
  }

  /**
	 * Exchange where two subtrees are located. If one does not have a parent,
	 * then the operation is identical to a replaceSubtree operation. If both do
	 * not have parents, this operation has no effect. @precondition
	 * nonNull(node1) && nonNull(node2)
	 * 
	 * @exception IllegalChildException
	 *              If either node is not legal in the place of the other.
	 */
  @Override
  public void exchangeSubtree(IRNode node1, IRNode node2)
    throws IllegalChildException {
    IRNode parent1 = getParentOrNull(node1);
    IRNode parent2 = getParentOrNull(node2);
    if (parent1 == null) {
      if (parent2 != null) {
        replaceSubtree(node2, node1);
      }
    } else if (parent2 == null) {
      replaceSubtree(node1, node2);
    } else {
      IRLocation location1 = getLocation(node1);
      IRLocation location2 = getLocation(node2);

      // First we unhook one child and then
      // perform two set operations.
      // The first operation is required to avoid a node
      // getting more than one parent.
      setChild(parent1, location1, null);
      setChild(parent2, location2, node1);
      setChild(parent1, location1, node2);
    }
  }

  /**
	 * Insert a subtree at the given point in a node's children sequence. The
	 * subtree is first removed from wherever it currently resides. @precondition
	 * nonNull(parent) && nonNull(newChild) && nonNull(ip)
	 */
  @Override
  public IRLocation insertSubtree(
    IRNode node,
    IRNode newChild,
    InsertionPoint ip) {
    removeSubtree(newChild);
    return insertChild(node, newChild, ip);
  }

  /**
	 * Insert a subtree at the beginning of the child sequence. The subtree is
	 * first removed from wherever it currently resides. @precondition
	 * nonNull(parent) && nonNull(newChild)
	 */
  @Override
  public void insertSubtree(IRNode parent, IRNode newChild) {
    removeSubtree(newChild);
    insertChild(parent, newChild);
  }

  /**
	 * Insert a subtree at the end of the children sequence. The subtree is first
	 * removed from wherever it currently resides. @precondition nonNull(parent) &&
	 * nonNull(newChild)
	 */
  @Override
  public void appendSubtree(IRNode parent, IRNode newChild) {
    removeSubtree(newChild);
    appendChild(parent, newChild);
  }

  /**
	 * Remove a subtree from a variable size sequence. The same function as
	 * {@link #clearParent}. @precondition nonNull(node)
	 */
  @Override
  public void removeSubtree(IRNode node) {
    clearParent(node);
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
  @Override
  public void insertSubtreeAfter(IRNode newChild, IRNode oldChild) {
    removeSubtree(newChild);
    insertChildAfter(getParent(oldChild), newChild, oldChild);
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
  @Override
  public void insertSubtreeBefore(IRNode newChild, IRNode oldChild) {
    removeSubtree(newChild);
    insertChildBefore(getParent(oldChild), newChild, oldChild);
  }

  /**
	 * Return the root of a subtree.
	 */
  @Override
  public IRNode getRoot(IRNode subtree) {
    IRNode parent;
    while (true) {
      try {
    	  parent = getParent(subtree);
    	  if (parent == null) {
    		  return subtree;
    	  }
      } catch (SlotUndefinedException e) {
    	  setParent(subtree, null);
    	  return subtree;    	  
      }
      subtree = parent;
    }
  }

  /**
	 * Return an enumeration of nodes in the subtree starting with leaves and
	 * working toward the node given. A postorder traversal.
	 */
  @Override
  public Iteratable<IRNode> bottomUp(IRNode subtree) {
    return mutator.protect(new TreeWalkIterator(this, subtree, true));
  }
  /**
	 * Return an enumeration of nodes in the subtree starting with this node and
	 * working toward the leaves. A preorder traversal.
	 */
  @Override
  public Iteratable<IRNode> topDown(IRNode subtree) {
    return mutator.protect(new TreeWalkIterator(this, subtree, false));
  }

  /**
	 * Return an enumeration of the nodes in a tree. This method overrides the
	 * method in Digraph, in the case we are guaranteed not to have cycles
	 */
  @Override
  public Iteratable<IRNode> depthFirstSearch(IRNode root) {
    if (getParentOrNull(root) == null) {
      // safe to ignore marks:
      return topDown(root);
    } else {
      // not safe to ignore marks:
      return super.depthFirstSearch(root);
    }
  }

  /**
	 * Return an enumeration of nodes from this one to a root.
	 */
  @Override
  public Iteratable<IRNode> rootWalk(final IRNode node) {
    return rootWalk(node, null);
  }

  /**
   * Return an enumeration of nodes from this one to a root.
   */
  public Iteratable<IRNode> rootWalk(final IRNode node, final IRNode root) {
    return mutator.protect(new AbstractRemovelessIterator<IRNode>() {
      IRNode n = node;
      @Override
      public boolean hasNext() {
        return n != null;
      }
      @Override
      public IRNode next() {
        if (n == null)
          throw new NoSuchElementException("to root");
        try {
          return n;
        } finally {
          // if root, then we're done
          n = (n.equals(root)) ? null : getParentOrNull(n);
        }
      }
    });
  }  
  
  /**
   * Finds the least common ancestor of two nodes, or null if there is 
   * none.
   */
  public IRNode leastCommonAncestor(final IRNode n1, final IRNode n2) {
    final Iterator<IRNode> ancestors1 = rootWalkDown(n1);
    final Iterator<IRNode> ancestors2 = rootWalkDown(n2);
    IRNode lca = null;
    while (ancestors1.hasNext() && ancestors2.hasNext()) {
      IRNode a1 = ancestors1.next();
      IRNode a2 = ancestors2.next();
      if (a1.equals(a2)) {
        lca = a1;
      } else {
        return lca;
      }
    }
    return lca;
  }
  
  private Iterator<IRNode> rootWalkDown(final IRNode n) {
    final List<IRNode> l = new ArrayList<IRNode>(); 
    // fill list
    Iterator<IRNode> walk = rootWalk(n);
    while (walk.hasNext()) {
      l.add(walk.next());
    }
    return mutator.protect(new SimpleRemovelessIterator<IRNode>() {
      int i = l.size()-1;
      @Override protected Object computeNext() {
        return (i>=0) ? l.get(i--) : IteratorUtil.noElement;
      }
    });
  }
  
  /**
	 * Compare the ordering of two nodes in a preorder traversal of a tree.
	 * 
	 * @throws IllegalArgumentEception
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
  @Override
  public int comparePreorder(IRNode node1, IRNode node2) {
    IRNode root1 = node1, root2 = node2;
    int d1 = 0, d2 = 0;

    if (node1.equals(node2))
      return 0;

    /* first find root and depth */

    for (;;) {
      IRNode tmp = getParentOrNull(root1);
      if (tmp == null)
        break;
      root1 = tmp;
      ++d1;
    }
    for (;;) {
      IRNode tmp = getParentOrNull(root2);
      if (tmp == null)
        break;
      root2 = tmp;
      ++d2;
    }

    /* now check that they are in the same tree */
    if (!root1.equals(root2))
      throw new IllegalArgumentException("nodes in different trees cannot be compared");

    while (d1 > d2) {
      node1 = getParent(node1);
      --d1;
      if (node1.equals(node2))
        return -1;
    }
    while (d1 < d2) {
      node2 = getParent(node2);
      --d2;
      if (node1.equals(node2))
        return 1;
    }

    for (;;) {
      IRNode p1 = getParent(node1);
      IRNode p2 = getParent(node2);
      if (p1.equals(p2)) {
        int loccomp =
          compareChildLocations(p1, getLocation(node1), getLocation(node2));
        return loccomp < 0 ? -2 : 2;
      }
      node1 = p1;
      node2 = p2;
    }
  }

  // Satisfying SymmetricDigraphInterface:

  @Override
  public boolean hasParents(IRNode node) {
    return true;
  }
  @Override
  public int numParents(IRNode node) {
    return 1;
  }

  @Override
  public IRLocation parentLocation(IRNode n, int i) {
	if (i != 0) {
	  return null;
	}
    return IRLocation.zeroPrototype;
  }

  @Override
  public int parentLocationIndex(IRNode n, IRLocation loc) {
	if (loc == null || loc != IRLocation.zeroPrototype) {
	  return -1;
	}
    return 0;
  }

  @Override
  public IRLocation firstParentLocation(IRNode n) {
	return IRLocation.zeroPrototype;
  }

  @Override
  public IRLocation lastParentLocation(IRNode n) {
	return IRLocation.zeroPrototype;
  }

  @Override
  public IRLocation nextParentLocation(IRNode n, IRLocation loc) {
    return null;
  }

  @Override
  public IRLocation prevParentLocation(IRNode n, IRLocation loc) {
    return null;
  }

  @Override
  public int compareParentLocations(
    IRNode n,
    IRLocation loc1,
    IRLocation loc2) {
    return loc1 == loc2 ? 0 : -1;
  }

  @Override
  public IRNode getParent(IRNode node, int i) {
    if (i == 0)
      return getParent(node);
    throw new IRSequenceException("only one parent");
  }
  @Override
  public IRNode getParent(IRNode node, IRLocation loc) {
    if (parentLocationIndex(node, loc) == 0)
      return getParent(node);
    throw new IRSequenceException("only one parent");
  }

  @Override
  public Iteratable<IRNode> parents(IRNode node) {
    return new SingletonIterator<IRNode>(getParent(node));
  }

  protected class StoredMutator
    extends Digraph.StoredMutator
    implements Mutator {
    protected StoredMutator(SlotFactory sf) {
      super(sf);
    }

    // Override to init the parents attribute to be an
    // IRSequence
    @Override
    public void initNode(IRNode node, int numChildren) {
      super.initNode(node, numChildren);
      // Already undefined for a single parent
      /*
      IRSequence<IRNode> seq = slotFactory.newSequence(1);
      node.setSlotValue(parentsSlotInfo, seq);
      */
    }

    @Override
    protected void checkNewChild(IRNode parent, IRLocation loc, IRNode child)
      throws IllegalChildException {
      if (child == null)
        return;
      else if (parentExists(child)) {
        if (getParent(child) != null)
          throw new IllegalChildException("child already has a parent");
      } else {
        /* parent is undefined, hence OK */
      }
      checkChild(parent, loc, child);
    }

    @Override
    protected void checkNewVariableChild(IRNode parent, IRNode child)
      throws IllegalChildException {
      super.checkNewVariableChild(parent, child);
      if (child == null)
        return;
      else if (parentExists(child)) {
        if (getParent(child) != null)
          throw new IllegalChildException("child already has a parent");
      } else {
        /* parent is undefined, hence OK */
      }
      checkVariableChild(parent, child);
    }

    protected void checkChild(IRNode parent, IRLocation loc, IRNode child)
      throws IllegalChildException {
      // do nothing
    }

    protected void checkVariableChild(IRNode parent, IRNode child)
      throws IllegalChildException {
      // do nothing
    }

    /** Called when a node gains a parent. */
    @Override
    protected boolean addParent(
      IRNode child,
      IRNode parent,
      IRLocation loc,
      boolean initial) {
    	/*
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine(
          "addParent("
            + child
            + ","
            + (parent == null ? "null" : parent.toString())
            + ","
            + loc
            + ","
            + initial
            + ")");
      }
      */
      if (initial) {
        if (parentExists(child))
          initial = false;
      } else if (!parentExists(child)) {
        setLocation(child, null);
      }
      setParent(child, parent);
      setLocation(child, loc);
      return initial;
    }

    /** Call when a node loses a parent. */
    @Override
    protected void removeParent(IRNode child, IRNode parent, IRLocation loc) {
      setParent(child, null);
      setLocation(child, null);
    }

    @Override
    public void addObserver(Observer o) {
      super.addObserver(o);
      //parentsSlotInfo.addObserver(o);
      //locationSlotInfo.addObserver(o);
    }
    

    @Override
    public void saveAttributes(Bundle b) {
      super.saveAttributes(b);
      b.saveAttribute(parentSlotInfo);
      b.saveAttribute(locationSlotInfo);
    }

    @Override
    public void clearParent(IRNode node) {
      IRNode parent;
      if (parentExists(node)) {
        parent = getParent(node);
        if (parent != null)
          Tree.this.removeChild(parent, node);
      } else {
        setParent(node, null);
        setLocation(node, null);
        IRLocation ploc = parentLocation(node, 0);
        if (hasListeners()) {
          informDigraphListeners(
            new NewParentEvent(Tree.this, node, ploc, null));
        }
      }
      return;
    }

    @SuppressWarnings("rawtypes")
    protected final SlotInfo wrappedParentAttribute =
        new WrappedParentSlotInfo();
    /*
    protected final SlotInfo wrappedParentsAttribute =
      new WrappedParentsSlotInfo();
    */

    @SuppressWarnings("rawtypes")
    protected final SlotInfo wrappedLocationAttribute =
      new WrappedLocationSlotInfo();

    @SuppressWarnings("rawtypes")
    @Override
    public SlotInfo getAttribute(String name) {
      if (name == PARENTS) {
        return wrappedParentAttribute;
      } else if (name == LOCATION) {
        return wrappedLocationAttribute;
      } else {
        return super.getAttribute(name);
      }
    }

    @SuppressWarnings("rawtypes")
    class WrappedParentSlotInfo extends DerivedSlotInfo {
        @Override
        public IRType type() {
          return parentType;
        }
        @Override
        protected boolean valueExists(IRNode node) {
          return parentExists(node);
        }
        @Override
        protected Object getSlotValue(IRNode node) {
          return getParent(node);
        }
      }
    
    /*
    // Wrap so that an IRSequence wrapper is supstituted for
    // the actual parents sequence.
    class WrappedParentsSlotInfo extends DerivedSlotInfo {
      @Override
      public IRType type() {
        return parentsType;
      }
      @Override
      protected boolean valueExists(IRNode node) {
        return parentExists(node);
      }
      @Override
      protected Object getSlotValue(IRNode node) {
        return new ParentsWrapper(node, getParents(node));
      }
    }
    */

    // Wrap the parents sequence
    public class ParentsWrapper extends IRSequenceWrapper<IRNode> {
      final IRNode child;
      public ParentsWrapper(IRNode node, IRSequence<IRNode> real) {
        super(real);
        child = node;
      }
      @Override
      public void setElementAt(IRNode value, IRLocation loc) {
        if (locationIndex(loc) != 0) {
          throw new IRSequenceException(
            "index out of bounds:"
              + " the parent of a tree must be at index 0");
        }
        IRNode parent = value;
        if (parentExists(child) || value == null)
          clearParent(child);
        if (value != null)
          addChild(parent, child);
      }
      @Override
      public IRLocation insertElementAt(IRNode node, InsertionPoint ip) {
        throw new IRSequenceException("arrays are fixed size");
      }
      @Override
      public void removeElementAt(IRLocation loc) {
        if (locationIndex(loc) != 0) {
          throw new IRSequenceException(
            "index out of bounds:"
              + " the parent of a tree must be at index 0");
        }
        // Only clear parent if there is a non-null parent
        if (validAt(0)) {
          if (elementAt(0) != null)
            clearParent(child);
        }
      }
    }

    @SuppressWarnings("rawtypes")
    class WrappedLocationSlotInfo extends DerivedSlotInfo {
      @Override
      public IRType type() {
        return IRLocationType.prototype;
      }
      @Override
      protected boolean valueExists(IRNode node) {
        return node.valueExists(locationSlotInfo);
      }
      @Override
      protected Object getSlotValue(IRNode node) {
        return getLocation(node);
      }
    }
  }

  protected class DelegatingMutator
    extends Digraph.DelegatingMutator
    implements Mutator {
    @Override
    public void clearParent(IRNode node) {
      setParent(node, null);
    }
	@SuppressWarnings("rawtypes")
  @Override
    public SlotInfo getAttribute(String name) {
      if (name.equals(PARENTS))
        return parentSlotInfo;
      else if (name.equals(LOCATION))
        return locationSlotInfo;
      else
        return super.getAttribute(name);
    }
  }
}

/**
 * Class of tree walk enumerations. This has code for bottom-up and top-down
 * tree walks ending/starting at a particular node. (These traversals are also
 * called postorder and preorder).
 */

class TreeWalkIterator extends DepthFirstSearch {
  TreeWalkIterator(Tree t, IRNode root, boolean bottomUp) {
    super(t, root, bottomUp);
  }

  @Override
  protected boolean mark(IRNode node) {
    return true;
  }
}
