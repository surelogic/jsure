/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis.effects;

import com.surelogic.analysis.effects.targets.Target;

import edu.cmu.cs.fluid.ir.IRNode;

public abstract class ElaborationEvidence {
  /** The target that was elaborated.  Must be an instance target. */
  protected final Target elaboratedFrom;
  
  protected ElaborationEvidence(final Target from) {
    elaboratedFrom = from;
  }
  
  public Target getElaboratedFrom() {
    return elaboratedFrom;
  }
  
  public abstract String getMessage();
  public abstract IRNode getLink();
}
