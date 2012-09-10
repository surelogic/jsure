package edu.cmu.cs.fluid.sea;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.WrappedSrcRef;

/**
 * Drop to represent promise scrubber warnings reported by the analysis
 * infrastructure. These warnings indicated a syntactical or semantic problem
 * with a user-expressed model of design intent.
 */
public final class PromiseWarningDrop extends IRReferenceDrop {

  private final int f_offset;

  public PromiseWarningDrop(int off) {
    f_offset = off;
  }

  public PromiseWarningDrop() {
    this(-1);
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
      return new WrappedSrcRef(getSrcRef()) {
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
