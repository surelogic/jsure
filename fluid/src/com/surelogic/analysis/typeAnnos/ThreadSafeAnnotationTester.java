package com.surelogic.analysis.typeAnnos;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.promise.AbstractModifiedBooleanNode;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.promises.ModifiedBooleanPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;

public final class ThreadSafeAnnotationTester extends TypeDeclAnnotationTester {
  public ThreadSafeAnnotationTester(
      final IBinder binder, final ITypeFormalEnv formalEnv, final boolean ex) {
    super(binder, formalEnv, ex);
  }
  
  @Override
  protected ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> testTypeDeclaration(
      final IRNode type) {
    return LockRules.getThreadSafeTypePromise(type);
  }
  
  @Override
  protected PromiseDrop<? extends IAASTRootNode> testFormalAgainstAnnotationBounds(
      final IJavaTypeFormal formal) {
    return formalEnv.isThreadSafe(formal, exclusive);
  }
  
  @Override
  protected boolean testArrayType(final IJavaArrayType type) {
    return false;
  }
}
