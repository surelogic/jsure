package com.surelogic.analysis.effects.targets;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.effects.targets.evidence.TargetEvidence;
import com.surelogic.analysis.regions.IRegion;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * An implementation of TargetFactory that frees the caller of
 * {@link #createInstanceTarget(IRNode, IRegion)} from generating the correct
 * ReceiverDeclaration and QualifiedReceiverDeclaration nodes.  It does this 
 * by running every object node given to {@link #createInstanceTarget(IRNode, IRegion)} 
 * through a {@code ThisExpressionBinder}.
 * 
 * @author aarong
 */
public final class ThisBindingTargetFactory extends AbstractTargetFactory {
  private final ThisExpressionBinder thisExprBinder;
  
  
  
  public ThisBindingTargetFactory(final ThisExpressionBinder teb) {
    thisExprBinder = teb;
  }


  
  @Override
  public InstanceTarget createInstanceTarget(
      final IRNode object, final IRegion region,
      final TargetEvidence evidence) {
    return new InstanceTarget(
        thisExprBinder.bindThisExpression(object), region, evidence);
  }
}
