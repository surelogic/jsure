
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class ThreadRoleRequireNode extends ThreadRoleAnnotationNode { 
  // Fields
  private final ThreadRoleExprNode trExpr;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("ThreadRoleRequire") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        ThreadRoleExprNode trExpr =  (ThreadRoleExprNode) _kids.get(0);
        return new ThreadRoleRequireNode (_start,
          trExpr        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ThreadRoleRequireNode(int offset,
                          ThreadRoleExprNode trExpr) {
    super(offset);
    if (trExpr == null) { throw new IllegalArgumentException("trExpr is null"); }
    ((AASTNode) trExpr).setParent(this);
    this.trExpr = trExpr;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("ThreadRoleRequire\n");
    sb.append(getTRExpr().unparse(debug, indent+2));
    return sb.toString();
  }

  /**
   * @return A non-null node
   */
  public ThreadRoleExprNode getTRExpr() {
    return trExpr;
  }
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
   
    return visitor.visit(this);
  }

	/* (non-Javadoc)
	 * @see com.surelogic.aast.AASTNode#cloneTree()
	 */
	@Override
	public IAASTNode cloneTree() {
		return new ThreadRoleRequireNode(getOffset(), (ThreadRoleExprNode)getTRExpr().cloneTree());
	}
}

