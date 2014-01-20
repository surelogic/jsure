package com.surelogic.analysis;

import java.util.List;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * <p>
 * This class is intended to be subclassed.
 */
public abstract class SuperVisitor extends JavaSemanticsVisitor {
  private List<SubVisitor<?>> subVisitors;
  
  
  
  protected SuperVisitor(final boolean skipA) {
    super(true, skipA);
    subVisitors = createSubVisitors();
  }
  
  
  
  protected abstract List<SubVisitor<?>> createSubVisitors();



  // ======================================================================
  // == Forward entries to flow units to the sub visitors 
  // ======================================================================
  
  @Override
  protected final void handleClassInitDeclaration(final IRNode classBody, final IRNode node) {
    for (final SubVisitor<?> sv : subVisitors) {
      sv.handleClassInitDeclaration(classBody, node);
    }
  }
  
  @Override
  protected final void handleConstructorDeclaration(final IRNode cdecl) {
    for (final SubVisitor<?> sv : subVisitors) {
      sv.enterConstructorDeclaration(cdecl);
    }
  }
  
  @Override
  protected final void handleEnumConstantClassDeclaration(final IRNode decl) {
    for (final SubVisitor<?> sv : subVisitors) {
      sv.visitEnumConstantClassDeclaration(decl);
    }
  }
 
  @Override
  protected final void handleFieldDeclaration(final IRNode fdecl) {
    for (final SubVisitor<?> sv : subVisitors) {
      sv.visitFieldDeclaration(fdecl);
    }
  }
  
  @Override
  protected final void handleInstanceInitializer(final IRNode init) {
    for (final SubVisitor<?> sv : subVisitors) {
      sv.handleInstanceInitializer(init);
    }
  }
  
  @Override
  protected final void handleMethodDeclaration(final IRNode mdecl) {
    for (final SubVisitor<?> sv : subVisitors) {
      sv.visitMethodDeclaration(mdecl);
    }
  }
  
  @Override
  protected final void handleNormalEnumConstantDeclaration(final IRNode decl) {
    for (final SubVisitor<?> sv : subVisitors) {
      sv.visitNormalEnumConstantDeclaration(decl);
    }
  }
  
  @Override
  protected final void handleSimpleEnumConstantDeclaration(final IRNode decl) {
    for (final SubVisitor<?> sv : subVisitors) {
      sv.visitSimpleEnumConstantDeclaration(decl);
    }
  }
  
  @Override
  protected final void handleStaticInitializer(final IRNode init) {
    for (final SubVisitor<?> sv : subVisitors) {
      sv.handleStaticInitializer(init);
    }
  }  



  // ======================================================================
  // == Handle enclosing state
  // ======================================================================

  @Override
  protected final void enteringEnclosingDecl(
      final IRNode enteringDecl, final IRNode anonClassDecl) {
    enteringEnclosingDeclPrefix(enteringDecl, anonClassDecl);
    for (final SubVisitor<?> sv : subVisitors) {
      sv.enterEnclosingDecl(enteringDecl, anonClassDecl);
    }    
  }
  
  protected void enteringEnclosingDeclPrefix(
      final IRNode newDecl, final IRNode anonClassDecl) {
    // do nothing
  }
  
  @Override
  protected final void leavingEnclosingDecl(
      final IRNode leavingDecl, final IRNode returningToDecl) {
    for (final SubVisitor<?> sv : subVisitors) {
      sv.leaveEnclosingDecl(returningToDecl);
    }    
    leavingEnclosingDeclPostfix(leavingDecl, returningToDecl);
  }
  
  protected void leavingEnclosingDeclPostfix(
      final IRNode oldDecl, final IRNode returningTo) {
    // do nothing
  }



  // ======================================================================
  // == The SubVisitor class
  // ======================================================================

  public abstract static class SubVisitor<Q> extends AbstractJavaAnalysisDriver<Q> {
    protected SubVisitor(final boolean skipA) {
      super(false, skipA);
    }
    
    
    
    // ======================================================================
    // == Special entry points for SuperVisitor
    // ======================================================================
    
    private void enterConstructorDeclaration(final IRNode cdecl) {
      /* Like JavaSemanticsVisitor.visitConstructorDeclaration() without
       * the calls to {enter, leave}EnclosingDecl().
       */
      // 1. Record that we are inside a constructor
      insideConstructor = true;
      try {
        // 2. Process the constructor declaration
        handleConstructorDeclaration(cdecl);
      } finally {
        // 3. Record we are no longer in a constructor
        insideConstructor = false;
      }
    }
  }
}
