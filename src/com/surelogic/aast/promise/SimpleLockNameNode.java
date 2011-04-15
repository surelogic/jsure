
package com.surelogic.aast.promise;


import java.util.List;
import java.util.Map;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.AASTBinder;
import com.surelogic.aast.bind.ILockBinding;
import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.aast.java.QualifiedThisExpressionNode;
import com.surelogic.aast.java.ThisExpressionNode;
import com.surelogic.aast.AbstractAASTNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.util.VisitUtil;

public class SimpleLockNameNode extends LockNameNode { 
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
  	return new SimpleLockNameNode(getOffset(), new String(getId()));
  }

  @Override
  public final boolean namesSameLockAs(final LockNameNode other,
      final Map<IRNode, Integer> positionMap) {
    return other.namesSameLockAsSimpleLock(this, positionMap);
  }

  @Override
  final boolean namesSameLockAsSimpleLock(final SimpleLockNameNode other,
      final Map<IRNode, Integer> positionMap) {
    // Two simple lock names: Names must match
    return other.getId().equals(this.getId());
  }

  @Override
  final boolean namesSameLockAsQualifiedLock(final QualifiedLockNameNode other,
      final Map<IRNode, Integer> positionMap) {
    /* Simple lock name and qualified lock name: They match if the
     * qualifier is "this" and the lock names match.
     */
    final ExpressionNode base = other.getBase();
    if (base instanceof ThisExpressionNode) {
      return other.getId().equals(this.getId());
    } else if (base instanceof QualifiedThisExpressionNode) {
      /* Qualified type must be the type that contains the annotated method */
      final IRNode otherType = ((IJavaDeclaredType) (((QualifiedThisExpressionNode) base).getType().resolveType().getJavaType())).getDeclaration();
      final IRNode otherEnclosingType = VisitUtil.getEnclosingType(base.getPromisedFor());
      return otherEnclosingType.equals(otherType) && getId().equals(other.getId());
    } else {
      return false;
    }
  }
}

