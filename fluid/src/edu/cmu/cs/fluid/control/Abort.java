/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/Abort.java,v 1.4 2005/05/19 23:22:21 chance Exp $ */
package edu.cmu.cs.fluid.control;

/** Control should never reach this point.
 * @author John Tang Boyland
 */

public class Abort extends Sink {
  public Abort() {
	  // Nothing to do
  }
  public Abort(ControlEdge prev) { setInput(prev); }
}
