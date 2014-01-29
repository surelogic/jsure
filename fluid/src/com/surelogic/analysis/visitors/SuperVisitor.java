package com.surelogic.analysis.visitors;

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
  protected final void handleClassInitDeclaration(
      final IRNode classBody, final IRNode classInit) {
    for (final SubVisitor<?> sv : subVisitors) {
      sv.visitClassInitDeclaration(classInit);
    }
  }
  
  @Override
  protected final void handleConstructorDeclaration(final IRNode cdecl) {
    for (final SubVisitor<?> sv : subVisitors) {
      sv.visitConstructorDeclaration(cdecl);
    }
  }
    
  @Override
  protected final void handleMethodDeclaration(final IRNode mdecl) {
    for (final SubVisitor<?> sv : subVisitors) {
      sv.visitMethodDeclaration(mdecl);
    }
  }



  // ======================================================================
  // == The SubVisitor class
  // ======================================================================

  /*
   * The main purpose of this class is to make sure that all sub visitors 
   * are analysis drivers, and that they are never set to enter into nested
   * types.
   * 
   * NOTE: The enclosingType field of the sub visitor instances will always
   * be null.
   */
  public abstract static class SubVisitor<Q> extends AbstractJavaAnalysisDriver<Q> {
    protected SubVisitor(final boolean skipA) {
      super(false, skipA);
    }
  }
}
