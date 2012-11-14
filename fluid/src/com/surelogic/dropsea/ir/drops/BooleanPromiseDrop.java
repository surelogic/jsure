/*$Header: /cvs/fluid/fluid/src/com/surelogic/sea/drops/BooleanPromiseDrop.java,v 1.2 2007/06/27 14:37:40 chance Exp $*/
package com.surelogic.dropsea.ir.drops;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.VisitUtil;


public class BooleanPromiseDrop<A extends IAASTRootNode> extends PromiseDrop<A> {
  public BooleanPromiseDrop(A a) {
    super(a);
  }
  
  public static IRNode computeAlternateDeclForUnparse(final IRNode node) {
	  if (VariableDeclarator.prototype.includes(node)) {
		  return null;
	  }
	  IRNode rv = VisitUtil.getEnclosingClassBodyDecl(node);
	  if (rv == null) {
		  return node;
	  }
	  return rv;
  }
}
