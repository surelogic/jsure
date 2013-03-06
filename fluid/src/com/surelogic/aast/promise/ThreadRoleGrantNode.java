package com.surelogic.aast.promise;

import java.util.*;

import com.surelogic.aast.*;

public class ThreadRoleGrantNode extends ThreadRoleNameListNode {

  public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
      "ThreadRoleGrant") {
    @Override
    public AASTNode create(String _token, int _start, int _stop, int _mods,
                           String _id, int _dims, List<AASTNode> _kids) {
      @SuppressWarnings({ "rawtypes", "unchecked" })
      List<ThreadRoleNameNode> tRoles = ((List) _kids);
      return new ThreadRoleGrantNode(_start, tRoles);
    }
  };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ThreadRoleGrantNode(int offset, List<ThreadRoleNameNode> tRoles) {
    super(offset, tRoles, "ThreadRoleGrant");
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) {
      indent(sb, indent);
    }
    sb.append("ThreadRoleGrant\n");
    for (AASTNode _n : getThreadRoleList()) {
      sb.append(_n.unparse(debug, indent + 2));
    }
    return sb.toString();
  }

  /**
   * @return A non-null, but possibly empty list of nodes
   */
  @Override
  public List<ThreadRoleNameNode> getThreadRoleList() {
    return tRoles;
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
    return new ThreadRoleGrantNode(getOffset(), cloneTRoleList());
  }
}
