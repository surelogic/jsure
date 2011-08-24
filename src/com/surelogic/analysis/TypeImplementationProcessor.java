package com.surelogic.analysis;

import com.surelogic.aast.IAASTRootNode;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.operator.ClassBody;
import edu.cmu.cs.fluid.java.operator.ClassInitializer;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.EnumConstantDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarators;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.proxy.ResultDropBuilder;
import edu.cmu.cs.fluid.tree.Operator;

public abstract class TypeImplementationProcessor {
  protected final AbstractWholeIRAnalysis<? extends IBinderClient, ?> analysis;
  protected final PromiseDrop<? extends IAASTRootNode> promiseDrop;
  protected final IRNode typeDecl;
  protected final IRNode typeBody;
  
  
  protected TypeImplementationProcessor(
      final AbstractWholeIRAnalysis<? extends IBinderClient, ?> a,
      final PromiseDrop<? extends IAASTRootNode> pd,
      final IRNode td, final IRNode tb) {
    analysis = a;
    promiseDrop = pd;
    typeDecl = td;
    typeBody = tb;
  }
  
  protected TypeImplementationProcessor(
      final AbstractWholeIRAnalysis<? extends IBinderClient, ?> a,
      final PromiseDrop<? extends IAASTRootNode> pd,
      final IRNode td) {
    this(a, pd, td, TypeUtil.getTypeBody(td));
  }
  
  
  
  protected final ResultDropBuilder createResult(
      final IRNode node, final boolean isConsistent, 
      final int msg, final Object... args) {
    final ResultDropBuilder result =
      ResultDropBuilder.create(analysis, message2string(msg));
    analysis.setResultDependUponDrop(result, node);
    result.addCheckedPromise(promiseDrop);
    result.setConsistent(isConsistent);
    result.setResultMessage(msg, args);
    return result;
  }

  protected abstract String message2string(int msg);
  
  
  
  
  public final void processType() {
    preProcess();
    
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

  protected void processFieldDeclaration(final IRNode decl) {
    // Visit variable declarators by default
    final boolean isStatic = JavaNode.getModifier(decl, JavaNode.STATIC);
    for (final IRNode varDecl : VariableDeclarators.getVarIterator(
        FieldDeclaration.getVars(decl))) {
      processVariableDeclarator(varDecl, isStatic);
    }
  }

  protected void processVariableDeclarator(
      final IRNode varDecl, final boolean isStatic) {
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
