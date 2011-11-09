package com.surelogic.analysis.effects.targets;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.regions.IRegion;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;

/**
 * An implementation of TargetFactory that frees the caller of
 * {@link #createInstanceTarget(IRNode, IRegion)} from generating the correct
 * ReceiverDeclaration and QualifiedReceiverDeclaration nodes.  It does this 
 * by running every object node given to {@link #createInstanceTarget(IRNode, IRegion)} 
 * through a {@code ThisExpressionBinder}.
 * 
 * @author aarong
 */
public final class ThisBindingTargetFactory implements TargetFactory {
  private final ThisExpressionBinder thisExprBinder;
  
  
  
  public ThisBindingTargetFactory(final ThisExpressionBinder teb) {
    thisExprBinder = teb;
  }

  
  
  public AnyInstanceTarget createAnyInstanceTarget(
      final IJavaReferenceType clazz, final IRegion region,
      final TargetEvidence evidence) {
    return new AnyInstanceTarget(clazz, region, evidence);
  }

  public ClassTarget createClassTarget(
      final IRegion region, final TargetEvidence evidence) {
    return new ClassTarget(region, evidence);
  }
  
  public EmptyTarget createEmptyTarget(final TargetEvidence evidence) {
    return new EmptyTarget(evidence);
  }

  public InstanceTarget createInstanceTarget(
      final IRNode object, final IRegion region,
      final TargetEvidence evidence) {
    return new InstanceTarget(
        thisExprBinder.bindThisExpression(object), region, evidence);
  }
  
  public LocalTarget createLocalTarget(final IRNode varDecl) {
    return new LocalTarget(varDecl);
  }
}
