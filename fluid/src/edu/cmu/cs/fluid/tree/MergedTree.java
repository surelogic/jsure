/*
 * Created on Jun 24, 2003
 *
 */
package edu.cmu.cs.fluid.tree;

import java.util.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Observer;

import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.NotImplemented;
import edu.cmu.cs.fluid.ir.DerivedSlotInfo;
import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;

/**
 * May not be right in general for methods that compute answers for the
 * "whole" tree
 * @author chance
 *
 */
public class MergedTree extends AbstractImmutableTree implements MutableTreeInterface {
	Map<IRNode,MutableTreeInterface> treeMap = new HashMap<IRNode,MutableTreeInterface>();
  MutableTreeInterface tree;
  
  public MergedTree(MutableTreeInterface defaultTree) {
  	tree = defaultTree;
  }

  /**
   * Sets n so that it gets its children from a different tree.
   * @param n
   * @param childrenTree
   */
  public void setChildrenTree(IRNode n, MutableTreeInterface childrenTree) {
    Iterator<IRNode> enm = childrenTree.topDown(n);
    // Skip first node
    enm.next(); 
    treeMap.put(n, new MergedTreeAdapter(tree, childrenTree));
    while (enm.hasNext()) {
      IRNode node = enm.next();
      treeMap.put(node, childrenTree);
    }
  }
  
