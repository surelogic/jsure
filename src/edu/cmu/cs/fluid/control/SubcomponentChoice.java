/* Header */
package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.IRNode;

/** Control-flow decision points in the graph that depend
 * on language-specific conditions.  The subcomponent is identified
 * as well as an arbitrary value that may be used to distinguish
 * multiple control points within a subcomponent.
 * This node is treated as a ComponentChoice during control flow analysis.
 * @see ComponentChoice
 */
public class SubcomponentChoice extends Choice {
  Subcomponent sub;
  public SubcomponentChoice(Subcomponent s, Object v) {
    super(v);
    sub = s;
  }
  public Subcomponent getSubcomponent() {
    return sub;
  }
  @Override
  public IRNode getSyntax() {
    return sub.getComponent().getSyntax();
  }
}
