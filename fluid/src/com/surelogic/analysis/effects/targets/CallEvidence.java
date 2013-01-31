package com.surelogic.analysis.effects.targets;

import edu.cmu.cs.fluid.ir.IRNode;

public class CallEvidence implements TargetEvidence {
  private final IRNode methodDecl;
  
  public CallEvidence(final IRNode m) {
    methodDecl = m;
  }

  public final IRNode getMethod() { return methodDecl; }
  
  @Override
  public IRNode getLink() {
    return methodDecl;
  }
  
  @Override
  public void visit(final EvidenceVisitor v) {
    v.visitCallEvidence(this);
  }
}
