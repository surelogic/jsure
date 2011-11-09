package com.surelogic.analysis.effects.targets;

import com.surelogic.analysis.regions.IRegion;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;

public interface TargetFactory {
  public AnyInstanceTarget createAnyInstanceTarget(
      IJavaReferenceType clazz, IRegion region, TargetEvidence evidence);

  public ClassTarget createClassTarget(IRegion region, TargetEvidence evidence);
  
  public EmptyTarget createEmptyTarget(TargetEvidence evidence);

  public InstanceTarget createInstanceTarget(
      IRNode object, IRegion region, TargetEvidence evidence);
  
  public LocalTarget createLocalTarget(IRNode varDecl);
}
