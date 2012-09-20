package com.surelogic.dropsea.ir;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.HINT_DROP;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.HINT_TYPE_ATTR;

import com.surelogic.MustInvokeOnOverride;
import com.surelogic.NonNull;
import com.surelogic.common.xml.XMLCreator;
import com.surelogic.dropsea.IAnalysisHintDrop;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Drops for the analyses to use to give "hints" (suggestions and warnings) to
 * the user.
 */
public final class AnalysisHintDrop extends IRReferenceDrop implements IAnalysisHintDrop {

  /**
   * Constructs a new suggestions pointing to the passed node.
   * 
   * @param node
   *          referenced by the suggestion.
   * @return a suggestion.
   */
  public static AnalysisHintDrop newSuggestion(IRNode node) {
    return new AnalysisHintDrop(node, HintType.SUGGESTION);
  }

  /**
   * Constructs a new warning pointing to the passed node.
   * 
   * @param node
   *          referenced by the warning.
   * @return a warning.
   */
  public static AnalysisHintDrop newWarning(IRNode node) {
    return new AnalysisHintDrop(node, HintType.WARNING);
  }

  private AnalysisHintDrop(IRNode node, HintType level) {
    super(node);
    f_type = level == null ? HintType.SUGGESTION : level;
  }

  private final HintType f_type;

  @NonNull
  public HintType getHintType() {
    return f_type;
  }

  /*
   * XML output methods are invoked single-threaded
   */

  @Override
  public String getXMLElementName() {
    return HINT_DROP;
  }

  @MustInvokeOnOverride
  public void snapshotAttrs(XMLCreator.Builder s) {
    super.snapshotAttrs(s);
    s.addAttribute(HINT_TYPE_ATTR, f_type.toString());
  }
}