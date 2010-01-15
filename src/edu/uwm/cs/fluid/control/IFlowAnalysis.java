/*$Header: /cvs/fluid/fluid/src/edu/uwm/cs/fluid/control/IFlowAnalysis.java,v 1.1 2007/05/07 14:05:53 boyland Exp $*/
package edu.uwm.cs.fluid.control;

import edu.cmu.cs.fluid.control.ControlEdge;
import edu.cmu.cs.fluid.control.ControlNode;
import edu.uwm.cs.fluid.util.Lattice;

public interface IFlowAnalysis<T, L extends Lattice<T>> {

  public abstract String getName();

  public abstract L getLattice();

  public abstract void initialize(ControlNode n);

  public abstract T getInfo(ControlEdge edge);

  /** Perform the analysis as specified.
   * NB: If new initializations have been performed since
   * the last time analysis was done, they will be
   * now taken into account.
   */
  public abstract void performAnalysis();

  /** Re-run the transfer functions for all edges
   * in the graph.  This function can only be called
   * once analysis is complete.
   */
  public abstract void reworkAll();

  /** Return an indication of how long the analysis took.
   * Currently it gives the total number of times an edge
   * in the control-flow graph was visited.
   */
  public abstract long getIterations();

}