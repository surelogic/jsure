package com.surelogic.analysis.effects.targets.evidence;

import com.surelogic.analysis.effects.targets.Target;

import edu.cmu.cs.fluid.ir.IRNode;

public final class EmptyEvidence implements TargetEvidence {
  public enum Reason {
    DECLARES_NO_EFFECTS(157),
    RECEIVER_IS_IMMUTABLE(158),
    FINAL_FIELD(160),
    NULL_REFERENCE(166),
    NEW_OBJECT(167),
    UNIQUE_RETURN(168),
    UNIQUE_PARAMETER(170),
    METHOD_CALL(172),
    UNDER_CONSTRUCTION(173);
    
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
  
  public EmptyEvidence(final Reason r) {
    this(r, null, null);
  }
  
  public EmptyEvidence(final Reason r, final IRNode n) {
    this(r, null, n);
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
