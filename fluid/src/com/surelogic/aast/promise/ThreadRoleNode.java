/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/ColorNode.java,v 1.1 2007/10/24 15:18:09 dfsuther Exp $*/
package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class ThreadRoleNode extends ThreadRoleExprPromiseNode {
  public ThreadRoleNode(int offset, ThreadRoleExprNode n) {
    super(offset, n, "ThreadRole");
  }

  public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
      "ThreadRole") {
    @Override
    public AASTNode create(String _token, int _start, int _stop, int _mods,
                           String _id, int _dims, List<AASTNode> _kids) {
      return new ThreadRoleNode(_start, (ThreadRoleExprNode) _kids.get(0));
    }
  };

}
