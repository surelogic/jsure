/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/TrackLabel.java,v 1.5 2005/05/23 18:32:39 chance Exp $ */
package edu.cmu.cs.fluid.control;

public class TrackLabel implements ControlLabel {
  private boolean condition;
  private TrackLabel(boolean b) {
    condition = b;
  }
  @Override
  public String toString() {
    return condition ? "true" : "false";
  }

  public static final TrackLabel trueTrack = new TrackLabel(true);
  public static final TrackLabel falseTrack = new TrackLabel(false);

  public static TrackLabel getLabel(boolean b) {
    if (b) {
      return trueTrack;
    } else {
      return falseTrack;
    }
  }

  public boolean getCondition() {
    return condition;
  }
}
