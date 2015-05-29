package com.surelogic.analysis.effects.targets.evidence;

import com.surelogic.analysis.effects.Messages;
import com.surelogic.analysis.effects.targets.Target;

import edu.cmu.cs.fluid.ir.IRNode;

public final class EmptyEvidence implements TargetEvidence {
  public enum Reason {
    DECLARES_NO_EFFECTS(Messages.REASON_NO_DECLARED_EFFECT),
    RECEIVER_IS_IMMUTABLE(Messages.REASON_RECEIVER_IS_IMMUTABLE),
    FINAL_FIELD(Messages.REASON_FINAL_FIELD),
    NULL_REFERENCE(Messages.REASON_NULL_REFERENCE),
    NEW_OBJECT(Messages.REASON_NEW_OBJECT),
    UNIQUE_RETURN(Messages.REASON_UNIQUE_RETURN),
    UNIQUE_PARAMETER(Messages.REASON_UNIQUE_PARAM);
    
    private int msg;

    private Reason(final int m) {
      msg = m;
    }
    
    public int getMessage() {
      return msg;
    }
  }

  
  
  private final Reason reason;
  
  // May be null
  private final Target comesFrom;
  
  // May be null
  private final IRNode link;
  
  
  
  public EmptyEvidence(final Reason r, final Target t, final IRNode n) {
    reason = r;
    comesFrom = t;
    link = n;
  }
  
  @Override
  public IRNode getLink() {
    return link;
  }
  
  public Reason getReason() {
    return reason;
  }
 
  public TargetEvidence getMoreEvidence() {
    return comesFrom == null ? null : comesFrom.getEvidence();
  }
  
  @Override
  public void visit(final EvidenceVisitor v) {
    v.visitEmptyEvidence(this);
  }
}
