/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis.effects;

import java.text.MessageFormat;

import com.surelogic.analysis.effects.targets.Target;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;

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
  public String getMessage() {
    return MessageFormat.format(
        "The value of variable {0} may originate from {1}",
        DebugUnparser.toString(useExpr), DebugUnparser.toString(srcExpr));
  }
  
  @Override
  public IRNode getLink() {
    return useExpr;
  }
}
