package com.surelogic.analysis.effects.targets;

import com.surelogic.analysis.effects.targets.evidence.TargetEvidence;
import com.surelogic.analysis.regions.IRegion;

import edu.cmu.cs.fluid.ir.IRNode;

public interface TargetFactory {
  public InstanceTarget createInstanceTarget(
      IRNode object, IRegion region, TargetEvidence evidence);
}
