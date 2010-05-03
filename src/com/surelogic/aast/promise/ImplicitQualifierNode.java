
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class ImplicitQualifierNode extends ExpressionNode { 
  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("ImplicitQualifier") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        return new ImplicitQualifierNode (_start);
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ImplicitQualifierNode(int offset) {
    super(offset);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    final StringBuilder sb = new StringBuilder();
    if (debug) {
      indent(sb, indent);
      sb.append("ImplicitQualifier\n");
    } else {
      sb.append("implicitQualifier");
    }
    return sb.toString();
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
   
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new ImplicitQualifierNode(getOffset());
  }
}