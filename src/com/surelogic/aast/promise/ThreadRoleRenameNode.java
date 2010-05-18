
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class ThreadRoleRenameNode extends ThreadRoleAnnotationNode { 
  // Fields
  private final ThreadRoleNameNode tRole;
  private final ThreadRoleExprNode trExpr;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("ThreadRoleRename") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        ThreadRoleNameNode trName =  (ThreadRoleNameNode) _kids.get(0);
        ThreadRoleExprNode trExpr =  (ThreadRoleExprNode) _kids.get(1);
        return new ThreadRoleRenameNode (_start,
          trName,
          trExpr        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ThreadRoleRenameNode(int offset,
                         ThreadRoleNameNode trName,
                         ThreadRoleExprNode trExpr) {
    super(offset);
    if (trName == null) { throw new IllegalArgumentException("trName is null"); }
    ((AASTNode) trName).setParent(this);
    this.tRole = trName;
    if (trExpr == null) { throw new IllegalArgumentException("trExpr is null"); }
    ((AASTNode) trExpr).setParent(this);
    this.trExpr = trExpr;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("ThreadRoleRename\n");
    sb.append(getThreadRole().unparse(debug, indent+2));
    sb.append(getTRExpr().unparse(debug, indent+2));
    return sb.toString();
  }

  /**
   * @return A non-null node
   */
  public ThreadRoleNameNode getThreadRole() {
    return tRole;
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
		return new ThreadRoleRenameNode(getOffset(), (ThreadRoleNameNode)getThreadRole().cloneTree(), (ThreadRoleExprNode)getTRExpr().cloneTree());
	}
}

