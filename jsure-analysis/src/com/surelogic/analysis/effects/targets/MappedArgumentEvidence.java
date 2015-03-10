package com.surelogic.analysis.effects.targets;

import edu.cmu.cs.fluid.ir.IRNode;

public final class MappedArgumentEvidence extends CallEvidence {
  private final IRNode formal;
  private final IRNode actual;
  
  public MappedArgumentEvidence(final IRNode m, final IRNode f, final IRNode a) {
    super(m);
    formal = f;
    actual = a;
  }

  public IRNode getFormal() { return formal; }
  public IRNode getActual() { return actual; }
  
  @Override
  public IRNode getLink() {
    return actual;
  }
  
  @Override
  public void visit(final EvidenceVisitor v) {
    v.visitMappedArgumentEvidence(this);
  }
}
