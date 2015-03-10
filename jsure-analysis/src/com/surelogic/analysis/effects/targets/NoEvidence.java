package com.surelogic.analysis.effects.targets;

import edu.cmu.cs.fluid.ir.IRNode;

public enum NoEvidence implements TargetEvidence {
  INSTANCE;

  @Override
  public IRNode getLink() {
    return null;
  }

  @Override
  public void visit(final EvidenceVisitor v) {
    v.visitNoEvidence(this);
  }
}
