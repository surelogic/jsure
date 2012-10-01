package com.surelogic.dropsea.ir;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.MODELING_PROBLEM_DROP;

import com.surelogic.Nullable;
import com.surelogic.dropsea.IModelingProblemDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.FluidJavaRef;
import edu.cmu.cs.fluid.java.IFluidJavaRef;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.WrappedSrcRef;

/**
 * Drop to represent modeling problems reported by the promise scrubber in the
 * analysis infrastructure. These problems indicated a syntactical or semantic
 * problem with a user-expressed model of design intent.
 */
public final class ModelingProblemDrop extends IRReferenceDrop implements IModelingProblemDrop {

  private final int f_offset;

  public ModelingProblemDrop(IRNode node, int off) {
    super(node);
    f_offset = off;
  }

  public ModelingProblemDrop(IRNode node) {
    this(node, -1);
  }

  @Override
  public ISrcRef getSrcRef() {
    final ISrcRef ref = super.getSrcRef();

    /*
     * If the overall source reference is null we can't wrap it.
     */
    if (ref == null)
      return null;

    if (f_offset >= 0) {
      /*
       * Wrap the source reference so that it returns the more precise offset
       * that this drop knows about (from the parser).
       */
      return new WrappedSrcRef(ref) {
        @Override
        public int getOffset() {
          return f_offset;
        }
      };
    } else {
      /*
       * The offset we have is nonsense, return the existing source reference.
       */
      return ref;
    }
  }

  @Override
  @Nullable
  public IFluidJavaRef getJavaRef() {
    final IFluidJavaRef javaRef = super.getJavaRef();

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
      return new FluidJavaRef.Builder(javaRef).setOffset(f_offset).build();
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
