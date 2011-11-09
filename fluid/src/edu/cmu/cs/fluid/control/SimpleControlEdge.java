/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/SimpleControlEdge.java,v 1.4 2005/05/23 18:32:39 chance Exp $ */
package edu.cmu.cs.fluid.control;

/** Class of edges that simply and directly refer to
 * sources and sinks.
 */

public class SimpleControlEdge extends ControlEdge {

  ControlNode source, sink;
  boolean source_secondary, sink_secondary;
  
  /** Create an edge between two nodes and attach immediately. */
  public SimpleControlEdge(ControlNode source, ControlNode sink) {
    super();
    attach(source,sink);
  }

  @Override public ControlNode getSource() { return source; }
  @Override public ControlNode getSink() { return sink; }
  @Override public boolean sourceIsSecondary() { return source_secondary; }
  @Override public boolean sinkIsSecondary() { return sink_secondary; }

  @Override protected void setSource(ControlNode source, boolean secondary) {
    this.source = source;
    source_secondary = secondary;
  }

  @Override protected void setSink(ControlNode sink, boolean secondary) {
    this.sink = sink;
    sink_secondary = secondary;
  }
}
