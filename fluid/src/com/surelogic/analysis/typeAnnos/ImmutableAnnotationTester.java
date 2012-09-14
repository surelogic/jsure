package com.surelogic.analysis.typeAnnos;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.annotation.rules.LockRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.ImmutablePromiseDrop;

public final class ImmutableAnnotationTester extends TypeDeclAnnotationTester {
  public ImmutableAnnotationTester(
      final IBinder binder, final ITypeFormalEnv formalEnv, final boolean ex) {
    super(binder, formalEnv, ex);
  }
  
  @Override
  protected ImmutablePromiseDrop testTypeDeclaration(final IRNode type) {
    return LockRules.getImmutableType(type);
  }           
  
  @Override
  protected PromiseDrop<? extends IAASTRootNode> testFormalAgainstAnnotationBounds(
      final IJavaTypeFormal formal) {
    return formalEnv.isImmutable(formal, exclusive);
  }
  
  @Override
  protected boolean testArrayType(final IJavaArrayType type) {
    return false;
  }
}