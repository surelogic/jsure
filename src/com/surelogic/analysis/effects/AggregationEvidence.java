/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis.effects;

import java.text.MessageFormat;

import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.regions.IRegion;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.operator.FieldRef;

public final class AggregationEvidence extends ElaborationEvidence {
  private final IRNode originalObjExpr;
  private final IRegion originalRegion;
  private final IRegion mappedRegion;
  
  public AggregationEvidence(final Target from,
      final IRNode originalObjExpr, final IRegion originalRegion,
      final IRegion mappedRegion) {
    super(from);
    this.originalObjExpr = originalObjExpr;
    this.originalRegion = originalRegion;
    this.mappedRegion = mappedRegion;
  }

  public IRNode getOriginalExpression() {
    return originalObjExpr;
  }
  
  public IRegion getOriginalRegion() {
    return originalRegion;
  }
  
  public IRegion getMappedRegion() {
    return mappedRegion;
  }
  
  @Override
  public String getMessage() {
    return MessageFormat.format(
        "Region {0} of object referenced by {1} is mapped into region {2} of the object referenced by {3}",
        originalRegion.getName(), DebugUnparser.toString(originalObjExpr),
        mappedRegion.getName(), DebugUnparser.toString(FieldRef.getObject(originalObjExpr)));
  }
  
  @Override
  public IRNode getLink() {
    return originalObjExpr;
  }
}
