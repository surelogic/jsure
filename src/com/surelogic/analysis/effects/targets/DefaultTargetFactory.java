/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/effects/targets/DefaultTargetFactory.java,v 1.2 2008/01/18 23:52:03 aarong Exp $*/
package com.surelogic.analysis.effects.targets;

import com.surelogic.analysis.effects.ElaborationEvidence;
import com.surelogic.analysis.regions.IRegion;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;

/**
 * An implementation of TargetFactory that assumes that
 * {@link #createInstanceTarget(IRNode, IRegion)} will never be passed an IRNode
 * whose operator is ThisExpression, SuperExpression, or
 * QualifiedThisExpression. That is, the caller is responsible for generating
 * the appropriate ReceiverDeclaration and QualifiedReceiverDeclaration nodes.
 * 
 * @author aarong
 */
public final class DefaultTargetFactory implements TargetFactory {
  public static final DefaultTargetFactory PROTOTYPE = new DefaultTargetFactory();
  
  
  
  private DefaultTargetFactory() {
    // Do nothing
  }
  
  public LocalTarget createLocalTarget(final IRNode varDecl) {
    return new LocalTarget(varDecl);
  }
  
  public AnyInstanceTarget createAnyInstanceTarget(
      final IJavaReferenceType clazz, final IRegion region) {
    return new AnyInstanceTarget(clazz, region);
  }
  
  public InstanceTarget createInstanceTarget(
      final IRNode object, final IRegion region) {
    return createInstanceTarget(object, region, null);
  }
  
  public InstanceTarget createInstanceTarget(
      final IRNode object, final IRegion region,
      final ElaborationEvidence elabEvidence) {
    return new InstanceTarget(object, region, elabEvidence);
  }

  public ClassTarget createClassTarget(
      final IRegion region, final ElaborationEvidence elabEvidence) {
    return new ClassTarget(region, elabEvidence);
  }

  public ClassTarget createClassTarget(final IRegion region) {
    return createClassTarget(region, null);
  }

  public ClassTarget createClassTarget(final IRNode field) {
    return createClassTarget(RegionModel.getInstance(field));
  }
}
