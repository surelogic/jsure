
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;

public class ImplicitClassLockExpressionNode extends ClassLockExpressionNode { 
  // Fields

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("ImplicitClassLockExpression") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        return new ImplicitClassLockExpressionNode (_start        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ImplicitClassLockExpressionNode(int offset) {
    super(offset);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { 
    	indent(sb, indent); 
    	sb.append("ImplicitClassLockExpression\n");
    } else {
    	sb.append("class");
    }
    return sb.toString();
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
   
    return visitor.visit(this);
  }
  
  @Override
  protected IAASTNode internalClone(final INodeModifier mod) {
  	return new ImplicitClassLockExpressionNode(getOffset());
  }
}

