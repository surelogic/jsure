/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/effects/targets/ThisBindingTargetFactory.java,v 1.2 2008/01/18 23:52:03 aarong Exp $*/
package com.surelogic.analysis.effects.targets;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.regions.IRegion;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;

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
  
  public LocalTarget createLocalTarget(final IRNode varDecl) {
    return new LocalTarget(varDecl);
  }
  
  public AnyInstanceTarget createAnyInstanceTarget(
      final IJavaReferenceType clazz, final IRegion region) {
    return new AnyInstanceTarget(clazz, region);
  }
  
  public InstanceTarget createInstanceTarget(
      final IRNode object, final IRegion region) {
    return new InstanceTarget(thisExprBinder.bindThisExpression(object), region);
  }

  public ClassTarget createClassTarget(final IRegion region) {
    return new ClassTarget(region);
  }

  public ClassTarget createClassTarget(final IRNode field) {
    return new ClassTarget(RegionModel.getInstance(field));
  }
}
