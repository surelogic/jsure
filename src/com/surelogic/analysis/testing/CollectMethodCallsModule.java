package com.surelogic.analysis.testing;

import java.text.MessageFormat;

import com.surelogic.analysis.AbstractJavaAnalysisDriver;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IAnalysisMonitor;
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
		return new CollectMethodCalls(binder);
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
	  final CM_Visitor v = new CM_Visitor();
	  v.doAccept(compUnit);
	}	
	
	@Override
	protected void clearCaches() {
		// Nothing to do
	}
	
	
	
  private final class CM_Visitor extends AbstractJavaAnalysisDriver<CollectMethodCalls.Query> {
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
    public void handleStaticInitializer(final IRNode node) {
      getResult(node);
      doAcceptForChildren(node);
    }
    
//    @Override
//    protected void handleAnonClassExpression(final IRNode node) {
//      getResult(node);
//      super.handleAnonClassExpression(node);
//    }
    
    
    
    private void getResult(final IRNode decl) {
      final ImmutableSet<IRNode> calls = currentQuery().getResultFor(decl);
      for (final IRNode call : calls) {      
        final InfoDrop drop = new InfoDrop();
        setResultDependUponDrop(drop, decl);
        drop.setCategory(CM_CATEGORY);
        final ISrcRef srcRef = JavaNode.getSrcRef(call);
        final int srcLine = srcRef == null ? -1 : srcRef.getLineNumber();
        drop.setMessage(MessageFormat.format("Calls {0} from line {1}", DebugUnparser.toString(call), srcLine));
      }
    }
  }
}
