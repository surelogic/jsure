/* $Header$ */

package edu.cmu.cs.fluid.java.operator;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IHasBinding;

/** Each Call node has a binding to a method or constructor
 * and a list of actual parameters (in an Arguments node).
 */
public interface CallInterface extends IHasBinding {
  public class NoArgs extends Exception {
    public NoArgs() {
      super();
    }
  }
  
  
  
  /**
   * @return May be null
   */
  public IRNode get_TypeArgs(IRNode node);
  
  /**
   * Get the actual parameters to the call.
   * 
   * @return An Arguments node. Never null.
   * @exception NoArgs
   *              Thrown if the call does not have a list of arguments.
   *              Currently, this only happens if the call is a
   *              SimpleEnumConstantDeclaration.
   */
  public IRNode get_Args(IRNode node) throws NoArgs;
}
