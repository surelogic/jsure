package com.surelogic.dropsea.ir;

import com.surelogic.dropsea.ISupportingInformation;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNode;

/**
 * Class to hold a string of supporting information about a drop.
 */
public final class SupportingInformationViaString implements ISupportingInformation {

  /**
   * fAST node the supporting information wants to reference.
   */
  IRNode location;

  /**
   * A message describing the point of the supporting information.
   */
  String message = "(NONE)";

  /**
   * @return the fAST location this supporting information references, can be
   *         <code>null</code>
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
   * @return the source reference of the fAST node this information references,
   *         can be <code>null</code>
   */
  public ISrcRef getSrcRef() {
    return (location != null ? JavaNode.getSrcRef(location) : null);
  }

  public boolean sameAs(IRNode link, int num, Object[] args) {
    return false;
  }

  public boolean sameAs(IRNode link, String message) {
    return message.equals(this.message) && this.location != null && (this.location == link || this.location.equals(link));
  }
}