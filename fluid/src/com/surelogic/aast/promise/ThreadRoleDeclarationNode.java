
package com.surelogic.aast.promise;


import java.util.*;



import com.surelogic.aast.*;

public class ThreadRoleDeclarationNode extends ThreadRoleNameListNode { 
  // Fields

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("ThreadRoleDeclaration") {
      @Override 
      public AASTNode create(String _token, int _start, int _stop,
          int _mods, String _id, int _dims, List<AASTNode> _kids) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        List<ThreadRoleNameNode> tRole = ((List) _kids);
        return new ThreadRoleDeclarationNode (_start, tRole);
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ThreadRoleDeclarationNode(int offset,
                              List<ThreadRoleNameNode> tRole) {
    super(offset, tRole, "ThreadRole");
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
		return new ThreadRoleDeclarationNode(getOffset(), cloneTRoleList());
	}
}

