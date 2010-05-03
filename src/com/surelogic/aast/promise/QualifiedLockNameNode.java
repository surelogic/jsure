
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.AASTBinder;
import com.surelogic.aast.bind.ILockBinding;
import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.aast.AbstractAASTNodeFactory;

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
}

