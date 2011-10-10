package com.surelogic.analysis.effects.targets;

import com.surelogic.analysis.effects.ElaborationEvidence;
import com.surelogic.analysis.effects.targets.EmptyTarget.Reason;
import com.surelogic.analysis.regions.IRegion;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;

public interface TargetFactory {
  public EmptyTarget createEmptyTarget(
      ElaborationEvidence elabEvidence, Reason reason);
  
  public LocalTarget createLocalTarget(IRNode varDecl);
  
  public AnyInstanceTarget createAnyInstanceTarget(
      IJavaReferenceType clazz, IRegion region);
  
  public InstanceTarget createInstanceTarget(IRNode object, IRegion region);

  public InstanceTarget createInstanceTarget(IRNode object, IRegion region,
      ElaborationEvidence elabEvidence);

  public ClassTarget createClassTarget(IRegion region);

  public ClassTarget createClassTarget(
      IRegion region, ElaborationEvidence elabEvidence);
  
  public ClassTarget createClassTarget(IRNode field);
}
