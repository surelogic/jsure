package com.surelogic.analysis.type.constraints;

import java.util.Map;

import com.surelogic.annotation.rules.LockRules;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ResultFolderDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.ImmutablePromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;

public final class ImmutableAnnotationTester extends TypeDeclAnnotationTester {
  private final boolean implOnly;
  
  public ImmutableAnnotationTester(
      final IBinder binder, final ITypeFormalEnv fe, 
      final Map<IJavaType, ResultFolderDrop> folders,
      final boolean ex, final boolean impl) {
    super(binder, fe, folders, ex);
    implOnly = impl;
  }
  
  @Override
  protected ImmutablePromiseDrop testTypeDeclaration(final IRNode type) {
    if (implOnly) {
      return LockRules.getImmutableImplementation(type);
    } else {
      return LockRules.getImmutableType(type); 
    }
  }           
  
  @Override
  protected PromiseDrop<?> testFormalAgainstAnnotationBounds(
      final IJavaTypeFormal formal) {
    return formalEnv.isImmutable(formal, exclusive);
  }
  
  @Override
  protected boolean testArrayType(final IJavaArrayType type) {
    return false;
  }
}
