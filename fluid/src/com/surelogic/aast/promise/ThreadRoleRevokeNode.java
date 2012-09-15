
package com.surelogic.aast.promise;


import java.util.*;



import com.surelogic.aast.*;

public class ThreadRoleRevokeNode extends ThreadRoleNameListNode { 

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("ColorRevoke") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        List<ThreadRoleNameNode> trNames = ((List) _kids);
        return new ThreadRoleRevokeNode (_start, trNames);
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ThreadRoleRevokeNode(int offset,
                         List<ThreadRoleNameNode> trNames) {
    super(offset, trNames, "ColorRevoke");
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
		return new ThreadRoleRevokeNode(getOffset(), cloneTRoleList());
	}
}

