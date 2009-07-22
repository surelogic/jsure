/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/effects/targets/TargetFactory.java,v 1.2 2008/01/18 23:52:03 aarong Exp $*/
package com.surelogic.analysis.effects.targets;

import com.surelogic.analysis.effects.ElaborationEvidence;
import com.surelogic.analysis.regions.IRegion;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;

public interface TargetFactory {
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
