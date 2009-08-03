/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis.effects;

import java.text.MessageFormat;
import java.util.Map;

import com.surelogic.analysis.effects.targets.InstanceTarget;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.regions.IRegion;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;

public final class AggregationEvidence extends ElaborationEvidence {
  private final Map<RegionModel, IRegion> regionMapping;
  private final IRegion mappedRegion;
  
  public AggregationEvidence(
      final Target from, final Map<RegionModel, IRegion> map,
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
  
  public Map<RegionModel, IRegion> getRegionMapping() {
    return regionMapping;
  }
  
  @Override
  public String getMessage() {
    final IRNode originalExpression = getOriginalExpression();
    return MessageFormat.format(
        "Region {0} of object referenced by {1} is mapped into region {2} of the object referenced by {3}",
        getOriginalRegion().getName(), DebugUnparser.toString(originalExpression),
        getMappedRegion().getName(), DebugUnparser.toString(FieldRef.getObject(originalExpression)));
  }
  
  @Override
  public IRNode getLink() {
    return getOriginalExpression();
  }
}
