package com.surelogic.dropsea.ir;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.MODELING_PROBLEM_DROP;

import com.surelogic.Nullable;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ref.JavaRef;
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

  @Override
  @Nullable
  public IJavaRef getJavaRef() {
    final IJavaRef javaRef = super.getJavaRef();

    /*
     * If the overall code reference is null we can't wrap it.
     */
    if (javaRef == null)
      return null;

    if (f_offset >= 0) {
      /*
       * Change the code reference so that it returns the more precise offset
       * that this drop knows about (from the parser).
       */
      return new JavaRef.Builder(javaRef).setOffset(f_offset).setLength(0).build();
    } else {
      /*
       * The offset we have is nonsense, return the existing source reference.
       */
      return javaRef;
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
