package edu.uwm.cs.fluid.control;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import edu.cmu.cs.fluid.control.ControlFlowGraph;
import edu.cmu.cs.fluid.control.ControlNode;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.uwm.cs.fluid.tree.SCCGraph;

/**
 * Worklist that uses a fixed schedule (same as that used by priority queue worklist).
 * Loops are iterated as long as there are any changes in the loops.
 * Avoids the memory and time overhead in managing a priority queue,
 * while sometimes evaluating loops a bit more than necessary.
 * @author boyland
 */
public class ScheduleWorklist implements Worklist {
	private final boolean isForward;
	private List<ControlNode> roots;
	private Iterator<SCCGraph.SCC> outerIterator;
	private SCCGraph.SCC current;
	private Iterator<IRNode> innerIterator;
	private boolean repeatCurrent;

	public ScheduleWorklist(boolean forward) {
		isForward = forward;
	}
	
	@Override
  public void initialize() {
		if (roots == null) roots = new ArrayList<ControlNode>();
		outerIterator = null;
		current = null;
		innerIterator = null;
	}

	@Override
  public void start() {
		SCCGraph sccs = new SCCGraph(ControlFlowGraph.prototype,roots,!isForward);
		outerIterator = sccs.iterator();
		current = null;
		innerIterator = null;
		roots = null;
	}

	@Override
  public boolean hasNext() {
		if (outerIterator == null) return false;
		if (outerIterator.hasNext()) return true;
		if (innerIterator == null) return false;
		return (innerIterator.hasNext() || repeatCurrent);
	}

	@Override
  public ControlNode next() {
		if (!hasNext()) throw new NoSuchElementException("no more elements in worklist");
		if (innerIterator != null) {
			if (!innerIterator.hasNext()) {
				if (repeatCurrent) {
					repeatCurrent = false;
					innerIterator = current.iterator();
				}
			}
			if (innerIterator.hasNext()) return (ControlNode)innerIterator.next();
		}
		current = outerIterator.next();
		if (current.size() == 1) {
			return (ControlNode) current.iterator().next(); // just once!
		}
		innerIterator = current.iterator();
		repeatCurrent = false;
		return (ControlNode)innerIterator.next();
	}

	@Override
  public int size() {
		if (hasNext()) return 1;
		return 0;
	}

	@Override
  public void add(ControlNode node) {
		if (roots != null) {
			roots.add(node);
		} else {
			if (!repeatCurrent && current != null && current.contains(node)) {
				repeatCurrent = true;
			}
		}
	}

	@Override
	public ScheduleWorklist clone() {
		try {
			ScheduleWorklist copy = (ScheduleWorklist) super.clone();
			copy.roots = null;
			copy.initialize();
			return copy;
		} catch (CloneNotSupportedException e) {
			// won't happen
			return null;
		}
	}


}
