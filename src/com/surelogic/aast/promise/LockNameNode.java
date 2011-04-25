
package com.surelogic.aast.promise;

import java.util.Map;

import com.surelogic.aast.INodeVisitor;
import com.surelogic.aast.java.QualifiedThisExpressionNode;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.util.VisitUtil;

public abstract class LockNameNode extends LockSpecificationNode { 
  // Fields
  private final String id;

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public LockNameNode(int offset,
                      String id) {
    super(offset);
    if (id == null) { throw new IllegalArgumentException("id is null"); }
    this.id = id;
  }

  /**
   * @return A non-null String
   */
  public String getId() {
    return id;
  }
  @Override
  public <T> T accept(INodeVisitor<T> visitor) { 
    return visitor.visit(this);
  }

  @Override
  public LockNameNode getLock() {
    return this;
  }
  
  @Override
  public LockType getType() {
    return LockType.RAW;
  }
  
  
  
  /**
   * Compare two lock names from two declarations of the same method to see if they refer to same lock.
   * This is complicated by the fact that the formal arguments of the two
   * declarations, while the same in number, can have different names.  The 
   * <code>positionMap</code> is used to map formal arguments of both 
   * declarations to their position in the argument list.  Both methods use
   * the same map because the keys, the <code>VariableUseExpressionNode</code> objects, are 
   * globally unique.
   * 
   * <p>Qualified receiver expressions do not require mapping because they
   * are identified by type name.  How does this work out in practice?  What if
   * we have different sets of outer types?  What if one is a nested type, but the
   * other isn't?  Need to play with this.  I still thing the unique names 
   * save us.
   */
  public abstract boolean namesSameLockAs(
      LockNameNode other, Map<IRNode, Integer> positionMap);
  
  abstract boolean namesSameLockAsSimpleLock(SimpleLockNameNode other,
      Map<IRNode, Integer> positionMap);
  
  abstract boolean namesSameLockAsQualifiedLock(
      QualifiedLockNameNode other, Map<IRNode, Integer> positionMap);

  static boolean namesEnclosingTypeOfAnnotatedMethod(final QualifiedThisExpressionNode base) {
    final IRNode declOfEnclosingType =
      VisitUtil.getEnclosingType(base.getPromisedFor());
    final IRNode declOfNamedType =
      ((IJavaDeclaredType) base.getType().resolveType().getJavaType()).getDeclaration();
    return declOfEnclosingType.equals(declOfNamedType);
  }
}

