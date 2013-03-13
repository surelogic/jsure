/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/EdgeDigraph.java,v 1.34 2007/07/05 18:15:16 aarong Exp $ */
package edu.cmu.cs.fluid.tree;

import java.util.*;

import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.NotImplemented;
import edu.cmu.cs.fluid.ir.*;

/** A form of graph in which edges have identity and may be attributed.
 * <P> Known bugs:
 * <ul>
 * <li> See {@link Digraph}.
 * </ul>
 */
public class EdgeDigraph extends DigraphMixin
     implements EdgeDigraphInterface, Observer, DigraphListener
{
  public static final String CHILD_EDGES = "childEdges";
  public static final String SINKS = "sinks";
  public static final String IS_EDGE = "isEdge";


  protected interface Mutator {
    public void initNode(IRNode n, int numChildren);
    public void initEdge(IRNode e);
    public void setSink(IRNode e, IRNode n);
    public void setChildEdge(IRNode node, IRLocation loc, IRNode newChildEdge);
    public IRLocation insertChildEdge(IRNode node, IRNode newChildEdge,
				      InsertionPoint ip);
    public void removeChildEdge(IRNode node, IRNode oldChildEdge);
    public Iteratable<IRNode> protect(Iteratable enm);
    public void saveAttributes(Bundle b);
    public SlotInfo getAttribute(String name);
  }

  protected /*final*/ Mutator mutator;

  protected Mutator createStoredMutator(SlotFactory sf) {
    return new StoredMutator(sf);
  }
  protected Mutator createDelegatingMutator() {
    return new DelegatingMutator();
  }

  protected /*final*/ Digraph asDigraph;

  @SuppressWarnings("unchecked")
  protected Digraph createAsDigraph() {
    return new Digraph(getAttribute("children"));
  }
  public Digraph getAsDigraph() {
    return asDigraph;
  }

  // We have two underlying directed graphs, one which
  // knows how to get from a node to an edge and the other
  // which knows how to go from an edge to a node.
  // In the case that we actually build the graph, we only
  // use one graph, but in the case we are delegating
  // we have two.

  /** An underlying digraph in which edges are nodes. */
  protected /*final*/ Digraph underlyingNodes;
  protected /*final*/ Digraph underlyingEdges;

  /** A slot specifying nodes are edges or node.
   * True for edges, False for nodes.
   */
  private /*final*/ SlotInfo<Boolean> isEdgeSlotInfo;

  public EdgeDigraph(String name, SlotFactory sf)
       throws SlotAlreadyRegisteredException
  {
    this(name,sf,new Digraph(name,sf));
  }

  @SuppressWarnings("unchecked")
  protected EdgeDigraph(String name, SlotFactory sf, Digraph dig)
       throws SlotAlreadyRegisteredException
  {
    Digraph underlying = dig;
    underlyingNodes = underlyingEdges = underlying;
    underlyingChildrenAttribute = underlyingNodes.getAttribute("children");
    underlying.addObserver(new Observer() {
      @Override
      public void update(Observable obs, Object node) {
	notifyIRObservers((IRNode)node);
      }
    });
    underlying.addDefineObserver(this);
    underlying.deleteObserver(this); // thus, just for define events.
    if (name == null)
	isEdgeSlotInfo = sf.newAttribute();
    else
      isEdgeSlotInfo = sf.newAttribute(name + ".EdgeDigraph.isEdge",
				       IRBooleanType.prototype);
    mutator = createStoredMutator(sf);
    asDigraph = createAsDigraph();
  }

  /** Create an edge directed graph that mirrors
   * the structures determined by the given (wrapped) attributes
   */
  public EdgeDigraph(SlotInfo<IRSequence<IRNode>> childEdgesAttribute,
		     SlotInfo<IRSequence<IRNode>> sinksAttribute,
		     SlotInfo<Boolean> isEdgeAttribute) {
    this (new Digraph(childEdgesAttribute),
	  new Digraph(sinksAttribute),
	  isEdgeAttribute);
  }

  /** Create an edge directed graph over the underlying
   * structure with the given (wrapped) "is edge" attribute.
   */
  @SuppressWarnings("unchecked")
  protected EdgeDigraph(Digraph nodes, Digraph edges,
			SlotInfo<Boolean> isEdgeAttribute) {
    underlyingNodes = nodes;
    underlyingEdges = edges;
    underlyingChildrenAttribute = underlyingNodes.getAttribute("children");
    isEdgeSlotInfo = isEdgeAttribute;
    mutator = createDelegatingMutator();
    asDigraph = createAsDigraph();
  }

  /** Relay define observation events from underlying graph to outside world.
   * NB: This works because we always do local modifications before
   * an underlying graph change.  Otherwise, we would be notifying
   * the observer while we were in an inconsistent state.
   */
  @Override
  public void update(Observable obs, Object node) {
    if (obs == underlyingNodes) notifyDefineObservers((IRNode)node);
  }


  /* true if we have listeners and need to relay events. */
  private boolean listening = false;

  @Override
  public void addDigraphListener(DigraphListener dl) {
    if (!listening) {
      underlyingNodes.addDigraphListener(this);
      listening = true;
    }
    super.addDigraphListener(dl);
  }

  @Override
  public void removeDigraphListener(DigraphListener dl) {
    super.removeDigraphListener(dl);
    if (listening && !hasListeners()) {
      listening = false;
      underlyingNodes.removeDigraphListener(this);
    }
  }

  /** Relay underlying events to listeners */
  @Override
  public void handleDigraphEvent(DigraphEvent ev) {
    if (hasListeners()) {
      informDigraphListeners(transformEvent(ev));
    }
  }

  protected DigraphEvent transformEvent(DigraphEvent ev) {
    if (ev instanceof NodeEvent) {
      IRNode n = ((NodeEvent)ev).getNode();
      if (isEdge(n)) {
	return transformEdgeEvent(n,ev);
      } else {
	return transformNodeEvent(n,ev);
      }
    }
    return ev;
  }

  protected DigraphEvent transformNodeEvent(IRNode n, DigraphEvent ev) {
    if (ev instanceof NewNodeEvent) {
      return new NewNodeEvent(this,n);
    } else if (ev instanceof ChildEvent) {
      IRLocation loc = ((ChildEvent)ev).getLocation();
      IRNode child = ((ChildEvent)ev).getChild();
      if (ev instanceof NewChildEvent) {
	return new NewChildEdgeEvent(this,n,loc,child);
      } else if (ev instanceof RemoveChildEvent) {
	return new RemoveChildEdgeEvent(this,n,loc,child);
      } else if (ev instanceof ChangedChildEvent) {
	IRNode oldChild = ((ChangedChildEvent)ev).getOldChild();
	return new ChangedChildEdgeEvent(this,n,loc,oldChild,child);
      }
    }
    return null;
  }
    
  protected DigraphEvent transformEdgeEvent(IRNode e, DigraphEvent ev) {
    if (ev instanceof NewNodeEvent) {
      return new NewEdgeEvent(this,e);
    } else if (ev instanceof ChildEvent) {
      IRNode sink = ((ChildEvent)ev).getChild();
      if (ev instanceof NewChildEvent) {
	return new NewSinkEvent(this,e,sink);
      } else if (ev instanceof ChangedChildEvent) {
	IRNode oldSink = ((ChangedChildEvent)ev).getOldChild();
	return new ChangedSinkEvent(this,e,oldSink,sink);
      }
    }
    return null;
  }
    

  public boolean isNode(IRNode n) {
    return underlyingNodes.isNode(n) &&
      !(n.getSlotValue(isEdgeSlotInfo)).booleanValue();
  }
  public boolean isEdge(IRNode n) {
    return underlyingEdges.isNode(n) &&
      (n.getSlotValue(isEdgeSlotInfo)).booleanValue();
  }

  public void initNode(IRNode n) {
    initNode(n,-1);
  }
  public void initNode(IRNode n, int numChildren) {
    mutator.initNode(n,numChildren);
  }
  public void initEdge(IRNode e) {
    mutator.initEdge(e);
  }
  
  /** Create a new IRNode for use as an edge.  The edge will be
   * the child edge of the given node at the given location.
   * This routine is required to implement
   * {@link #setChild(IRNode,IRLocation,IRNode)} and
   * {@link #insertChild(IRNode,IRNode,InsertionPoint)}
   * when no child edge current exists.
   * @param loc the location to create the edge for,
   *            if null, the location will be created as
   *            the child edge is inserted.
   * @return an IRNode initialized as an edge in this graph.
   */
  protected IRNode newEdge(IRNode node, IRLocation loc)
  {
    IRNode n = new PlainIRNode();
    initEdge(n);
    return n;
  }

  protected void assertNode(IRNode n) throws EdgeDigraphException
  {
    if (n == null) throw new EdgeDigraphException("null is not a node");
    if (isEdge(n)) throw new EdgeDigraphException("not a node " + n);
  }
  protected void assertEdge(IRNode e) throws EdgeDigraphException
  {
    if (e == null) throw new EdgeDigraphException("null is not an edge");
    if (!isEdge(e)) throw new EdgeDigraphException("not an edge " + e);
  }

  /** Return true if the given edge (which must be an edge in this graph)
   * currently has a sink defined, or not.
   * @exception EdgeDigraphException if e is not initialized as an edge.
   */
  public boolean hasSink(IRNode e) {
    assertEdge(e);
    return underlyingEdges.hasChild(e,0);
  }

  /** Return the sink that this edge has.
   * @return null if edge has an empty sink.
   * @exception EdgeDigraphException if e is not initialized as an edge.
   * @exception SlotUndefinedException if this edge has no sink.
   */
  @Override
  public IRNode getSink(IRNode e) {
    assertEdge(e);
    return underlyingEdges.getChild(e,0);
  }

  /** Set the sink of an edge, and if necessary keep the structure
   * consistent.
   * @throws StructureException if sink unacceptable or if node not a node
   */
  public void setSink(IRNode e, IRNode n) throws StructureException {
    mutator.setSink(e,n);
  }

  /** Return true if this node has an edge outgoing at
   * the given index.
   * @exception EdgeDigraphException if the node is not an initialized
   *            node of this graph
   * @exception IRSequenceException if the index is out of range
   */
  public boolean hasChildEdge(IRNode node, int i) {
    assertNode(node);
    return underlyingNodes.hasChild(node,i);
  }

  /** Return true if this node has an edge outgoing at
   * the given location.
   * @exception EdgeDigraphException if the node is not an initialized
   *            node of this graph
   * @exception IRSequenceException if the location is out of range
   */
  public boolean hasChildEdge(IRNode node, IRLocation loc) {
    assertNode(node);
    return underlyingNodes.hasChild(node,loc);
  }

  /** Return the i'th edge leaving a node. */
  @Override
  public IRNode getChildEdge(IRNode node, int i) {
    assertNode(node);
    return underlyingNodes.getChild(node,i);
  }

  /** Return the outgoing edge at location loc. */
  @Override
  public IRNode getChildEdge(IRNode node, IRLocation loc) {
    assertNode(node);
    return underlyingNodes.getChild(node,loc);
  }

  /** Set the i'th outgoing edge of the node to be newChildEdge.
   */
  public void setChildEdge(IRNode node, int i, IRNode newChildEdge) {
    setChildEdge(node,underlyingNodes.childLocation(node,i),newChildEdge);
  }

  /** Set the outgoing edge at location loc of the node to be newChildEdge.
   */
  public void setChildEdge(IRNode node, IRLocation loc, IRNode newChildEdge) {
    mutator.setChildEdge(node,loc,newChildEdge);
  }
  

  /** Adopt a new outgoing edge without disturbing existing
   * edges.  If number of outgoing edges are fixed in size, we look for
   * an undefined child location.
   * If the number of outgoing edges are variable in size,
   * we append to the end.
   * @exception StructureException if there is no space to add
   */
  public void addChildEdge(IRNode node, IRNode newChildEdge)
       throws StructureException
  {
    assertNode(node);
    assertEdge(newChildEdge);
    underlyingNodes.addChild(node,newChildEdge);
  }

  /** Replace the node's oldChildEdge with newChildEdge.
   * @exception StructureException if oldChildEdge is not a childEdge, or
   *            newChildEdge is not suitable.
   */
  public void replaceChildEdge(IRNode node,
			       IRNode oldChildEdge,
			       IRNode newChildEdge)
       throws StructureException
  {
    setChildEdge(node,underlyingNodes.findChild(node,oldChildEdge),newChildEdge);
  }

  /** Add newChildEdge as a new outgoing edge of the node
   * @exception StructureException if newChildEdge is not suitable
   *            or the parent cannot accept new outgoing edges.
   */
  public IRLocation insertChildEdge(IRNode node,
				    IRNode newChildEdge,
				    InsertionPoint ip)
      throws StructureException
  {
    return mutator.insertChildEdge(node,newChildEdge,ip);
  }

  /** Add newChildEdge as a new first outgoing edge of node.
   * @exception StructureException if newChildEdge is not suitable
   *            or the parent cannot accept new outgoing edges.
   */
  public void insertChildEdge(IRNode node, IRNode newChildEdge)
       throws StructureException
  {
    insertChildEdge(node,newChildEdge,InsertionPoint.first);
  }

  /** Add newChildEdge as a new last childEdge of node.
   * @exception StructureException if newChildEdge is not suitable
   *            or the parent cannot accept new outgoing edges.
   */
  public void appendChildEdge(IRNode node, IRNode newChildEdge)
       throws StructureException
  {
    insertChildEdge(node,newChildEdge,InsertionPoint.last);
  }

  /** Add newChildEdge as a new childEdge after the given childEdge of node.
   * @exception StructureException if oldChildEdge is not a childEdge, 
   *		newChildEdge is not suitable, or
   *            the parent cannot accept new outgoing edges
   */
  public void insertChildEdgeAfter(IRNode node,
				   IRNode newChildEdge,
				   IRNode oldChildEdge)
       throws StructureException
  {
    IRLocation loc = underlyingNodes.findChild(node,oldChildEdge);
    insertChildEdge(node,newChildEdge,InsertionPoint.createAfter(loc));
  }

  /** Add newChildEdge as a new childEdge before the given childEdge of node.
   * @exception StructureException if if oldChildEdge is not a childEdge, 
   *		newChildEdge is not suitable, or
   *            the parent cannot accept new outgoing edges.
   */
  public void insertChildEdgeBefore(IRNode node,
				    IRNode newChildEdge,
				    IRNode oldChildEdge)
       throws StructureException
  {
    IRLocation loc = underlyingNodes.findChild(node,oldChildEdge);
    insertChildEdge(node,newChildEdge,InsertionPoint.createBefore(loc));
  }

  /** Remove oldChildEdge from the sequence of childEdgeren of a node.
   * If the sequence is variable, we get rid of its location too,
   * otherwise, we substitute null.
   * @see #addChildEdge
   * @exception StructureException if oldChildEdge is not a childEdge,
   */
  public void removeChildEdge(IRNode node, IRNode oldChildEdge)
       throws StructureException
  {
    assertNode(node);
    if (oldChildEdge != null) assertEdge(oldChildEdge);
    underlyingNodes.removeChild(node,oldChildEdge);
  }

  /** Add edge as the link between two (non-null) nodes. */
  public void connect(IRNode edge, IRNode parent, IRNode child) {
    // redundant:
    //   assertEdge(edge);
    //   assertNode(parent);
    //   assertNode(child);
    addChildEdge(parent,edge);
    setSink(edge,child);
  }

  @Override
  public Iteratable<IRNode> childEdges(IRNode node) {
    assertNode(node);
    return underlyingNodes.children(node);
  }

  
  // And now the routines in DigraphInterface

  @Override
  public boolean hasChildren(IRNode node) {
    assertNode(node);
    return underlyingNodes.hasChildren(node);
  }
  @Override
  public int numChildren(IRNode node) {
    assertNode(node);
    return underlyingNodes.numChildren(node);
  }

  @Override
  public IRLocation childLocation(IRNode node, int i) {
    assertNode(node);
    return underlyingNodes.childLocation(node,i);
  }

  @Override
  public int childLocationIndex(IRNode node, IRLocation loc) {
    assertNode(node);
    return underlyingNodes.childLocationIndex(node,loc);
  }

  @Override
  public IRLocation firstChildLocation(IRNode node) {
    assertNode(node);
    return underlyingNodes.firstChildLocation(node);
  }

  @Override
  public IRLocation lastChildLocation(IRNode node) {
    assertNode(node);
    return underlyingNodes.lastChildLocation(node);
  }

  @Override
  public IRLocation nextChildLocation(IRNode node, IRLocation loc) {
    assertNode(node);
    return underlyingNodes.nextChildLocation(node,loc);
  }

  @Override
  public IRLocation prevChildLocation(IRNode node, IRLocation loc) {
    assertNode(node);
    return underlyingNodes.prevChildLocation(node,loc);
  }

  @Override
  public int compareChildLocations(IRNode node,
				   IRLocation loc1, IRLocation loc2) {
    assertNode(node);
    return underlyingNodes.compareChildLocations(node,loc1,loc2);
  }

  // Now the routines dealing directly with children.
  // We implement all those in DigraphInterface,
  // The side-effecting ones sometimes require the creation
  // of edges.

  /** Return true if the node has a child at the given index.
   * This condition is only satisfied if there is an outgoing edge
   * at the given index, and that edge has a (possibly null) sink.
   */
  @Override
  public boolean hasChild(IRNode node, int i) {    
    return hasChildEdge(node,i) &&
      hasSink(getChildEdge(node,i));
  }
  /** Return true if the node has a child at the given index.
   * This condition is only satisfied if there is an outgoing edge
   * at the given location, and that edge has a (possibly null) sink.
   */
  @Override
  public boolean hasChild(IRNode node, IRLocation loc) {
    return hasChildEdge(node,loc) &&
      hasSink(getChildEdge(node,loc));
  }

  @Override
  public IRNode getChild(IRNode node, int i) {
    return getSink(getChildEdge(node,i));
  }
  @Override
  public IRNode getChild(IRNode node, IRLocation loc) {
    return getSink(getChildEdge(node,loc));
  }

  @Override
  public Iteratable<IRNode> children(IRNode n) {
    return mutator.protect(new EdgeDigraphChildIterator(this,n));
  }
  
  @Override
  public List<IRNode> childList(IRNode n) {
    throw new NotImplemented();
  }

  /**
   * Set the i'th child of the node to be newChild.
   * We may need to allocate an edge between the nodes.
   *
   * @exception IllegalChildException if the child is not suitable
   */ 
  public void setChild(IRNode node, int i, IRNode newChild)
       throws IllegalChildException
  {
    if (!hasChildEdge(node,i) || getChildEdge(node,i) == null)
      setChildEdge(node,i,newEdge(node,childLocation(node,i)));
    setSink(getChildEdge(node,i),newChild);
  }

  /** Set the child at location loc of the node to be newChild.
   * We may need to allocate an edge between the nodes.
   *
   * @exception IllegalChildException if the child is not suitable
   */
  public void setChild(IRNode node, IRLocation loc, IRNode newChild)
       throws IllegalChildException
  {
    if (!hasChildEdge(node,loc) || getChildEdge(node,loc) == null)
      setChildEdge(node,loc,newEdge(node,loc));
    setSink(getChildEdge(node,loc),newChild);
  }

  // TODO: addChild

  // TODO: replaceChild

  
  @Override public IRLocation insertChild(IRNode node, IRNode newChild, InsertionPoint ip)
    throws IllegalChildException
  {
    IRNode edge = newEdge(node,null);
    IRLocation loc = insertChildEdge(node,edge,ip);
    setSink(edge,newChild);
    return loc;
  }

  public void removeChild(IRNode node, IRLocation loc) {
    if (hasChild(node,loc)) {
      IRNode e = getChild(node,loc);
      setSink(e,null);
    }
    underlyingNodes.removeChild(node,loc);
  }
  
  // TODO: removeChildren

  @Override
  public Iterator<IRNode> connections(IRNode n1, IRNode n2) {
    assertNode(n1);
    if (n2 != null) assertNode(n2);
    return mutator.protect(new EdgeDigraphConnections(this,n1,n2));
  }

  public void saveAttributes(Bundle b) {
    mutator.saveAttributes(b);
  }

  private final SlotInfo<IRSequence<IRNode>> wrappedChildrenAttribute =
    new WrappedChildrenSlotInfo();
  private /*final*/ SlotInfo<IRSequence<IRNode>> underlyingChildrenAttribute;

  class WrappedChildrenSlotInfo extends DerivedSlotInfo<IRSequence<IRNode>> {
    @Override
    protected boolean valueExists(IRNode node) {
      return isNode(node);
    }
    @Override
    protected IRSequence<IRNode> getSlotValue(IRNode node) {
      assertNode(node);
      IRSequence<IRNode> childEdges = node.getSlotValue(underlyingChildrenAttribute);
      return new ChildrenWrapper(node,childEdges);
    }
    @Override
    protected void setSlotValue(IRNode node, IRSequence<IRNode> seq) {
      int numChildren = seq.size();
      int initChildren = numChildren;
      if (seq.isVariable()) initChildren = ~initChildren;
      initNode(node,initChildren);
      IRLocation loc, locp;
      for (loc = seq.firstLocation(),
	     locp = underlyingNodes.firstChildLocation(node);
	   loc != null;
	   loc = seq.nextLocation(loc),
	     locp = underlyingNodes.nextChildLocation(node,locp)) {
	if (seq.validAt(loc)) {
	  setChild(node,locp,seq.elementAt(loc));
	}
      }
    }
  }

  class ChildrenWrapper extends IRSequenceWrapper<IRNode> {
    IRNode parent;
    ChildrenWrapper(IRNode p, IRSequence<IRNode> seq) {
      super(seq);
      parent = p;
    }
    @Override public void setElementAt(IRNode child, IRLocation loc) {
      setChild(parent,loc,child);
    }
    @Override public IRLocation insertElementAt(IRNode child, InsertionPoint ip) {
      return insertChild(parent,child,ip);
    }
    @Override public void removeElementAt(IRLocation loc) {
      removeChild(parent,loc);
    }
    @Override public IRNode elementAt(IRLocation loc) {
      IRNode e = super.elementAt(loc);
      if (e == null) {
        throw new SlotUndefinedException("null edge has no sink");
      }
      return getSink(e);
    }
    @Override public Iteratable<IRNode> elements() {
      return new AbstractRemovelessIterator<IRNode>() {
        @SuppressWarnings("unchecked")
        final Iterator<IRNode> edges = ChildrenWrapper.super.elements();
        @Override
        public boolean hasNext() {
          return edges.hasNext();
        }
        @Override
        public IRNode next() {
          IRNode e = edges.next();
          if (e == null) {
            throw new SlotUndefinedException("null edge has no sink");
          }
          return getSink(e);
        }
      };
    }
  }
  
  @Override
  public SlotInfo getAttribute(String name) {
    if (name.equals("children")) return wrappedChildrenAttribute;
    else return mutator.getAttribute(name);
  }

  protected class StoredMutator implements Mutator {
    protected /*final*/ SlotFactory slotFactory;
    public StoredMutator(SlotFactory sf) {
      slotFactory = sf;
    }
    protected final void initBareNode(IRNode n) {
      n.setSlotValue(isEdgeSlotInfo,Boolean.FALSE);
    }
    protected final void initBareEdge(IRNode e) {
      e.setSlotValue(isEdgeSlotInfo,Boolean.TRUE);
    }
    @Override
    public void initNode(IRNode n, int numChildren) {
      initBareNode(n);
      underlyingNodes.initNode(n,numChildren);
    }
    @Override
    public void initEdge(IRNode e) {
      initBareEdge(e);
      underlyingEdges.initNode(e,1);
    }

    /** Called before adding an outgoing edge to a node. */
    protected final void checkNewChildEdge(IRNode n, IRLocation loc, IRNode e)
      throws EdgeDigraphException
      {}
    
    protected final void checkNewVariableChildEdge(IRNode node, IRNode edge)
      throws EdgeDigraphException
      {}
    
    /** Called before adding a new sink to an edge */
    protected final void checkNewSink(IRNode e, IRNode sink)
      throws EdgeDigraphException
      {}

    @Override
    public void setSink(IRNode e, IRNode n) throws StructureException {
      assertEdge(e);
      if (n != null) {
	assertNode(n);
	checkNewSink(e,n);
      }
      underlyingEdges.setChild(e,0,n);
    }

    @Override
    public void setChildEdge(IRNode node, IRLocation loc, IRNode newChildEdge){
      assertNode(node);
      if (newChildEdge != null) {
	assertEdge(newChildEdge);
	checkNewChildEdge(node,loc,newChildEdge);
      }
      underlyingNodes.setChild(node,loc,newChildEdge);
    }

    @Override
    public IRLocation insertChildEdge(IRNode node,
				      IRNode newChildEdge,
				      InsertionPoint ip)
      throws StructureException
    {
      assertNode(node);
      if (newChildEdge != null) {
	assertEdge(newChildEdge);
	checkNewVariableChildEdge(node,newChildEdge);
      }
      return underlyingNodes.insertChild(node,newChildEdge,ip);
    }

    @Override
    public void removeChildEdge(IRNode node, IRNode oldChildEdge)
      throws StructureException
    {
      assertNode(node);
      if (oldChildEdge != null) assertEdge(oldChildEdge);
      underlyingNodes.removeChild(node,oldChildEdge);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iteratable<IRNode> protect(Iteratable enm) {
      return slotFactory.newIterator((Iteratable<IRNode>) enm);
    }

    @Override
    public void saveAttributes(Bundle b) {
      underlyingNodes.saveAttributes(b);
      b.saveAttribute(isEdgeSlotInfo);
    }

    protected final SlotInfo wrappedIsEdgeAttribute =
      new WrappedIsEdgeSlotInfo();
    protected final SlotInfo wrappedChildEdgesAttribute =
      new WrappedChildEdgesSlotInfo();
    protected final SlotInfo wrappedSinksAttribute =
      new WrappedSinksSlotInfo();

    @Override
    public SlotInfo getAttribute(String name) {
      if (name.equals(IS_EDGE)) return wrappedIsEdgeAttribute;
      else if (name.equals(CHILD_EDGES)) return wrappedChildEdgesAttribute;
      else if (name.equals(SINKS)) return wrappedSinksAttribute;
      else return null;
    }

    class WrappedIsEdgeSlotInfo extends DerivedSlotInfo<Boolean> {
      @Override
      protected boolean valueExists(IRNode node) {
        return node.valueExists(isEdgeSlotInfo);
      }
      @Override
      protected Boolean getSlotValue(IRNode node) {
        return node.getSlotValue(isEdgeSlotInfo);
      }
      @Override
      protected void setSlotValue(IRNode node, Boolean b) {
        if (b.booleanValue()) {
          initEdge(node);
        } else {
          initNode(node,-1);
        }
      }
    }
    
    @SuppressWarnings("unchecked")
    private final SlotInfo<IRSequence<IRNode>> underlyingChildEdgesAttribute =
      underlyingNodes.getAttribute("children");
    
    @SuppressWarnings("unchecked")
    private final SlotInfo<IRSequence<IRNode>> underlyingSinksAttribute =
      underlyingEdges.getAttribute("children");
    
    class WrappedChildEdgesSlotInfo extends DerivedSlotInfo<IRSequence<IRNode>> {
      @Override
      protected boolean valueExists(IRNode node) {
        return isNode(node);
      }
      @Override
      protected IRSequence<IRNode> getSlotValue(IRNode node) {
        assertNode(node);
        IRSequence<IRNode> childEdges =
          node.getSlotValue(underlyingChildEdgesAttribute);
        return new ChildEdgesWrapper(node,childEdges);
      }
      @Override
      protected void setSlotValue(IRNode node, IRSequence<IRNode> seq) {
        int numChildren = seq.size();
        int initChildren = numChildren;
        if (seq.isVariable()) initChildren = ~initChildren;
        initNode(node,initChildren);
        IRLocation loc, locp;
        for (loc = seq.firstLocation(),
            locp = underlyingNodes.firstChildLocation(node);
        loc != null;
        loc = seq.nextLocation(loc),
        locp = underlyingNodes.nextChildLocation(node,locp)) {
          if (seq.validAt(loc)) {
            setChildEdge(node,locp,seq.elementAt(loc));
          }
        }
      }
    }

    class ChildEdgesWrapper extends IRSequenceWrapper<IRNode> {
      final IRNode parent;
      public ChildEdgesWrapper(IRNode p, IRSequence<IRNode> realedges) {
        super(realedges);
        parent = p;
      }
      @Override public void setElementAt(IRNode child, IRLocation loc) {
        setChildEdge(parent,loc,child);
      }
      @Override public IRLocation insertElementAt(IRNode child, InsertionPoint ip) {
        return insertChildEdge(parent,child,ip);
      }
      @Override public void removeElementAt(IRLocation loc) {
        // no need to override since it doesn't affect node/edge relations
        super.removeElementAt(loc);
      }
    }
    
    //! this class is unused
    class WrappedSinkSlotInfo extends DerivedSlotInfo {
      @Override public boolean valueExists(IRNode n) {
        return isEdge(n) && underlyingEdges.hasChild(n,0);
      }
      @Override public Object getSlotValue(IRNode node) {
        return getSink(node);
      }
      @Override public void setSlotValue(IRNode node, Object value) {
        setSink(node,(IRNode)value);
      }
    }
    
    class WrappedSinksSlotInfo extends DerivedSlotInfo<IRSequence<IRNode>> {
      @Override
      protected boolean valueExists(IRNode edge) {
        return isEdge(edge);
      }
      @Override
      protected IRSequence<IRNode> getSlotValue(IRNode edge) {
        assertEdge(edge);
        IRSequence<IRNode> sinks =
          edge.getSlotValue(underlyingSinksAttribute);
        return new SinksWrapper(edge,sinks);
      }
      @Override
      protected void setSlotValue(IRNode edge, IRSequence<IRNode> seq) {
        if (seq.isVariable() || seq.size() != 1)
          throw new StructureException("sinks must be a fixed sequence of one");
        initBareEdge(edge);
        edge.setSlotValue(underlyingSinksAttribute,seq);
      }
    }

    class SinksWrapper extends IRSequenceWrapper<IRNode> {
      final IRNode edge;
      public SinksWrapper(IRNode e, IRSequence<IRNode> realedges) {
        super(realedges);
        edge = e;
      }
      @Override public void setElementAt(IRNode child, IRLocation loc) {
        if (locationIndex(loc) != 0)
          throw new IRSequenceException("out of range");
        setSink(edge,child);
      }
      @Override public IRLocation insertElementAt(IRNode o, InsertionPoint ip) {
        throw new IRSequenceException("not variable");
      }
      @Override public void removeElementAt(IRLocation loc) {
        throw new IRSequenceException("not variable");
      }
      
    }
  }
  @SuppressWarnings("unchecked")
  protected class DelegatingMutator implements Mutator {
    final SlotInfo<IRSequence<IRNode>> childEdgesAttribute =
      underlyingNodes.getAttribute("children");
    final SlotInfo<IRSequence<IRNode>> sinksAttribute =
      underlyingEdges.getAttribute("children");

    @Override
    public void initNode(IRNode n, int numChildren) {
      IRSequence<IRNode> seq = SimpleSlotFactory.prototype.newSequence(numChildren);
      n.setSlotValue(childEdgesAttribute,seq);
    }
    @Override
    public void initEdge(IRNode e) {
      e.setSlotValue(isEdgeSlotInfo,Boolean.TRUE);
    }
    @Override
    public void setSink(IRNode e, IRNode n) {
      underlyingEdges.setChild(e,0,n);
    }
    @Override
    public void setChildEdge(IRNode node, IRLocation loc, IRNode newChildEdge){
      underlyingNodes.setChild(node,loc,newChildEdge);
    }
    @Override
    public IRLocation insertChildEdge(IRNode node, IRNode newChildEdge,
				      InsertionPoint ip) {
      return underlyingNodes.insertChild(node,newChildEdge,ip);
    }
    @Override
    public void removeChildEdge(IRNode node, IRNode oldChildEdge) {
      underlyingNodes.removeChild(node,oldChildEdge);
    }
    @Override
    @SuppressWarnings("unchecked")
    public Iteratable<IRNode>  protect(Iteratable enm) {
      return enm;      
    }
    @Override
    public void saveAttributes(Bundle b) { }

    @Override
    public SlotInfo getAttribute(String name) {
      if (name.equals(IS_EDGE)) return isEdgeSlotInfo;
      else if (name.equals(CHILD_EDGES)) return childEdgesAttribute;
      else if (name.equals(SINKS)) return sinksAttribute;
      else return null;
    }
  }
}

class EdgeDigraphChildIterator extends AbstractRemovelessIterator<IRNode> {
  private final EdgeDigraph digraph;
  private final IRNode parent;
  private IRLocation next = null;

  EdgeDigraphChildIterator(EdgeDigraph ed, IRNode node) {
    digraph = ed;
    parent = node;
    try {
      next = digraph.firstChildLocation(parent);
    } catch (IRSequenceException e) {
      next = null;
    }
  }

  @Override
  public boolean hasNext() {
    return next != null;
  }

  @Override
  public IRNode next() throws NoSuchElementException {
    if (next == null) throw new NoSuchElementException("no more children");
    try {
      return digraph.getChild(parent,next);
    } finally {
      try {
        next = digraph.nextChildLocation(parent,next);
      } catch (IRSequenceException e) {
        next = null;
      }
    }
  }
}
