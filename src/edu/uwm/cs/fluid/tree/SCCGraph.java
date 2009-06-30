/*$Header: /cvs/fluid/fluid/src/edu/uwm/cs/fluid/tree/SCCGraph.java,v 1.4 2007/07/10 22:16:36 aarong Exp $*/
package edu.uwm.cs.fluid.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.tree.SymmetricDigraphInterface;

/**
 * Express a directed graph as a series of strongly-connected components
 * arranged in reverse-postorder.
 * XXX: The nodes in the SCCs are NOT in RPO.  Bug already fixed in SKLNST code.
 * @author boyland
 */
public class SCCGraph implements Iterable<SCCGraph.SCC> {

  private final Map<IRNode,Integer> nodeIndex = new HashMap<IRNode,Integer>();
  private final List<SCC> sccForNodeIndex = new ArrayList<SCC>();
  private final List<SCC> allSCCs = new ArrayList<SCC>();
  // if this structure is too space-inefficient, we can redefine SCC
  // to take two ints, and add a nodeForNodeIndex array list.
  
  public SCCGraph(final SymmetricDigraphInterface dig, final Collection<? extends IRNode> roots) {
    Creator creator = new Creator(dig,roots);
    List<List<IRNode>> sccs = creator.getSCCs();
    int i = 0;
    for (List<IRNode> nodes : sccs) {
      SCC scc = new SCC(nodes);
      for (IRNode node : scc) {
        nodeIndex.put(node,i);
        sccForNodeIndex.add(scc);
      }
    }
  }

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
    return nodeIndex.get(n1) < nodeIndex.get(n2);
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
  }
  
  private static class Creator {
    final SymmetricDigraphInterface graph;
    final HashSet<IRNode> visited = new HashSet<IRNode>();
    final List<IRNode> finished = new ArrayList<IRNode>();
    final HashSet<IRNode> visited2 = new HashSet<IRNode>();
    final List<List<IRNode>> sccs = new ArrayList<List<IRNode>>();
    List<IRNode> scc = null;
    
    public Creator(SymmetricDigraphInterface dig, Collection<? extends IRNode> roots) {
      graph = dig;
      for (IRNode root : roots) {
        visit(root);
      }
      for (ListIterator<IRNode> rit = finished.listIterator(finished.size()); rit.hasPrevious();) {
        visit2(rit.previous());
        if (scc != null) {
          sccs.add(scc);
          scc = null;
        }
      }
    }
    
    private void visit(IRNode n) {
      if (visited.contains(n)) return;
      visited.add(n);
      for (IRNode ch : graph.children(n)) {
        visit(ch);
      }
      finished.add(n);
    }
    
    private void visit2(IRNode n) {
      if (visited.contains(n)) {
        // otherwise, unreachable from roots
        if (visited2.contains(n)) return;
        if (scc == null) scc = new ArrayList<IRNode>();
        scc.add(n);
        visited2.add(n);
        for (IRNode p : graph.parents(n)) {
          visit2(p);
        }
      }
    }
    
    public List<List<IRNode>> getSCCs() {
      return sccs;
    }
  }
}
