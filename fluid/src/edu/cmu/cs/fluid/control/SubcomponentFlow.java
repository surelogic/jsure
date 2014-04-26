/* Header */
package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.IRNode;

/** Control-flow decision points in the graph that depend
 * on language-specific conditions.  The subcomponent is identified
 * as well as an arbitrary value that may be used to distinguish
 * multiple control points within a subcomponent.
 * This node is treated as a ComponentFlow during control flow analysis.
 * @see ComponentFlow
 */
public class SubcomponentFlow extends Flow implements SubcomponentNode {
  ISubcomponent sub;
  Object value;
  public SubcomponentFlow(ISubcomponent s, Object v) {
    super();
    sub = s;
    value = v;
  }
  public ISubcomponent getSubcomponent() {
    return sub;
  }
  public IRNode getSyntax() {
    return sub.getComponent().getSyntax();
  }
  public Object getValue() {
    return value;
  }
  public Object getInfo() {
    return value;
  }
}
