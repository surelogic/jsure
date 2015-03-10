/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/AddLabel.java,v 1.4 2003/07/02 20:19:22 thallora Exp $ */
package edu.cmu.cs.fluid.control;

/** When flow goes through this node, a label is added onto
 * what we already have.
 * @author John Tang Boyland
 * @see LabelTest
 * @see PendingLabelStrip
 */

public class AddLabel extends Flow {
  protected ControlLabel label;
  public AddLabel(ControlLabel label) {
    this.label = label;
  }
  public ControlLabel getLabel() {
    return label;
  }
}

