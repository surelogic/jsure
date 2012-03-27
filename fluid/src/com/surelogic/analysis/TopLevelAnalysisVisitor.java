package com.surelogic.analysis;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.EnumConstantClassDeclaration;
import edu.cmu.cs.fluid.java.operator.EnumDeclaration;
import edu.cmu.cs.fluid.java.operator.InterfaceDeclaration;
import edu.cmu.cs.fluid.java.operator.VoidTreeWalkVisitor;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.util.Pair;

public final class TopLevelAnalysisVisitor extends VoidTreeWalkVisitor {
  // ======================================================================
  // == Nested types
  // ======================================================================

  public static interface ClassProcessor {
    /**
     * Visit an anonymous class declaration.
     * 
     * @param classDecl
     *          A node that satisfies
     *          <code>AnonClassExpression.prototype.includes(classDecl)</code>
     * @param classBody
     *          The class body node of <code>classDecl</code>
     */
    public void visitAnonymousClass(IRNode classDecl, IRNode classBody);

    /**
     * Visit a class declaration.
     * 
     * @param classDecl
     *          A node that satisfies
     *          <code>ClassDeclaration.prototype.includes(classDecl)</code>
     * @param classBody
     *          The class body node of <code>classDecl</code>
     */
    public void visitClass(IRNode classDecl, IRNode classBody);

    /**
     * Visit an enum declaration.
     * 
     * @param classDecl
     *          A node that satisfies
     *          <code>EnumDeclaration.prototype.includes(classDecl)</code>
     * @param classBody
     *          The class body node of <code>classDecl</code>
     */
    public void visitEnum(IRNode classDecl, IRNode classBody);

    /**
     * Visit an enumeration constant class declaration.
     * 
     * @param classDecl
     *          A node that satisfies
     *          <code>EnumConstantClassDeclaration.prototype.includes(classDecl))</code>
     * @param classBody
     *          The class body node of <code>classDecl</code>
     */
    public void visitEnumConstantClass(IRNode classDecl, IRNode classBody);

    /**
     * Visit an interface declaration.
     * 
     * @param classDecl
     *          A node that satisfies
     *          <code>InterfaceDeclaration.prototype.includes(classDecl)</code>
     * @param classBody
     *          The class body node of <code>classDecl</code>
     */
    public void visitInterface(IRNode classDecl, IRNode classBody);
  }
  
  // ----------------------------------------------------------------------
  
  public static abstract class SimpleClassProcessor implements ClassProcessor {
    protected abstract void visitTypeDecl(IRNode typeDecl, IRNode classBody);
    
    

    public final void visitAnonymousClass(final IRNode classDecl, final IRNode classBody) {
      visitTypeDecl(classDecl, classBody);
    }

    public final void visitClass(final IRNode classDecl, final IRNode classBody) {
      visitTypeDecl(classDecl, classBody);
    }

    public final void visitEnum(final IRNode classDecl, final IRNode classBody) {
      visitTypeDecl(classDecl, classBody);
    }

    public final void visitEnumConstantClass(final IRNode classDecl, final IRNode classBody) {
      visitTypeDecl(classDecl, classBody);
    }

    public final void visitInterface(final IRNode classDecl, final IRNode classBody) {
      visitTypeDecl(classDecl, classBody);
    }
  }
  
  // ----------------------------------------------------------------------

  public final static class TypeBodyPair extends Pair<IRNode, IRNode> implements ICompUnitContext {
    public TypeBodyPair(final IRNode td, final IRNode cb) {
      super(td, cb);
    }
    
    public IRNode typeDecl() { return first(); }
    public IRNode classBody() { return second(); }

	public IRNode getCompUnit() {
		return VisitUtil.getEnclosingCompilationUnit(first());
	}
  }

  
  
  // ======================================================================
  // == Fields
  // ======================================================================
  
  private final ClassProcessor classProcessor;
  
  
  
  // ======================================================================
  // == Constructor
  // ======================================================================
  
  private TopLevelAnalysisVisitor(final ClassProcessor cp) {
    classProcessor = cp;
  }
  
  
  
  // ======================================================================
  // == Entry point
  // ======================================================================

  public static void processCompilationUnit(
      final ClassProcessor cp, final IRNode compUnit) {
    new TopLevelAnalysisVisitor(cp).doAccept(compUnit);
  }
  
  
  
  // ======================================================================
  // == Overridden visitor methods
  // ======================================================================
  
  @Override
  public Void visitAnonClassExpression(final IRNode node) {
    classProcessor.visitAnonymousClass(node, AnonClassExpression.getBody(node));
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
    classProcessor.visitEnumConstantClass(node, EnumConstantClassDeclaration.getBody(node));
    doAcceptForChildren(node);
    return null;
  }
  
  @Override
  public Void visitEnumDeclaration(final IRNode node) {
    classProcessor.visitEnum(node, EnumDeclaration.getBody(node));
    doAcceptForChildren(node);
    return null;
  }

  @Override
  public Void visitInterfaceDeclaration(final IRNode node) {
    classProcessor.visitInterface(node, InterfaceDeclaration.getBody(node));
    doAcceptForChildren(node);
    return null;
  }
}
