package edu.cmu.cs.fluid.mvc.tree;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

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
import edu.cmu.cs.fluid.util.EmptyIterator;
import edu.cmu.cs.fluid.util.ImmutableSet;
import edu.cmu.cs.fluid.util.Iteratable;

/**
 * Abstract implemenation of core class for <code>DigraphModel</code>.
 * <p>
 * Adds the node-level attribute {@link DigraphModel#CHILDREN}.
 * 
 * <p>
 * The callback provided to the constructor must handle changes to the
 * sequences in the {@link SymmetricDigraphModel#PARENTS}attribute.
 * 
 * @author Aaron Greenhouse
 */
public final class DigraphModelCore extends AbstractCore {
  public static final Logger CORE = SLLogger.getLogger("MV.core");

  //===========================================================
  //== Fields
  //===========================================================

  /**
	 * Whether the Attributes/Sequences in the attributes are allowed to be
	 * mutated by clients.
	 */
  protected final boolean isMutable;

  /** The structrue storing the digraph */
  protected final MutableDigraphInterface digraph;

  /** The children attribute */
  protected final SlotInfo<IRSequence<IRNode>> children;

  /** Storage for the {@link Model#isPresent}results. */
  private final SlotInfo<Boolean> isPresent;

  //===========================================================
  //== Constructor
  //===========================================================

  private DigraphModelCore(
    final String name,
    final SlotInfo<IRSequence<IRNode>> childrenSI,
    final SlotFactory sf,
    final boolean mutable,
    final Model model,
    final Object lock,
    final AttributeManager manager,
    final AttributeChangedCallback cb)
    throws SlotAlreadyRegisteredException {
    // Init the tree delegate
    super(model, lock, manager);
    digraph = new Digraph(new MaskingAttributeWrapper(childrenSI));
    isMutable = mutable;

    // Init node attributes
    children = digraph.getAttribute(Digraph.CHILDREN);
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

    // init the local storage for explicitly managing what nodes
    // are part of the model
    isPresent =
      sf.newAttribute(
        name + "-isPresent",
        IRBooleanType.prototype,
        Boolean.FALSE);
  }

  /**
	 * Create a new model core around an existing Digraph. In particular, the
	 * child attribute is derived from
	 * {@link edu.cmu.cs.fluid.tree.Digraph#CHILDREN}attribute of the given
	 * Digraph.
	 */
  protected DigraphModelCore(
    final String name,
    final MutableDigraphInterface dg,
    final SlotFactory sf,
    final boolean mutable,
    final Model model,
    final Object lock,
    final AttributeManager manager,
    final AttributeChangedCallback cb)
    throws SlotAlreadyRegisteredException {
    this(
      name,
      dg.getAttribute(Digraph.CHILDREN),
      sf,
      mutable,
      model,
      lock,
      manager,
      cb);
  }

