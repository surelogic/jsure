package edu.cmu.cs.fluid.sea;

import com.surelogic.common.i18n.AnalysisResultMessage;
import com.surelogic.common.i18n.JavaSourceReference;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNode;

/**
 * Class to hold an {@link AnalysisResultMessage} as supporting information
 * about a drop.
 */
public final class SupportingInformationViaAnalysisResultMessage implements ISupportingInformation {
  /**
   * fAST node the supporting information wants to reference.
   */
  final IRNode location;

  /**
   * A message describing the point of the supporting information.
   */
  final AnalysisResultMessage message;

  public SupportingInformationViaAnalysisResultMessage(IRNode link, int number, Object[] args) {
    location = link;

    final ISrcRef ref = JavaNode.getSrcRef(link);
    JavaSourceReference srcRef = IRReferenceDrop.createSourceRef(link, ref);
    message = AnalysisResultMessage.getInstance(srcRef, number, args);
  }

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
    return message.getResultString();
  }

  /**
   * @return the source reference of the fAST node this information references,
   *         can be <code>null</code>
   */
  public ISrcRef getSrcRef() {
    return (location != null ? JavaNode.getSrcRef(location) : null);
  }

  public boolean sameAs(IRNode link, int num, Object[] args) {
    return message.sameAs(num, args) && this.location != null && (this.location == link || this.location.equals(link));
  }

  public boolean sameAs(IRNode link, String message) {
    return false;
  }
}