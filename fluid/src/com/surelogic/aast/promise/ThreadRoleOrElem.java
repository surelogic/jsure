/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/ColorOrElem.java,v 1.2 2007/10/24 15:18:09 dfsuther Exp $*/
package com.surelogic.aast.promise;

import com.surelogic.analysis.threadroles.TRExpr;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Implementors of this interface are possible children of a ThreadRoleOrNode. 
 * The total list of implementors should be:
 *     ThreadRoleNameNode, ThreadRoleNotNode, ThreadRoleAnd
 * @author dfsuther
 */
public interface ThreadRoleOrElem extends IAASTSubNode {
  public TRExpr buildTRExpr(IRNode where);

}
