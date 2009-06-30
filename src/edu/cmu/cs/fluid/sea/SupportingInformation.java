package edu.cmu.cs.fluid.sea;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNode;

/**
 * Class to hold supporting information about a drop.
 * 
 * @see ProofDrop#addSupportingInformation(String,IRNode)
 * @see PromiseDrop
 * @see ResultDrop
 */
public final class SupportingInformation {

  /**
   * fAST node the supporting information wants to reference.
   */
  IRNode location;

  /**
   * A message describing the point of the supporting information.
   */
  String message = "(NONE)";

  /**
   * @return the fAST location this supporting information references, can
   *   be <code>null</code>
   */
  public final IRNode getLocation() {
    return location;
  }

  /**
   * @return a message describing the point of this supporting information
   */
  public final String getMessage() {
    return message;
  }

  /**
   * @return the source reference of the fAST node this information
   *   references, can be <code>null</code>
   */
  public ISrcRef getSrcRef() {
    return (location != null ? JavaNode.getSrcRef(location) : null);
  }
}