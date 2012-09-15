package com.surelogic.dropsea.ir;

import com.surelogic.dropsea.IModelingProblem;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.WrappedSrcRef;

/**
 * Drop to represent modeling problems reported by the promise scrubber in the
 * analysis infrastructure. These problems indicated a syntactical or semantic
 * problem with a user-expressed model of design intent.
 */
public final class ModelingProblemDrop extends IRReferenceDrop implements IModelingProblem {

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
}
