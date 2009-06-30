/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/ForwardTransfer.java,v 1.5 2007/03/09 21:54:19 chance Exp $ */
package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.util.Lattice;

/** The interface for the analysis engine which uses forward
 * control-flow analysis.  The transfer methods indicate how
 * the value carried by analysis should be modified as it is sent
 * to an outgoing control-flow edge.  A boolean argument to a
 * transfer method indicates which output the value is desried for,
 * the primary one (if true) or the secondary one (if false).
 * A before argument indicates the analysis value on entering the node.
 *
 * @see ForwardAnalysis
 */
public interface ForwardTransfer<T> {
  /** When a component finishes executing and
   * the super component wishes to test the component value
   * as a boolean (by using a DoubleOutputPort), this
   * transfer method is called.  The boolean indicates which
   * output the transfer is for: the true or false branches.
   */
  public Lattice<T> transferConditional(IRNode node, 
				     boolean flag,
				     Lattice<T> before);

  /** Compute the value after executing a node with meaning
   * determined by the component it is in.  The node and
   * an uninterpreted and component-supplied "info" field
   * is passed to help determine what sort of transfer
   * to analyze.
   */
  public Lattice<T> transferComponentFlow(IRNode node,
				       Object info,
				       Lattice<T> before);

  /** Compute the value for a branch of a semantically-based
   * conditional.  This transfer is for explicit ComponentChoice
   * control nodes, which include extra uninterpreted "info" values.
   * If the flag is true, then the value to be computed is for
   * the "true" branch of the condition, otherwise the "false" branch.
   */
  public Lattice<T> transferComponentChoice(IRNode node,
					 Object info,
					 boolean flag,
					 Lattice<T> before);

  /** Compute the value for a branch that affects labels.
   * @param node the syntax for the component this node exists in.
   * @param info the extra information stored in the node.
   * @param label the label to be tested.
   * @param flag <dl>
   * <dt>true<dd> if the value to be computed is for a successful test.
   * <dt>false><dd> if the value to be computed is for a failed test. </dl>
   * @param before the lattice value before the label test node.
   */
  public Lattice<T> transferLabelTest(IRNode node,
				   Object info,
				   ControlLabel label,
				   boolean flag,
				   Lattice<T> before);
}