  /**
	 * Create a new model core whose children attribute is newly created using
	 * the given SlotFactory.
	 */
  protected DigraphModelCore(
    final String name,
    final SlotFactory sf,
    final boolean mutable,
    final Model model,
    final Object lock,
    final AttributeManager manager,
    final AttributeChangedCallback cb)
    throws SlotAlreadyRegisteredException {
    this(
      name,
      sf.newAttribute(name + ".Digraph.children", Digraph.childrenType),
      sf,
      mutable,
      model,
      lock,
      manager,
      cb);
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin Digraph
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //===========================================================
  //== DigraphInterface Methods
  //===========================================================

  public SlotInfo getAttribute(String name) {
    if (name.equals(Digraph.CHILDREN)) {
      return attrManager.getNodeAttribute(DigraphModel.CHILDREN);
    }
    return null;
  }

  /**
	 * Return true when the node has at first one child location.
	 */
  public boolean hasChildren(final IRNode node) {
    return digraph.hasChildren(node);
  }

  /**
	 * Return the number of children, defined or undefined, null or nodes.
	 */
  public int numChildren(final IRNode node) {
    return digraph.numChildren(node);
  }

  /** Return the location for child #i */
  public IRLocation childLocation(final IRNode node, final int i) {
    return digraph.childLocation(node, i);
  }

  /** Return the numeric location of a location */
  public int childLocationIndex(final IRNode node, final IRLocation loc) {
    return digraph.childLocationIndex(node, loc);
  }

  /** Return the location of the first child. */
  public IRLocation firstChildLocation(final IRNode node) {
    return digraph.firstChildLocation(node);
  }

  /** Return the location of the last child. */
  public IRLocation lastChildLocation(final IRNode node) {
    return digraph.lastChildLocation(node);
  }

  /** Return the location of the next child (or null). */
  public IRLocation nextChildLocation(
    final IRNode node,
    final IRLocation loc) {
    return digraph.nextChildLocation(node, loc);
  }

  /** Return the location of the previous child (or null). */
  public IRLocation prevChildLocation(
    final IRNode node,
    final IRLocation loc) {
    return digraph.prevChildLocation(node, loc);
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
  public int compareChildLocations(
    final IRNode node,
    final IRLocation loc1,
    final IRLocation loc2) {
    return digraph.compareChildLocations(node, loc1, loc2);
  }

  /**
	 * Return true if the child is defined.
	 * 
	 * @exception IRSequenceException
	 *              if the index is out of range
	 */
  public boolean hasChild(final IRNode node, final int i) {
    return digraph.hasChild(node, i);
  }

  /**
	 * Return true if the child is defined.
	 * 
	 * @exception IRSequenceException
	 *              if the location is invalid (or null).
	 */
  public boolean hasChild(final IRNode node, final IRLocation loc) {
    return digraph.hasChild(node, loc);
  }

  /** Return the i'th child of a node. */
  public IRNode getChild(final IRNode node, final int i) {
    if (!isPresent(node)) {
      return null;
    }
    return digraph.getChild(node, i);
  }

  /** Return the child at location loc. */
  public IRNode getChild(final IRNode node, final IRLocation loc) {
    if (!isPresent(node)) {
      return null;
    }
    return digraph.getChild(node, loc);
  }

  /** Return the children of a node in order. */
  public Iteratable<IRNode> children(final IRNode node) {
    if (!isPresent(node)) {
      return new EmptyIterator<IRNode>();
    }
    return digraph.children(node);
  }

  public List<IRNode> childList(IRNode node) {
    if (!isPresent(node)) {
      return Collections.emptyList();
    }
    return digraph.childList(node);
  }
  
  /**
	 * Attach a listener to the digraph. If the digraph is mutated, the listener
	 * should be called.
	 */
  public void addDigraphListener(final DigraphListener dl) {
    digraph.addDigraphListener(dl);
  }

  /** Detach a listener from the digraph. */
  public void removeDigraphListener(DigraphListener dl) {
    digraph.removeDigraphListener(dl);
  }

  //===========================================================
  //== Mutable Digraph Methods
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
  public void initNode(final IRNode n) {
    digraph.initNode(n);
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
  public void initNode(final IRNode n, final int numChildren) {
    digraph.initNode(n, numChildren);
  }

  /** Return true if the node is part of this direction graph. */
  public boolean isNode(final IRNode n) {
    return digraph.isNode(n);
  }

  /**
	 * Set the i'th child of the node to be newChild.
	 * 
	 * @exception StructureException
	 *              if the child is not suitable
	 */
  public void setChild(final IRNode node, final int i, final IRNode newChild)
    throws StructureException {
    digraph.setChild(node, i, newChild);
  }

  /**
	 * Set the child at location loc of the node to be newChild.
	 * 
	 * @exception StructureException
	 *              if the child is not suitable
	 */
  public void setChild(
    final IRNode node,
    final IRLocation loc,
    final IRNode newChild)
    throws StructureException {
    digraph.setChild(node, loc, newChild);
  }

  /**
	 * Adopt a new child to the children without disturbing existing children. If
	 * the children are fixed in size, we look for an undefined or null child
	 * location. If the children are variable in size, we append to the end.
	 * 
	 * @exception StructureException
	 *              if there is no space to add
	 */
  public void addChild(final IRNode node, final IRNode newChild)
    throws StructureException {
    digraph.addChild(node, newChild);
  }

  /**
	 * Replace the node's oldChild with newChild.
	 * 
	 * @exception StructureException
	 *              if oldChild is not a child, or newChild is not suitable.
	 */
  public void replaceChild(
    final IRNode node,
    final IRNode oldChild,
    final IRNode newChild)
    throws StructureException {
    digraph.replaceChild(node, oldChild, newChild);
  }

  /**
	 * Add new child as a new child of node at the given insertion point.
	 * 
	 * @exception StructureException
	 *              if newChild is not suitable or the parent cannot accept new
	 *              children.
	 * @return location of new child
	 */
  public IRLocation insertChild(
    final IRNode node,
    final IRNode newChild,
    final InsertionPoint ip)
    throws StructureException {
    return digraph.insertChild(node, newChild, ip);
  }

  /**
	 * Add newChild as a new first child of node.
	 * 
	 * @exception StructureException
	 *              if newChild is not suitable or the parent cannot accept new
	 *              children.
	 */
  public void insertChild(final IRNode node, final IRNode newChild)
    throws StructureException {
    digraph.insertChild(node, newChild);
  }

  /**
	 * Add newChild as a new last child of node.
	 * 
	 * @exception StructureException
	 *              if newChild is not suitable or the parent cannot accept new
	 *              children.
	 */
  public void appendChild(final IRNode node, final IRNode newChild)
    throws StructureException {
    digraph.appendChild(node, newChild);
  }

  /**
	 * Add newChild as a new child after the given child of node.
	 * 
	 * @exception StructureException
	 *              if oldChild is not a child, newChild is not suitable, or the
	 *              parent cannot accept new children.
	 */
  public void insertChildAfter(
    final IRNode node,
    final IRNode newChild,
    final IRNode oldChild)
    throws StructureException {
    digraph.insertChildAfter(node, newChild, oldChild);
  }

  /**
	 * Add newChild as a new child before the given child of node.
	 * 
	 * @exception StructureException
	 *              if if oldChild is not a child, newChild is not suitable, or
	 *              the parent cannot accept new children.
	 */
  public void insertChildBefore(
    final IRNode node,
    final IRNode newChild,
    final IRNode oldChild)
    throws StructureException {
    digraph.insertChildBefore(node, newChild, oldChild);
  }

  /**
	 * Remove oldChild from the sequence of children of a node. If the sequence
	 * is variable, we get rid of its location too, otherwise, we substitute
	 * null.
	 * 
	 * @exception StructureException
	 *              if oldChild is not a child,
	 */
  public void removeChild(final IRNode node, final IRNode oldChild)
    throws StructureException {
    digraph.removeChild(node, oldChild);
  }

  /**
	 * Remove the child (if any) at the given location. Replace with null if
	 * sequence is fixed.
	 */
  public void removeChild(final IRNode node, final IRLocation loc) {
    digraph.removeChild(node, loc);
  }

  /** Remove all the children of a node. */
  public void removeChildren(final IRNode node) {
    digraph.removeChildren(node);
  }

  /**
	 * Return an enumeration of the nodes in the graph. First we return the root
	 * given and then recursively the enumerations of each of its children.
	 */
  public Iteratable<IRNode>depthFirstSearch(final IRNode node) {
    return digraph.depthFirstSearch(node);
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End Digraph
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

  public boolean isPresent(final IRNode node) {
    return (node.getSlotValue(isPresent)).booleanValue();
  }

  public Iterator<IRNode> getNodes() {
    final ImmutableSet<IRNode> nodes = isPresent.index(Boolean.TRUE);
    if ((nodes != null) && !nodes.isInfinite()) {
      return nodes.iterator();
    } else {
      return new EmptyIterator<IRNode>();
    }
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End Model Portion
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin DigraphModel Portion
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  public void removeEdges(final IRNode node) {
    // Simple part, remove all my children
    digraph.removeChildren(node);

    // harder part, remove all my parents
    final SlotInfo<IRSequence> childrenMap = digraph.getAttribute(Digraph.CHILDREN);
    final Iterator<IRNode> nodes = getNodes();
    while (nodes.hasNext()) {
      final IRNode curNode = nodes.next();
      final Iterator<IRNode>children =
        new IRSequenceValidatingIterator<IRNode>(
          curNode.getSlotValue(childrenMap));
      while (children.hasNext()) {
        final IRNode n = children.next();
        if (n.equals(node))
          children.remove();
      }
    }
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End DigraphModel Portion
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Node membership management
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  /**
	 * Insure that a node is not in the model. Caller must hold the model's
	 * structural lock.
	 */
  public void removeNode(final IRNode node) {
    node.setSlotValue(isPresent, Boolean.FALSE);
  }

  /**
	 * Insure that a node is in the model. Invo Caller must hold the model's
	 * structural lock.
	 */
  public void addNode(final IRNode node) {
    if (!((node.getSlotValue(isPresent)).booleanValue())) {
      if (!digraph.isNode(node))
        digraph.initNode(node);
      node.setSlotValue(isPresent, Boolean.TRUE);
    }
  }

  /**
	 * Remove all nodes from model, giving it a cardinality of zero. Also clears
	 * all the edges. Caller must hold the model's structural lock.
	 */
  public void clearModel() {
    final Iterator<IRNode> nodes = isPresent.index(Boolean.TRUE).iterator();
    while (nodes.hasNext()) {
      final IRNode node = nodes.next();
      node.setSlotValue(isPresent, Boolean.FALSE);
      digraph.removeChildren(node);
    }
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End node management
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin Factories
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  public static interface Factory {
    public DigraphModelCore create(
      String name,
      Model model,
      Object structLock,
      AttributeManager manager,
      AttributeChangedCallback cb)
      throws SlotAlreadyRegisteredException;
  }

  public static class StandardFactory implements Factory {
    protected final SlotFactory slotFactory;
    protected final boolean isMutable;

    public StandardFactory(final SlotFactory sf, final boolean mutable) {
      slotFactory = sf;
      isMutable = mutable;
    }

    @Override
    public DigraphModelCore create(
      final String name,
      final Model model,
      final Object structLock,
      final AttributeManager manager,
      final AttributeChangedCallback cb)
      throws SlotAlreadyRegisteredException {
      DigraphModelCore dmc =
        new DigraphModelCore(
          name,
          slotFactory,
          isMutable,
          model,
          structLock,
          manager,
          cb);
      return dmc;
    }
  }

  public static class DelegatingFactory implements Factory {
    protected final MutableDigraphInterface digraph;
    protected final IRSequence roots;
    protected final boolean isMutable;
    protected final SlotFactory slotFactory;

    public DelegatingFactory(
      final MutableDigraphInterface dg,
      final IRSequence rts,
      final SlotFactory sf,
      final boolean mutable) {
      digraph = dg;
      roots = rts;
      slotFactory = sf;
      isMutable = mutable;
    }

    @Override
    public DigraphModelCore create(
      final String name,
      final Model model,
      final Object structLock,
      final AttributeManager manager,
      final AttributeChangedCallback cb)
      throws SlotAlreadyRegisteredException {
      return new DigraphModelCore(
        name,
        digraph,
        slotFactory,
        isMutable,
        model,
        structLock,
        manager,
        cb);
    }
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End Factories
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Wrappers for making the children attribute mask out
  //-- nodes that are not present in the model.
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  private final class MaskingSequenceWrapper extends IRSequenceWrapper<IRNode> {
    public MaskingSequenceWrapper(final IRSequence<IRNode> s) {
      super(s);
    }

    @Override
    public Iteratable<IRNode> elements() {
      /*
			 * Create a new enumeration based on the wrapped presentation. This way
			 * the enumeration picks up any changes made by the wrapper.
			 */
      return new IRSequenceIterator<IRNode>(this);
    }

    @Override
    public boolean validAt(final IRLocation loc) {
      /*
			 * peek at the element in the underlying sequence. If it is not a part of
			 * the model then the location is not valid.
			 */
      if (sequence.validAt(loc)) {
        final IRNode elt = sequence.elementAt(loc);
        return isPresent(elt);
      } else {
        return false;
      }
    }

    @Override
    public IRNode elementAt(final IRLocation loc) {
      /*
			 * peek at the element in the underlying sequence. If it is not part of
			 * the model then throw a SlotUndefinedException. Peeking may cause an
			 * SlotUndefinedException if the underlying element is not defined. This
			 * is okay, and we propogate the exception.
			 */
      final IRNode elt = sequence.elementAt(loc);
      if (isPresent(elt)) {
        return elt;
      } else {
        throw new SlotUndefinedException("Element masked out of sequence");
      }
    }
  }

  private class MaskingAttributeWrapper extends SlotInfoWrapper<IRSequence<IRNode>> {
    /** Creates a new instance of MutableSequenceReturningImmutableAttribute */
    public MaskingAttributeWrapper(final SlotInfo<IRSequence<IRNode>> si) {
      super(si);
    }

    @Override
    protected IRSequence<IRNode> getSlotValue(final IRNode node)
      throws SlotUndefinedException {
      // Wrap the result on the way out. Could cache this, but
      // is it worth it?
      final IRSequence<IRNode> seq = node.getSlotValue(wrapped);
      return new MaskingSequenceWrapper(seq);
    }
  }
}
