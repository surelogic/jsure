package com.surelogic.analysis;

import com.surelogic.aast.IAASTRootNode;

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
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.ResultDrop;
import edu.cmu.cs.fluid.sea.ResultFolderDrop;
import edu.cmu.cs.fluid.tree.Operator;

public abstract class TypeImplementationProcessor<P extends PromiseDrop<? extends IAASTRootNode>> {
  protected final AbstractWholeIRAnalysis<? extends IBinderClient, ?> analysis;
  protected final IBinder binder;
  protected final P promiseDrop;
  protected final IRNode typeDecl;
  protected final IRNode typeBody;

  protected TypeImplementationProcessor(final AbstractWholeIRAnalysis<? extends IBinderClient, ?> a, final P pd, final IRNode td,
      final IRNode tb) {
    analysis = a;
    binder = a.getBinder();
    promiseDrop = pd;
    typeDecl = td;
    typeBody = tb;
  }

  protected TypeImplementationProcessor(final AbstractWholeIRAnalysis<? extends IBinderClient, ?> a, final P pd, final IRNode td) {
    this(a, pd, td, VisitUtil.getClassBody(td));
  }

  protected final ResultFolderDrop createResultFolder(final IRNode node) {
    final ResultFolderDrop folder = new ResultFolderDrop(node);
    folder.addCheckedPromise(promiseDrop);
    return folder;
  }

  protected final ResultFolderDrop createSubFolder(
      final ResultFolderDrop parent, final IRNode node) {
    final ResultFolderDrop folder = new ResultFolderDrop(node);
    parent.add(folder);
    return folder;
  }

  protected final ResultDrop createResult(final IRNode node,
      final boolean isConsistent, final int msg, final Object... args) {
    final ResultDrop result = createResultSimple(node, isConsistent, msg, args);
    result.addCheckedPromise(promiseDrop);
    return result;
  }

  protected final ResultDrop createResultInFolder(
      final ResultFolderDrop folder, final IRNode node,
      final boolean isConsistent, final int msg, final Object... args) {
    final ResultDrop result = createResultSimple(node, isConsistent, msg, args);
    folder.add(result);
    return result;
  }
  
  private final ResultDrop createResultSimple(final IRNode node,
      final boolean isConsistent, final int msg, final Object... args) {
    final ResultDrop result = new ResultDrop(node);
    result.setConsistent(isConsistent);
    result.setMessage(msg, args);
    return result;
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
        processSuperType(name, analysis.getBinder().getBinding(name));
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
