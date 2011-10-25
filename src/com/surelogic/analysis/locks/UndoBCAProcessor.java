package com.surelogic.analysis.locks;

import com.surelogic.analysis.effects.BCAEvidence;
import com.surelogic.analysis.effects.targets.EvidenceProcessor;
import com.surelogic.analysis.effects.targets.Target;

final class UndoBCAProcessor extends EvidenceProcessor {
  private Target result = null;
  
  
  
  private UndoBCAProcessor() {
    super();
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
