
package com.surelogic.aast.promise;


import java.util.List;
import java.util.Map;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.AASTBinder;
import com.surelogic.aast.bind.ILockBinding;
import com.surelogic.aast.bind.IType;
import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.aast.java.QualifiedThisExpressionNode;
import com.surelogic.aast.java.ThisExpressionNode;
import com.surelogic.aast.java.TypeExpressionNode;
import com.surelogic.aast.java.VariableUseExpressionNode;
import com.surelogic.aast.AbstractAASTNodeFactory;
import com.surelogic.dropsea.ir.drops.locks.LockModel;

import edu.cmu.cs.fluid.ir.IRNode;

public final class QualifiedLockNameNode extends LockNameNode { 
  // Fields
  private final ExpressionNode base;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("QualifiedLockName") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        ExpressionNode base =  (ExpressionNode) _kids.get(0);
        String id = _id;
        return new QualifiedLockNameNode (_start,
          base,
          id        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public QualifiedLockNameNode(int offset,
                               ExpressionNode base,
                               String id) {
    super(offset, id);
    if (base == null) { throw new IllegalArgumentException("base is null"); }
    ((AASTNode) base).setParent(this);
    this.base = base;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { 
      indent(sb, indent); 
      sb.append("QualifiedLockName\n");
      sb.append(getBase().unparse(debug, indent+2));
      indent(sb, indent+2);
      sb.append("id=").append(getId());
      sb.append("\n");
    } else {
      sb.append(getBase().unparse(false));
      sb.append(':');
      sb.append(getId());
    }
    return sb.toString();
  }

  /**
   * @return A non-null node
   */
  public ExpressionNode getBase() {
    return base;
  }
  
  public boolean bindingExists() {
    return AASTBinder.getInstance().isResolvable(this);
  }

  public ILockBinding resolveBinding() {
    return AASTBinder.getInstance().resolve(this);
  }
  
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {   
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
    return new QualifiedLockNameNode(getOffset(),
        (ExpressionNode)getBase().cloneTree(), new String(getId()));
  }

  
  
  @Override
  public final boolean namesSameLockAs(final LockNameNode ancestor,
      final Map<IRNode, Integer> positionMap, final How how) {
    return ancestor.namesSameLockAsQualifiedLock(this, positionMap, how);
  }

  @Override
  final boolean namesSameLockAsSimpleLock(final SimpleLockNameNode overriding,
      final Map<IRNode, Integer> positionMap, final How how) {
    /* Duplicated from SimpleLockNameNode.nameSameLockAsQualifiedLock(). Don't
     * share code directly because we want to maintain the overriding/ancestor
     * distinction.
     */

    if (getId().equals(overriding.getId())) {
      final ExpressionNode ancestorBase = getBase();
      final LockModel overridingModel = overriding.resolveBinding().getModel();
      if (!overridingModel.isLockStatic()) { // first lock is from the receiver
        return specifiesTheReceiver(ancestorBase);
      } else { // First lock is a static lock from the current class
        if (ancestorBase instanceof TypeExpressionNode) {
          // must refer to the same static lock model
          return overridingModel.equals(overriding.resolveBinding().getModel());
        }
      }
    }
    return false;
  }

  @Override
  final boolean namesSameLockAsQualifiedLock(final QualifiedLockNameNode overriding,
      final Map<IRNode, Integer> positionMap, final How how) {
    if (getId().equals(overriding.getId())) {
      final ExpressionNode ancestorBase = getBase();
      final ExpressionNode overridingBase = overriding.getBase();
      
      // Static locks: must be the same lock model
      if ((ancestorBase instanceof TypeExpressionNode) &&
          (overridingBase instanceof TypeExpressionNode)) {
        final LockModel model = resolveBinding().getModel();
        final LockModel overridingModel = overriding.resolveBinding().getModel();
        return model.equals(overridingModel);
      }
      
      // Variable use expression: Must name the same formal parameter.  
      // Normalize names by checking for the parameter position.
      if ((ancestorBase instanceof VariableUseExpressionNode) && 
          (overridingBase instanceof VariableUseExpressionNode)) {
        final IRNode formal = ((VariableUseExpressionNode) ancestorBase).resolveBinding().getNode();
        final IRNode overridingFormal = ((VariableUseExpressionNode) overridingBase).resolveBinding().getNode();
        final int pos = positionMap.get(formal);
        final int overridingPos = positionMap.get(overridingFormal);
        return (pos == overridingPos);
      }        
      
      if (ancestorBase instanceof ThisExpressionNode) {
        return specifiesTheReceiver(overridingBase);
      }
      
      if (ancestorBase instanceof QualifiedThisExpressionNode) {
        // Check if the ancestor really is just the 0th-qualified outer class, that is, the normal receiver
        if (((QualifiedThisExpressionNode) ancestorBase).namesEnclosingTypeOfAnnotatedMethod()) {
          return specifiesTheReceiver(overridingBase);
        } else { // ancestor is a real qualified receiver
          if (overridingBase instanceof QualifiedThisExpressionNode) {
            // C. this and D.this.  Equal if C and D are the same type...
            final IType type = ((QualifiedThisExpressionNode) ancestorBase).getType().resolveType();
            final IType overridingType = ((QualifiedThisExpressionNode) overridingBase).getType().resolveType();
            return type.getJavaType().equals(overridingType.getJavaType());
          }
        }
      }
    }
    return false;
  }
  
  private static boolean specifiesTheReceiver(final ExpressionNode base) {
    if (base instanceof ThisExpressionNode) {
      // Other expression is an explicit this
      return true;
    } else if (base instanceof QualifiedThisExpressionNode) {
      /* Qualified type must be the type that contains the annotated method */
      return ((QualifiedThisExpressionNode) base).namesEnclosingTypeOfAnnotatedMethod();
    }
    return false;
  }
}

