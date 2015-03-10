/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/VersionedCompound.java,v 1.1 2004/06/25 15:36:49 boyland Exp $
 * Created on Jun 5, 2004
 */
package edu.cmu.cs.fluid.version;

import edu.cmu.cs.fluid.ir.IRCompound;
import edu.cmu.cs.fluid.ir.IRState;


/**
 * A versioned compound has internal versioned slots
 * for which we need to keep change information.
 * @author boyland
 */
public interface VersionedCompound extends IRCompound {
  public boolean isChanged(Version v);
  public void setParent(IRState st);
}
