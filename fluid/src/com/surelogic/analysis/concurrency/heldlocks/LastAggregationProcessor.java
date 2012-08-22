package com.surelogic.analysis.concurrency.heldlocks;

import com.surelogic.analysis.effects.Effect;
import com.surelogic.analysis.effects.targets.AggregationEvidence;
import com.surelogic.analysis.effects.targets.AnonClassEvidence;
import com.surelogic.analysis.effects.targets.BCAEvidence;
import com.surelogic.analysis.effects.targets.EvidenceProcessor;
import com.surelogic.analysis.effects.targets.IteratorEvidence;
import com.surelogic.analysis.effects.targets.Target;

final class LastAggregationProcessor extends EvidenceProcessor {
  private AggregationEvidence result = null;
  
  
  
  private LastAggregationProcessor() {
    super();
  }
  


  @Override
  public void visitAggregationEvidence(final AggregationEvidence e) {
    result = e;
  }
  
  @Override
  public void visitAnonClassEvidence(final AnonClassEvidence e) {
    accept(e.getOriginalEffect().getTargetEvidence());
  }
  
  @Override
  public void visitBCAEvidence(final BCAEvidence e) {
    accept(e.getElaboratedFrom().getEvidence());
  }

  @Override
  public void visitIteratorEvidence(final IteratorEvidence e) {
    accept(e.getMoreEvidence());
  }


  public static AggregationEvidence get(final Target target) {
    final LastAggregationProcessor getter = new LastAggregationProcessor();
    getter.accept(target.getEvidence());
    return getter.result;
  }
  
  public static AggregationEvidence get(final Effect effect) {
    return get(effect.getTarget());
  }
}
