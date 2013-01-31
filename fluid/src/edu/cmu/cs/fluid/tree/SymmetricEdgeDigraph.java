/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/SymmetricEdgeDigraph.java,v 1.18 2007/07/05 18:15:16 aarong Exp $ */
package edu.cmu.cs.fluid.tree;

import java.util.Iterator;
import java.util.NoSuchElementException;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.util.AbstractRemovelessIterator;
import edu.cmu.cs.fluid.util.Iteratable;

/** Graphs with explicit edges which can be traversed in either direction.
 * <P> Known bugs:
 * <ul>
 * <li> The insertion point for parents is ignored.
 * <li> See @{link SymmetricDigraph} for additional bugs.
 * </ul>
 */
public class SymmetricEdgeDigraph extends EdgeDigraph
implements MutableSymmetricEdgeDigraphInterface
{
  protected interface Mutator extends EdgeDigraph.Mutator {
    public void initNode(IRNode n, int numParents, int numChildren);
  }

  @Override
  protected EdgeDigraph.Mutator createStoredMutator(SlotFactory sf) {
    return new StoredMutator(sf);
  }
  @Override
  protected EdgeDigraph.Mutator createDelegatingMutator() {
    return new DelegatingMutator();
  }
  
  /*final*/ protected SymmetricDigraph underlyingNodes;
  /*final*/ protected SymmetricDigraph underlyingEdges;
  
  public SymmetricEdgeDigraph(String name, SlotFactory sf) 
       throws SlotAlreadyRegisteredException
  {
    super(name,sf, new SymmetricDigraph(name,sf));
    underlyingNodes = (SymmetricDigraph)super.underlyingNodes;
    underlyingEdges = (SymmetricDigraph)super.underlyingEdges;
  }
  
  public SymmetricEdgeDigraph(SlotInfo<IRSequence<IRNode>> childEdgesAttribute,
      SlotInfo<IRSequence<IRNode>> parentEdgesAttribute,
      SlotInfo<IRSequence<IRNode>> sinksAttribute,
      SlotInfo<IRSequence<IRNode>> sourcesAttribute,
      SlotInfo<Boolean> isEdgeAttribute)
  {
    super(new SymmetricDigraph(childEdgesAttribute,parentEdgesAttribute),
        new SymmetricDigraph(sinksAttribute,sourcesAttribute),
        isEdgeAttribute);
    underlyingNodes = (SymmetricDigraph)super.underlyingNodes;
    underlyingEdges = (SymmetricDigraph)super.underlyingEdges;
  }

  public void initNode(IRNode n, int numParents, int numChildren) {
    ((Mutator)mutator).initNode(n,numParents,numChildren);
  }
  
  @Override
  protected DigraphEvent transformNodeEvent(IRNode n, DigraphEvent ev) {
    if (ev instanceof ParentEvent) {
      IRLocation loc = ((ParentEvent)ev).getLocation();
      IRNode parent = ((ParentEvent)ev).getParent();
      if (ev instanceof NewParentEvent) {
	return new NewParentEdgeEvent(this,n,loc,parent);
      } else if (ev instanceof RemoveParentEvent) {
	return new RemoveParentEdgeEvent(this,n,loc,parent);
      } else if (ev instanceof ChangedParentEvent) { /* won't happen */
	IRNode oldParent = ((ChangedParentEvent)ev).getOldParent();
	return new ChangedParentEdgeEvent(this,n,loc,oldParent,parent);
      }
    } 
    return super.transformNodeEvent(n,ev);
  }

  @Override
  protected DigraphEvent transformEdgeEvent(IRNode e, DigraphEvent ev) {
    if (ev instanceof ParentEvent) {
      //IRLocation loc = ((ParentEvent)ev).getLocation();
      IRNode source = ((ParentEvent)ev).getParent();
      if (ev instanceof NewParentEvent) {
        return new NewSourceEvent(this, e, source);
      } else if (ev instanceof ChangedParentEvent) { /* won't happen */
        IRNode oldSource = ((ChangedParentEvent) ev).getOldParent();
        return new ChangedSourceEvent(this, e, oldSource, source);
      }
    } 
    return super.transformEdgeEvent(e,ev);
  }

  @Override
  public IRNode getSource(IRNode e) {
    assertEdge(e);
    return underlyingEdges.getParent(e,0);
  }

  // NB: we use a different technique for handling mutations than EdgeDigraph:
  // we go ahead and perform the (partially redundant) checks.

  /** Set the source of an edge keeping the structure consistent. */
  @Override
  public void setSource(IRNode e, IRNode n)
       throws StructureException
  {
    assertEdge(e);
    if (n != null) assertNode(n);
    underlyingEdges.setParent(e,0,n);
  }

  /** Return the i'th edge arriving at a node. */
  @Override
  public IRNode getParentEdge(IRNode node, int i) {
    assertNode(node);
    return underlyingNodes.getParent(node,i);
  }

  /** Return the ingoing edge at location loc. */
  @Override
  public IRNode getParentEdge(IRNode node, IRLocation loc) {
    assertNode(node);
    return underlyingNodes.getParent(node,loc);
  }

  /** Add a new incoming edge to a node.
   * If the parents are fixed in size, we look for
   * a null or undefined parent to replace.
   * If the parents are variable in size, we append to the end.
   * @exception StructureException if there is no space to add
   */
  @Override
  public void addParentEdge(IRNode node, IRNode newParentEdge)
       throws StructureException
  {
    assertNode(node);
    if (newParentEdge != null) assertEdge(newParentEdge);
    underlyingNodes.addParent(node,newParentEdge);
  }

  /** Remove the link between an incoming edge and a node.
   * Neither may be null.
   * @see #addParentEdge
   * @exception StructureException
   *            if parentEdge is not an incoming edge of node
   */
  @Override
  public void removeParentEdge(IRNode node, IRNode parentEdge)
       throws StructureException
  {
    assertNode(node);
    if (parentEdge != null) assertEdge(parentEdge);
    underlyingNodes.removeParent(node,parentEdge);
  }

  /** Replace the node's oldParentEdge with newParentEdge.
   * @exception StructureException if oldParentEdge is not an incoming edge, 
   *            or newParentEdge is not suitable.
   */
  @Override
  public void replaceParentEdge(IRNode node,
				IRNode oldParentEdge,
				IRNode newParentEdge)
       throws StructureException
  {
    assertNode(node);
    if (oldParentEdge != null) assertEdge(oldParentEdge);
    if (newParentEdge != null) assertEdge(newParentEdge);
    underlyingNodes.replaceParent(node,oldParentEdge,newParentEdge);
  }

  /** Set the parent edge.
   * See caveats on @{link SymmetricDigraph#setParent(IRNode,int,IRNode)}.
   * @param parentEdge incoming edge to use (may be null)
   */
  @Override
  public void setParentEdge(IRNode node, int i, IRNode parentEdge) {
    assertNode(node);
    if (parentEdge != null) assertEdge(parentEdge);
    underlyingNodes.setParent(node,i,parentEdge);
  }

  /** Set the parent edge. See caveats
   * on @{link SymmetricDigraph#setParent(IRNode,IRLocation,IRNode)}.
   * @param parentEdge incoming edge to use (may be null)
   */
  @Override
  public void setParentEdge(IRNode node, IRLocation loc, IRNode parentEdge) {
    assertNode(node);
    if (parentEdge != null) assertEdge(parentEdge);
    underlyingNodes.setParent(node,loc,parentEdge);
  }

  /** Insert a parent.
   * <strong>The insertion point is ignored.</strong>
   */
  @Override
  public IRLocation insertParentEdge(IRNode node, IRNode parentEdge,
				     InsertionPoint ip)
  {
    assertNode(node);
    if (parentEdge != null) assertEdge(parentEdge);
    underlyingNodes.addParent(node,parentEdge);
    return underlyingNodes.findParent(node,parentEdge);
  }

  /** Return an enumeration of the incoming edges to a node. */
  @Override
  public Iterator<IRNode> parentEdges(IRNode node) {
    assertNode(node);
    return underlyingNodes.parents(node);
  }

  /** Remove an edge from the graph. */
  @Override
  public void disconnect(IRNode edge) {
    setSource(edge,null);
    setSink(edge,null);
  }

  /** Remove the children of a node and the connecting edge. */
  public void removeChildren(IRNode node) {
    Iterator<IRNode> e = childEdges(node);
    while (e.hasNext()) {
      try {
        IRNode edge = e.next();
        disconnect(edge);
      } catch (SlotUndefinedException unused) {
      }
    }
  }

  /** Remove the parents of a node and the connecting edge. */
  public void removeParents(IRNode node) {
    Iterator<IRNode> e = parentEdges(node);
    while (e.hasNext()) {
      try {
        IRNode edge = e.next();
        disconnect(edge);
      } catch (SlotUndefinedException unused) {
      }
    }
  }

  /** Remove a node from the graph and all connecting edges. */
  public void removeNode(IRNode node) {
    assertNode(node);
    removeChildren(node);
    removeParents(node);
  }

  // routines to satisfy SymmetricDigraphInterface

  @Override
  public boolean hasParents(IRNode node) {
    return underlyingNodes.hasParents(node);
  }
  @Override
  public int numParents(IRNode node) {
    return underlyingNodes.numParents(node);
  }

  @Override
  public IRLocation parentLocation(IRNode node, int i) {
    return underlyingNodes.parentLocation(node,i);
  }
  @Override
  public int parentLocationIndex(IRNode node, IRLocation loc) {
    return underlyingNodes.parentLocationIndex(node,loc);
  }

  @Override
  public IRLocation firstParentLocation(IRNode node) {
    return underlyingNodes.firstParentLocation(node);
  }
  @Override
  public IRLocation lastParentLocation(IRNode node) {
    return underlyingNodes.firstParentLocation(node);
  }
  @Override
  public IRLocation nextParentLocation(IRNode node, IRLocation loc) {
    return underlyingNodes.nextParentLocation(node,loc);
  }
  @Override
  public IRLocation prevParentLocation(IRNode node, IRLocation loc) {
    return underlyingNodes.prevParentLocation(node,loc);
  }

  @Override
  public int compareParentLocations(IRNode node,
				    IRLocation loc1, IRLocation loc2) {
    return underlyingNodes.compareParentLocations(node,loc1,loc2);
  }

  @Override
  public IRNode getParent(IRNode node, int i) {
    return getSource(getParentEdge(node,i));
  }
  @Override
  public IRNode getParent(IRNode node, IRLocation loc) {
    return getSource(getParentEdge(node,loc));
  }

  @Override
  public Iteratable<IRNode> parents(IRNode node) {
    return mutator.protect(new ParentIterator(this,node));
  }

  private final SlotInfo<IRSequence<IRNode>> wrappedParentsAttribute =
    new WrappedParentsSlotInfo();
  private SlotInfo<IRSequence<IRNode>> underlyingParentsAttribute = null;

  @Override
  public SlotInfo getAttribute(String name) {
    if (name.equals("parents")) return wrappedParentsAttribute;
    if (name.equals("children")) return super.getAttribute(name);
    else return mutator.getAttribute(name);
  }

  class WrappedParentsSlotInfo extends DerivedSlotInfo<IRSequence<IRNode>> {
    @Override
    protected boolean valueExists(IRNode node) {
      return isNode(node);
    }
    @Override
    @SuppressWarnings("unchecked")
    protected IRSequence<IRNode> getSlotValue(IRNode node) {
      // make sure initialized
      if (underlyingParentsAttribute == null)
	underlyingParentsAttribute = underlyingNodes.getAttribute("parents");
      assertNode(node);
      IRSequence<IRNode> parentEdges =
        node.getSlotValue(underlyingParentsAttribute);
      return new ParentsWrapper(parentEdges);
    }
  }

  class ParentsWrapper extends IRSequenceWrapper<IRNode> {
    ParentsWrapper(IRSequence<IRNode> seq) {
      super(seq);
    }
    @Override
    public void setElementAt(IRNode parent, IRLocation loc) {
      setSource(super.elementAt(loc), parent);
    }
    @Override
    public IRLocation insertElementAt(IRNode parent, InsertionPoint ip) {
      throw new IRSequenceException("Cannot insert a parent without parent edge");
    }
    @Override
    public void removeElementAt(IRLocation loc) {
      throw new IRSequenceException("Cannot remove a parent without parent edge");
    }
    @Override
    public IRNode elementAt(IRLocation loc) {
      return getSource(super.elementAt(loc));
    }
    @Override
    public Iteratable<IRNode> elements() {
      return new AbstractRemovelessIterator<IRNode>() {
        @SuppressWarnings("unchecked")
        final Iterator<IRNode> edges = ParentsWrapper.super.elements();
        @Override
        public boolean hasNext() {
          return edges.hasNext();
        }
        @Override
        public IRNode next() {
          return getSource(edges.next());
        }
      };
    }
  }

  class StoredMutator extends EdgeDigraph.StoredMutator implements Mutator {
    public StoredMutator(SlotFactory sf) {
      super(sf);
    }

    @Override
    public void initNode(IRNode n, int numParents, int numChildren) {
      initBareNode(n);
      underlyingNodes.initNode(n,numParents,numChildren);
    }
    @Override
    public void initEdge(IRNode e) {
      initBareEdge(e);
      underlyingEdges.initNode(e,1,1);
    }

    private final SlotInfo wrappedParentEdgesAttribute =
      new WrappedParentEdgesSlotInfo();
    private final SlotInfo wrappedSourcesAttribute =
      new WrappedSourcesSlotInfo();

    @Override
    public SlotInfo getAttribute(String name) {
      if (name.equals("parentEdges")) return wrappedParentEdgesAttribute;
      else if (name.equals("sources")) return wrappedSourcesAttribute;
      else return super.getAttribute(name);
    }

    @SuppressWarnings("unchecked")
    class WrappedParentEdgesSlotInfo extends DerivedSlotInfo<IRSequence<IRNode>> {
      private SlotInfo<IRSequence<IRNode>> underlyingParentEdgesAttribute;
      @Override
      protected boolean valueExists(IRNode node) {
        return isNode(node);
      }
      @Override
      protected IRSequence<IRNode> getSlotValue(IRNode node) {
        if (underlyingParentEdgesAttribute == null)
          underlyingParentEdgesAttribute =
            underlyingNodes.getAttribute("parents");
        assertNode(node);
        IRSequence<IRNode> parentEdges =
          node.getSlotValue(underlyingParentEdgesAttribute);
        return new ParentEdgesWrapper(node,parentEdges);
      }
      @Override
      protected void setSlotValue(IRNode node, IRSequence<IRNode> value) {
        if (underlyingParentEdgesAttribute == null)
          underlyingParentEdgesAttribute =
            underlyingNodes.getAttribute("parents");
        IRSequence<IRNode> seq = value; // force exception early
        initBareNode(node);
        node.setSlotValue(underlyingParentEdgesAttribute,seq);
      }
    }

    class ParentEdgesWrapper extends IRSequenceWrapper<IRNode> {
      final IRNode child;
      public ParentEdgesWrapper(IRNode c, IRSequence<IRNode> realedges) {
        super(realedges);
        child = c;
      }
      @Override
      public void setElementAt(IRNode parent, IRLocation loc) {
        setParentEdge(child,loc,parent);
      }
      @Override
      public IRLocation insertElementAt(IRNode parent, InsertionPoint ip) {
        return insertParentEdge(child,parent,ip);
      }
      @Override
      public void removeElementAt(IRLocation loc) {
        // no need to override since it does not affect node/edge relations
        super.removeElementAt(loc);
      }
    }
    @SuppressWarnings("unchecked")
    class WrappedSourcesSlotInfo extends DerivedSlotInfo<IRSequence<IRNode>> {
      private SlotInfo<IRSequence<IRNode>> underlyingSourcesAttribute;
      
      @Override
      protected boolean valueExists(IRNode edge) {
        return isEdge(edge);
      }
      @Override
      protected IRSequence<IRNode> getSlotValue(IRNode edge) {
        if (underlyingSourcesAttribute == null)
          underlyingSourcesAttribute =
            underlyingEdges.getAttribute("parents");
        assertEdge(edge);
        IRSequence<IRNode> sources =
          edge.getSlotValue(underlyingSourcesAttribute);
        return new SourcesWrapper(edge,sources);
      }
      @Override
      protected void setSlotValue(IRNode edge, IRSequence<IRNode> value) {
        if (underlyingSourcesAttribute == null)
          underlyingSourcesAttribute =
            underlyingEdges.getAttribute("parents");
        IRSequence<IRNode> seq = value; // force exception early
        if (seq.isVariable() || seq.size() != 1)
          throw new StructureException("sources must be a fixed sequence of one");
        initBareEdge(edge);
        edge.setSlotValue(underlyingSourcesAttribute,seq);
      }
    }

    class SourcesWrapper extends IRSequenceWrapper<IRNode> {
      final IRNode edge;
      public SourcesWrapper(IRNode e, IRSequence<IRNode> realedges) {
        super(realedges);
        edge = e;
      }
      @Override
      public void setElementAt(IRNode child, IRLocation loc) {
        if (locationIndex(loc) != 0)
          throw new IRSequenceException("out of range");
        setSource(edge,child);
      }
      @Override
      public IRLocation insertElementAt(IRNode o, InsertionPoint ip) {
        throw new IRSequenceException("not variable");
      }
      @Override
      public void removeElementAt(IRLocation loc) {
        throw new IRSequenceException("not variable");
      }
    }
  }

  class DelegatingMutator extends EdgeDigraph.DelegatingMutator implements Mutator {
    @Override
    public void initNode(IRNode n, int numParents, int numChildren) {
      underlyingNodes.initNode(n,numParents,numChildren);
    }

    @Override
    public SlotInfo getAttribute(String name) {
     if (name.equals("parentEdges")) return underlyingNodes.getAttribute("parents");
     else if (name.equals("sinks")) return underlyingEdges.getAttribute("parents");
     else return super.getAttribute(name);
   }
  }
}

class ParentIterator extends AbstractRemovelessIterator<IRNode> {
  private final SymmetricDigraphInterface digraph;
  private final IRNode parent;
  private IRLocation next = null;

  ParentIterator(SymmetricEdgeDigraph ed, IRNode node) {
    digraph = ed;
    parent = node;
    try {
      next = digraph.firstParentLocation(parent);
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
      return digraph.getParent(parent,next);
    } finally {
      try {
        next = digraph.nextParentLocation(parent,next);
      } catch (IRSequenceException e) {
        next = null;
      }
    }
  }
}





