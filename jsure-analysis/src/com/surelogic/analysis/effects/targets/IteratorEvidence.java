package com.surelogic.analysis.effects.targets;

import edu.cmu.cs.fluid.ir.IRNode;

public final class IteratorEvidence implements TargetEvidence {
  private final IRNode iterator;
  
  private final TargetEvidence moreEvidence;

  
  
  public IteratorEvidence(final IRNode iter, final TargetEvidence more) {
    iterator = iter;
    moreEvidence = more;
  }
  
  @Override
  public IRNode getLink() {
    return iterator;
  }
 
  public TargetEvidence getMoreEvidence() {
    return moreEvidence;
  }
  
  @Override
  public void visit(final EvidenceVisitor v) {
    v.visitIteratorEvidence(this);
  }
}