  private MutableTreeInterface getTree(IRNode n) {    
    MutableTreeInterface t = treeMap.get(n);
    return (t != null) ? t : tree;
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.MutableTreeInterface#getParentOrNull(fluid.ir.IRNode)
   */
  @Override
  public IRNode getParentOrNull(IRNode node) {
    return getTree(node).getParentOrNull(node);
  }


  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.MutableTreeInterface#rootWalk(fluid.ir.IRNode)
   */
  @Override
  public Iteratable<IRNode> rootWalk(IRNode node) {
    throw new NotImplemented("Needs more thought"); // TODO
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.MutableTreeInterface#comparePreorder(fluid.ir.IRNode, fluid.ir.IRNode)
   */
  @Override
  public int comparePreorder(IRNode node1, IRNode node2) {
    throw new NotImplemented("Needs more thought"); // TODO
  }


  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.MutableSymmetricDigraphInterface#connectedNodes(fluid.ir.IRNode)
   */
  @Override
  public Iteratable<IRNode> connectedNodes(IRNode root) {
    throw new NotImplemented("Needs more thought"); // TODO
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.TreeInterface#getParent(fluid.ir.IRNode)
   */
  @Override
  public IRNode getParent(IRNode node) {
    return getTree(node).getParent(node);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.TreeInterface#getLocation(fluid.ir.IRNode)
   */
  @Override
  public IRLocation getLocation(IRNode node) {
    return getTree(node).getLocation(node);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.TreeInterface#getRoot(fluid.ir.IRNode)
   */
  @Override
  public IRNode getRoot(IRNode subtree) {
    throw new NotImplemented("Needs more thought"); // TODO
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.TreeInterface#bottomUp(fluid.ir.IRNode)
   */
  @Override
  public Iteratable<IRNode> bottomUp(IRNode subtree) {
    throw new NotImplemented("Needs more thought"); // TODO
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.TreeInterface#topDown(fluid.ir.IRNode)
   */
  @Override
  public Iteratable<IRNode> topDown(IRNode subtree) {
    throw new NotImplemented("Needs more thought"); // TODO
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.MutableDigraphInterface#isNode(fluid.ir.IRNode)
   */
  @Override
  public boolean isNode(IRNode n) {
    return getTree(n).isNode(n);
  }


  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.MutableDigraphInterface#depthFirstSearch(fluid.ir.IRNode)
   */
  @Override
  public Iteratable<IRNode> depthFirstSearch(IRNode node) {
    throw new NotImplemented("Needs more thought"); // TODO
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.SymmetricDigraphInterface#hasParents(fluid.ir.IRNode)
   */
  @Override
  public boolean hasParents(IRNode node) {
    return getTree(node).hasParents(node);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.SymmetricDigraphInterface#numParents(fluid.ir.IRNode)
   */
  @Override
  public int numParents(IRNode node) {
    return getTree(node).numParents(node);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.SymmetricDigraphInterface#parentLocation(fluid.ir.IRNode, int)
   */
  @Override
  public IRLocation parentLocation(IRNode node, int i) {
    return getTree(node).parentLocation(node, i);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.SymmetricDigraphInterface#parentLocationIndex(fluid.ir.IRNode, fluid.ir.IRLocation)
   */
  @Override
  public int parentLocationIndex(IRNode node, IRLocation loc) {
    return getTree(node).parentLocationIndex(node, loc);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.SymmetricDigraphInterface#firstParentLocation(fluid.ir.IRNode)
   */
  @Override
  public IRLocation firstParentLocation(IRNode node) {
    return getTree(node).firstParentLocation(node);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.SymmetricDigraphInterface#lastParentLocation(fluid.ir.IRNode)
   */
  @Override
  public IRLocation lastParentLocation(IRNode node) {
    return getTree(node).lastParentLocation(node);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.SymmetricDigraphInterface#nextParentLocation(fluid.ir.IRNode, fluid.ir.IRLocation)
   */
  @Override
  public IRLocation nextParentLocation(IRNode node, IRLocation ploc) {
    return getTree(node).nextParentLocation(node, ploc);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.SymmetricDigraphInterface#prevParentLocation(fluid.ir.IRNode, fluid.ir.IRLocation)
   */
  @Override
  public IRLocation prevParentLocation(IRNode node, IRLocation ploc) {
    return getTree(node).prevParentLocation(node, ploc);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.SymmetricDigraphInterface#compareParentLocations(fluid.ir.IRNode, fluid.ir.IRLocation, fluid.ir.IRLocation)
   */
  @Override
  public int compareParentLocations(
    IRNode node,
    IRLocation loc1,
    IRLocation loc2) {
      return getTree(node).compareParentLocations(node, loc1, loc2);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.SymmetricDigraphInterface#getParent(fluid.ir.IRNode, int)
   */
  @Override
  public IRNode getParent(IRNode node, int i) {
    return getTree(node).getParent(node, i);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.SymmetricDigraphInterface#getParent(fluid.ir.IRNode, fluid.ir.IRLocation)
   */
  @Override
  public IRNode getParent(IRNode node, IRLocation loc) {
    return getTree(node).getParent(node, loc);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.SymmetricDigraphInterface#parents(fluid.ir.IRNode)
   */
  @Override
  public Iteratable<IRNode> parents(IRNode node) {
    return getTree(node).parents(node);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#hasChildren(fluid.ir.IRNode)
   */
  @Override
  public boolean hasChildren(IRNode node) {
    return getTree(node).hasChildren(node);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#numChildren(fluid.ir.IRNode)
   */
  @Override
  public int numChildren(IRNode node) {
    return getTree(node).numChildren(node);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#childLocation(fluid.ir.IRNode, int)
   */
  @Override
  public IRLocation childLocation(IRNode node, int i) {
    return getTree(node).childLocation(node, i);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#childLocationIndex(fluid.ir.IRNode, fluid.ir.IRLocation)
   */
  @Override
  public int childLocationIndex(IRNode node, IRLocation loc) {
    return getTree(node).childLocationIndex(node, loc);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#firstChildLocation(fluid.ir.IRNode)
   */
  @Override
  public IRLocation firstChildLocation(IRNode node) {
    return getTree(node).firstChildLocation(node);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#lastChildLocation(fluid.ir.IRNode)
   */
  @Override
  public IRLocation lastChildLocation(IRNode node) {
    return getTree(node).lastChildLocation(node);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#nextChildLocation(fluid.ir.IRNode, fluid.ir.IRLocation)
   */
  @Override
  public IRLocation nextChildLocation(IRNode node, IRLocation loc) {
    return getTree(node).nextChildLocation(node, loc);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#prevChildLocation(fluid.ir.IRNode, fluid.ir.IRLocation)
   */
  @Override
  public IRLocation prevChildLocation(IRNode node, IRLocation loc) {
    return getTree(node).prevChildLocation(node, loc);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#compareChildLocations(fluid.ir.IRNode, fluid.ir.IRLocation, fluid.ir.IRLocation)
   */
  @Override
  public int compareChildLocations(
    IRNode node,
    IRLocation loc1,
    IRLocation loc2) {
      return getTree(node).compareChildLocations(node, loc1, loc2);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#hasChild(fluid.ir.IRNode, int)
   */
  @Override
  public boolean hasChild(IRNode node, int i) {
    return getTree(node).hasChild(node, i);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#hasChild(fluid.ir.IRNode, fluid.ir.IRLocation)
   */
  @Override
  public boolean hasChild(IRNode node, IRLocation loc) {
    return getTree(node).hasChild(node, loc);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#getChild(fluid.ir.IRNode, int)
   */
  @Override
  public IRNode getChild(IRNode node, int i) {
    return getTree(node).getChild(node, i);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#getChild(fluid.ir.IRNode, fluid.ir.IRLocation)
   */
  @Override
  public IRNode getChild(IRNode node, IRLocation loc) {
    return getTree(node).getChild(node, loc);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#children(fluid.ir.IRNode)
   */
  @Override
  public Iteratable<IRNode> children(IRNode node) {
    return getTree(node).children(node);
  }
  
  @Override
  public List<IRNode> childList(IRNode node) {
    return getTree(node).childList(node);
  }
  
	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.DigraphInterface#addDigraphListener(fluid.tree.DigraphListener)
	 */
	@Override
  public void addDigraphListener(DigraphListener dl) {
		throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.DigraphInterface#removeDigraphListener(fluid.tree.DigraphListener)
	 */
	@Override
  public void removeDigraphListener(DigraphListener dl) {
		throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.DigraphInterface#getAttribute(java.lang.String)
	 */
	@Override
  @SuppressWarnings("unchecked")
    public SlotInfo getAttribute(final String name) {
	  final SlotInfo treeSI = tree.getAttribute(name);
	  if (treeSI == null) {
	    return null;
	  }
	  // assume that the attribute is defined
	  return new DerivedSlotInfo() {
	    @Override
      protected boolean valueExists(IRNode node) {
	      SlotInfo si = getTree(node).getAttribute(name);
	      return node.valueExists(si);
	    }
	    @Override
      protected Object getSlotValue(IRNode node) {
	      SlotInfo si = getTree(node).getAttribute(name);
	      return node.getSlotValue(si);
	    }
	  };
	}

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.MutableDigraphInterface#addObserver(java.util.Observer)
   */
  @Override
  public void addObserver(Observer o) {
    // TODO Auto-generated method stub
    
  }  
}

/**
 * @author chance
 *
 */
class MergedTreeAdapter extends AbstractImmutableTree implements MutableTreeInterface {
  final MutableTreeInterface parentTree, childTree;
  
  public MergedTreeAdapter(MutableTreeInterface parent, MutableTreeInterface child) {
    parentTree = parent;
    childTree  = child;
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.MutableTreeInterface#getParentOrNull(fluid.ir.IRNode)
   */
  @Override
  public IRNode getParentOrNull(IRNode node) {
    return parentTree.getParentOrNull(node);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.MutableTreeInterface#rootWalk(fluid.ir.IRNode)
   */
  @Override
  public Iteratable<IRNode> rootWalk(IRNode node) {
    return parentTree.rootWalk(node);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.MutableTreeInterface#comparePreorder(fluid.ir.IRNode, fluid.ir.IRNode)
   */
  @Override
  public int comparePreorder(IRNode node1, IRNode node2) {
    throw new NotImplemented();
     // TODO Auto-generated method stub
  }


  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.MutableSymmetricDigraphInterface#connectedNodes(fluid.ir.IRNode)
   */
  @Override
  public Iteratable<IRNode> connectedNodes(IRNode root) {
    // TODO Auto-generated method stub
    throw new NotImplemented();
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.TreeInterface#getParent(fluid.ir.IRNode)
   */
  @Override
  public IRNode getParent(IRNode node) {
    return parentTree.getParent(node);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.TreeInterface#getLocation(fluid.ir.IRNode)
   */
  @Override
  public IRLocation getLocation(IRNode node) {
    // TODO Auto-generated method stub
    throw new NotImplemented();
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.TreeInterface#getRoot(fluid.ir.IRNode)
   */
  @Override
  public IRNode getRoot(IRNode subtree) {
    return parentTree.getRoot(subtree);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.TreeInterface#bottomUp(fluid.ir.IRNode)
   */
  @Override
  public Iteratable<IRNode> bottomUp(IRNode subtree) {
    return childTree.bottomUp(subtree);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.TreeInterface#topDown(fluid.ir.IRNode)
   */
  @Override
  public Iteratable<IRNode> topDown(IRNode subtree) {
    return childTree.topDown(subtree);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.MutableDigraphInterface#isNode(fluid.ir.IRNode)
   */
  @Override
  public boolean isNode(IRNode n) {
    return parentTree.isNode(n) && childTree.isNode(n);
  }


  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.MutableDigraphInterface#depthFirstSearch(fluid.ir.IRNode)
   */
  @Override
  public Iteratable<IRNode> depthFirstSearch(IRNode node) {
    return childTree.depthFirstSearch(node);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.SymmetricDigraphInterface#hasParents(fluid.ir.IRNode)
   */
  @Override
  public boolean hasParents(IRNode node) {
    return parentTree.hasParents(node);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.SymmetricDigraphInterface#numParents(fluid.ir.IRNode)
   */
  @Override
  public int numParents(IRNode node) {
    return parentTree.numParents(node);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.SymmetricDigraphInterface#parentLocation(fluid.ir.IRNode, int)
   */
  @Override
  public IRLocation parentLocation(IRNode node, int i) {
    return parentTree.parentLocation(node, i);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.SymmetricDigraphInterface#parentLocationIndex(fluid.ir.IRNode, fluid.ir.IRLocation)
   */
  @Override
  public int parentLocationIndex(IRNode node, IRLocation loc) {
    return parentTree.parentLocationIndex(node, loc);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.SymmetricDigraphInterface#firstParentLocation(fluid.ir.IRNode)
   */
  @Override
  public IRLocation firstParentLocation(IRNode node) {
    return parentTree.firstParentLocation(node);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.SymmetricDigraphInterface#lastParentLocation(fluid.ir.IRNode)
   */
  @Override
  public IRLocation lastParentLocation(IRNode node) {
    return parentTree.lastParentLocation(node);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.SymmetricDigraphInterface#nextParentLocation(fluid.ir.IRNode, fluid.ir.IRLocation)
   */
  @Override
  public IRLocation nextParentLocation(IRNode node, IRLocation ploc) {
    return parentTree.nextParentLocation(node, ploc);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.SymmetricDigraphInterface#prevParentLocation(fluid.ir.IRNode, fluid.ir.IRLocation)
   */
  @Override
  public IRLocation prevParentLocation(IRNode node, IRLocation ploc) {
    return parentTree.prevParentLocation(node, ploc);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.SymmetricDigraphInterface#compareParentLocations(fluid.ir.IRNode, fluid.ir.IRLocation, fluid.ir.IRLocation)
   */
  @Override
  public int compareParentLocations(
    IRNode node,
    IRLocation loc1,
    IRLocation loc2) {
    return parentTree.compareParentLocations(node, loc1, loc2);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.SymmetricDigraphInterface#getParent(fluid.ir.IRNode, int)
   */
  @Override
  public IRNode getParent(IRNode node, int i) {
    return parentTree.getParent(node, i);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.SymmetricDigraphInterface#getParent(fluid.ir.IRNode, fluid.ir.IRLocation)
   */
  @Override
  public IRNode getParent(IRNode node, IRLocation loc) {
    return parentTree.getParent(node, loc);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.SymmetricDigraphInterface#parents(fluid.ir.IRNode)
   */
  @Override
  public Iteratable<IRNode> parents(IRNode node) {
    return parentTree.parents(node);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#hasChildren(fluid.ir.IRNode)
   */
  @Override
  public boolean hasChildren(IRNode node) {
    return childTree.hasChildren(node);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#numChildren(fluid.ir.IRNode)
   */
  @Override
  public int numChildren(IRNode node) {
    return childTree.numChildren(node);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#childLocation(fluid.ir.IRNode, int)
   */
  @Override
  public IRLocation childLocation(IRNode node, int i) {
    return childTree.childLocation(node, i);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#childLocationIndex(fluid.ir.IRNode, fluid.ir.IRLocation)
   */
  @Override
  public int childLocationIndex(IRNode node, IRLocation loc) {
    return childTree.childLocationIndex(node, loc);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#firstChildLocation(fluid.ir.IRNode)
   */
  @Override
  public IRLocation firstChildLocation(IRNode node) {
    return childTree.firstChildLocation(node);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#lastChildLocation(fluid.ir.IRNode)
   */
  @Override
  public IRLocation lastChildLocation(IRNode node) {
    return childTree.lastChildLocation(node);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#nextChildLocation(fluid.ir.IRNode, fluid.ir.IRLocation)
   */
  @Override
  public IRLocation nextChildLocation(IRNode node, IRLocation loc) {
    return childTree.nextChildLocation(node, loc);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#prevChildLocation(fluid.ir.IRNode, fluid.ir.IRLocation)
   */
  @Override
  public IRLocation prevChildLocation(IRNode node, IRLocation loc) {
    return childTree.prevChildLocation(node, loc);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#compareChildLocations(fluid.ir.IRNode, fluid.ir.IRLocation, fluid.ir.IRLocation)
   */
  @Override
  public int compareChildLocations(
    IRNode node,
    IRLocation loc1,
    IRLocation loc2) {
    return childTree.compareChildLocations(node, loc1, loc2);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#hasChild(fluid.ir.IRNode, int)
   */
  @Override
  public boolean hasChild(IRNode node, int i) {
    return childTree.hasChild(node, i);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#hasChild(fluid.ir.IRNode, fluid.ir.IRLocation)
   */
  @Override
  public boolean hasChild(IRNode node, IRLocation loc) {
    return childTree.hasChild(node, loc);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#getChild(fluid.ir.IRNode, int)
   */
  @Override
  public IRNode getChild(IRNode node, int i) {
    return childTree.getChild(node, i);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#getChild(fluid.ir.IRNode, fluid.ir.IRLocation)
   */
  @Override
  public IRNode getChild(IRNode node, IRLocation loc) {
    return childTree.getChild(node, loc);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#children(fluid.ir.IRNode)
   */
  @Override
  public Iteratable<IRNode> children(IRNode node) {
    return childTree.children(node);
  }
  
  @Override
  public List<IRNode> childList(IRNode node) {
    return childTree.childList(node);
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#addDigraphListener(fluid.tree.DigraphListener)
   */
  @Override
  public void addDigraphListener(DigraphListener dl) {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#removeDigraphListener(fluid.tree.DigraphListener)
   */
  @Override
  public void removeDigraphListener(DigraphListener dl) {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.DigraphInterface#getAttribute(java.lang.String)
   */
  @Override
  public SlotInfo getAttribute(String name) {
    // TODO Auto-generated method stub
    throw new NotImplemented();
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.tree.MutableDigraphInterface#addObserver(java.util.Observer)
   */
  @Override
  public void addObserver(Observer o) {
    // TODO Auto-generated method stub
    
  }  
}


