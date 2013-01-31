/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/SymmetricDigraph.java,v 1.35 2007/07/10 22:16:32 aarong Exp $ */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.util.Iteratable;
import edu.cmu.cs.fluid.util.ThreadGlobal;

/** Directed graphs that can be traversed in either direction.
 * <P> Known bugs:
 * <ul>
 * <li> If the number of parents is the default (~0)
 *      and we are using constant slots, this node cannot have a parent.
 *      (More of a surprising feature than a bug.)
 * <li> The location of parent references cannot be specified,
 *      and even the relative order cannot be guaranteed.
 *	This bug affects @{link #setParent} and delegating syntax trees
 *	that are being mutated backwards.
 *      We try to fix up the parent vector after a change,
 *      but this only works for fixed parent sequences,
 *      and even then 'null' is left behind (even if the slot
 *      was undefined before).
 * <li> The number of parents can specified through a delegating
 *      symmetric directed graph only through a thread global.
 *      In other words, not all the information is going through
 *      the normal channels.  This will cause things to break,
 *      if there are attribute wrappes between that queue requests.
 * <li> See @{link Digraph} as well.
 * </ul>
 */
public class SymmetricDigraph extends Digraph
implements MutableSymmetricDigraphInterface {
  interface Mutator extends Digraph.Mutator {
    public void initNode(IRNode n, int numParents, int numChildren);
    public void setParent(IRNode node, IRLocation loc, IRNode newParent);
    public void addNullParent(IRNode n);
    public void removeNullParent(IRNode n);
  }

  @Override
  protected Digraph.Mutator createStoredMutator(SlotFactory sf) {
    return new StoredMutator(sf);
  }
  @Override
  protected Digraph.Mutator createDelegatingMutator() {
    return new DelegatingMutator();
  }
  
  /*final*/ SlotInfo<IRSequence<IRNode>> parentsSlotInfo;
  
  public IRSequence<IRNode> getParents(IRNode node) {
    return node.getSlotValue(parentsSlotInfo);
  }
  private void setParents(IRNode node, IRSequence<IRNode> parents)
      throws SlotImmutableException
  {
    node.setSlotValue(parentsSlotInfo,parents);
  }

  static IRType<IRSequence<IRNode>> parentsType = new IRSequenceType<IRNode>(IRNodeType.prototype);

  /** Create a new stored directed graph using
   * the given slot factory for attribute creation.
   */
  public SymmetricDigraph(String name, SlotFactory sf)
       throws SlotAlreadyRegisteredException
  {
    super(name,sf);
    if (name == null)
      parentsSlotInfo = sf.newAttribute();
    else
      parentsSlotInfo =
	sf.newAttribute(name + ".SymmetricDigraph.parents", parentsType);
  }

  /** Create a directed graph that uses the attributes
   * passed in.  This will work only if there is a symmetric graph
   * behind these attributes.
   */
  public SymmetricDigraph(SlotInfo<IRSequence<IRNode>> childrenAttribute,
			  SlotInfo<IRSequence<IRNode>> parentsAttribute)
  {
    super(childrenAttribute);
    parentsSlotInfo = parentsAttribute;
  }

      
  /** Create a new node in the graph and ready space for
   * parents and children.
   * @param n node to add to graph
   * @param numParents fixed number of parents if >= 0, ~initial
   *        number of variable parents if < 0.
   *        (defaults to -1 if omitted, see @{link #initNode(IRNode,int)})
   * @param numChildren fixed number of children if >= 0, ~initial
   *        number of variable children if < 0.
   * @exception SlotImmutableException
   * 	if node already in graph.
   */
  @Override
  public void initNode(IRNode n, int numParents, int numChildren) {
    ((Mutator)mutator).initNode(n,numParents,numChildren);
  }

  @Override
  public boolean hasParents(IRNode node) {
    return getParents(node).hasElements();
  }
  @Override
  public int numParents(IRNode node) {
    return getParents(node).size();
  }

  @Override
  public IRLocation parentLocation(IRNode node, int i) {
    return getParents(node).location(i);
  }
  @Override
  public int parentLocationIndex(IRNode node, IRLocation loc) {
    return getParents(node).locationIndex(loc);
  }

  @Override
  public IRLocation firstParentLocation(IRNode node) {
    return getParents(node).firstLocation();
  }
  @Override
  public IRLocation lastParentLocation(IRNode node) {
    return getParents(node).lastLocation();
  }
  @Override
  public IRLocation nextParentLocation(IRNode node, IRLocation loc) {
    return getParents(node).nextLocation(loc);
  }
  @Override
  public IRLocation prevParentLocation(IRNode node, IRLocation loc) {
    return getParents(node).prevLocation(loc);
  }

  @Override
  public int compareParentLocations(IRNode node,
				    IRLocation loc1, IRLocation loc2) {
    return getParents(node).compareLocations(loc1,loc2);
  }

  @Override
  public IRNode getParent(IRNode node, int i) {
    return (getParents(node).elementAt(i));
  }
  @Override
  public IRNode getParent(IRNode node, IRLocation loc) {
    return (getParents(node).elementAt(loc));
  }

  @Override
  public Iteratable<IRNode> parents(IRNode node) {
    return mutator.protect(getParents(node).elements());
  }

  protected IRLocation findParent(IRNode node, IRNode parent)
       throws IllegalChildException
  {
    IRSequence<IRNode> parents = getParents(node);
    for (IRLocation loc = parents.firstLocation(); loc != null;
	 loc=parents.nextLocation(loc)) {
      if (parents.validAt(loc)) {
	IRNode p = parents.elementAt(loc);
	if (p == parent || (p != null && p.equals(parent))) {
	  return loc;
	}
      }
    }
    throw new IllegalChildException("not a parent of node");
  }

  protected IRLocation findUndefinedParent(IRNode node)
       throws IllegalChildException
  {
    IRSequence parents = getParents(node);
    for (IRLocation loc = parents.firstLocation(); loc != null;
	 loc=parents.nextLocation(loc)) {
      if (!parents.validAt(loc)) return loc;
    }
    throw new IllegalChildException("no undefined parent of node");
  }

  /** Check to see if a node can accept another parent. */
  protected boolean isAdoptable(IRNode child) {
    if (child == null) return true;
    IRSequence parents = getParents(child);
    if (parents.isVariable()) return true;
    for (IRLocation loc = parents.firstLocation(); loc != null;
	 loc=parents.nextLocation(loc)) {
      if (!parents.validAt(loc) || parents.elementAt(loc) == null)
	return true;
    }
    return false;
  }

  /** Set the specified parent.
   *  <strong>Warning: if the list of parents is
   *  variable, it may be reordered. </strong>
   * @param i 0-based indicator of parent to change
   * @param newParent new node to be parent, may be null.
   */
  @Override
  public void setParent(IRNode node, int i, IRNode newParent) {
    setParent(node,getParents(node).location(i),newParent);
  }

  /** Set the specified parent.
   *  <strong>Warning: if the list of parents is
   *  variable, it may be reordered. </strong>
   * @param loc location to change parent.
   * @param newParent new node to be parent, may be null.
   */
  @Override
  public void setParent(IRNode node, IRLocation loc, IRNode newParent) {
    ((Mutator)mutator).setParent(node,loc,newParent);
  }

  /** Add a new parent to the parents list without disturbing existing
   * parents.  If the parents are fixed in size, we look for
   * a null or undefined parent to replace.
   * If the parents are variable in size, we append to the end.
   * @exception IllegalChildException if there is no space to add
   */
  @Override
  public void addParent(IRNode node, IRNode newParent)
       throws IllegalChildException
  {
    if (newParent != null) {
      addChild(newParent,node);
    } else {
      ((Mutator)mutator).addNullParent(node);
    }
  }

  /** Remove the link between a parent and a node.
   * Neither may be null.
   * @see #addParent
   * @exception IllegalChildException if parent is not a parent of node
   */
  @Override
  public void removeParent(IRNode node, IRNode parent)
       throws IllegalChildException
  {
    if (parent != null) {
      removeChild(parent,node);
    } else {
      ((Mutator)mutator).removeNullParent(node);
    }
  } 

  /** Replace the node's oldParent with newParent.
   * @exception IllegalChildException if oldParent is not a parent, or
   *            newParent is not suitable.
   */
  @Override
  public void replaceParent(IRNode node, IRNode oldParent, IRNode newParent)
       throws IllegalChildException
  {
    removeParent(node,oldParent);
    addParent(node,newParent);
  }

  /** Remove all the parents of a node. */
  @Override
  public void removeParents(IRNode node) {
    IRSequence parents = getParents(node);
    //boolean variable = parents.isVariable();
    IRLocation next;
    for (IRLocation loc = parents.firstLocation();
    loc != null;
    loc = next) {
      next = parents.nextLocation(loc);
      if (parents.validAt(loc)) {
        removeParent(node,getParent(node,loc));
      }
    }
  }

  /** Remove a node from the graph. */
  @Override
  public void removeNode(IRNode node) {
    removeChildren(node);
    removeParents(node);
  }
  
  /** Return all the nodes connected with a root. */
  @Override
  public Iteratable<IRNode> connectedNodes(IRNode root) {
    return mutator.protect(new ConnectedNodes(this,root));
  }

  @Override
  public void saveAttributes(Bundle b) {
    mutator.saveAttributes(b);
  }

    /* The following thread global is used to make
     * initNodes for delegatings SD's (and SED's) work.
     * Essentialy it is used to pass a secret parameter,
     */
  private static ThreadGlobal<Integer> initNumParents = new ThreadGlobal<Integer>(null);

  protected class StoredMutator extends Digraph.StoredMutator 
				implements Mutator
  {
    public StoredMutator(SlotFactory sf) {
      super(sf);
    }
    @Override
    public void initNode(IRNode n, int numChildren) {
      Integer p = initNumParents.getValue();
      if (p == null) {
	initNode(n,-1,numChildren);
      } else {
	initNode(n,p.intValue(),numChildren);
      }
    }
    @Override
    public void initNode(IRNode n, int numParents, int numChildren) {
      super.initNode(n,numChildren);
      IRSequence<IRNode> seq = slotFactory.newSequence(numParents);
      setParents(n, seq);
    }

    /** Add a parent to the list.
     * @param initial if parent wishes this to be an initial binding
     * @return true if initial and can make this an initial binding
     */
    @Override
    protected boolean addParent(IRNode child, IRNode parent,
				IRLocation ignored, boolean initial) 
    {
      IRSequence<IRNode> parents = getParents(child);
      if (initial) {
	try {
	  parents.setElementAt(parent,findUndefinedParent(child));
	  notifyIRObservers(child); //! Called when in an inconsistent state
	  return true;
	} catch (StructureException e) {
	}
      }
      try {
	parents.setElementAt(parent,findParent(child,null));
	notifyIRObservers(child); //! Called when in an inconsistent state
	return false;
      } catch (StructureException e) {
      }
      if (parents.isVariable()) {
	parents.appendElement(parent);
	notifyIRObservers(child); //! Called when in an inconsistent state
	return false;
      }
      IRLocation loc = findUndefinedParent(child);
      parents.setElementAt(null,loc);
      parents.setElementAt(parent,loc);
      notifyIRObservers(child); //! Called when in an inconsistent state
      return false;
    }

    /** Remove a parent from the list. */
    @Override
    protected void removeParent(IRNode child, IRNode parent, IRLocation loc) {
      IRSequence<IRNode> parents = getParents(child);
      IRLocation ploc = findParent(child,parent);
      if (parents.isVariable()) {
	parents.removeElementAt(ploc);
      } else {
	parents.setElementAt(null,ploc);
      }
      notifyIRObservers(child); //! Called when in an inconsistent state
    }

    /** Called to check if a node is suitable as a new child for
     * a particular parent node and location.
     */
    @Override
    public void checkNewChild(IRNode parent, IRLocation loc, IRNode child) 
        throws IllegalChildException
    {
      if (!isAdoptable(child))
	throw new IllegalChildException("cannot add a parent to node");
    }
    
    /** Called to check if a node is suitable as an additional child for
     * a particular parent node.
     */
    @Override
    public void checkNewVariableChild(IRNode parent, IRNode child) 
        throws IllegalChildException
    {
      super.checkNewVariableChild(parent,child);
      if (!isAdoptable(child))
	throw new IllegalChildException("cannot add a parent to node");
    }

    /** Called to check if a node is suitable as a new parent for
     * a particular child node and location.
     */
    public void checkNewParent(IRNode child, IRLocation loc, IRNode parent)
        throws IllegalChildException
    {
      if (!canAdopt(parent))
	throw new IllegalChildException("cannot add a child to node");
    }

    /** Called to check if a node is suitable as an additional parent
     * for a particular child.
     */
    public void checkNewVariableParent(IRNode child, IRNode parent)
         throws IllegalChildException
    {
      if (!getParents(child).isVariable())
	throw new IllegalChildException("cannot add a new parent to node");
      if (!canAdopt(parent))
	throw new IllegalChildException("cannot add a child to node");
    }

    @Override
    public void setParent(IRNode node, IRLocation loc, IRNode newParent) {
      IRSequence<IRNode> parents = getParents(node);
      IRNode oldParent = null;
      boolean oldParentDefined = false;
      if (parents.validAt(loc)) {
        /** convert into child operations */
        oldParent = parents.elementAt(loc);
        oldParentDefined = true;
        if (oldParent != null) SymmetricDigraph.this.removeChild(oldParent,node);
      } else {
        parents.setElementAt(null,loc);
      }
      if (newParent != null) {
        addChild(newParent,node);
      }
      if (!parents.isVariable()) { // fix up parents vector
        if (oldParent != newParent || oldParent != null && !oldParent.equals(newParent)) {
          /* patch things up */
          IRLocation wloc = findParent(node,newParent);
          parents.setElementAt(parents.elementAt(loc),wloc);
          parents.setElementAt(newParent,loc);
        }
      }
      notifyIRObservers(node); //! Called when in an inconsistent state
      if (!oldParentDefined && newParent == null && hasListeners()) {
        informDigraphListeners(new NewParentEvent(SymmetricDigraph.this,
            node,loc,null));
      }
    }

    @Override
    public void addNullParent(IRNode node)
       throws IllegalChildException
    {
      IRSequence<IRNode> parents = getParents(node);
      IRLocation loc;
      if (parents.isVariable()) {
        loc = parents.appendElement(null);
      } else {
        try {
          loc = findUndefinedParent(node);
          parents.setElementAt(null,loc);
        } catch (StructureException e) {
          loc = findParent(node,null);
        }
      }
      notifyIRObservers(node);
      if (hasListeners()) {
        informDigraphListeners(new NewParentEvent(SymmetricDigraph.this,
						  node,loc,null));
      }
    }

    @Override
    public void removeNullParent(IRNode node)
       throws IllegalChildException
    {
      IRSequence parents = getParents(node);
      if (parents.isVariable()) {
	IRLocation loc = findParent(node,null);
	parents.removeElementAt(loc);
	notifyIRObservers(node);
	if (hasListeners()) {
	  informDigraphListeners(new RemoveParentEvent(SymmetricDigraph.this,
						       node,loc,null));
	}
      }
    }

    @Override
    public void saveAttributes(Bundle b) {
      super.saveAttributes(b);
      b.saveAttribute(parentsSlotInfo);
    }

    final SlotInfo wrappedParentsAttribute = new WrappedParentsSlotInfo();
    
    @Override
    public SlotInfo getAttribute(String name) {
      if (name.equals("parents")) {
	return wrappedParentsAttribute;
      } else {
	return super.getAttribute(name);
      }
    }

    public class WrappedParentsSlotInfo extends DerivedSlotInfo {
      @Override
      protected boolean valueExists(IRNode node) {
	return isNode(node);
      }
      @Override
      protected Object getSlotValue(IRNode node) {
	return new ParentsWrapper(node,getParents(node));
      }
      @Override
      protected void setSlotValue(IRNode node, Object value) {
	IRSequence seq = (IRSequence)value;
	int numParents = seq.size();
	int initParents = numParents;
	if (seq.isVariable()) initParents = ~initParents;
	initNode(node,initParents,-1);
	IRSequence seqp = getParents(node);
	IRLocation loc, locp;
	for (loc = seq.firstLocation(),locp = seqp.firstLocation();
	     loc != null;
	     loc = seq.nextLocation(loc), locp = seqp.nextLocation(locp)) {
	  if (seq.validAt(loc)) {
	    setParent(node,locp,(IRNode)seq.elementAt(loc));
	  }
	}
      }
    }

    public class ParentsWrapper extends IRSequenceWrapper<IRNode> {
      final IRNode child;
      public ParentsWrapper(IRNode node, IRSequence<IRNode> real) {
        super(real);
        child = node;
      }
      @Override
      public void setElementAt(IRNode node, IRLocation loc) {
        setParent(child,loc,node);
      }
      @Override
      public IRLocation insertElementAt(IRNode node, InsertionPoint ip) {
        //! must ignore ip
        SymmetricDigraph.this.addParent(child,node);
        return null;
      }
      @Override
      public void removeElementAt(IRLocation loc) {
	IRSequence parents = getParents(child);
	if (parents.validAt(loc)) {
	  IRNode parent = (IRNode)parents.elementAt(loc);
	  SymmetricDigraph.this.removeParent(child,parent);
	} else {
	  parents.removeElementAt(loc);
	}
      }
    }
  }

  protected class DelegatingMutator extends Digraph.DelegatingMutator
				    implements Mutator
  {
    @Override
    public void initNode(IRNode n, int numParents, int numChildren) {
      initNumParents.pushValue(new Integer(numParents));
      try {
	super.initNode(n,numChildren);
      } finally {
	initNumParents.popValue();
      }
    }
    @Override
    public void setParent(IRNode node, IRLocation loc, IRNode newParent) {
      getParents(node).setElementAt(newParent,loc);
    }
    @Override
    public void addNullParent(IRNode node) throws IllegalChildException {
      IRSequence<IRNode> parents = getParents(node);
      if (parents.isVariable()) {
	parents.appendElement(null);
      } else {
        try {
          parents.setElementAt(null,findUndefinedParent(node));
        } catch (StructureException e) {
        }
	findParent(node,null); // for exception side-effect
      }
    }
    @Override
    public void removeNullParent(IRNode node) throws IllegalChildException {
      IRSequence<IRNode> parents = getParents(node);
      if (parents.isVariable()) {
	IRLocation loc = findParent(node,null);
	parents.removeElementAt(loc);
      }
    }
    @Override
    public SlotInfo getAttribute(String name) {
      if (name == "parents") return parentsSlotInfo;
      else return super.getAttribute(name);
    }
  }
}
