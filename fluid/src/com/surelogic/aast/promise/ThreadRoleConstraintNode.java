/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/ColorConstraintNode.java,v 1.2 2007/10/27 17:11:10 dfsuther Exp $*/
package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class ThreadRoleConstraintNode extends ThreadRoleExprPromiseNode {
  public ThreadRoleConstraintNode(int offset, ThreadRoleExprNode n) {
    super(offset, n, "ThreadRoleConstraint");
  }

  public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
      "ThreadRoleConstraint") {
    @Override
    public AASTNode create(String _token, int _start, int _stop, int _mods,
                           String _id, int _dims, List<AASTNode> _kids) {
      return new ThreadRoleConstraintNode(_start, (ThreadRoleExprNode) _kids.get(0));
    }
  };
}
