
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class AnyInstanceExpressionNode extends ExpressionNode { 
  // Fields
  private final NamedTypeNode type;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("AnyInstanceExpression") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        NamedTypeNode type =  (NamedTypeNode) _kids.get(0);
        return new AnyInstanceExpressionNode (_start,
          type        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public AnyInstanceExpressionNode(int offset,
                                   NamedTypeNode type) {
    super(offset);
    if (type == null) { throw new IllegalArgumentException("type is null"); }
    ((AASTNode) type).setParent(this);
    this.type = type;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    final StringBuilder sb = new StringBuilder();
    if (debug) {
      indent(sb, indent);
      sb.append("AnyInstanceExpression\n");
      sb.append(getType().unparse(debug, indent+2));
    } else {
      sb.append("any(");
      sb.append(getType().unparse(false, 0));
      sb.append(')');
    }
    return sb.toString();
  }

  /**
   * @return A non-null node
   */
  public NamedTypeNode getType() {
    return type;
  }
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
   
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new AnyInstanceExpressionNode(getOffset(), (NamedTypeNode)getType().cloneTree());
  }
}