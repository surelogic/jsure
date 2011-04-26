
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

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.drops.promises.LockModel;

public class QualifiedLockNameNode extends LockNameNode { 
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
  	return new QualifiedLockNameNode(getOffset(), (ExpressionNode)getBase().cloneTree(), new String(getId()));
  }

  @Override
  public final boolean namesSameLockAs(final LockNameNode other,
      final Map<IRNode, Integer> positionMap) {
    return other.namesSameLockAsQualifiedLock(this, positionMap);
  }

  @Override
  final boolean namesSameLockAsSimpleLock(final SimpleLockNameNode other,
      final Map<IRNode, Integer> positionMap) {
    // Avoid code duplication: forward to simple lock name
    return other.namesSameLockAsQualifiedLock(this, positionMap);
  }

  @Override
  final boolean namesSameLockAsQualifiedLock(final QualifiedLockNameNode other,
      final Map<IRNode, Integer> positionMap) {
    if (getId().equals(other.getId())) {
      final ExpressionNode base = getBase();
      final ExpressionNode otherBase = other.getBase();
      
      // Static locks: must be the same lock model
      if ((base instanceof TypeExpressionNode) &&
          (otherBase instanceof TypeExpressionNode)) {
        final LockModel model = resolveBinding().getModel();
        final LockModel otherModel = other.resolveBinding().getModel();
        return model.equals(otherModel);
      }
      
      // Variable use expression: Must name the same formal parameter.  
      // Normalize names by checking for the parameter position.
      if ((base instanceof VariableUseExpressionNode) && 
          (otherBase instanceof VariableUseExpressionNode)) {
        final IRNode formal = ((VariableUseExpressionNode) base).resolveBinding().getNode();
        final IRNode otherFormal = ((VariableUseExpressionNode) otherBase).resolveBinding().getNode();
        final int pos = positionMap.get(formal);
        final int otherPos = positionMap.get(otherFormal);
        return (pos == otherPos);
      }
      
      if (base instanceof ThisExpressionNode) {
        if (otherBase instanceof ThisExpressionNode) {
          // Two "this" expressions
          return true;
        } else if (otherBase instanceof QualifiedThisExpressionNode) {
          // One "this" expression, and one "C.this".  Equal if C is the 
          // class that contains the annotated method.
          return namesEnclosingTypeOfAnnotatedMethod((QualifiedThisExpressionNode) otherBase);
        }
      }
      
      if (base instanceof QualifiedThisExpressionNode) {
        if (otherBase instanceof QualifiedThisExpressionNode) {
          // C. this and D.this.  Equal if C and D are the same type...
          final IType type = ((QualifiedThisExpressionNode) base).getType().resolveType();
          final IType otherType = ((QualifiedThisExpressionNode) otherBase).getType().resolveType();
          if (type.getJavaType().equals(otherType.getJavaType())) {
//          if (type.equals(otherType)) {
            return true;
          } else {
            // ...or if C and D are the types that contain the annotated methods
            return namesEnclosingTypeOfAnnotatedMethod((QualifiedThisExpressionNode) base)
                && namesEnclosingTypeOfAnnotatedMethod((QualifiedThisExpressionNode) otherBase);
          }
        } else if (otherBase instanceof ThisExpressionNode) {
          // One "this" expression, and one "C.this".  Equal if C is the 
          // class that contains the annotated method.
          return namesEnclosingTypeOfAnnotatedMethod((QualifiedThisExpressionNode) base);
        }
      }
    }
    return false;
  }
}

