// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/template/ReorderEvent.java,v 1.7 2007/05/17 18:38:04 chance Exp $
package edu.cmu.cs.fluid.java.template;

import java.util.Vector;

import edu.cmu.cs.fluid.template.*;

@SuppressWarnings({"deprecation","unchecked"})
public abstract class ReorderEvent extends TemplateEvent.TemplateDoneEvent 
  implements TemplateConflictResults 
{
  public ReorderEvent(Template t, boolean status, String msg) {
    super(t, status, msg, new Vector());
  }
  public ReorderEvent(Template t, boolean status, String msg, Vector v) {
    super(t, status, msg, v);
    nodeSets = v;
  }
  public /* final */ Vector nodeSets;
}
