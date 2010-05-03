/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/ColorConstraintNode.java,v 1.2 2007/10/27 17:11:10 dfsuther Exp $*/
package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class ColorConstraintNode extends ColorExprPromiseNode {
  public ColorConstraintNode(int offset, ColorExprNode n) {
    super(offset, n, "ColorConstraint");
  }

  public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
      "ColorConstraint") {
    @Override
    public AASTNode create(String _token, int _start, int _stop, int _mods,
                           String _id, int _dims, List<AASTNode> _kids) {
      return new ColorConstraintNode(_start, (ColorExprNode) _kids.get(0));
    }
  };
}
