package com.surelogic.analysis.visitors;

import java.util.List;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * <p>
 * This class is intended to be subclassed.
 */
public abstract class SuperVisitor extends FlowUnitFinder {
  private List<FlowUnitVisitor<?>> subVisitors;
  
  
  
  protected SuperVisitor(final boolean skipA) {
    super(skipA);
    subVisitors = createSubVisitors();
  }
  
  
  
  protected abstract List<FlowUnitVisitor<?>> createSubVisitors();



  // ======================================================================
  // == Forward entries to flow units to the sub visitors 
  // ======================================================================
  
  @Override
  protected final Callback createCallback() {
    return new Callback() {
      @Override
      public void foundClassInitializer(final IRNode classInit) {
        for (final FlowUnitVisitor<?> sv : subVisitors) {
          sv.visitClassInitDeclaration(classInit);
        }
      }

      @Override
      public void foundConstructorDeclaration(final IRNode cdecl) {
        for (final FlowUnitVisitor<?> sv : subVisitors) {
          sv.visitConstructorDeclaration(cdecl);
        }
      }

      @Override
      public void foundMethodDeclaration(final IRNode mdecl) {
        for (final FlowUnitVisitor<?> sv : subVisitors) {
          sv.visitMethodDeclaration(mdecl);
        }
      }
    };
  }
}
