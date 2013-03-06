/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/Digraph.java,v 1.37
 * 2003/07/15 17:30:40 aarong Exp $
 */
package edu.cmu.cs.fluid.tree;

import java.io.PrintStream;
import java.util.*;
import java.util.logging.Logger;

import com.surelogic.ThreadSafe;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.util.EmptyIterator;
import edu.cmu.cs.fluid.util.Iteratable;

/**
 * A class with functions for creating and traversing directed graphs. The
 * slots of this class keep track of following nodes (children) but not the
 * preceding nodes (parents).
 * <P>
 * Known bugs:
 * <ul>
 * <li>Listeners and observers are never informed of changes when this
 * directed graph is built to delegate to (wrapped) attributes.
 * <p>
 * A fix would require wiring listeners/observers through the wrapper slots,
 * with some mechanism to avoid the need when no one is requesting it.
 * </p>
 * </ul>
 * 
 * @see Tree
 * @see SymmetricDigraph
 */
@ThreadSafe
public class Digraph extends DigraphMixin implements MutableDigraphInterface {
  /**
	 * Logger for this class
	 */
  protected static final Logger LOG = SLLogger.getLogger("IR.tree");

  public static final String CHILDREN = "children";

  /**
	 * Interface for mutators of directed graphs.
	 */
  protected interface Mutator {
    public void addObserver(Observer o);
    public void initNode(IRNode n, int numChildren);
    public void setChild(IRNode parent, IRLocation loc, IRNode child);
    public IRLocation insertChild(
      IRNode parent,
      IRNode child,
      InsertionPoint ip);
    public void removeChild(IRNode parent, IRLocation loc);
    public void saveAttributes(Bundle b);
    public SlotInfo getAttribute(String name);
    public Iteratable<IRNode> protect(Iteratable enm);
  }

  protected Mutator createStoredMutator(SlotFactory sf) {
    return new StoredMutator(sf);
  }
  protected Mutator createDelegatingMutator() {
    return new DelegatingMutator();
  }

  protected Mutator mutator;

  /* final */
  SlotInfo<IRSequence<IRNode>> childrenSlotInfo;

  protected IRSequence<IRNode> getChildren(IRNode node) {
    return node.getSlotValue(childrenSlotInfo);
  }
  protected void setChildren(IRNode parent, IRSequence<IRNode> children)
    throws SlotImmutableException {
    parent.setSlotValue(childrenSlotInfo, children);
  }

  /**
	 * The IRType of the children attribute.
	 */
  public static final IRType<IRSequence<IRNode>> childrenType =
    new IRSequenceType<IRNode>(IRNodeType.prototype);

  /**
	 * Create a new (stored) digraph.
	 * 
	 * @param name
	 *          If non-null this name is used to form registered slots to store
	 *          information in.
	 * @param sf
	 *          the kind of slots to use to store the graph information in
	 * @exception SlotAlreadyRegisteredException
	 *              if the given name has already been used to create a Digraph
	 */
  public Digraph(String name, final SlotFactory sf)
    throws SlotAlreadyRegisteredException {
    if (name == null)
      childrenSlotInfo = sf.newAttribute();
    else
      childrenSlotInfo =
        sf.newAttribute(name + ".Digraph.children", childrenType);
    // we used to use ConstantSlotFactory,
    // and so for backward compatability (reading in old bundles)
    // we need to ensure that it is registered.
    ConstantSlotFactory.ensureLoaded();
    mutator = createStoredMutator(sf);
  }

  /**
	 * Create a Digraph that delegates to a Digraph presumed to lie behind the
	 * given attribute.
	 */
  public Digraph(SlotInfo<IRSequence<IRNode>> childrenAttribute) {
    childrenSlotInfo = childrenAttribute;
    mutator = createDelegatingMutator();
  }

  protected Digraph(final SlotFactory sf, Digraph orig) 
  throws SlotAlreadyRegisteredException {
    childrenSlotInfo = new MutableDelegatingSlotInfo<IRSequence<IRNode>>(orig.childrenSlotInfo, sf);

    // we used to use ConstantSlotFactory,
    // and so for backward compatability (reading in old bundles)
    // we need to ensure that it is registered.
    ConstantSlotFactory.ensureLoaded();
    mutator = createStoredMutator(sf);    
  }
  
