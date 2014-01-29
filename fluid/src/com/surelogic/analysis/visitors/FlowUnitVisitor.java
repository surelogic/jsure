package com.surelogic.analysis.visitors;

/**
 * A Subclass of {@link AbstractJavaAnalysisDriver} that is meant to be
 * called only by the {@link #visitClassInitDeclaration(edu.cmu.cs.fluid.ir.IRNode)},
 * {@link #visitConstructorDeclaration(edu.cmu.cs.fluid.ir.IRNode)}, and
 * {@link #visitMethodDeclaration(edu.cmu.cs.fluid.ir.IRNode)} methods.
 * The main purpose of this class is to make sure that all sub visitors 
 * are analysis drivers, and that they are never set to enter into nested
 * types.
 * 
 * <p><b>Note:</b> The {@link #getEnclosingType() enclosing type} will always
 * be <code>null</code>.
 */
public abstract class FlowUnitVisitor<Q> extends AbstractJavaAnalysisDriver<Q> {
  protected FlowUnitVisitor(final boolean skipA) {
    super(false, skipA);
  }
}
