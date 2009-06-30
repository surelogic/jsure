// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/template/TemplateConflictResults.java,v 1.5 2003/07/15 19:26:07 aarong Exp $ 
package edu.cmu.cs.fluid.java.template;

import java.util.Vector;

/**
 * Tag interface for {@link edu.cmu.cs.fluid.template.TemplateEvent}s that 
 * indicates the event is carrying effect conflict data.
 */
public interface TemplateConflictResults
{
  /**
   * Get the conflicting effects.
   * @return A Vector of {@link ConflictResult} objects.
   */
  public Vector getConflicts();
}
