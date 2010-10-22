package com.surelogic.analysis.testing;

import java.text.MessageFormat;

import com.surelogic.analysis.AbstractJavaAnalysisDriver;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IAnalysisMonitor;
import com.surelogic.analysis.TopLevelAnalysisVisitor;
import com.surelogic.analysis.TopLevelAnalysisVisitor.ClassProcessor;
import com.surelogic.analysis.testing.CollectMethodCalls.Query;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.sea.Category;
import edu.cmu.cs.fluid.sea.InfoDrop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.util.ImmutableSet;

public class CollectMethodCallsModule extends AbstractWholeIRAnalysis<CollectMethodCalls, Void> {
	private static final Category CM_CATEGORY = Category.getInstance("CMCategory");
	
	
	
	public CollectMethodCallsModule() {
		super("CMCategory");
	}

	@Override
	protected CollectMethodCalls constructIRAnalysis(IBinder binder) {
		final CollectMethodCalls collectMethodCalls = new CollectMethodCalls(binder);
    return collectMethodCalls;
	}

	@Override
	protected boolean doAnalysisOnAFile(CUDrop cud, final IRNode compUnit, IAnalysisMonitor monitor) {
		runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
			public void run() {
				runOverFile(compUnit);
			}
		});
		return true;
	}

	protected void runOverFile(final IRNode compUnit) {
	  new TopLevelAnalysisVisitor(
	      new ClassProcessor() {
          public void visitClass(final IRNode classDecl, final IRNode classBody) {
            new CM_Visitor(classDecl).doAccept(classBody);
          }
        }).doAccept(compUnit);
	}	
	
	@Override
	protected void clearCaches() {
		// Nothing to do
	}
	
	
	
  private final class CM_Visitor extends AbstractJavaAnalysisDriver<CollectMethodCalls.Query> {
    public CM_Visitor(final IRNode typeDecl) {
      super(typeDecl, false);
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
        final InfoDrop drop = new InfoDrop();
        setResultDependUponDrop(drop, decl);
        drop.setCategory(CM_CATEGORY);
        final ISrcRef srcRef = JavaNode.getSrcRef(call);
        final int srcLine = srcRef == null ? -1 : srcRef.getLineNumber();
        final String callString = DebugUnparser.toString(call);
        drop.setMessage(MessageFormat.format("Calls {0} from line {1}", callString, srcLine));
      }
    }
  }
}
