/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/ColorExprElem.java,v 1.2 2007/10/24 15:18:09 dfsuther Exp $*/
package com.surelogic.aast.promise;

import com.surelogic.analysis.threadroles.TRExpr;

import edu.cmu.cs.fluid.ir.IRNode;

public interface ThreadRoleExprElem extends IAASTSubNode {
  
  public TRExpr buildTRExpr(IRNode where);

}
