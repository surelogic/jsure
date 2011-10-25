package com.surelogic.analysis.effects.targets;

import com.surelogic.analysis.effects.AggregationEvidence;
import com.surelogic.analysis.effects.BCAEvidence;
import com.surelogic.analysis.effects.ElaborationEvidence;

public abstract class EvidenceProcessor implements EvidenceVisitor {
  protected EvidenceProcessor() {
    super();
  }
  
  
  
  public final void accept(final TargetEvidence e) {
    if (e != null) e.visit(this);
  }
  
  
  
  /**
   * Generic visit method: if no specific {@code visit} method handles the 
   * evidence, this method handles it.  
   */
  protected void visit(final TargetEvidence e) {
    // do nothing
  }
  

  
  public void visitAggregationEvidence(final AggregationEvidence e) {
    visitElaborationEvidence(e);
  }

  public void visitBCAEvidence(final BCAEvidence e) {
    visitElaborationEvidence(e);
  }

  public void visitElaborationEvidence(final ElaborationEvidence e) {
    visit(e);
  }

  public void visitEmptyEvidence(final EmptyEvidence e) {
    visit(e);
  }

  public void visitNoEvidence(final NoEvidence e) {
    visit(e);
  }
}
