
package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.AASTRootNode;
import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.INodeVisitor;
import com.surelogic.aast.java.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

public final class GuardedByNode extends AASTRootNode 
{ 
  // Fields
  private final ExpressionNode lock;

  public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
  "GuardedBy") {
	  @Override
	  public AASTNode create(String _token, int _start, int _stop, int _mods,
			  String _id, int _dims, List<AASTNode> _kids) {
		  ExpressionNode field = (ExpressionNode) _kids.get(0);
		  return new GuardedByNode(_start, field);
	  }
  };
  
  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public GuardedByNode(int offset, ExpressionNode lock) {
    super(offset);
    if (lock == null) { throw new IllegalArgumentException("field is null"); }
    ((AASTNode) lock).setParent(this);
    this.lock = lock;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { 
    	indent(sb, indent);     
    	sb.append("GuardedBy\n");
    	indent(sb, indent+2);
    	sb.append(getLock().unparse(debug, indent+2));
    } else {
    	sb.append("GuardedBy(\"").append(getLock().unparse(debug, indent));
    	sb.append("\")");
    }
    return sb.toString();  
  }

  @Override
  public String unparseForPromise() {
	  return unparse(false);
  }
  
  /**
   * @return A non-null node
   */
  public ExpressionNode getLock() {
    return lock;
  }
  
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public IAASTNode cloneTree() {
	  return new GuardedByNode(offset, (ExpressionNode) lock.cloneTree());
  }
}

