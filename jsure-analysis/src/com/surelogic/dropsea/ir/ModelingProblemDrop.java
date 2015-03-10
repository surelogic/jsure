package com.surelogic.dropsea.ir;

import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.MODELING_PROBLEM_DROP;

import com.surelogic.Nullable;
import com.surelogic.common.Pair;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ref.JavaRef;
import com.surelogic.dropsea.DropType;
import com.surelogic.dropsea.IModelingProblemDrop;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Drop to represent modeling problems reported by the promise scrubber in the
 * analysis infrastructure. These problems indicated a syntactical or semantic
 * problem with a user-expressed model of design intent.
 */
public final class ModelingProblemDrop extends Drop implements IModelingProblemDrop {

  private final int f_offset;

  /**
   * @param offset
   *          The offset in characters into the compilation unit file.
   */
  public ModelingProblemDrop(IRNode node, int offset) {
    super(node);
    f_offset = offset;
  }

  public ModelingProblemDrop(IRNode node) {
    this(node, -1);
  }

  public final DropType getDropType() {
	return DropType.MODELING_PROBLEM;
  }

  @Override
  protected String resolveMessage(final int number) {
    return I18N.mp(number);
  }

  @Override
  protected String resolveMessage(final int number, final Object... args) {
    return I18N.mp(number, args);
  }
  
  @Override
  protected String resolveMessageCanonical(final int number) {
    return I18N.mpc(number);
  }
  
  @Override
  protected String resolveMessageCanonical(final int number, final Object... args) {
    return I18N.mpc(number, args);
  }

  
  
  @Override
  @Nullable
  protected Pair<IJavaRef, IRNode> getJavaRefAndCorrespondingNode() {
    final Pair<IJavaRef, IRNode> info = super.getJavaRefAndCorrespondingNode();

    /*
     * If the overall code reference is null we can't wrap it.
     */
    if (info == null)
      return null;

    if (f_offset >= 0) {
      /*
       * Change the code reference so that it returns the more precise offset
       * that this drop knows about (from the parser).
       */
      IJavaRef newRef = new JavaRef.Builder(info.first()).setOffset(f_offset).setLength(0).build();
      return new Pair<IJavaRef, IRNode>(newRef, info.second());
    } else {
      /*
       * The offset we have is nonsense, return the existing source reference.
       */
      return info;
    }
  }

  /*
   * XML output methods are invoked single-threaded
   */

  @Override
  public String getXMLElementName() {
    return MODELING_PROBLEM_DROP;
  }
}
