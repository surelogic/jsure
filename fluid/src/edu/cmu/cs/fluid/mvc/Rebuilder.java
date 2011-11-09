/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/Rebuilder.java,v 1.5 2003/07/15 18:39:10 thallora Exp $
 *
 * Rebuilder.java
 * Created on May 17, 2002, 2:07 PM
 */

package edu.cmu.cs.fluid.mvc;

import java.util.List;

/**
 * Interface for rebuilder plug-ins used by AbstractModelToModelStatefulView.
 * 
 * @see AbstractModelToModelStatefulView#addRebuilder
 */
public interface Rebuilder
{
  /**
   * Execute a rebuild action.
   * @param sv The stateful view that broke.
   * @param events A list of {@link ModelEvent}s that have arrived since the
   * last rebuild.
   */
  public void rebuild( ModelToModelStatefulView sv, List events )
    throws InterruptedException;
}
