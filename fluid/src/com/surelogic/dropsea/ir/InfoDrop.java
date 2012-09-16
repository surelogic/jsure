package com.surelogic.dropsea.ir;

import com.surelogic.MustInvokeOnOverride;
import com.surelogic.NonNull;
import com.surelogic.common.jsure.xml.AbstractXMLReader;
import com.surelogic.common.xml.XMLCreator;
import com.surelogic.dropsea.IAnalysisHintDrop;
import com.surelogic.dropsea.IAnalysisOutputDrop;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Drops for reporting inferred or information to the user, "i" results.
 * <p>
 * The only subtype of this should be {@link WarningDrop}. This type is not
 * intended to be otherwise subtyped.
 */
public final class InfoDrop extends IRReferenceDrop implements IAnalysisHintDrop, IAnalysisOutputDrop {

  /**
   * Constructs a new suggestion pointing to the passed node.
   * 
   * @param node
   *          referenced in the suggestion
   * @return a suggestion.
   */
  public static InfoDrop newSuggestion(IRNode node) {
    return new InfoDrop(node, HintType.SUGGESTION);
  }

  /**
   * Constructs a new warning pointing to the passed node.
   * 
   * @param node
   *          referenced in the warning
   * @return a warning.
   */
  public static InfoDrop newWarning(IRNode node) {
    return new InfoDrop(node, HintType.WARNING);
  }

  private InfoDrop(IRNode node, HintType level) {
    super(node);
    f_type = level == null ? HintType.SUGGESTION : level;
  }

  private final HintType f_type;

  @Override
  @NonNull
  public HintType getLevel() {
    // TODO Auto-generated method stub
    return f_type;
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
    s.addAttribute(AbstractXMLReader.HINT_TYPE_ATTR, f_type.toString());
  }
}