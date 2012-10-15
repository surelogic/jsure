package com.surelogic.analysis.type.constraints;

import java.util.Map;
import java.util.Set;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ResultFolderDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.ContainablePromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;

public final class ContainableAnnotationTester extends TypeDeclAnnotationTester {
  private final boolean implOnly;
  
  public ContainableAnnotationTester(
      final IBinder binder, final ITypeFormalEnv fe, 
      final Map<IJavaType, ResultFolderDrop> folders,
      final boolean ex, final boolean impl) {
    super(binder, fe, folders, ex);
    implOnly = impl;
  }
  
  @Override
  protected ContainablePromiseDrop testTypeDeclaration(final IRNode type) {
    if (implOnly) {
      return LockRules.getContainableImplementation(type);
    } else {
      return LockRules.getContainableType(type);
    }
  }
  
  @Override
  protected Set<PromiseDrop<? extends IAASTRootNode>> testFormalAgainstAnnotationBounds(
      final IJavaTypeFormal formal) {
    return formalEnv.isContainable(formal, exclusive);
  }
  
  @Override
  protected boolean testArrayType(final IJavaArrayType type) {
    if (type.getDimensions() == 1) {
      final IJavaType baseType = type.getBaseType();
      return baseType instanceof IJavaPrimitiveType;
    } else {
      return false;
    }
  }
}
