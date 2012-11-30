// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/MarkedIRNode.java,v 1.4 2005/06/10 20:55:55 chance Exp $
package edu.cmu.cs.fluid.ir;

import com.surelogic.ThreadSafe;

/** A variant of PlainIRNode that includes a marker for it's origin
 */
@ThreadSafe
public class MarkedIRNode extends PlainIRNode {
  final String marker;

  /** Create a new IRNode.  Add it to current region, if any.
   */
  public MarkedIRNode(String mark) {
    this(getCurrentRegion(), mark);
  }

  /** Create a new IRNode.
   * @param region region to add node to.
   */
  public MarkedIRNode(IRRegion region, String mark) {
    super(region);
    marker = mark;
  }

  /** If in a region, then self-identify */
  @Override
  public String toString() {
    return super.toString() + "--" + marker;
  }
}