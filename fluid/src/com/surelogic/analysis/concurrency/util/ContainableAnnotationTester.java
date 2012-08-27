package com.surelogic.analysis.concurrency.util;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.annotation.rules.LockRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.ContainablePromiseDrop;

public final class ContainableAnnotationTester extends TypeDeclAnnotationTester {
  public ContainableAnnotationTester(
      final IBinder binder, final ITypeFormalEnv formalEnv) {
    super(binder, formalEnv);
  }
  
  @Override
  protected ContainablePromiseDrop testTypeDeclaration(final IRNode type) {
    return LockRules.getContainableType(type);
  }
  
  @Override
  protected PromiseDrop<? extends IAASTRootNode> testFormalAgainstAnnotationBounds(
      final IJavaTypeFormal formal) {
    return formalEnv.isContainable(formal);
  }
  
  @Override
  protected boolean testArrayType(final IJavaArrayType type) {
    if (type.getDimensions() == 1) {
      final IJavaType baseType = type.getBaseType();
      if (baseType instanceof IJavaPrimitiveType) {
        return true;
      } else {
        return testType(baseType);
      }
    } else {
      return false;
    }
  }
}