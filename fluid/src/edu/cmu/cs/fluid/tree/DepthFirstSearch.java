/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/DepthFirstSearch.java,v 1.25 2007/10/31 04:57:52 chance Exp $ */
package edu.cmu.cs.fluid.tree;

import java.util.*;
import org.apache.commons.collections15.*;

import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.ir.*;

/** An enumeration of the nodes in a directed graph.
 * "Depth-first" means we look at all nodes reachable through
 * one child before looking at nodes reachable through
 * the next child.  The enumeration is <em>not</em> protected
 * from mutation.
 * @see DigraphInterface
 */
public class DepthFirstSearch extends AbstractRemovelessIterator<IRNode> {
  protected final DigraphInterface digraph;
  @SuppressWarnings("rawtypes")
  protected final ArrayStack stack = new ArrayStack();
  private IRNode node = null;
  // private IRLocation loc = null;
  private Iterator<IRNode> children = new EmptyIterator<IRNode>();
  
//  private final SlotInfo<Boolean> markInfo =
//      SimpleSlotFactory.prototype.newAttribute(Boolean.FALSE);

  private final IRNodeHashedMap<Boolean> markInfo = new IRNodeHashedMap<Boolean>();
  
  private final boolean bottomUp;

  private IRNode nextResult;

  public DepthFirstSearch(DigraphInterface dig, IRNode root) {
    digraph = dig;
    bottomUp = false;
    visit(root);
    mark(root);
  }

  public DepthFirstSearch(DigraphInterface dig, IRNode root, boolean bu) {
    digraph = dig;
    bottomUp = bu;
    visit(root);
    mark(root);
  }

  @Override
  protected void finalize() throws Throwable {
    try {
      //markInfo.destroy();  
    } finally {
      super.finalize();
    }
  }  
  
  @SuppressWarnings("unchecked")
  protected void pushState() {
    stack.push(node);
    // stack.push(loc);
    stack.push(children);
  }

  @SuppressWarnings("unchecked")
  protected void popState() {
    // loc = (IRLocation)stack.pop();
	children = (Iterator<IRNode>) stack.pop();
    node = (IRNode)stack.pop();
  }
    
  protected void visit(IRNode n) {
    if (bottomUp) {
      nextResult = null;
    } else {
      nextResult = n;
    }
    pushState();
    node = n;
    // loc = digraph.firstChildLocation(n);
    children = digraph.children(n);
  }

  protected static final IRNode noNext = new MarkedIRNode("noNext");

  protected IRNode getNext() {
	  while (nextResult == null) {
//		  if (loc != null) {
//		  IRNode newNode = digraph.getChild(node, loc);
//		  loc = digraph.nextChildLocation(node, loc);
		  if (/*children != null && */ children.hasNext()) {
			  IRNode newNode = children.next();
			  if (mark(newNode)) {
				  if (newNode == null) {
					  // System.out.println("DFS IRNode is null");
				  } else {
					  visit(newNode);
				  }
			  }
		  } else if (node == null) {
			  // throw new NoSuchElementException("depth first search is over");
			  return noNext;
		  } else if (!additionalChildren(node)) {
			  if (bottomUp)
				  nextResult = node;
			  popState();
		  }
	  }
    try {
      return nextResult;
    } finally {
      nextResult = null;
    }
  }

  @Override
  public boolean hasNext() {
    if (nextResult != null) return true;
    if (node == null) return false;
    
    IRNode rv = getNext();
    if (rv.equals(noNext)) {
      return false;
    } 
    nextResult = rv;
    return true;
  }

  @Override
  public IRNode next() throws NoSuchElementException {
    IRNode rv = getNext();
    if (rv.equals(noNext)) {
      throw new NoSuchElementException("depth first search is over");
    } 
    return rv;
  }

  /** Mark this node and return true if it was not marked before. */
  protected boolean mark(IRNode node) {
    // System.out.println("IRNode = "+node);
    if (node == null) return false;
    //if (node.getSlotValue(markInfo).booleanValue()) return false;
    //node.setSlotValue(markInfo,Boolean.TRUE);
    
    Boolean old = markInfo.put(node, Boolean.TRUE);
    return old == null;
  }

  /** Handle additional children.
   * Call visit(...) on additional children.
   * Overridden in some subclasses.
   * @return true if additional children are to be traversed.
   */
  protected boolean additionalChildren(IRNode node) {
    return false;
  }
}
