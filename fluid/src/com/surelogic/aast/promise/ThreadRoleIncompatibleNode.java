
package com.surelogic.aast.promise;


import java.util.*;

import com.surelogic.aast.*;

public class ThreadRoleIncompatibleNode extends ThreadRoleNameListNode { 
  // Fields
  private final List<ThreadRoleNameNode> tRoles;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("ThreadRoleIncompatible") {
      @SuppressWarnings("unchecked")
	@Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        @SuppressWarnings("rawtypes")
        List<ThreadRoleNameNode> tRoles = ((List) _kids);
        return new ThreadRoleIncompatibleNode (_start, tRoles);
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ThreadRoleIncompatibleNode(int offset,
                               List<ThreadRoleNameNode> tRoles) {
    super(offset, tRoles, "ThreadRoleIncompatible");
    if (tRoles == null) { throw new IllegalArgumentException("tRoles is null"); }
    for (ThreadRoleNameNode _tr : tRoles) {
      ((AASTNode) _tr).setParent(this);
    }
    this.tRoles = Collections.unmodifiableList(tRoles);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("ThreadRolesIncompatible\n");
    for(AASTNode _n : getThreadRoleList()) {
      sb.append(_n.unparse(debug, indent+2));
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
		List<ThreadRoleNameNode> tRolesCopy = new ArrayList<ThreadRoleNameNode>(tRoles.size());
		for (ThreadRoleNameNode trNameNode : tRoles) {
			tRolesCopy.add((ThreadRoleNameNode)trNameNode.cloneTree());
		}	
		return new ThreadRoleIncompatibleNode(getOffset(), tRolesCopy);
	}
}

