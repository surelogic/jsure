package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;

public class ThreadRoleTransparentNode extends AbstractBooleanNode {
  // Fields

  public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory("ThreadRoleTransparent") {
	  @Override
	  public AASTNode create(String _token, int _start, int _stop,
			  int _mods, String _id, int _dims, List<AASTNode> _kids) {
		  return new ThreadRoleTransparentNode();
    }
  };

  // Constructors
  /**
   * Lists passed in as arguments must be
   * 
   * @unique
   */
  public ThreadRoleTransparentNode() {
    super();
  }

  @Override
  public String unparse(boolean debug, int indent) {
    return unparse(debug, indent, "ThreadRoleTransparent");
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {

    return visitor.visit(this);
  }

  @Override
  public IAASTNode cloneTree() {
    return new ThreadRoleTransparentNode();
  }
}
