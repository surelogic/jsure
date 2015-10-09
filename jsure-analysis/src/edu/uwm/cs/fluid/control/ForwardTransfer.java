package edu.uwm.cs.fluid.control;

import edu.cmu.cs.fluid.control.ControlLabel;
import edu.cmu.cs.fluid.ir.IRNode;

/**
 * The interface for the analysis engine which uses forward control-flow
 * analysis. The transfer methods indicate how the value carried by analysis
 * should be modified as it is sent to an outgoing control-flow edge. A boolean
 * argument to a transfer method indicates which output the value is desried
 * for, the primary one (if true) or the secondary one (if false). A before
 * argument indicates the analysis value on entering the node.
 *
 * @see ForwardAnalysis
 */
public interface ForwardTransfer<T> {

  /**
   * Generate the value that comes from the source of the graph.
   * 
   * @param node
   *          syntax node for source
   * @return initial value for CFG.
   */
  public T transferComponentSource(IRNode node);

  /**
   * When a component finishes executing and the super component wishes to test
   * the component value as a boolean (by using a DoubleOutputPort), this
   * transfer method is called. The boolean indicates which output the transfer
   * is for: the true or false branches.
   */
  public T transferConditional(IRNode node, boolean flag, T before);

  /**
   * Compute the value after executing a node with meaning determined by the
   * component it is in. The node and an uninterpreted and component-supplied
   * "info" field is passed to help determine what sort of transfer to analyze.
   */
  public T transferComponentFlow(IRNode node, Object info, T before);

  /**
   * Compute the value for a branch of a semantically-based conditional. This
   * transfer is for explicit ComponentChoice control nodes, which include extra
   * uninterpreted "info" values. If the flag is true, then the value to be
   * computed is for the "true" branch of the condition, otherwise the "false"
   * branch.
   */
  public T transferComponentChoice(IRNode node, Object info, boolean flag, T before);

  /**
   * Compute whether a label is a possible/impossible match. For a true flag,
   * return whether it is possible the label matches. For a false flag, return
   * whether it is possible the label doesn't match.
   * 
   * @param node
   *          the syntax for the component this node exists in.
   * @param info
   *          the extra information stored in the LabelTest node.
   * @param label
   *          the label to be tested.
   * @param flag
   *          <dl>
   *          <dt>true
   *          <dd>if the value to be computed is for a successful test.
   *          <dt>false>
   *          <dd>if the value to be computed is for a failed test.
   *          </dl>
   * @return true if the flag tests is a possible result.
   */
  public boolean transferLabelTest(IRNode node, Object info, ControlLabel label, boolean flag);

  /**
   * Transfer flow information around to the top of a loop. By default, this
   * transfer function should just call the widening operator for the lattice,
   * but sometimes, we need to use the
   * 
   * @param node
   *          loop node
   * @param initial
   *          lattice value from before loop
   * @param looped
   *          lattice value from bottom of loop.
   * @return an upperbound of initial and looped.
   */
  public T transferLoopMerge(IRNode node, T initial, T looped);
}
