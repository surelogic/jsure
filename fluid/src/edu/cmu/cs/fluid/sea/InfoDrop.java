package edu.cmu.cs.fluid.sea;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Drops for reporting inferred or information to the user, "i" results.
 * <p>
 * The only subtype of this should be {@link WarningDrop}. This type is not
 * intended to be otherwise subtyped.
 */
public class InfoDrop extends IRReferenceDrop implements IReportedByAnalysisDrop {

  public InfoDrop(IRNode node) {
    super(node);
  }
}