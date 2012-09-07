package com.surelogic.analysis.concurrency.util;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.annotation.rules.EqualityRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.ValueObjectPromiseDrop;

public final class ValueObjectAnnotationTester extends TypeDeclAnnotationTester {
  public ValueObjectAnnotationTester(
      final IBinder binder, final ITypeFormalEnv formalEnv) {
    super(binder, formalEnv);
  }
  
  @Override
  protected ValueObjectPromiseDrop testTypeDeclaration(final IRNode type) {
    return EqualityRules.getValueObjectDrop(type);
  }           
  
  @Override
  protected PromiseDrop<? extends IAASTRootNode> testFormalAgainstAnnotationBounds(
      final IJavaTypeFormal formal) {
    return formalEnv.isValueObject(formal);
  }
  
  @Override
  protected boolean testArrayType(final IJavaArrayType type) {
    return false;
  }
}
