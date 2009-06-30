/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/ColorExprElem.java,v 1.2 2007/10/24 15:18:09 dfsuther Exp $*/
package com.surelogic.aast.promise;

import com.surelogic.analysis.colors.CExpr;

import edu.cmu.cs.fluid.ir.IRNode;

public interface ColorExprElem extends IAASTSubNode {
  
  public CExpr buildCExpr(IRNode where);

}
