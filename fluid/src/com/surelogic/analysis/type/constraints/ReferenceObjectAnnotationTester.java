package com.surelogic.analysis.type.constraints;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.annotation.rules.EqualityRules;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.RefObjectPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;

public final class ReferenceObjectAnnotationTester extends TypeDeclAnnotationTester {
  public ReferenceObjectAnnotationTester(
      final IBinder binder, final ITypeFormalEnv formalEnv, final boolean ex) {
    super(binder, formalEnv, ex);
  }
  
  @Override
  protected RefObjectPromiseDrop testTypeDeclaration(final IRNode type) {
    return EqualityRules.getRefObjectDrop(type);
  }           
  
  @Override
  protected PromiseDrop<? extends IAASTRootNode> testFormalAgainstAnnotationBounds(
      final IJavaTypeFormal formal) {
    return formalEnv.isReferenceObject(formal, exclusive);
  }
  
  @Override
  protected boolean testArrayType(final IJavaArrayType type) {
    return false;
  }
}