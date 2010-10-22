package com.surelogic.analysis;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.EnumConstantClassDeclaration;
import edu.cmu.cs.fluid.java.operator.EnumDeclaration;
import edu.cmu.cs.fluid.java.operator.InterfaceDeclaration;
import edu.cmu.cs.fluid.java.operator.VoidTreeWalkVisitor;

public final class TopLevelAnalysisVisitor extends VoidTreeWalkVisitor {
  public static interface ClassProcessor {
    /**
     * Visit a class declaration.
     * 
     * @param currentQuery
     *          The current query, as applicable for the current class.
     *          This is usually null, unless the class is an anonymous class
     *          or enum constant class declaration.
     * @param classDecl A node that satisfies
     * <code>AnonClassExpression.prototype.includes(classDecl) ||
     *       ClassDeclaration.prototype.includes(classDecl) ||
     *       EnumConstantClassDeclaration.prototype.includes(classDecl) ||
     *       EnumDeclaration.prototype.includes(classDecl) ||
     *       InterfaceDeclaration.prototype.includes(classDecl)</code>
     * @param classBody The class body node of <code>classDecl</code>
     */
    public void visitClass(IRNode classDecl, IRNode classBody);
  }
  
  
  
  private final ClassProcessor classProcessor;
  
  

  public TopLevelAnalysisVisitor(final ClassProcessor cp) {
    classProcessor = cp;
  }
  

  
  @Override
  public Void visitAnonClassExpression(final IRNode node) {
    classProcessor.visitClass(node, AnonClassExpression.getBody(node));
    doAcceptForChildren(node);
    return null;
  }

  @Override
  public Void visitClassDeclaration(final IRNode node) {
    classProcessor.visitClass(node, ClassDeclaration.getBody(node));
    doAcceptForChildren(node);
    return null;
  }

  @Override
  public Void visitEnumConstantClassDeclaration(final IRNode node) {
    classProcessor.visitClass(node, EnumConstantClassDeclaration.getBody(node));
    doAcceptForChildren(node);
    return null;
  }
  
  @Override
  public Void visitEnumDeclaration(final IRNode node) {
    classProcessor.visitClass(node, EnumDeclaration.getBody(node));
    doAcceptForChildren(node);
    return null;
  }

  @Override
  public Void visitInterfaceDeclaration(final IRNode node) {
    classProcessor.visitClass(node, InterfaceDeclaration.getBody(node));
    doAcceptForChildren(node);
    return null;
  }
}
