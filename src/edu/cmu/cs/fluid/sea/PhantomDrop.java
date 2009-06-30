package edu.cmu.cs.fluid.sea;

/**
 * Abstract base class for phantom drops.  These drops are used for models
 * that bridge compilation unit boundaries.  For example, region models.
 * A RegionModel drop instance exists without compilation unit dependency,
 * however, when it has no dependent promises it disappears.  A single
 * phantom drop, RegionDeclarationDrop, maintains the compilation unit
 * dependency link for the model. 
 * 
 * @see edu.cmu.cs.fluid.sea.drops.promises.RegionModel
 * @see edu.cmu.cs.fluid.sea.drops.promises.RegionDeclarationDrop
 */
public abstract class PhantomDrop extends IRReferenceDrop {

  public PhantomDrop() {
    /*
     // set up dependencies if this is being created within a ScopedPromise
     ScopedPromises sp = ScopedPromises.getInstance();
     if (sp != null) {
     sp.initDrop(this);
     } else {
     setFromSrc(false); // One of the built-in regions like []
     }
     */
  }

  /**
   * Returns if this phantom is from source code or from another location, such
   * as XML.  The default value for a promise drop is <code>true</code>.
   * 
   * @return <code>true</code> if the phantom was created from an annotation
   *   in source code, <code>false</code> otherwise
   */
  public final boolean isFromSrc() {
    return fromSrc;
  }

  /**
   * Sets if this phantom is from source code or from another location, such
   * as XML.  The default value for a promise drop is <code>true</code>.
   * 
   * @param fromSrc <code>true</code> if the phantom was created from an annotation
   *   in source code, <code>false</code> otherwise
   */
  public final void setFromSrc(boolean fromSrc) {
    this.fromSrc = fromSrc;
  }

  /**
   * Flags if this phantom is from source code or from another location, such
   * as XML.
   */
  private boolean fromSrc = true;
}