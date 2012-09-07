package com.surelogic.analysis.concurrency.util;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.annotation.rules.EqualityRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.RefObjectPromiseDrop;

public final class ReferenceObjectAnnotationTester extends TypeDeclAnnotationTester {
  public ReferenceObjectAnnotationTester(
      final IBinder binder, final ITypeFormalEnv formalEnv) {
    super(binder, formalEnv);
  }
  
  @Override
  protected RefObjectPromiseDrop testTypeDeclaration(final IRNode type) {
    return EqualityRules.getRefObjectDrop(type);
  }           
  
  @Override
  protected PromiseDrop<? extends IAASTRootNode> testFormalAgainstAnnotationBounds(
      final IJavaTypeFormal formal) {
    return formalEnv.isReferenceObject(formal);
  }
  
  @Override
  protected boolean testArrayType(final IJavaArrayType type) {
    return false;
  }
}
