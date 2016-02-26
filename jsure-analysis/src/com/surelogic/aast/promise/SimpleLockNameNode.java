
package com.surelogic.aast.promise;


import java.util.List;
import java.util.Map;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.AASTBinder;
import com.surelogic.aast.bind.ILockBinding;
import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.aast.java.QualifiedThisExpressionNode;
import com.surelogic.aast.java.ThisExpressionNode;
import com.surelogic.aast.java.TypeExpressionNode;
import com.surelogic.aast.AbstractAASTNodeFactory;
import com.surelogic.dropsea.ir.drops.locks.LockModel;

import edu.cmu.cs.fluid.ir.IRNode;

public final class SimpleLockNameNode extends LockNameNode { 
  // Fields

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("SimpleLockName") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        String id = _id;
        return new SimpleLockNameNode (_start,
          id        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public SimpleLockNameNode(int offset,
                            String id) {
    super(offset, id);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    if (!debug) {
      return getId();
    }
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("SimpleLockName\n");
    indent(sb, indent+2);
    sb.append("id=").append(getId());
    sb.append("\n");
    return sb.toString();
  }
  
  @Override
  public boolean bindingExists() {
    return AASTBinder.getInstance().isResolvable(this);
  }

  @Override
  public ILockBinding resolveBinding() {
    return AASTBinder.getInstance().resolve(this);
  }
  
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  protected IAASTNode internalClone(final INodeModifier mod) {
  	return new SimpleLockNameNode(getOffset(), getId());
  }
  
  

  @Override
  public final boolean namesSameLockAs(final LockNameNode ancestor,
      final Map<IRNode, Integer> positionMap, final How how) {
    return ancestor.namesSameLockAsSimpleLock(this, positionMap, how);
  }

  @Override
  final boolean namesSameLockAsSimpleLock(final SimpleLockNameNode overriding,
      final Map<IRNode, Integer> positionMap, final How how) {
    // Two simple lock names: Both represent a lock of the receiver or a static lock
    // Lock models must be the same
    return resolveBinding().getModel().equals(
        overriding.resolveBinding().getModel());
  }

  @Override
  final boolean namesSameLockAsQualifiedLock(
      final QualifiedLockNameNode overriding,
      final Map<IRNode, Integer> positionMap, final How how) {
    if (getId().equals(overriding.getId())) {
      final ExpressionNode overridingBase = overriding.getBase();
      final LockModel model = resolveBinding().getModel();
      if (!model.isLockStatic()) { // first lock is from the receiver
        if (overridingBase instanceof ThisExpressionNode) {
          // Other expression is an explicit this
          return true;
        } else if (overridingBase instanceof QualifiedThisExpressionNode) {
          /* Qualified type must be the type that contains the annotated method */
          return ((QualifiedThisExpressionNode) overridingBase).namesEnclosingTypeOfAnnotatedMethod();
        }
      } else { // First lock is a static lock from the current class
        if (overridingBase instanceof TypeExpressionNode) {
          // must refer to the same static lock model
          return model.equals(overriding.resolveBinding().getModel());
        }
      }
    }
    return false;
  }
}

