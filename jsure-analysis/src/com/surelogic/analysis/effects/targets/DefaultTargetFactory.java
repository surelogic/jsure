package com.surelogic.analysis.effects.targets;

import com.surelogic.analysis.effects.targets.evidence.TargetEvidence;
import com.surelogic.analysis.regions.IRegion;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * An implementation of TargetFactory that assumes that
 * {@link #createInstanceTarget(IRNode, IRegion)} will never be passed an IRNode
 * whose operator is ThisExpression, SuperExpression, or
 * QualifiedThisExpression. That is, the caller is responsible for generating
 * the appropriate ReceiverDeclaration and QualifiedReceiverDeclaration nodes.
 */
public final class DefaultTargetFactory extends AbstractTargetFactory {
  public static final DefaultTargetFactory PROTOTYPE = new DefaultTargetFactory();
  
  
  
  private DefaultTargetFactory() {
    super();
  }
  
  
  
  @Override
  public InstanceTarget createInstanceTarget(
      final IRNode object, final IRegion region,
      final TargetEvidence evidence) {
    return new InstanceTarget(object, region, evidence);
  }
}
