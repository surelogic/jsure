
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.parse.AbstractSingleNodeFactory;

public class PolicyLockDeclarationNode extends AbstractLockDeclarationNode { 
  // Fields

  public static final AbstractSingleNodeFactory factory =
    new AbstractSingleNodeFactory("PolicyLockDeclaration") {
      @Override
      @SuppressWarnings("unchecked")      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        String id = _id;
        ExpressionNode field =  (ExpressionNode) _kids.get(0);
        return new PolicyLockDeclarationNode (_start,
          id,
          field        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public PolicyLockDeclarationNode(int offset,
                                   String id,
                                   ExpressionNode field) {
    super(offset, id, field);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { 
      indent(sb, indent); 
      sb.append("PolicyLockDeclaration\n");
      indent(sb, indent+2);
      sb.append("id=").append(getId());
      sb.append("\n");
      sb.append(getField().unparse(debug, indent+2));
    } else {
      sb.append("PolicyLock ");
      sb.append(getId());
      sb.append(" is ");
      sb.append(getField().toString());
    }
    return sb.toString();
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
   
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new PolicyLockDeclarationNode(getOffset(), new String(getId()), (ExpressionNode)getField().cloneTree());
  }
}

