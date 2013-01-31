/*$Header: /cvs/fluid/fluid/src/edu/uwm/cs/fluid/control/BufferWorklist.java,v 1.5 2008/06/24 19:13:16 thallora Exp $*/
package edu.uwm.cs.fluid.control;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections15.Buffer;
import org.apache.commons.collections15.buffer.*;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.control.ControlNode;

public class BufferWorklist implements Worklist {
  private static Logger LOG = SLLogger.getLogger("FLUID.analysis");
  
  private Buffer<ControlNode> queue = new UnboundedFifoBuffer<ControlNode>();
  
  public BufferWorklist() {
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
  public BufferWorklist clone() {
    try {
      BufferWorklist copy = (BufferWorklist) super.clone();
      copy.queue = new UnboundedFifoBuffer<ControlNode>();
      return copy;
    } catch (CloneNotSupportedException e) {
      // won't happen
      return null;
    }
  }
}
