/* $Header$ */

package edu.cmu.cs.fluid.java.operator;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IHasBinding;

/** Each Call node has a binding to a method or constructor
 * and a list of actual parameters (in an Arguments node).
 */
public interface CallInterface extends IHasBinding {
  /**
   * @return May be null
   */
  public IRNode get_TypeArgs(IRNode node);
  public IRNode get_Args(IRNode node);
}
