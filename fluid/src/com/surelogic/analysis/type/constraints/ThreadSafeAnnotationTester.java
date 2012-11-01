package com.surelogic.analysis.type.constraints;

import java.util.Map;

import com.surelogic.aast.promise.AbstractModifiedBooleanNode;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ResultFolderDrop;
import com.surelogic.dropsea.ir.drops.ModifiedBooleanPromiseDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.ThreadSafePromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;

public final class ThreadSafeAnnotationTester extends TypeDeclAnnotationTester {
  private final boolean exclusive;
  private final boolean implOnly;
  
  public ThreadSafeAnnotationTester(
      final IBinder binder,
      final Map<IJavaType, ResultFolderDrop> folders,
      final boolean ex, final boolean impl) {
    super(binder, folders);
    exclusive = ex;
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
  protected PromiseDrop<?> testFormalAgainstAnnotationBounds(
      final IJavaTypeFormal formal) {
    return isThreadSafe(formal, exclusive);
  }
  
  @Override
  protected boolean testArrayType(final IJavaArrayType type) {
    return false;
  }
}

abstract class ThreadSafeAnnotationTester2 extends TypeDeclAnnotationTester {
  private final boolean exclusive;
  
  public ThreadSafeAnnotationTester2(
      final IBinder binder, 
      final Map<IJavaType, ResultFolderDrop> folders,
      final boolean ex) {
    super(binder, folders);
    exclusive = ex;
  }
  
  
  /* XXX: This needs to be switched out for the annotation testing case!
   * For field testing and final object testing we tolerate immutable or threadsafe.
   * For annotation testing we only tolerate threadsafe.
   */
  @Override
  protected PromiseDrop<?> testFormalAgainstAnnotationBounds(
      final IJavaTypeFormal formal) {
    return isThreadSafe(formal, exclusive);
  }
  
  @Override
  protected boolean testArrayType(final IJavaArrayType type) {
    return false;
  }
}

/**
 * Used to test that a field declaration MUST be annotated ThreadSafe.
 * 
 * Annotation may be Immutable or ThreadSafe
 */
final class FieldDeclarationMustBeThreadSafeTester extends ThreadSafeAnnotationTester2 {
  public FieldDeclarationMustBeThreadSafeTester(
      final IBinder binder, 
      final Map<IJavaType, ResultFolderDrop> folders,
      final boolean ex) {
    super(binder, folders, true);
  }
  
  @Override
  protected ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> testTypeDeclaration(final IRNode type) {
    return LockRules.getThreadSafeTypePromise(type);
  }
}

/**
 * Used to test that the initialization expression of a field declaration
 * generates a type whose implementation MUST be ThreadSafe.
 * 
 * Annotation may be Immutable or ThreadSafe
 */
final class FinalObjectThreadSafeTester extends ThreadSafeAnnotationTester2 {
  public FinalObjectThreadSafeTester(
      final IBinder binder,
      final Map<IJavaType, ResultFolderDrop> folders,
      final boolean ex) {
    super(binder, folders, true);
  }
  
  @Override
  protected ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> testTypeDeclaration(final IRNode type) {
    return LockRules.getThreadSafeImplPromise(type);
  }
}

/**
 * Used to test that a type actual in a parameter type MAY be annotated
 * as ThreadSafe.
 * 
 * Annotation must be ThreadSafe
 */
final class MightHaveThreadSafeAnnotationTester extends ThreadSafeAnnotationTester2 {
  public MightHaveThreadSafeAnnotationTester(
      final IBinder binder,
      final Map<IJavaType, ResultFolderDrop> folders,
      final boolean ex) {
    super(binder, folders, false);
  }
  
  @Override
  protected ThreadSafePromiseDrop testTypeDeclaration(final IRNode type) {
    return LockRules.getThreadSafeType(type);
  }
}
