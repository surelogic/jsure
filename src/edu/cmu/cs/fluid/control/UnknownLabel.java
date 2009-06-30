/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/UnknownLabel.java,v 1.3 2005/05/23 18:32:39 chance Exp $ */
package edu.cmu.cs.fluid.control;

/** The label used in backward analysis when going back over a
 * <tt>PendingLabelStrip</tt> node.  An unknown label
 * does <em>not</em> cover tracking labels.
 * @see PendingLabelStrip
 * @see BackwardAnalysis
 * @see TrackLabel
 */
public class UnknownLabel implements ControlLabel {
  public static final UnknownLabel prototype = new UnknownLabel();
  public UnknownLabel() {}
  @Override
  public String toString() { return "unknown"; }
}
