package com.surelogic.analysis.effects.targets;

import edu.cmu.cs.fluid.ir.IRNode;

public interface TargetEvidence {
  /**
   * Get a parse tree node that provides elaboration for why the target this
   * object is associated with exists.  May be <code>null</code>.
   */
  public IRNode getLink();
  
  
  
  /**
   * Accept a visitor.
   */
  public void visit(EvidenceVisitor v);
}
