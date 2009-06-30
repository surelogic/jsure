/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/LabelTest.java,v 1.3 2006/04/14 19:32:32 boyland Exp $ */
package edu.cmu.cs.fluid.control;

/** When flow goes through this node, the choice that
 * is made depends on the top label in the node.
 * (And as with all choices, it is possible for analysis
 * to let control flow on both exits).
 * XXX: This class should NOT inherit from ComponentChoice,
 * because it affects the label.  It should instead inherit directly from Split.
 */

public class LabelTest extends ComponentChoice {
  private final ControlLabel testLabel;
  /** Create a label test node.
   * @param c the component this node lives in.
   * @param label the label to add for reverse analysis.
   */
  public LabelTest(Component c, ControlLabel label) {
    super(c,label);
    testLabel = label;
  }
  /** Create a label test node.
   * @param c the component this node lives in.
   * @param v some information for analysis
   * @param label the label to add for reverse analysis.
   */
  public LabelTest(Component c, Object v, ControlLabel label) {
    super(c,v);
    testLabel = label;
  }
  public ControlLabel getTestLabel() {
    return testLabel;
  }
}
