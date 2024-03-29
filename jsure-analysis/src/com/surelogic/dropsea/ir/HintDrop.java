package com.surelogic.dropsea.ir;

import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.HINT_DROP;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.HINT_TYPE_ATTR;

import com.surelogic.MustInvokeOnOverride;
import com.surelogic.NonNull;
import com.surelogic.RequiresLock;
import com.surelogic.common.xml.XmlCreator;
import com.surelogic.dropsea.DropType;
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

  public static HintDrop newInformation(
      final IRNode node, final int category, final int msg, final Object... args) {
    final HintDrop hint = new HintDrop(node, HintType.INFORMATION);
    hint.setCategorizingMessage(category);
    hint.setMessage(msg, args);
    return hint;
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

  public static HintDrop newWarning(
      final IRNode node, final int category, final int msg, final Object... args) {
    final HintDrop hint = new HintDrop(node, HintType.WARNING);
    hint.setCategorizingMessage(category);
    hint.setMessage(msg, args);
    return hint;
  }
  
  
  
  HintDrop(IRNode node, HintType level) {
    super(node);
    f_type = level == null ? HintType.INFORMATION : level;
  }

  private final HintType f_type;

  @Override
  @NonNull
  public final DropType getDropType() {
    return DropType.HINT;
  }

  @Override
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

  @Override
  @MustInvokeOnOverride
  @RequiresLock("SeaLock")
  public void snapshotAttrs(XmlCreator.Builder s) {
    super.snapshotAttrs(s);
    s.addAttribute(HINT_TYPE_ATTR, f_type.toString());
  }
}