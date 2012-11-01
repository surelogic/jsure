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
  private final boolean exclusive;
  private final boolean implOnly;
  
  public ImmutableAnnotationTester(
      final IBinder binder, 
      final Map<IJavaType, ResultFolderDrop> folders,
      final boolean ex, final boolean impl) {
    super(binder, folders);
    exclusive = ex;
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
    return isImmutable(formal, exclusive);
  }
  
  @Override
  protected boolean testArrayType(final IJavaArrayType type) {
    return false;
  }
}

abstract class ImmutableAnnotationTester2 extends TypeDeclAnnotationTester {
  private final boolean exclusively;
  
  public ImmutableAnnotationTester2(
      final IBinder binder,
      final Map<IJavaType, ResultFolderDrop> folders,
      final boolean ex) {
    super(binder, folders);
    exclusively = ex;
  }
  
  @Override
  protected PromiseDrop<?> testFormalAgainstAnnotationBounds(
      final IJavaTypeFormal formal) {
    return isImmutable(formal, exclusively);
  }
  
  @Override
  protected boolean testArrayType(final IJavaArrayType type) {
    return false;
  }
}

final class FieldDeclarationMustBeImmutableTester extends ImmutableAnnotationTester2 {
  public FieldDeclarationMustBeImmutableTester(
      final IBinder binder,
      final Map<IJavaType, ResultFolderDrop> folders) {
    super(binder, folders, true);
  }
  
  @Override
  protected ImmutablePromiseDrop testTypeDeclaration(final IRNode type) {
    return LockRules.getImmutableType(type); 
  }           
}

final class FinalObjectImmutableTester extends ImmutableAnnotationTester2 {
  public FinalObjectImmutableTester(
      final IBinder binder,
      final Map<IJavaType, ResultFolderDrop> folders) {
    super(binder, folders, true);
  }
  
  @Override
  protected ImmutablePromiseDrop testTypeDeclaration(final IRNode type) {
    return LockRules.getImmutableImplementation(type); 
  }           
}

final class MightHaveImmutableAnnotationTester extends ImmutableAnnotationTester2 {
  public MightHaveImmutableAnnotationTester(
      final IBinder binder,
      final Map<IJavaType, ResultFolderDrop> folders) {
    super(binder, folders, false);
  }
  
  @Override
  protected ImmutablePromiseDrop testTypeDeclaration(final IRNode type) {
    return LockRules.getImmutableType(type); 
  }           
}
