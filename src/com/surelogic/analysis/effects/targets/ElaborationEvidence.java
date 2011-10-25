package com.surelogic.analysis.effects;

import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.effects.targets.TargetEvidence;

public abstract class ElaborationEvidence implements TargetEvidence {
  /** The target that was elaborated.  Must be an instance target. */
  protected final Target elaboratedFrom;
  
  protected ElaborationEvidence(final Target from) {
    elaboratedFrom = from;
  }
  
  public final Target getElaboratedFrom() {
    return elaboratedFrom;
  }
  
  public final TargetEvidence getMoreEvidence() {
    return (elaboratedFrom == null) ? null : elaboratedFrom.getEvidence();
  }
}
