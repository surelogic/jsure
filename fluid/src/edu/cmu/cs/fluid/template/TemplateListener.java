/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/template/TemplateListener.java,v 1.5 2007/03/09 16:45:24 chance Exp $ */
package edu.cmu.cs.fluid.template;

import java.util.EventListener;

/**
 * This interface is implemented by classes that receive the results of 
 * executing a template.
 */
@SuppressWarnings("deprecation")
public interface TemplateListener
extends EventListener
{
  /**
   * Called by a template when it is about to being running.
   */

  public void templateIsRunning( TemplateEvent r );

  /**
   * This method is called when a Template has finished executing.
   * @param r The results of the execution.
   */
  public void templateIsDone( TemplateEvent r );
}
