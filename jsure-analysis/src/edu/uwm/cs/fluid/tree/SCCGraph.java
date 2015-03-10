/*$Header: /cvs/fluid/fluid/src/edu/uwm/cs/fluid/tree/SCCGraph.java,v 1.4 2007/07/10 22:16:36 aarong Exp $*/
package edu.uwm.cs.fluid.tree;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Stack;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.tree.SymmetricDigraphInterface;

/**
 * Express a directed graph as a series of strongly-connected components
 * arranged in reverse-postorder.
 * @author boyland
 */
public class SCCGraph implements Iterable<SCCGraph.SCC> {
  private final Map<IRNode,Integer> nodeIndex = new HashMap<IRNode,Integer>(); // TODO
  private final List<SCC> sccForNodeIndex = new ArrayList<SCC>();
  private final List<SCC> allSCCs = new ArrayList<SCC>();
  // if this structure is too space-inefficient, we can redefine SCC
  // to take two ints, and add a nodeForNodeIndex array list.
  
  public SCCGraph(final SymmetricDigraphInterface dig, final Collection<? extends IRNode> roots, boolean rev) {
    Creator creator = new Creator(dig,roots,rev);
    List<List<IRNode>> sccs = creator.getSCCs();
    int i = 0;
    for (List<IRNode> nodes : sccs) {
      SCC scc = new SCC(nodes);
      for (IRNode node : scc) {
        nodeIndex.put(node,i);
        sccForNodeIndex.add(scc);
      }
      allSCCs.add(scc);
    }
  }

  @Override
  public Iterator<SCC> iterator() {
    return allSCCs.iterator();
  }
  
  /**
   * Return the SCC that this node is in.
   * @param n node of graph
   * @return SCC or null if node wasn't reachable from original roots.
   */
  public SCC SCCforNode(IRNode n) {
    Integer i = nodeIndex.get(n);
    if (i == null) return null;
    return sccForNodeIndex.get(i);
  }
  
  public boolean precedes(IRNode n1, IRNode n2) {
	  Integer i1 = nodeIndex.get(n1);
	  Integer i2 = nodeIndex.get(n2);
	  if (i1 == null) {
		  complainUnreachable(n1);
		  return true;
	  } else if (i2 == null) {
		  complainUnreachable(n2);
		  return false;
	  } else return i1 < i2;
  }

  /**
   * The node given is not reachable in the CFG and yet was added.
   * This should only happen in reworkAll.
   * <p>
   * In general (with the exception of reworkAll),
   * this would be a serious error: why is someone asking
   * us to compare priority of nodes not reachable when the SCCs were built?
   * @param n node not in any of the SCCs
   */
  protected void complainUnreachable(IRNode n) {
	  // System.err.println("Got a node not reachable in CFG: " + n);
  }
  
  public void print(PrintWriter pw) {
	  for (SCC s : this) {
		  s.print(pw);
	  }
	  pw.flush();
  }
  
  /**
   * Strongly-Connected Component from original graph
   * @author boyland
   */
  public class SCC implements Iterable<IRNode> {
    final List<IRNode> nodes;
    SCC(List<IRNode> ns) {
      nodes = ns;
    }
    
    @Override
    public Iterator<IRNode> iterator() {
      return nodes.iterator();
    }
    
    /**
     * Return whether this node is in this SCC
     * @param n a node
     * @return true if n is one of the nodes in this SCC
     */
    public boolean contains(IRNode n) {
      return SCCforNode(n) == this;
    }
    
    /**
     * Return how many nodes in the SCC
     * @return number of nodes in the SCC
     */
    public int size() {
    	return nodes.size();
    }
    
    public void print(PrintWriter pw) {
    	if (nodes.size() > 1) {
    		pw.println("SCC:");
    		for (IRNode n : nodes) {
    			pw.println("  " + n);
    		}
    	} else {
    		IRNode n = nodes.get(0);
    		pw.println(n);
    	}
    }
  }
  
