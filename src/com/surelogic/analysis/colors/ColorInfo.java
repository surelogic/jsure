/*
 * Created on Nov 30, 2004
 */
package com.surelogic.analysis.colors;


import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.ConstructorCall;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.NewExpression;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.colors.ColorReqSummaryDrop;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.tree.SyntaxTreeInterface;

/**
 * @author Edwin
 */
public class ColorInfo {

  private static String getColorReqStringForDecl(IRNode n) {
    final ColorReqSummaryDrop reqSumm = ColorReqSummaryDrop.getSummaryFor(n);

    final String reqString = reqSumm.getReqString();
    return reqString;
  }
  
  private static IRNode getBinding(IRNode n) {
    return ColorSecondPass.getInstance().getBinder().getBinding(n);
  }

  public static String GetColorInfo(IRNode n) {
    SyntaxTreeInterface tree = JJNode.tree;
    if (!tree.isNode(n)) {
      return null;
    }
    final Operator op = tree.getOperator(n);
    if (MethodDeclaration.prototype.includes(op)
        || ConstructorDeclaration.prototype.includes(op)) { 
      return getColorReqStringForDecl(n);
    } else if (MethodCall.prototype.includes(op) || 
        ConstructorCall.prototype.includes(op) ||
        NewExpression.prototype.includes(op)) {
      final IRNode mDecl = getBinding(n);
      return getColorReqStringForDecl(mDecl);
    }
    return null;
  }

  public static String GetColorContextInfo(IRNode n) {
    // TODO Auto-generated method stub
    return null;
  }

}