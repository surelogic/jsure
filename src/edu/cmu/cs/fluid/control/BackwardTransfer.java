/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/BackwardTransfer.java,v 1.6 2007/03/09 21:54:19 chance Exp $ */
package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.util.Lattice;

/** The interface for the analysis engine which uses backward
 * control-flow analysis.  The transfer methods indicate how
 * the value carried by analysis should be modified as it is sent
 * to an ingoing control-flow edge.  A boolean argument to a
 * transfer method indicates which output the value came from.
 * the primary one (if true) or the secondary one (if false).
 * An after argument indicates the analysis value on entering the node.
 *
 * @see BackwardAnalysis
 */
public interface BackwardTransfer<T> {
  /** Compute the value before executing a node with meaning
   * determined by the component it is in.  The node and
   * an uninterpreted and component-supplied "info" field
   * is passed to help determine what sort of transfer
   * to analyze.
   */
  public Lattice<T> transferComponentFlow(IRNode node,
				       Object info,
				       Lattice<T> after);

  /** Compute the value for a branch of a semantically-based
   * conditional.  This transfer is for explicit ComponentChoice
   * control nodes, which include extra uninterpreted "info" values.
   * If the flag is true, then the after value is from the
   * the "true" branch of the condition, otherwise from the "false" branch.
   */
  public Lattice<T> transferComponentChoice(IRNode node,
					 Object info,
					 boolean flag,
					 Lattice<T> after);

  /*!!! I'm not sure what is going to happen for backward analysis and
   *!!! labels.  It should be *somewhat* comparable to ForwardAnalysis.
   */

  /** Determine if the label could have been added here.
   * @param matchLabel the label in the node.
   * @param label the label to be tested.
   * @return true if possible
   **/
  public boolean testAddLabel(ControlLabel matchLabel,
			      ControlLabel label);
}
