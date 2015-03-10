/*$Header: /cvs/fluid/fluid/src/com/surelogic/sea/drops/BooleanPromiseDrop.java,v 1.2 2007/06/27 14:37:40 chance Exp $*/
package com.surelogic.dropsea.ir.drops;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;


public class BooleanPromiseDrop<A extends IAASTRootNode> extends PromiseDrop<A> {
  public BooleanPromiseDrop(A a) {
    super(a);
  }
  
  public static IRNode computeAlternateDeclForUnparse(final IRNode node) {
	  final Operator op = JJNode.tree.getOperator(node);
	  // Not VariableDeclaration, since it includes ParameterDeclaration
	  if (VariableDeclarator.prototype.includes(op) || EnumConstantDeclaration.prototype.includes(op)) {
		  return null;
	  }
	  IRNode rv = VisitUtil.getEnclosingClassBodyDecl(node);
	  if (rv == null) {
		  return node;
	  }
	  return rv;
  }
}
