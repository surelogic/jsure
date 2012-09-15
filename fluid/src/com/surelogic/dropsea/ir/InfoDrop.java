package com.surelogic.dropsea.ir;

import com.surelogic.common.jsure.xml.AbstractXMLReader;
import com.surelogic.dropsea.IInfoDrop;
import com.surelogic.dropsea.IReportedByAnalysisDrop;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Drops for reporting inferred or information to the user, "i" results.
 * <p>
 * The only subtype of this should be {@link WarningDrop}. This type is not
 * intended to be otherwise subtyped.
 */
public class InfoDrop extends IRReferenceDrop implements IInfoDrop, IReportedByAnalysisDrop {

  public InfoDrop(IRNode node) {
    super(node);
  }

  /*
   * XML Methods are invoked single-threaded
   */

  @Override
  public String getXMLElementName() {
    return AbstractXMLReader.INFO_DROP;
  }
}