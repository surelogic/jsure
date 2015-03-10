/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/VersionTrackerListener.java,v 1.2 2003/07/02 20:19:22 thallora Exp $ */
package edu.cmu.cs.fluid.version;

import java.util.EventListener;

public interface VersionTrackerListener extends EventListener
{
  public void versionChanged( VersionTrackerEvent e );
}
