/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/TemporaryEra.java,v 1.1 2004/06/25 15:36:49 boyland Exp $
 */
package edu.cmu.cs.fluid.version;

import edu.cmu.cs.fluid.ir.IRState;



/**
 * An era used by versions that don't have their permanent era yet.
 * The parent version of this placeholder indicates what old version or assigned version
 * that this version was created from (perhaps transitively).  There will never
 * be an old version between this version and the version using this temporary era.
 * A temporary era cannot be saved (of course) and cannot be used to enumerate
 * versions.
 * @author boyland
 */
public class TemporaryEra implements PossibleEra {
  private final Version root;
  
  /**
   * Create a temporary era rooted ast the given known version.
   * The version <strong>must</strong> be assigned to
   * an existing era, old or new.
   * @param r
   * @throws DisconnectedEraException
   */
  public TemporaryEra(Version r) throws DisconnectedEraException {
    root = r;
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.version.PossibleEra#isLoaded(edu.cmu.cs.fluid.ir.IRState)
   */
  @Override
  public boolean isLoaded(IRState st) {
    // defer to the root version.
    return root.isLoaded(st);
  }
}
