package com.surelogic.analysis.concurrency.threadsafe;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.promise.AbstractModifiedBooleanNode;
import com.surelogic.analysis.concurrency.annotationbounds.ITypeFormalEnv;
import com.surelogic.annotation.rules.LockRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.ModifiedBooleanPromiseDrop;

final class ThreadSafeAnnotationTester extends TypeDeclAnnotationTester {
  public ThreadSafeAnnotationTester(
      final IBinder binder, final ITypeFormalEnv formalEnv) {
    super(binder, formalEnv);
  }
  
  @Override
  protected ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> testTypeDeclaration(
      final IRNode type) {
    return LockRules.getThreadSafeTypePromise(type);
  }
  
  @Override
  protected PromiseDrop<? extends IAASTRootNode> testFormalAgainstAnnotationBounds(
      final IJavaTypeFormal formal) {
    return formalEnv.isThreadSafe(formal);
  }
  
  @Override
  protected boolean testArrayType(final IJavaArrayType type) {
    return false;
  }
}
