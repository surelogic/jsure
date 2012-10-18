package com.surelogic.dropsea.ir;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.HINT_DROP;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.HINT_TYPE_ATTR;

import com.surelogic.MustInvokeOnOverride;
import com.surelogic.NonNull;
import com.surelogic.common.xml.XMLCreator;
import com.surelogic.dropsea.IHintDrop;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Drops for the analyses to use to give "hints" (suggestions and warnings) to
 * the user.
 */
public final class HintDrop extends Drop implements IHintDrop {

  /**
   * Constructs a new information drop pointing to the passed node.
   * 
   * @param node
   *          referenced by the information.
   * @return an information drop.
   */
  public static HintDrop newInformation(IRNode node) {
    return new HintDrop(node, HintType.INFORMATION);
  }

  /**
   * Constructs a new warning drop pointing to the passed node.
   * 
   * @param node
   *          referenced by the warning.
   * @return a warning drop.
   */
  public static HintDrop newWarning(IRNode node) {
    return new HintDrop(node, HintType.WARNING);
  }

  HintDrop(IRNode node, HintType level) {
    super(node);
    f_type = level == null ? HintType.INFORMATION : level;
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