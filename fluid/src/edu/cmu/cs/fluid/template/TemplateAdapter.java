/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/template/TemplateAdapter.java,v 1.4 2007/03/09 16:45:24 chance Exp $ */
package edu.cmu.cs.fluid.template;


/**
 * This interface is implemented by classes that receive the results of 
 * executing a template.
 */
@SuppressWarnings("deprecation")
public class TemplateAdapter
implements TemplateListener
{
  public void templateIsRunning( final TemplateEvent r )
  {
  }

  public void templateIsDone( final TemplateEvent r )
  {
  }
}