  private static class Creator {
	  final boolean reverse;
    final SymmetricDigraphInterface graph;
    final HashSet<IRNode> visited = new HashSet<IRNode>();  //TODO
    final List<IRNode> finished = new ArrayList<IRNode>();
    final HashSet<IRNode> visited2 = new HashSet<IRNode>(); //TODO
    final List<List<IRNode>> sccs = new ArrayList<List<IRNode>>();
    List<IRNode> scc = null;
    
    public Creator(SymmetricDigraphInterface dig, Collection<? extends IRNode> roots, boolean rev) {
      graph = dig;
      reverse = rev;
      for (IRNode root : roots) {    	
    	  try {
    		  visit(root);
          } catch(StackOverflowError e) {
        	  /*
        	  IRNode last = null;
        	  IRNode here = null;
        	  int i=0;
        	  do {
        		  here = stack.get(i);
        		  i++;
        	  } while (last != here);
        	  */
        	  System.out.println("Died with a stack of "+stack.size());
        	  throw e;
          }
      }

      for (ListIterator<IRNode> rit = finished.listIterator(finished.size()); rit.hasPrevious();) {
        visit2(rit.previous());
        if (scc != null) {
          sccs.add(scc);
          scc = null;
        }
      }
    }
    
    final Stack<IRNode> stack = new Stack<IRNode>();
    /*
    private void visit(IRNode n) {
      if (visited.contains(n)) return;
      visited.add(n);
      stack.push(n);
      for (IRNode ch : (reverse ? graph.parents(n) : graph.children(n))) {
        visit(ch);
      }
      stack.pop();
      finished.add(n);
    }
    */
    private void visit(IRNode n) {
    	stack.clear();
    	stack.push(n);
    	dfs();
    }
    
    private void dfs() {
    	while (!stack.isEmpty()) {
    		final IRNode n = stack.pop();
    		if (n instanceof FinishedNode) {
    			FinishedNode f = (FinishedNode) n;
    			finished.add(f.getIRNode());
    			continue;
    		}
    		if (visited.contains(n)) {
    			continue;
    		}
    		visited.add(n);
        
    		stack.push(new FinishedNode(n));
    		for (IRNode ch : (reverse ? graph.parents(n) : graph.children(n))) {
    			stack.push(ch);
    		}        
    	}
    }
    
    static class FinishedNode extends ProxyNode {
    	FinishedNode(IRNode n) {
    		super(n);
    	}
    	@Override
    	public IRNode getIRNode() {
    		return super.getIRNode();
    	}
    }
    
    /*
    private void visit2(IRNode n) {
      if (visited.contains(n)) {
        // otherwise, unreachable from roots
        if (visited2.contains(n)) return;
        if (scc == null) scc = new ArrayList<IRNode>();
        visited2.add(n);
        for (IRNode p : (reverse ? graph.children(n) : graph.parents(n))) {
          visit2(p);
        }
        scc.add(n);
      }
    }
    */
    private void visit2(IRNode n) {
       	stack.clear();
    	stack.push(n);
    	dfs2();
    }
    
    private void dfs2() {
      	while (!stack.isEmpty()) {
    		final IRNode n = stack.pop();
    		if (!visited.contains(n)) {
    			continue;
    		}
    		if (n instanceof FinishedNode) {
    			FinishedNode f = (FinishedNode) n;
    			if (scc == null) {
    				scc = new ArrayList<IRNode>();
    			}
    			scc.add(f.getIRNode());
    			continue;
    		}
    		if (visited2.contains(n)) {
    			continue;
    		}
    		visited2.add(n);
        
    		stack.push(new FinishedNode(n));
    		for (IRNode ch : (reverse ? graph.parents(n) : graph.children(n))) {
    			stack.push(ch);
    		}        
    	}
    }
    
    public List<List<IRNode>> getSCCs() {
      return sccs;
    }
  }
}
