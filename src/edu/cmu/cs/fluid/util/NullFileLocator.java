/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/NullFileLocator.java,v 1.1 2004/06/25 15:20:06 boyland Exp $
 */
package edu.cmu.cs.fluid.util;

import java.io.File;


/**
 * A placeholder for a FileLocator that does not work.
 * @author boyland
 */
public class NullFileLocator extends AbstractFileLocator {

  public static final NullFileLocator prototype = new NullFileLocator();
  
  /**
   * Create a FileLocator that never succeeds.
   */
  public NullFileLocator() {
    super();
    // TODO Auto-generated constructor stub
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.util.FileLocator#locateFile(java.lang.String, boolean)
   */
  public File locateFile(String name, boolean mustExist) {
    return null;
  }

}
