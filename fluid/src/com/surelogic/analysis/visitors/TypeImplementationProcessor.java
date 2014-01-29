package com.surelogic.analysis.visitors;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.ClassBody;
import edu.cmu.cs.fluid.java.operator.ClassInitializer;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.EnumConstantClassDeclaration;
import edu.cmu.cs.fluid.java.operator.EnumConstantDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarators;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public abstract class TypeImplementationProcessor {
  protected final IBinder binder;

  protected final IRNode typeDecl;
  protected final IRNode typeBody;

  protected TypeImplementationProcessor(
      final IBinder b, final IRNode td, final IRNode tb) {
    binder = b;
    typeDecl = td;
    typeBody = tb;
  }

  protected TypeImplementationProcessor(final IBinder b, final IRNode td) {
    this(b, td, VisitUtil.getClassBody(td));
  }

  
  
  public final void processType() {
    preProcess();

    // First process the super type list
    if (EnumConstantClassDeclaration.prototype.includes(typeDecl)) {
      /*
       * VisitUtil.getSupertypeNames() doesn't work
       * EnumConstantClassDeclarations. We want the enumeration that contains
       * the declaration to be the supertype.
       */
      processSuperType(typeDecl, JJNode.tree.getParent(JJNode.tree.getParent(typeDecl)));
    } else {
      for (final IRNode name : VisitUtil.getSupertypeNames(typeDecl)) {
        processSuperType(name, binder.getBinding(name));
      }
    }

    // Then process the body elements
    for (final IRNode decl : ClassBody.getDeclIterator(typeBody)) {
      final Operator op = JJNode.tree.getOperator(decl);
      if (ClassInitializer.prototype.includes(op)) {
        processClassInitializer(decl);
      } else if (ConstructorDeclaration.prototype.includes(op)) {
        processConstructorDeclaration(decl);
      } else if (MethodDeclaration.prototype.includes(op)) {
        processMethodDeclaration(decl);
      } else if (FieldDeclaration.prototype.includes(op)) {
        processFieldDeclaration(decl);
      } else if (EnumConstantDeclaration.prototype.includes(op)) {
        processEnumConstantDeclaration(decl);
      }
    }

    postProcess();
  }

  protected void preProcess() {
    // Do nothing by default
  }

  protected void postProcess() {
    // Do nothing by default
  }

  protected void processSuperType(final IRNode name, final IRNode decl) {
    // Do nothing by default
  }

  protected void processFieldDeclaration(final IRNode decl) {
    // Visit variable declarators by default
    final boolean isStatic = JavaNode.getModifier(decl, JavaNode.STATIC);
    for (final IRNode varDecl : VariableDeclarators.getVarIterator(FieldDeclaration.getVars(decl))) {
      processVariableDeclarator(decl, varDecl, isStatic);
    }
  }

  protected void processVariableDeclarator(final IRNode fieldDecl, final IRNode varDecl, final boolean isStatic) {
    // Do nothing by default
  }

  protected void processMethodDeclaration(final IRNode decl) {
    // Do nothing by default
  }

  protected void processConstructorDeclaration(final IRNode decl) {
    // Do nothing by default
  }

  protected void processClassInitializer(final IRNode decl) {
    // Do nothing by default
  }

  protected void processEnumConstantDeclaration(final IRNode element) {
    // Do nothing by default
  }
}