  @Override
  public synchronized void addObserver(Observer o) {
    super.addObserver(o);
    mutator.addObserver(o);
  }
  
  /**
	 * Add a node to the directed graph. Notify define observers and inform
	 * listeners of this new node.
	 * 
	 * @param n
	 *          a new node to add to the graph
	 * @throws SlotImmutableException
	 *           if node already in graph
	 */
  @Override
  public void initNode(IRNode n) {
    initNode(n, -1);
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
  @Override
  public void initNode(IRNode n, int numChildren) {
    mutator.initNode(n, numChildren);
  }

  /** Return true if the node is part of this direction graph. */
  @Override
  public boolean isNode(IRNode n) {
    return n.valueExists(childrenSlotInfo);
  }

  @Override
  public boolean hasChildren(IRNode node) {
    return getChildren(node).hasElements();
  }
  @Override
  public int numChildren(IRNode node) {
    return getChildren(node).size();
  }
  /** Return true if children sequence is variable */
  public boolean childrenIsVariable(IRNode node) {
    return getChildren(node).isVariable();
  }

  @Override
  public IRLocation childLocation(IRNode node, int i) {
    return getChildren(node).location(i);
  }
  @Override
  public int childLocationIndex(IRNode node, IRLocation loc) {
    return getChildren(node).locationIndex(loc);
  }

  @Override
  public IRLocation firstChildLocation(IRNode node) {
    IRSequence<IRNode> children = getChildren(node);
    if (children != null) {
      return children.firstLocation();
    } else {
      if (this instanceof SyntaxTree) {
        System.out.print(
          "op = " + ((SyntaxTree) this).getOperator(node) + ", ");
      }
      System.out.println("n = " + node);

      throw new FluidError("Null pointer");
    }
  }
  @Override
  public IRLocation lastChildLocation(IRNode node) {
    return getChildren(node).lastLocation();
  }
  @Override
  public IRLocation nextChildLocation(IRNode node, IRLocation loc) {
    return getChildren(node).nextLocation(loc);
  }
  @Override
  public IRLocation prevChildLocation(IRNode node, IRLocation loc) {
    return getChildren(node).prevLocation(loc);
  }

  @Override
  public int compareChildLocations(
    IRNode node,
    IRLocation loc1,
    IRLocation loc2) {
    return getChildren(node).compareLocations(loc1, loc2);
  }

  @Override
  public boolean hasChild(IRNode node, int i) {
    return getChildren(node).validAt(i);
  }
  @Override
  public boolean hasChild(IRNode node, IRLocation loc) {
    return getChildren(node).validAt(loc);
  }
  @Override
  public IRNode getChild(IRNode node, int i) {
    try {
      return (getChildren(node).elementAt(i));
    } catch (SlotUndefinedException e) {
      if (this instanceof SyntaxTree) {
        SyntaxTree t = (SyntaxTree) this;
        LOG.severe(
          "i = " + i + ", op = " + t.getOperator(node) + " for " + node);
      }
      throw e;
    }
  }
  @Override
  public IRNode getChild(IRNode node, IRLocation loc) {
    try {
      return (getChildren(node).elementAt(loc));
    } catch (SlotUndefinedException e) {
      if (this instanceof SyntaxTree) {
        SyntaxTree t = (SyntaxTree) this;
        LOG.severe(
          "loc = " + loc + ", op = " + t.getOperator(node) + " for " + node);
      }
      throw e;
    }
  }

  /**
	 * return the location of a child within the children of node.
	 * 
	 * @exception IllegalChildException
	 *              if the child is not present
	 */
  protected IRLocation findChild(IRNode node, IRNode child)
    throws IllegalChildException {
    IRSequence<IRNode> children = getChildren(node);
    for (IRLocation loc = children.firstLocation();
      loc != null;
      loc = children.nextLocation(loc)) {
      if (children.validAt(loc)) {
        IRNode ch = children.elementAt(loc);
        if (child == ch || (child != null && child.equals(ch))) {
          return loc;
        }
      }
    }
    throw new IllegalChildException("not a child of node");
  }

  /**
	 * Return the location within the children of a node that is current
	 * undefined.
	 * 
	 * @exception IllegalChildException
	 *              if all are defined.
	 */
  protected IRLocation findUndefinedChild(IRNode node)
    throws IllegalChildException {
    IRSequence<IRNode> children = getChildren(node);
    for (IRLocation loc = children.firstLocation();
      loc != null;
      loc = children.nextLocation(loc)) {
      if (!children.validAt(loc))
        return loc;
    }
    throw new IllegalChildException("no undefined children of node");
  }

  /**
	 * Set the i'th child of the node to be newChild.
	 * 
	 * @exception IllegalChildException
	 *              if the child is not suitable
	 */
  @Override
  public void setChild(IRNode node, int i, IRNode newChild)
    throws IllegalChildException {
    IRLocation loc = getChildren(node).location(i);
    if (loc == null) {
      System.out.println("operator is " + JJNode.tree.getOperator(node).name());
      throw new IllegalChildException("can't set " + i + "'th child of fixed size sequence of size" +
          getChildren(node).size());
    }
    setChild(node, loc, newChild);
  }
  /**
	 * Set the child at location loc of the node to be newChild.
	 * 
	 * @exception IllegalChildException
	 *              if the child is not suitable
	 */
  @Override
  public void setChild(IRNode node, IRLocation loc, IRNode newChild)
    throws IllegalChildException {
    mutator.setChild(node, loc, newChild);
  }

  /**
	 * Check to see if a node can accept another child.
	 * 
	 * @see #addChild
	 */
  protected boolean canAdopt(IRNode node) {
    if (node == null)
      return true;
    IRSequence<IRNode> children = getChildren(node);
    if (children.isVariable())
      return true;
    for (IRLocation loc = children.firstLocation();
      loc != null;
      loc = children.nextLocation(loc)) {
      if (!children.validAt(loc) || children.elementAt(loc) == null)
        return true;
    }
    return false;
  }

  /**
	 * Adopt a new child to the children without disturbing existing children. If
	 * the children are fixed in size, we look for an undefined or null child
	 * location. If the children are variable in size, we append to the end.
	 * 
	 * @exception IllegalChildException
	 *              if there is no space to add
	 */
  @Override
  public void addChild(IRNode node, IRNode newChild)
    throws IllegalChildException {
    if (getChildren(node).isVariable()) {
      appendChild(node, newChild);
    } else {
      try {
        setChild(node, findUndefinedChild(node), newChild);
      } catch (IllegalChildException ex) {
        setChild(node, findChild(node, null), newChild);
      }
    }
  }

  /**
	 * Replace the node's oldChild with newChild.
	 * 
	 * @exception IllegalChildException
	 *              if oldChild is not a child, or newChild is not suitable.
	 */
  @Override
  public void replaceChild(IRNode node, IRNode oldChild, IRNode newChild)
    throws IllegalChildException {
    setChild(node, findChild(node, oldChild), newChild);
  }

  /**
	 * Add new child as a new child of node at the given insertion point.
	 * 
	 * @exception IllegalChildException
	 *              if newChild is not suitable or the parent cannot accept new
	 *              children.
	 * @return location of new child
	 */
  @Override
  public IRLocation insertChild(
    IRNode node,
    IRNode newChild,
    InsertionPoint ip)
    throws IllegalChildException {
    return mutator.insertChild(node, newChild, ip);
  }

  /**
	 * Add newChild as a new first child of node.
	 * 
	 * @exception IllegalChildException
	 *              if newChild is not suitable or the parent cannot accept new
	 *              children.
	 */
  @Override
  public void insertChild(IRNode node, IRNode newChild)
    throws IllegalChildException {
    insertChild(node, newChild, InsertionPoint.first);
  }

  /**
	 * Add newChild as a new last child of node.
	 * 
	 * @exception IllegalChildException
	 *              if newChild is not suitable or the parent cannot accept new
	 *              children.
	 */
  @Override
  public void appendChild(IRNode node, IRNode newChild)
    throws IllegalChildException {
    insertChild(node, newChild, InsertionPoint.last);
  }

  /**
	 * Add newChild as a new child after the given child of node.
	 * 
	 * @exception IllegalChildException
	 *              if oldChild is not a child, newChild is not suitable, or the
	 *              parent cannot accept new children.
	 */
  @Override
  public void insertChildAfter(IRNode node, IRNode newChild, IRNode oldChild)
    throws IllegalChildException {
    insertChild(
      node,
      newChild,
      InsertionPoint.createAfter(findChild(node, oldChild)));
  }

  /**
	 * Add newChild as a new child before the given child of node.
	 * 
	 * @exception IllegalChildException
	 *              if if oldChild is not a child, newChild is not suitable, or
	 *              the parent cannot accept new children.
	 */
  @Override
  public void insertChildBefore(IRNode node, IRNode newChild, IRNode oldChild)
    throws IllegalChildException {
    insertChild(
      node,
      newChild,
      InsertionPoint.createBefore(findChild(node, oldChild)));
  }

  /**
	 * Remove oldChild from the sequence of children of a node. If the sequence
	 * is variable, we get rid of its location too, otherwise, we substitute
	 * null.
	 * 
	 * @see #addChild
	 * @exception IllegalChildException
	 *              if oldChild is not a child,
	 */
  @Override
  public void removeChild(IRNode node, IRNode oldChild)
    throws IllegalChildException {
    removeChild(node, findChild(node, oldChild));
  }

  /**
	 * Remove the child (if any) at the given location. Replace with null if
	 * sequence is fixed.
	 */
  @Override
  public void removeChild(IRNode node, IRLocation loc) {
    if (getChildren(node).isVariable()) {
      mutator.removeChild(node, loc);
    } else {
      setChild(node, loc, null);
    }
  }

  /** Remove all the children of a node. */
  @Override
  public void removeChildren(IRNode node) {
    IRSequence children = getChildren(node);
    for (IRLocation loc = firstChildLocation(node); loc != null;) {
      IRLocation next = children.nextLocation(loc);
      removeChild(node, loc);
      loc = next;
    }
  }

  /** Return the children of a node in order. */
  @Override
  public Iteratable<IRNode> children(IRNode node) {
	final IRSequence<IRNode> seq = getChildren(node);
	if (seq == null) {
		return new EmptyIterator<IRNode>();
	}
    return mutator.protect(seq.elements());
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public List<IRNode> childList(IRNode node) {
    // rather roundabout, but it uses existing abstractions
    // and it isn't very inefficient actually -- just two levels of wrappers.
    // NB: We MUST use the wrapped children so that mutations are properly tracked.  Otherwise,
    // tree invariants will be broken.
    SlotInfo<IRSequence<IRNode>> wrapped_children_attr = mutator.getAttribute(CHILDREN);
    IRSequence<IRNode> wrapped_children = node.getSlotValue(wrapped_children_attr);
    return new IRSequenceList<IRNode>(wrapped_children);
  }

  /**
	 * Return an enumeration of the nodes in the graph. First we return the root
	 * given and then recursively the enumerations of each of its children.
	 */
  @Override
  public Iteratable<IRNode> depthFirstSearch(IRNode node) {
    return mutator.protect(new DepthFirstSearch(this, node));
  }

  /**
	 * Return slot info for given name. Consistency is
	 */
  @Override
  public SlotInfo getAttribute(String name) {
    return mutator.getAttribute(name);
  }

  /**
	 * Add digraph attributes to a bundle.
	 */
  public void saveAttributes(Bundle b) {
    mutator.saveAttributes(b);
  }

  /**
	 * Describe information about this node for debugging.
	 */
  public void describeNode(IRNode n, PrintStream out) {
    childrenSlotInfo.describeSlot(n, out);
    try {
      IRSequence ch = getChildren(n);
      out.print("  children => ");
      ch.describe(out);
    } catch (SlotUndefinedException ex) {
      // discard exception (just debugging)
    }
  }

  protected class StoredMutator implements Mutator {
    protected /* final */
    SlotFactory slotFactory;

    protected StoredMutator(SlotFactory sf) {
      slotFactory = sf;
    }

    @Override
    public void initNode(IRNode n, int numChildren) {
      IRSequence<IRNode> seq = slotFactory.newSequence(numChildren);
      setChildren(n, seq);
      notifyIRObservers(n); // the children attribute is no longer constant.
      if (hasListeners()) {
        informDigraphListeners(new NewNodeEvent(Digraph.this, n));
      }
    }

    /**
		 * Called to declare that a node has a new parent. Overridden in subclasses
		 * to record the information.
		 * 
		 * @param initial
		 *          true if this parent->child binding is the first definition for
		 *          this parent.
		 * @return true if initial and this is initial parent for child.
		 * @see Tree#addParent
		 * @see SymmetricDigraph#addParent
		 */
    protected boolean addParent(
      IRNode child,
      IRNode parent,
      IRLocation loc,
      boolean initial) {
      return initial;
    }

    /**
		 * Called to declare that a node has lost a parent. Overridden in
		 * subclasses to record the information.
		 * 
		 * @see Tree#removeParent
		 * @see SymmetricDigraph#removeParent
		 */
    protected void removeParent(IRNode child, IRNode parent, IRLocation loc) {
    }

    /**
		 * Called to check if a node is suitable as a new child for a particular
		 * parent node and location.
		 * 
		 * @exception IllegalChildException
		 *              if the child is not suitable
		 */
    protected void checkNewChild(IRNode parent, IRLocation loc, IRNode child)
      throws IllegalChildException {
    }

    /**
		 * Called to check if a node is suitable as an additional child for a
		 * particular parent node.
		 * 
		 * @exception IllegalChildException
		 *              if the child is not suitable or if the node cannot take a
		 *              variable number of children.
		 */
    protected void checkNewVariableChild(IRNode parent, IRNode child)
      throws IllegalChildException {
      if (!getChildren(parent).isVariable()) {
        throw new IllegalChildException("node cannot accept new children");
      }
    }

    @Override
    public void setChild(IRNode node, IRLocation loc, IRNode newChild)
      throws IllegalChildException {
      /*
			 * if (newChild == null) { LOG.debug("newChild is null at version
			 * "+Version.getVersion()); }
			 */
      IRSequence<IRNode> children = getChildren(node);
      IRNode oldChild;
      boolean oldChildDefined;

      checkNewChild(node, loc, newChild);

      if (children.validAt(loc)) {
        oldChild = children.elementAt(loc);
        oldChildDefined = true;
        if (oldChild == newChild
          || (oldChild != null && oldChild.equals(newChild)))
          return;
      } else {
        oldChild = null;
        oldChildDefined = false;
      }
      boolean initial = !oldChildDefined;
      if (newChild != null) {
        initial = addParent(newChild, node, loc, initial);
      }
      if (!oldChildDefined && !initial) // force null to be initial binding
        children.setElementAt(null, loc);

      children.setElementAt(newChild, loc);
      if (oldChild != null) {
        removeParent(oldChild, node, loc);
      }
      if (!initial)
        notifyIRObservers(node);
      else
        notifyDefineObservers(node); //? Is this a definition still ?
      if (hasListeners()) {
        DigraphEvent e;
        if (!initial)
          e =
            new ChangedChildEvent(Digraph.this, node, loc, oldChild, newChild);
        else
          e = new NewChildEvent(Digraph.this, node, loc, newChild);
        informDigraphListeners(e);
      }
    }

    @Override
    public IRLocation insertChild(
      IRNode node,
      IRNode newChild,
      InsertionPoint ip)
      throws IllegalChildException {
      IRSequence<IRNode> children = getChildren(node);
      checkNewVariableChild(node, newChild);
      IRLocation newloc = ip.insert(children, newChild);
      if (newChild != null) {
        addParent(newChild, node, newloc, false);
      }
      notifyIRObservers(node);
      if (hasListeners()) {
        DigraphEvent ev =
          new NewChildEvent(Digraph.this, node, newloc, newChild);
        informDigraphListeners(ev);
      }
      return newloc;
    }

    @Override
    public void removeChild(IRNode node, IRLocation loc) {
      IRSequence children = getChildren(node);
      IRNode oldChild = null;
      if (children.validAt(loc))
        oldChild = (IRNode) children.elementAt(loc);
      children.removeElementAt(loc);
      if (oldChild != null)
        removeParent(oldChild, node, loc);
      notifyIRObservers(node);
      if (hasListeners()) {
        informDigraphListeners(
          new RemoveChildEvent(Digraph.this, node, loc, oldChild));
      }
    }

    @Override
    public void addObserver(Observer o) {
      childrenSlotInfo.addObserver(o);
    }

    @Override
    public void saveAttributes(Bundle b) {
      b.saveAttribute(childrenSlotInfo);
    }
    @Override
    @SuppressWarnings("unchecked")
    public Iteratable<IRNode> protect(Iteratable e) {
      return slotFactory.newIterator((Iteratable<IRNode>)e);
    }

    protected final SlotInfo wrappedChildrenAttribute =
      new WrappedChildrenSlotInfo();

    @Override
    public SlotInfo getAttribute(String name) {
      if (name.equals(CHILDREN))
        return wrappedChildrenAttribute;
      else
        return null;
    }

    class WrappedChildrenSlotInfo extends DerivedSlotInfo<IRSequence<IRNode>> {
      @Override
      public IRType<IRSequence<IRNode>> type() {
        return childrenType;
      }
      @Override
      protected boolean valueExists(IRNode node) {
        return isNode(node);
      }
      @Override
      protected IRSequence<IRNode> getSlotValue(IRNode node) {
        return new ChildrenWrapper(node, getChildren(node));
      }
      @Override
      protected void setSlotValue(IRNode node, IRSequence<IRNode> seq) {
        int numChildren = seq.size();
        int initChildren = numChildren;
        if (seq.isVariable())
          initChildren = ~initChildren;
        initNode(node, initChildren);
        IRSequence<IRNode> seqp = getChildren(node);
        IRLocation loc, locp;
        for (loc = seq.firstLocation(), locp = seqp.firstLocation();
          loc != null;
          loc = seq.nextLocation(loc), locp = seqp.nextLocation(locp)) {
          if (seq.validAt(loc)) {
            setChild(node, locp, seq.elementAt(loc));
          }
        }
      }
    }

    protected class ChildrenWrapper extends IRSequenceWrapper<IRNode> {
      final IRNode parent;
      public ChildrenWrapper(IRNode node, IRSequence<IRNode> real) {
        super(real);
        parent = node;
      }
      @Override
      public void setElementAt(IRNode node, IRLocation loc) {
        setChild(parent, loc, node);
      }
      @Override
      public IRLocation insertElementAt(IRNode obj, InsertionPoint ip) {
        return insertChild(parent, obj, ip);
      }
      @Override
      public void removeElementAt(IRLocation loc) {
        removeChild(parent, loc);
      }
    }
  }

  protected class DelegatingMutator implements Mutator {
    @Override
    public void initNode(IRNode node, int numChildren) {
      IRSequence<IRNode> seq = SimpleSlotFactory.prototype.newSequence(numChildren);
      setChildren(node, seq);
    }
    @Override
    public void setChild(IRNode node, IRLocation loc, IRNode child) {
      getChildren(node).setElementAt(child, loc);
    }
    @Override
    public IRLocation insertChild(
      IRNode node,
      IRNode newChild,
      InsertionPoint ip) {
      return ip.insert(getChildren(node), newChild);
    }
    @Override
    public void removeChild(IRNode node, IRLocation loc) {
      getChildren(node).removeElementAt(loc);
    }
    
    @Override
    public void addObserver(Observer o) {
      // nothing
    }
    
    @Override
    public void saveAttributes(Bundle b) {
    }
    @Override
    public SlotInfo getAttribute(String name) {
      if (name == CHILDREN)
        return childrenSlotInfo;
      else
        return null;
    }
    @Override
    @SuppressWarnings("unchecked")
    public Iteratable<IRNode> protect(Iteratable e) {
      return e;
    }
  }
}
