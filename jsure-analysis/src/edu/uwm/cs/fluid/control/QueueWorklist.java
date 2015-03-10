/*$Header: /cvs/fluid/fluid/src/edu/uwm/cs/fluid/control/QueueWorklist.java,v 1.6 2008/06/24 19:13:16 thallora Exp $*/
package edu.uwm.cs.fluid.control;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.control.ControlNode;

public class QueueWorklist implements Worklist {

  private static Logger LOG = SLLogger.getLogger("FLUID.analysis");
  
  private Queue<ControlNode> queue = new LinkedList<ControlNode>();
  
  public QueueWorklist() {
    super();
  }

  @Override
  public void initialize() {
    if (LOG.isLoggable(Level.FINER)) {
      LOG.finer("initializing");
    }
   // do nothing
  }

  @Override
  public void start() {
    // do nothing
  }

  @Override
  public boolean hasNext() {
    return !queue.isEmpty();
  }

  @Override
  public int size() {
	return queue.size();
  }
  
  @Override
  public ControlNode next() {
    return queue.remove();
  }

  @Override
  public void add(ControlNode node) {
    if (LOG.isLoggable(Level.FINER)) {
      LOG.finer("Adding " + node + " to worklist");
    }
    queue.add(node);
  }

  @Override
  public QueueWorklist clone() {
    try {
      QueueWorklist copy = (QueueWorklist) super.clone();
      copy.queue = new LinkedList<ControlNode>();
      return copy;
    } catch (CloneNotSupportedException e) {
      // won't happen
      return null;
    }
  }
}
