package com.surelogic.analysis.effects.targets;

import com.surelogic.analysis.effects.targets.evidence.TargetEvidence;
import com.surelogic.analysis.regions.IRegion;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;

abstract class AbstractTargetFactory implements TargetFactory {
  @Override
  public AnyInstanceTarget createAnyInstanceTarget(
      final IJavaReferenceType clazz, final IRegion region,
      final TargetEvidence evidence) {
    return new AnyInstanceTarget(clazz, region, evidence);
  }

  @Override
  public ClassTarget createClassTarget(
      final IRegion region, final TargetEvidence elabEvidence) {
    return new ClassTarget(region, elabEvidence);
  }

  @Override
  public EmptyTarget createEmptyTarget(final TargetEvidence evidence) {
    return new EmptyTarget(evidence);
  }
  
  @Override
  public LocalTarget createLocalTarget(final IRNode varDecl) {
    return new LocalTarget(varDecl);
  }
}
