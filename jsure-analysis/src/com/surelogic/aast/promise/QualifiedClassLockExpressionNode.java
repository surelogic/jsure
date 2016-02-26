
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class QualifiedClassLockExpressionNode extends ClassLockExpressionNode { 
  // Fields
  private final NamedTypeNode type;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("QualifiedClassLockExpression") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        NamedTypeNode type =  (NamedTypeNode) _kids.get(0);
        return new QualifiedClassLockExpressionNode (_start,
          type        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public QualifiedClassLockExpressionNode(int offset,
                                          NamedTypeNode type) {
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
    	sb.append("QualifiedClassLockExpression\n");
    }
    sb.append(getType().unparse(debug, indent+2));
    sb.append(".class");
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
  protected IAASTNode internalClone(final INodeModifier mod) {
  	return new QualifiedClassLockExpressionNode(getOffset(), (NamedTypeNode)getType().cloneOrModifyTree(mod));
  }
}

