/*
 * Created on Mar 9, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.analysis.colors;

import com.surelogic.sea.drops.colors.ColorCtxSummaryDrop;

import SableJBDD.bdd.JBDD;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.Visitor;

/**
 * @author dfsuther
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class DataColoring {

  /**
   * @author dfsuther
   * 
   */
  static class DataColoringVisitor extends Visitor<Void> {

    JBDD currCTX = null;

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visit(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visit(IRNode node) {
      // visit all children by default.
      doAcceptForChildren(node);
      return null;
    }

    
    
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.IVisitor#visitBlockStatement(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitBlockStatement(IRNode node) {
      final JBDD saveCurrCTX = currCTX;
      
      try {
        ColorCtxSummaryDrop currCtxSumm = ColorCtxSummaryDrop.getSummaryFor(node);
        JBDD localCTX = currCtxSumm.getFullExpr();
        if (localCTX != null) {
          currCTX = localCTX;
        }
        
        super.visitBlockStatement(node);
      } finally {
        currCTX = saveCurrCTX;
      }
      return null;
    }
    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.cs.fluid.java.operator.IVisitor#visitConstructorDeclaration(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitConstructorDeclaration(IRNode node) {
      final JBDD saveCurrCTX = currCTX;

      try {
        super.visitConstructorDeclaration(node);
      } finally {
        currCTX = saveCurrCTX;
      }
      return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.cs.fluid.java.operator.IVisitor#visitMethodDeclaration(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitMethodDeclaration(IRNode node) {
      final JBDD saveCurrCTX = currCTX;

      try {
        super.visitMethodDeclaration(node);
      } finally {
        currCTX = saveCurrCTX;
      }
      return null;
    }
  }

  private static DataColoring INSTANCE = new DataColoring();

  public static DataColoring getInstance() {
    return INSTANCE;
  }

  public void doDataColoringforOneCU(IRNode CU) {

  }

}