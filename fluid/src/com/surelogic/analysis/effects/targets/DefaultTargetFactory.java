package com.surelogic.analysis.effects.targets;

import com.surelogic.analysis.regions.IRegion;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;

/**
 * An implementation of TargetFactory that assumes that
 * {@link #createInstanceTarget(IRNode, IRegion)} will never be passed an IRNode
 * whose operator is ThisExpression, SuperExpression, or
 * QualifiedThisExpression. That is, the caller is responsible for generating
 * the appropriate ReceiverDeclaration and QualifiedReceiverDeclaration nodes.
 */
public enum DefaultTargetFactory implements TargetFactory {
  PROTOTYPE;
  
  
  
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
  public InstanceTarget createInstanceTarget(
      final IRNode object, final IRegion region,
      final TargetEvidence evidence) {
    return new InstanceTarget(object, region, evidence);
  }
  
  @Override
  public LocalTarget createLocalTarget(final IRNode varDecl) {
    return new LocalTarget(varDecl);
  }
}
