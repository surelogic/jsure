package com.surelogic.analysis.type.constraints;

import java.util.Map;

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
  private final boolean exclusively;
  private final boolean implOnly;

  public ContainableAnnotationTester(
      final IBinder binder,
      final Map<IJavaType, ResultFolderDrop> folders, final boolean ex,
      final boolean impl) {
    super(binder, folders);
    exclusively = ex;
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
  protected final PromiseDrop<?> testFormalAgainstAnnotationBounds(
      final IJavaTypeFormal formal) {
    return isContainable(formal, exclusively);
  }
  
  @Override
  protected final boolean testArrayType(final IJavaArrayType type) {
    if (type.getDimensions() == 1) {
      final IJavaType baseType = type.getBaseType();
      return baseType instanceof IJavaPrimitiveType;
    } else {
      return false;
    }
  }
}

abstract class ContainableAnnotationTester2 extends TypeDeclAnnotationTester {
  private final boolean exclusively;
  
  public ContainableAnnotationTester2(
      final IBinder binder,
      final Map<IJavaType, ResultFolderDrop> folders, final boolean ex) {
    super(binder, folders);
    exclusively = ex;
  }
  
  @Override
  protected final PromiseDrop<?> testFormalAgainstAnnotationBounds(
      final IJavaTypeFormal formal) {
    return isContainable(formal, exclusively);
  }
  
  @Override
  protected final boolean testArrayType(final IJavaArrayType type) {
    if (type.getDimensions() == 1) {
      final IJavaType baseType = type.getBaseType();
      return baseType instanceof IJavaPrimitiveType;
    } else {
      return false;
    }
  }
}

/**
 * Used to test that a field declaration MUST be annotated containable.
 */
final class FieldDeclarationMustBeContainableTester extends ContainableAnnotationTester2 {
  public FieldDeclarationMustBeContainableTester(
      final IBinder binder,
      final Map<IJavaType, ResultFolderDrop> folders,
      final boolean ex) {
    super(binder, folders, true);
  }
  
  @Override
  protected ContainablePromiseDrop testTypeDeclaration(final IRNode type) {
    return LockRules.getContainableType(type);
  }
}

/**
 * Used to test that the initialization expression of a field declaration
 * generates a type whose implementation MUST be containable.
 */
final class FinalObjectContainableTester extends ContainableAnnotationTester2 {
  public FinalObjectContainableTester(
      final IBinder binder,
      final Map<IJavaType, ResultFolderDrop> folders,
      final boolean ex) {
    super(binder, folders, true);
  }
  
  @Override
  protected ContainablePromiseDrop testTypeDeclaration(final IRNode type) {
    return LockRules.getContainableImplementation(type);
  }
}

/**
 * Used to test that a type actual in a parameter type MAY be annotated
 * as containable.
 */
final class MightHaveContainableAnnotationTester extends ContainableAnnotationTester2 {
  public MightHaveContainableAnnotationTester(
      final IBinder binder,
      final Map<IJavaType, ResultFolderDrop> folders,
      final boolean ex) {
    super(binder, folders, false);
  }
  
  @Override
  protected ContainablePromiseDrop testTypeDeclaration(final IRNode type) {
    return LockRules.getContainableType(type);
  }
}
