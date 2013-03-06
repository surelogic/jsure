
package com.surelogic.aast.java;


import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.AASTBinder;
import com.surelogic.aast.bind.IVariableBinding;
import com.surelogic.aast.AbstractAASTNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.operator.QualifiedThisExpression;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.tree.Operator;

public class QualifiedThisExpressionNode extends SomeThisExpressionNode { 
  // Fields
  private final ClassTypeNode type;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("QualifiedThisExpression") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        ClassTypeNode type =  (ClassTypeNode) _kids.get(0);
        return new QualifiedThisExpressionNode (_start,
          type        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public QualifiedThisExpressionNode(int offset,
                                     ClassTypeNode type) {
    super(offset);
    if (type == null) { throw new IllegalArgumentException("type is null"); }
    ((AASTNode) type).setParent(this);
    this.type = type;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) {
      indent(sb, indent);
      sb.append("QualifiedThisExpression\n");
      sb.append(getType().unparse(debug, indent+2));
    } else {
      sb.append(getType().unparse(false));
      sb.append(".this");
    }
    return sb.toString();
  }

  /**
   * @return A non-null node
   */
  public ClassTypeNode getType() {
    return type;
  }
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public boolean bindingExists() {
    return AASTBinder.getInstance().isResolvable(this);
  }

  @Override
  public IVariableBinding resolveBinding() {
    return AASTBinder.getInstance().resolve(this);
  }

  @Override
  public Operator getOp() {
    return QualifiedThisExpression.prototype;
  }

	/* (non-Javadoc)
	 * @see com.surelogic.aast.AASTNode#cloneTree()
	 */
	@Override
	public IAASTNode cloneTree() {
		return new QualifiedThisExpressionNode(getOffset(), (ClassTypeNode)getType().cloneTree());
	}

	/**
	 * Does this node actually refer to the immediately enclosing type?  That is,
	 * is this qualified receiver really just a fancy way of naming the regular
	 * receiver?
	 */
  public boolean namesEnclosingTypeOfAnnotatedMethod() {
    final IRNode declOfEnclosingType = VisitUtil.getEnclosingType(getPromisedFor());
    final IRNode declOfNamedType =
      ((IJavaDeclaredType) getType().resolveType().getJavaType()).getDeclaration();
    return declOfEnclosingType.equals(declOfNamedType);
  }
}

