/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis.effects.targets.evidence;

import com.surelogic.analysis.effects.targets.Target;

import edu.cmu.cs.fluid.ir.IRNode;

public final class BCAEvidence extends ElaborationEvidence {
  private final IRNode useExpr;
  private final IRNode srcExpr;
  
  public BCAEvidence(
      final Target from, final IRNode useExpr, final IRNode srcExpr) {
    super(from);
    this.useExpr = useExpr;
    this.srcExpr = srcExpr;
  }

  public IRNode getUseExpression() {
    return useExpr;
  }
  
  public IRNode getSourceExpression() {
    return srcExpr;
  }
  
  @Override
  public IRNode getLink() {
    return useExpr;
  }
  
  @Override
  public void visit(final EvidenceVisitor v) {
    v.visitBCAEvidence(this);
  }
}
