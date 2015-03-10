/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/PossibleEra.java,v 1.1 2004/06/25 15:36:49 boyland Exp $
 */
package edu.cmu.cs.fluid.version;

import edu.cmu.cs.fluid.ir.IRState;


/**
 * An era or a stub that takes the place of an era.
 * @author boyland
 */
public interface PossibleEra {
  /**
   * Return whether this state is properly loaded in the system.
   * @param st state to check
   * @return whether the information is available to use this state.
   */
  public boolean isLoaded(IRState st);
}
