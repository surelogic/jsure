// Generated code.  Do *NOT* edit!
package com.surelogic.aast.java;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

import edu.cmu.cs.fluid.java.operator.TypeExpression;
import edu.cmu.cs.fluid.tree.Operator;

public class TypeExpressionNode extends PrimaryExpressionNode { 
  // Fields
  private final ReturnTypeNode type;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("TypeExpression") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        ReturnTypeNode type =  (ReturnTypeNode) _kids.get(0);
        return new TypeExpressionNode (_start,
          type        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public TypeExpressionNode(int offset,
                            ReturnTypeNode type) {
    super(offset);
    if (type == null) { throw new IllegalArgumentException("type is null"); }
    ((AASTNode) type).setParent(this);
    this.type = type;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    if (debug) {
      final StringBuilder sb = new StringBuilder();
      indent(sb, indent);
      sb.append("TypeExpression\n");
      sb.append(getType().unparse(debug, indent+2));
      return sb.toString();
    } else {
      return getType().unparse(false, 0);
    }
  }

  /**
   * @return A non-null node
   */
  public ReturnTypeNode getType() {
    return type;
  }
  
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public Operator getOp() {
    return TypeExpression.prototype;
  }

	/* (non-Javadoc)
	 * @see com.surelogic.aast.AASTNode#cloneTree()
	 */
	@Override
	public IAASTNode cloneTree() {
		return new TypeExpressionNode(getOffset(), (ReturnTypeNode)getType().cloneTree());
	}
}

