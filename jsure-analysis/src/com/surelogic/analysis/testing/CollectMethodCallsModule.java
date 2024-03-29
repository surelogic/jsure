package com.surelogic.analysis.testing;

import com.surelogic.analysis.*;
import com.surelogic.analysis.testing.CollectMethodCalls.Query;
import com.surelogic.analysis.visitors.AbstractJavaAnalysisDriver;
import com.surelogic.analysis.visitors.TopLevelAnalysisVisitor;
import com.surelogic.analysis.visitors.TopLevelAnalysisVisitor.SimpleClassProcessor;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.util.ImmutableSet;

public final class CollectMethodCallsModule extends AbstractWholeIRAnalysis<CollectMethodCalls, CUDrop> {
	public CollectMethodCallsModule() {
		super("CMCategory");
	}

	@Override
	protected CollectMethodCalls constructIRAnalysis(IBinder binder) {
		final CollectMethodCalls collectMethodCalls = new CollectMethodCalls(binder);
    return collectMethodCalls;
	}

	@Override
	protected boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud, final IRNode compUnit) {
		runOverFile(compUnit);
		return true;
	}

	protected void runOverFile(final IRNode compUnit) {
	  TopLevelAnalysisVisitor.processCompilationUnit(
	      new SimpleClassProcessor() {
	        @Override
          public void visitTypeDecl(final IRNode classDecl, final IRNode classBody) {
            new CM_Visitor(classDecl).doAccept(classBody);
          }
        },
        compUnit);
	}	
	
	@Override
	protected void clearCaches() {
		// Nothing to do
	}
	
	
	
  private final class CM_Visitor extends AbstractJavaAnalysisDriver<CollectMethodCalls.Query> {
    public CM_Visitor(final IRNode typeDecl) {
      super(typeDecl, VisitInsideTypes.NO, SkipAnnotations.YES, CreateTransformer.NO);
    }
    
    
    
    @Override
    protected Query createNewQuery(final IRNode decl) {
      return getAnalysis().getQuery(decl);
    }

    @Override
    protected Query createSubQuery(final IRNode caller) {
      return currentQuery().getSubAnalysisQuery(caller);
    }
    
    @Override
    public Void visitMethodBody(final IRNode body) {
      getResult(body);
      doAcceptForChildren(body);
      return null;
    }
    
    @Override
    protected void handleClassInitDeclaration(
        final IRNode classBody, final IRNode classInit) {
      getResult(classBody);
    }   
    
    private void getResult(final IRNode decl) {
      final ImmutableSet<IRNode> calls = currentQuery().getResultFor(decl);
      for (final IRNode call : calls) {      
        final HintDrop drop = HintDrop.newInformation(decl);
        drop.setCategorizingMessage(Messages.DSC_COLLECT_METHOD_CALLS);
        final IJavaRef javaRef = JavaNode.getJavaRef(call);
        final int srcLine = javaRef == null ? -1 : javaRef.getLineNumber();
        drop.setMessage(Messages.CALLS, DebugUnparser.toString(call), srcLine);
      }
    }
  }
}
