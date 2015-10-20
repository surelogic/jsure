package edu.uwm.cs.fluid.control;

import edu.cmu.cs.fluid.control.ControlLabel;
import edu.cmu.cs.fluid.ir.IRNode;

/**
 * The interface for the analysis engine which uses backward control-flow
 * analysis. The transfer methods indicate how the value carried by analysis
 * should be modified as it is sent to an ingoing control-flow edge. A boolean
 * argument to a transfer method indicates which output the value came from. the
 * primary one (if true) or the secondary one (if false). An after argument
 * indicates the analysis value on entering the node.
 *
 * @see BackwardAnalysis
 */
public interface BackwardTransfer<T> {

  /**
   * Compute the value that flows backward from a component sink node.
   * 
   * @param node
   *          syntax node associated with this component
   * @param info
   *          information in the component sink node.
   * @return lattice value that flows into the sink.
   */
  public T transferComponentSink(IRNode node, boolean normal);

  /**
   * Compute the value before executing a node with meaning determined by the
   * component it is in. The node and an uninterpreted and component-supplied
   * "info" field is passed to help determine what sort of transfer to analyze.
   */
  public T transferComponentFlow(IRNode node, Object info, T after);

  /**
   * Compute the value for a branch of a semantically-based conditional. This
   * transfer is for explicit ComponentChoice control nodes, which include extra
   * uninterpreted "info" values. If the flag is true, then the after value is
   * from the the "true" branch of the condition, otherwise from the "false"
   * branch.
   */
  public T transferComponentChoice(IRNode node, Object info, boolean flag, T after);

  /**
   * When a component finishes executing and the super component wishes to test
   * the component value as a boolean (by using a DoubleOutputPort), this
   * transfer method is called. The boolean indicates which output the transfer
   * is for: the true or false branches.
   */
  public T transferConditional(IRNode node, boolean flag, T after);

  /**
   * Determine if the label could have been added here.
   * 
   * @param matchLabel
   *          the label in the node.
   * @param label
   *          the label to be tested.
   * @return true if possible
   **/
  public boolean testAddLabel(ControlLabel matchLabel, ControlLabel label);
}
