/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/template/TemplateSpawnedListener.java,v 1.4 2003/07/15 19:26:06 aarong Exp $ */
package edu.cmu.cs.fluid.template;

import java.util.EventListener;

/**
 * This interface is implemented by classes that wish to be
 * notified whenever a template spawns a subsidiary template.
 */
public interface TemplateSpawnedListener
extends EventListener
{
  /**
   * Called when a template has spawned a subsidiary template.
   * @param e The template event
   */
  public void templateSpawned( TemplateSpawnedEvent e );
}
