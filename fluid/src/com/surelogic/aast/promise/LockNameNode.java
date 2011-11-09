
package com.surelogic.aast.promise;

import java.util.Map;

import com.surelogic.aast.INodeVisitor;

import edu.cmu.cs.fluid.ir.IRNode;

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
  
  
  
  @Override
  public final boolean satisfiesSpecfication(
      final LockSpecificationNode ancestor, final Map<IRNode, Integer> positionMap,
      final How how) {
    return ancestor.lockNameSatisfiesSpecification(this, positionMap, how);
  }

  @Override
  final boolean jucLockSatistiesSpecification(
      final JUCLockNode overriding, final Map<IRNode, Integer> positionMap,
      final How how) {
    // Never the same as a JUC lock
    return false;
  }
  
  @Override
  final boolean lockNameSatisfiesSpecification(
      final LockNameNode overriding, final Map<IRNode, Integer> positionMap,
      final How how) {
    // forward to namesSameLockAs()
    return overriding.namesSameLockAs(this, positionMap, how);
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
   * <p>This is the same as {@link #satisfiesSpecfication(LockSpecificationNode, Map)},
   * but the implementation is specific to LockNameNodes.
   */
  public abstract boolean namesSameLockAs(
      LockNameNode ancestor, Map<IRNode, Integer> positionMap, How how);
  
  abstract boolean namesSameLockAsSimpleLock(SimpleLockNameNode overriding,
      Map<IRNode, Integer> positionMap, How how);
  
  abstract boolean namesSameLockAsQualifiedLock(
      QualifiedLockNameNode overriding, Map<IRNode, Integer> positionMap,
      How how);
}

