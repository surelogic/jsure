package com.surelogic.analysis.type.constraints;

import java.util.Map;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.promise.AbstractModifiedBooleanNode;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ResultFolderDrop;
import com.surelogic.dropsea.ir.drops.ModifiedBooleanPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;

public final class ThreadSafeAnnotationTester extends TypeDeclAnnotationTester {
  private final boolean implOnly;
  
  public ThreadSafeAnnotationTester(
      final IBinder binder, final ITypeFormalEnv fe, 
      final Map<IJavaType, ResultFolderDrop> folders,
      final boolean ex, final boolean impl) {
    super(binder, fe, folders, ex);
    implOnly = impl;
  }
  
  @Override
  protected ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> testTypeDeclaration(
      final IRNode type) {
    if (implOnly) {
      return LockRules.getThreadSafeImplPromise(type);
    } else {
      return LockRules.getThreadSafeTypePromise(type);
    }
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
