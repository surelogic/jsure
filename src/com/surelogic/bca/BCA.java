package com.surelogic.bca;

import org.eclipse.core.resources.IProject;

import com.surelogic.analysis.AbstractJavaAnalysisDriver;
import com.surelogic.analysis.IAnalysisMonitor;
import com.surelogic.analysis.bca.uwm.BindingContext;
import com.surelogic.analysis.bca.uwm.BindingContextAnalysis;
import com.surelogic.analysis.bca.uwm.BindingContextAnalysis.Query;

import edu.cmu.cs.fluid.analysis.util.AbstractWholeIRAnalysisModule;
import edu.cmu.cs.fluid.dc.IAnalysis;
import edu.cmu.cs.fluid.eclipse.Eclipse;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.sea.Category;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.IRReferenceDrop;
import edu.cmu.cs.fluid.sea.InfoDrop;
import edu.cmu.cs.fluid.util.ImmutableSet;

public final class BCA extends AbstractWholeIRAnalysisModule {
  private static final Category BCA_CATEGORY =
    Category.getInstance("BCACategory");

	static private class ResultsDepDrop extends Drop {
		// Marker class
	}

	private Drop resultDependUpon = null;

	private static BCA INSTANCE;

	private IBinder binder = null;
	private BindingContextAnalysis bca = null;

	/**
	 * Provides a reference to the sole object of this class.
	 * 
	 * @return a reference to the only object of this class
	 */
	public static IAnalysis getInstance() {
		return INSTANCE;
	}

	/**
	 * Public constructor that will be called by Eclipse when this analysis
	 * module is created.
	 */
	public BCA() {
		super(ParserNeed.EITHER);
		INSTANCE = this;
	}

	/**
	 * @see edu.cmu.cs.fluid.dc.IAnalysis#analyzeBegin(org.eclipse.core.resources.IProject)
	 */
	@Override
	public void analyzeBegin(IProject project) {
		super.analyzeBegin(project);

		if (resultDependUpon != null) {
			resultDependUpon.invalidate();
			resultDependUpon = new ResultsDepDrop();
		} else {
			resultDependUpon = new ResultsDepDrop();
		}
	}

	private void setLockResultDep(IRReferenceDrop drop, IRNode node) {
		drop.setNode(node);
		if (resultDependUpon != null && resultDependUpon.isValid()) {
			resultDependUpon.addDependent(drop);
		}
	}

	@Override
	protected void constructIRAnalysis() {
		// FIX temporary -- should be in super class
		runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
			public void run() {
				binder = Eclipse.getDefault().getTypeEnv(getProject()).getBinder();
				bca = new BindingContextAnalysis(binder, true);
			}
		});
	}

	@Override
	protected boolean doAnalysisOnAFile(final IRNode compUnit, IAnalysisMonitor monitor) {
		runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
			public void run() {
				runOverFile(compUnit);
			}
		});
		return true;
	}

	protected void runOverFile(final IRNode compUnit) {
	  final BCAVisitor v = new BCAVisitor();
	  v.doAccept(compUnit);
	  bca.clear();
//	  JavaComponentFactory.clearCache();
	}
	
	private final class BCAVisitor extends AbstractJavaAnalysisDriver<BindingContextAnalysis.Query> {
    @Override
    protected Query createNewQuery(final IRNode decl) {
      return bca.getExpressionObjectsQuery(decl);
    }

    @Override
    protected Query createSubQuery(final IRNode caller) {
      return currentQuery().getSubAnalysisQuery(caller);
    }
    
    

    @Override
    protected void enteringEnclosingType(final IRNode newType) {
      System.out.println(">>> Entering type " + JavaNames.getTypeName(newType));
    }
    
    @Override
    protected void leavingEnclosingType(final IRNode newType) {
      System.out.println("<<< Leaving type " + JavaNames.getTypeName(newType));
    }
    
	  @Override
	  protected void enteringEnclosingDeclPrefix(
	      final IRNode newDecl, final IRNode anonClassDecl) {
	    final String name = JavaNames.genQualifiedMethodConstructorName(newDecl);
      System.out.println("Running BCA on " + name);
	  }
	  
    
    
	  @Override
	  public Void visitVariableUseExpression(final IRNode use) {
	    // See if the current variable is a primitive or not
	    final IJavaType type = binder.getJavaType(use);
	    if (type instanceof IJavaReferenceType) {
	      final ImmutableSet<IRNode> bindings = currentQuery().getResultFor(use);
        final InfoDrop drop = new InfoDrop();
        setLockResultDep(drop, use);
        drop.setCategory(BCA_CATEGORY);
        final String varName = VariableUseExpression.getId(use);
        drop.setMessage("{0} binds to {1}", varName, BindingContext.setToString(bindings));
	    }
	    
	    return null;
	  }
	}
}