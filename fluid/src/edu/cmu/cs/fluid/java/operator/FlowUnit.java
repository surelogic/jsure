package edu.cmu.cs.fluid.java.operator;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaComponentFactory;
import edu.cmu.cs.fluid.control.*;

/** An IRNode that contains an intraprocedural flow-graph.
 * the source node and sink nodes can be computed.
 */
public interface FlowUnit {
  /** Return the special source node that starts the graph. */
  public Source getSource(IRNode node, JavaComponentFactory f);
  /** Return the special sink node that ends normal execution. */
  public Sink getNormalSink(IRNode node, JavaComponentFactory f);
  /** Return the special sink node that ends abrupt termination. */
  public Sink getAbruptSink(IRNode node, JavaComponentFactory f);
}
