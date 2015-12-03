package com.surelogic.analysis.visitors;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * <p>
 * This class is intended to be subclassed.
 */
public abstract class FlowUnitFinder extends JavaSemanticsVisitor {
  public interface Callback {
    public void foundClassInitializer(IRNode classInit);
    public void foundConstructorDeclaration(IRNode cdecl);
    public void foundMethodDeclaration(IRNode mdecl);
  }
  
  
  
  private final Callback callback;
  
  
  
  protected FlowUnitFinder(final boolean skipA) {
    /* Want to go into nested classes to find the methods, etc. of them.  But
     * until today (2015-12-02) the methods handleClassInitDeclaration(), etc.,
     * below were wrong.  I forgot to continue the visitation by calling 
     * the super implementation!
     */
    super(true, skipA);
    callback = createCallback();
  }
  
  
  
  protected abstract Callback createCallback();



  // ======================================================================
  // == Forward entries to flow units to the helper methods
  // ======================================================================
  
  @Override
  protected final void handleClassInitDeclaration(
      final IRNode classBody, final IRNode classInit) {
    callback.foundClassInitializer(classInit);
    super.handleClassInitDeclaration(classBody, classInit);
  }
  
  @Override
  protected final void handleConstructorDeclaration(final IRNode cdecl) {
    callback.foundConstructorDeclaration(cdecl);
    super.handleConstructorDeclaration(cdecl);
  }
    
  @Override
  protected final void handleMethodDeclaration(final IRNode mdecl) {
    callback.foundMethodDeclaration(mdecl);
    super.handleMethodDeclaration(mdecl);
  }
}
