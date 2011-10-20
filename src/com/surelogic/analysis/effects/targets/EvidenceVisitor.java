package com.surelogic.analysis.effects.targets;

import com.surelogic.analysis.effects.AggregationEvidence;
import com.surelogic.analysis.effects.BCAEvidence;
import com.surelogic.analysis.effects.ElaborationEvidence;

public interface EvidenceVisitor {
  /* 'e' may be null */
  public void accept(TargetEvidence e);
  
  public void visitAggregationEvidence(AggregationEvidence e);
  public void visitBCAEvidence(BCAEvidence e);
  public void visitElaborationEvidence(ElaborationEvidence e);
  public void visitEmptyEvidence(EmptyEvidence e);
}
