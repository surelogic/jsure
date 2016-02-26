// Generated code.  Do *NOT* edit!
package com.surelogic.aast.java;

import java.util.List;

import com.surelogic.aast.*;

public class ClassExpressionNode extends ExpressionNode { 
  // Fields
  private final ReturnTypeNode type;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("ClassExpression") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        ReturnTypeNode type =  (ReturnTypeNode) _kids.get(0);
        return new ClassExpressionNode (_start,
          type        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ClassExpressionNode(int offset,
                             ReturnTypeNode type) {
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
    	sb.append("ClassExpression\n");
    	sb.append(getType().unparse(debug, indent+2));
    } else {
    	sb.append(getType().unparse(debug, indent)).append(".class");
    }
    return sb.toString();
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

	/* (non-Javadoc)
	 * @see com.surelogic.aast.AASTNode#cloneTree()
	 */
	@Override
	protected IAASTNode internalClone(final INodeModifier mod) {
		return new ClassExpressionNode(getOffset(), (ReturnTypeNode)getType().cloneOrModifyTree(mod));
	}
}

