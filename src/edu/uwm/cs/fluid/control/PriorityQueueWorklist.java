/*$Header: /cvs/fluid/fluid/src/edu/uwm/cs/fluid/control/PriorityQueueWorklist.java,v 1.2 2007/05/23 06:42:44 boyland Exp $*/
package edu.uwm.cs.fluid.control;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import edu.cmu.cs.fluid.control.ControlFlowGraph;
import edu.cmu.cs.fluid.control.ControlNode;
import edu.uwm.cs.fluid.tree.SCCGraph;


/**
 * Worklist that uses a priority on nodes based on the SCC ordering.
 * TODO: this does exactly the wrong thing for backwards analysis.
 * @author boyland
 */
public class PriorityQueueWorklist implements Worklist, Comparator<ControlNode> {
  
  private List<ControlNode> roots;
  private SCCGraph sccs;
  private PriorityQueue<ControlNode> pqueue;
  
  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.control.Worklist#initialize()
   */
  public void initialize() {
    if (roots == null) roots = new ArrayList<ControlNode>();
    sccs = null;
    pqueue = null;
  }

  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.control.Worklist#start()
   */
  public void start() {
    sccs = new SCCGraph(ControlFlowGraph.prototype,roots);
    pqueue = new PriorityQueue<ControlNode>(10,this);
    for (ControlNode n : roots) {
      pqueue.add(n);
    }
    roots = null;
  }

  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.control.Worklist#hasNext()
   */
  public boolean hasNext() {
    return !pqueue.isEmpty();
  }

  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.control.Worklist#next()
   */
  public ControlNode next() {
    return pqueue.remove();
  }

  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.control.Worklist#add(edu.cmu.cs.fluid.control.ControlNode)
   */
  public void add(ControlNode node) {
    if (pqueue == null) {
      roots.add(node);
    } else {
      pqueue.add(node);
    }

  }

  @Override
  public PriorityQueueWorklist clone() {
    try {
      PriorityQueueWorklist copy = (PriorityQueueWorklist) super.clone();
      copy.roots = null;
      copy.initialize();
      return copy;
    } catch (CloneNotSupportedException e) {
      // won't happen
      return null;
    }
  }

  /* (non-Javadoc)
   * @see java.util.Comparator#compare(T, T)
   */
  public int compare(ControlNode o1, ControlNode o2) {
    if (o1.equals(o2)) return 0;
    return sccs.precedes(o1,o2) ? -1 : 1;
  }

}
