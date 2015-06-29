package com.surelogic.analysis.concurrency.heldlocks;

import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.effects.targets.evidence.AnonClassEvidence;
import com.surelogic.analysis.effects.targets.evidence.BCAEvidence;
import com.surelogic.analysis.effects.targets.evidence.EvidenceProcessor;

final class UndoBCAProcessor extends EvidenceProcessor {
  private Target result = null;
  
  
  
  private UndoBCAProcessor() {
    super();
  }
  

  
  @Override
  public void visitAnonClassEvidence(final AnonClassEvidence e) {
    accept(e.getOriginalEffect().getTargetEvidence());
  }

  @Override
  public void visitBCAEvidence(final BCAEvidence e) {
    result = e.getElaboratedFrom();
    accept(result.getEvidence());
  }



  public static Target undo(final Target target) {
    final UndoBCAProcessor undo = new UndoBCAProcessor();
    undo.accept(target.getEvidence());
    return undo.result == null ? target : undo.result;
  }
}
