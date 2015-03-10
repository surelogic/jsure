// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/template/ConflictResult.java,v 1.5 2008/01/30 15:47:04 aarong Exp $
package edu.cmu.cs.fluid.java.template;

import com.surelogic.analysis.effects.ConflictingEffects;

@Deprecated
public class ConflictResult
{
  private final String message;
  private final ConflictingEffects conflicts;

  public ConflictResult( final String msg, final ConflictingEffects ce )
  {
    message = msg;
    conflicts = ce;
  }

  public String getMessage()
  {
    return message;
  }

  public ConflictingEffects getConflicts()
  {
    return conflicts;
  }
}
