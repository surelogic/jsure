package com.surelogic.analysis.concurrency.model.implementation;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;

public abstract class ClassMemberImplementation implements UnnamedLockImplementation {
  /** The name of the interface {@code java.util.concurrent.locks.Lock}. */
  private static final String JAVA_UTIL_CONCURRENT_LOCKS_LOCK =
      "java.util.concurrent.locks.Lock";

  /** The name of the interface {@code java.util.concurrent.locks.ReadWriteLock}. */
  private static final String JAVA_UTIL_CONCURRENT_LOCKS_READWRITELOCK =
      "java.util.concurrent.locks.ReadWriteLock";


  
  protected final IRNode memberDecl;
  
  protected ClassMemberImplementation(final IRNode memberDecl) {
    this.memberDecl = memberDecl;
  }
  
  
  
  protected final int partialHashCode() {
    int result = 17;
    result += 31 * memberDecl.hashCode();
    return result;
  }
  
  protected final boolean partialEquals(final ClassMemberImplementation other) {
    return this.memberDecl.equals(other.memberDecl);
  }

  
  
  // XXX: This is probably expensive?  Find a way to cache this later
  private static IJavaType getJUCLockType(
      final IBinder binder, final ITypeEnvironment typeEnv) {
    return JavaTypeFactory.convertNodeTypeToIJavaType(
        typeEnv.findNamedType(JAVA_UTIL_CONCURRENT_LOCKS_LOCK), binder);
  }

  // XXX: This is probably expensive?  Find a way to cache this later
  private static IJavaType getJUCReadWriteLockType(
      final IBinder binder, final ITypeEnvironment typeEnv) {
    return JavaTypeFactory.convertNodeTypeToIJavaType(
        typeEnv.findNamedType(
            JAVA_UTIL_CONCURRENT_LOCKS_READWRITELOCK), binder);
  }
  
  @Override
  public final String getClassName() {
    return JavaNames.getFullTypeName(VisitUtil.getEnclosingType(memberDecl));
  }
  
  @Override
  public String getDeclaredInClassName() {
    return getClassName();
  }
  
  @Override
  public final boolean isStatic() {
    return TypeUtil.isStatic(memberDecl);
  }
  
  @Override
  public final boolean isIntrinsic(final IBinder binder) {
    return !isJUC(binder);
  }
  
  @Override
  public final boolean isJUC(final IBinder binder) {
    final IJavaType memberLockType = getMemberLockType(binder);
    final ITypeEnvironment typeEnv = binder.getTypeEnvironment();
    final IJavaType lockType = getJUCLockType(binder, typeEnv);
    final IJavaType readWriteLockType = getJUCReadWriteLockType(binder, typeEnv);
    return (lockType != null && typeEnv.isRawSubType(memberLockType, lockType)) ||
        (readWriteLockType != null && typeEnv.isRawSubType(memberLockType, readWriteLockType));
  }
  
  @Override
  public final boolean isReadWrite(final IBinder binder) {
    final IJavaType memberLockType = getMemberLockType(binder);
    final ITypeEnvironment typeEnv = binder.getTypeEnvironment();
    final IJavaType readWriteLockType = getJUCReadWriteLockType(binder, typeEnv);
    return (readWriteLockType != null && typeEnv.isRawSubType(memberLockType, readWriteLockType));
  }

  protected abstract IJavaType getMemberLockType(IBinder binder);
}
