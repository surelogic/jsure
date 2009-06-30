/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/ColorOrElem.java,v 1.2 2007/10/24 15:18:09 dfsuther Exp $*/
package com.surelogic.aast.promise;

import com.surelogic.analysis.colors.CExpr;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Implementors of this interface are possible children of a ColorOrNode. 
 * The total list of implementors should be:
 *     ColorNameNode, ColorNotNode, ColorAnd
 * @author dfsuther
 */
public interface ColorOrElem extends IAASTSubNode {
  public CExpr buildCExpr(IRNode where);

}
