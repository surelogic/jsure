package com.surelogic.analysis.effects.targets;

import java.util.Map;

import com.surelogic.analysis.regions.IRegion;

import edu.cmu.cs.fluid.ir.IRNode;

public final class AggregationEvidence extends ElaborationEvidence {
  private final Map<IRegion, IRegion> regionMapping;
  private final IRegion mappedRegion;
  
  public AggregationEvidence(
      final Target from, final Map<IRegion, IRegion> map,
      final IRegion mappedRegion) {
    super(from);
    this.regionMapping = map;
    this.mappedRegion = mappedRegion;
  }

  public IRNode getOriginalExpression() {
    return ((InstanceTarget) elaboratedFrom).getReference();
  }
  
  public IRegion getOriginalRegion() {
    return elaboratedFrom.getRegion();
  }
  
  public IRegion getMappedRegion() {
    return mappedRegion;
  }
  
  public Map<IRegion, IRegion> getRegionMapping() {
    return regionMapping;
  }
  
  public IRNode getLink() {
    return getOriginalExpression();
  }
  
  public void visit(final EvidenceVisitor v) {
    v.visitAggregationEvidence(this);
  }
}
