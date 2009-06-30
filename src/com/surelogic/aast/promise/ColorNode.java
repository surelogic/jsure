/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/ColorNode.java,v 1.1 2007/10/24 15:18:09 dfsuther Exp $*/
package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.AASTNode;
import com.surelogic.parse.AbstractSingleNodeFactory;

public class ColorNode extends ColorExprPromiseNode {
  public ColorNode(int offset, ColorExprNode n) {
    super(offset, n, "Color");
  }

  public static final AbstractSingleNodeFactory factory = new AbstractSingleNodeFactory(
      "Color") {
    @Override
    @SuppressWarnings("unchecked")
    public AASTNode create(String _token, int _start, int _stop, int _mods,
                           String _id, int _dims, List<AASTNode> _kids) {
      return new ColorNode(_start, (ColorExprNode) _kids.get(0));
    }
  };

}
