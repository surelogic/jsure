package com.surelogic.dropsea.ir;

import com.surelogic.MustInvokeOnOverride;
import com.surelogic.NonNull;
import com.surelogic.common.jsure.xml.AbstractXMLReader;
import com.surelogic.common.xml.XMLCreator;
import com.surelogic.dropsea.IAnalysisHintDrop;
import com.surelogic.dropsea.IAnalysisOutputDrop;
import com.surelogic.dropsea.InfoDropLevel;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Drops for reporting inferred or information to the user, "i" results.
 * <p>
 * The only subtype of this should be {@link WarningDrop}. This type is not
 * intended to be otherwise subtyped.
 */
public final class InfoDrop extends IRReferenceDrop implements IAnalysisHintDrop, IAnalysisOutputDrop {

  public InfoDrop(IRNode node) {
    this(node, null);
  }

  public InfoDrop(IRNode node, InfoDropLevel level) {
    super(node);
    f_level = level == null ? InfoDropLevel.INFORMATION : level;
  }

  private final InfoDropLevel f_level;

  @Override
  @NonNull
  public InfoDropLevel getLevel() {
    // TODO Auto-generated method stub
    return f_level;
  }

  /*
   * XML Methods are invoked single-threaded
   */

  @Override
  public String getXMLElementName() {
    return AbstractXMLReader.INFO_DROP;
  }

  @MustInvokeOnOverride
  public void snapshotAttrs(XMLCreator.Builder s) {
    s.addAttribute(AbstractXMLReader.INFO_LEVEL_ATTR, f_level.toString());
  }
}