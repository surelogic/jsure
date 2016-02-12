
package com.surelogic.aast.promise;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.INodeVisitor;
import com.surelogic.aast.bind.IHasVariableBinding;
import com.surelogic.aast.java.*;
import com.surelogic.analysis.concurrency.heldlocks_new.LockUtils;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.java.bind.*;

public abstract class AbstractLockDeclarationNode extends PromiseDeclarationNode 
{ 
  // Fields
  private final ExpressionNode field;

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public AbstractLockDeclarationNode(int offset,
                                     String id,
                                     ExpressionNode field) {
    super(offset, id);
    if (field == null) { throw new IllegalArgumentException("field is null"); }
    ((AASTNode) field).setParent(this);
    this.field = field;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("AbstractLockDeclaration\n");
    indent(sb, indent+2);
    sb.append("id=").append(getId());
    sb.append("\n");
    sb.append(getField().unparse(debug, indent+2));
    return sb.toString();
  }

  /**
   * @return A non-null node
   */
  public ExpressionNode getField() {
    return field;
  }
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }

  /**
   * Is the declared lock static?  A lock is static if the lock field that
   * represents the lock is static (or is a class expression).
   * 
   * @return Whether the lock is static
   */
  public boolean isLockStatic() {
    final ExpressionNode lockImpl = getField();
    if (lockImpl instanceof ThisExpressionNode ||
        lockImpl instanceof QualifiedThisExpressionNode) {
      return false;
    } else if (lockImpl instanceof ClassLockExpressionNode) {
      return true;
    } else if (lockImpl instanceof IHasVariableBinding) {
      IHasVariableBinding b = (IHasVariableBinding) lockImpl;
      return edu.cmu.cs.fluid.java.util.TypeUtil.isStatic(b.resolveBinding().getNode());
    }
    throw new IllegalArgumentException("Unknown field "+lockImpl);
  }
 
  public boolean isJUCLock() {
    final ExpressionNode lockImpl = getField();
    /* XXX: This check is incorrect for the case of a class that implements
     * ReadWriteLock and that has a declared lock implemented using "this",
     * and for similar cases with Qualified This expressions.
     * 
     * XXX: Would like to use LockUtils.implementsReadWriteLock() and 
     * LockUtils.implementsLock() here, but I don't have an instance of LockUtils.
     */
    final IJavaType testType = lockImpl.resolveType().getJavaType();
    
  
    final ITypeEnvironment typeEnvironment = IDE.getInstance().getTypeEnv();
    final IBinder binder = typeEnvironment.getBinder();
    final IJavaDeclaredType lockType = (IJavaDeclaredType) JavaTypeFactory.convertNodeTypeToIJavaType(
        typeEnvironment.findNamedType(LockUtils.JAVA_UTIL_CONCURRENT_LOCKS_LOCK),
        binder);
    final IJavaDeclaredType readWriteLockType = (IJavaDeclaredType) JavaTypeFactory.convertNodeTypeToIJavaType(
        typeEnvironment.findNamedType(LockUtils.JAVA_UTIL_CONCURRENT_LOCKS_READWRITELOCK),
        binder);
    return typeEnvironment.isRawSubType(testType, lockType) || typeEnvironment.isRawSubType(testType, readWriteLockType);
  }

  public boolean isJUCLock(final LockUtils lockUtils) {
    final ExpressionNode lockImpl = getField();
    /* XXX: This check is incorrect for the case of a class that implements
     * Lock and that has a declared lock implemented using "this",
     * and for similar cases with Qualified This expressions.
     */
    final IJavaType testType = lockImpl.resolveType().getJavaType();
    return lockUtils.implementsLock(testType) || lockUtils.implementsReadWriteLock(testType);
  }
 
  public boolean isReadWriteLock() {
    final ExpressionNode lockImpl = getField();
    /* XXX: This check is incorrect for the case of a class that implements
     * ReadWriteLock and that has a declared lock implemented using "this",
     * and for similar cases with Qualified This expressions.
     * 
     * XXX: Would like to use LockUtils.implementsReadWriteLock() 
     * here, but I don't have an instance of LockUtils.
     */
    final IJavaType testType = lockImpl.resolveType().getJavaType();
    final ITypeEnvironment typeEnvironment = IDE.getInstance().getTypeEnv();
    final IBinder binder = typeEnvironment.getBinder();
    final IJavaDeclaredType readWriteLockType = (IJavaDeclaredType) JavaTypeFactory.convertNodeTypeToIJavaType(
        typeEnvironment.findNamedType(LockUtils.JAVA_UTIL_CONCURRENT_LOCKS_READWRITELOCK),
        binder);
    if (readWriteLockType == null) {
   	    // Probably running on pre-1.5 code
    	return false;
    }
    return typeEnvironment.isRawSubType(testType, readWriteLockType);
  }

  public boolean isReadWriteLock(final LockUtils lockUtils) {
    final ExpressionNode lockImpl = getField();
    /* XXX: This check is incorrect for the case of a class that implements
     * Lock and that has a declared lock implemented using "this",
     * and for similar cases with Qualified This expressions.
     */
    final IJavaType testType = lockImpl.resolveType().getJavaType();
    return lockUtils.implementsReadWriteLock(testType);
  }
}

