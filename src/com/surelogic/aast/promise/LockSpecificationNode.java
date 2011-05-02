package com.surelogic.aast.promise;

import java.util.Map;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.IHasLockBinding;

import edu.cmu.cs.fluid.ir.IRNode;

public abstract class LockSpecificationNode extends AASTNode 
implements IHasLockBinding { 
  public enum How { EQUAL, CONTRAVARIANT, COVARIANT }
  
  
  
  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public LockSpecificationNode(int offset) {
    super(offset);
  }
  
  public abstract LockNameNode getLock();
  
  public abstract LockType getType();

  /**
   * Compare two lock specifications from two declarations of the same method to
   * see if they refer to same lock. This is complicated by the fact that the
   * formal arguments of the two declarations, while the same in number, can
   * have different names. The <code>positionMap</code> is used to map formal
   * arguments of both declarations to their position in the argument list. Both
   * methods use the same map because the keys, the
   * <code>VariableUseExpressionNode</code> objects, are globally unique.
   * 
   * <p>
   * The receiver is the specification from the overriding method.
   * 
   * @param ancestor
   *          The specification from the ancestor method
   * @param positionMap
   *          The map of <code>VariableUseExpressionNode</code>s from both
   *          methods to their position in the argument lists.
   * @return Whether the specification from the overriding method satisfies the
   *         specification of the ancestor method.
   */
  public abstract boolean satisfiesSpecfication(
      LockSpecificationNode ancestor,
      Map<IRNode, Integer> positionMap,
      How how);

  /**
   * Note, the roles are reversed.  The receiver is the ancestor method
   * specification, and the formal argument is the specification from the
   * overriding method.
   */
  abstract boolean jucLockSatistiesSpecification(
      JUCLockNode overriding, Map<IRNode, Integer> positionMap,
      How how);
  
  /**
   * Note, the roles are reversed.  The receiver is the ancestor method
   * specification, and the formal argument is the specification from the
   * overriding method.
   */
  abstract boolean lockNameSatisfiesSpecification(
      LockNameNode overriding, Map<IRNode, Integer> positionMap,
      How how);
}

