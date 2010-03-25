package com.surelogic.nonnull;

import java.util.LinkedList;
import java.util.Set;

import org.eclipse.core.resources.IProject;

import com.surelogic.analysis.IAnalysisMonitor;
import com.surelogic.analysis.JavaSemanticsVisitor;

import edu.cmu.cs.fluid.analysis.util.AbstractWholeIRAnalysisModule;
import edu.cmu.cs.fluid.dc.IAnalysis;
import edu.cmu.cs.fluid.eclipse.Eclipse;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaComponentFactory;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.sea.Category;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.IRReferenceDrop;
import edu.cmu.cs.fluid.sea.InfoDrop;
import edu.uwm.cs.fluid.java.analysis.SimpleNonnullAnalysis;

public final class NonNull extends AbstractWholeIRAnalysisModule {
	private static final Category NONNULL_CATEGORY = Category
			.getInstance("NonNullCategory");

	static private class ResultsDepDrop extends Drop {
		// Marker class
	}

	private Drop resultDependUpon = null;

	private static NonNull INSTANCE;

	private IBinder binder = null;
	private SimpleNonnullAnalysis nonNullAnalysis = null;

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
	public NonNull() {
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
				binder = Eclipse.getDefault().getTypeEnv(getProject())
						.getBinder();
				nonNullAnalysis = new SimpleNonnullAnalysis(binder);
			}
		});
	}

	@Override
	protected boolean doAnalysisOnAFile(final IRNode compUnit, IAnalysisMonitor monitor) {
		runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
			public void run() {
				checkNonNullForFile(compUnit);
			}
		});
		return true;
	}

	protected void checkNonNullForFile(final IRNode compUnit) {
	  final NonNullVisitor v = new NonNullVisitor();
	  v.doAccept(compUnit);
	  nonNullAnalysis.clear();
//	  JavaComponentFactory.clearCache();
	}
	
	private final class NonNullVisitor extends JavaSemanticsVisitor {
	  private SimpleNonnullAnalysis.Query query = null;
	  private final LinkedList<SimpleNonnullAnalysis.Query> oldQueries =
	    new LinkedList<SimpleNonnullAnalysis.Query>();
	  
	  public NonNullVisitor() {
	    super(true);
	  }
	  
	  
	  
	  private void newQuery(final SimpleNonnullAnalysis.Query q) {
	    oldQueries.addFirst(query);
	    query = q;
	  }
	  
	  private void restoreQuery() {
	    query = oldQueries.removeFirst();
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
	  protected void enteringEnclosingDecl(final IRNode newDecl) {
	    System.out.println("############################ Running non null on " + JavaNames.genQualifiedMethodConstructorName(newDecl) + "############################");
	    newQuery(nonNullAnalysis.getNonnullBeforeQuery(newDecl));
	  }
	  
	  @Override
	  protected void leavingEnclosingDecl(final IRNode oldDecl) {
	    restoreQuery();
	  }
	  
    /* Need to override this to return NULL_ACTION so that we process the 
     * field inits and instance init of anon class expressions in expression
     * statements.
     */
    @Override
    protected InstanceInitAction getAnonClassInitAction(final IRNode expr) {
      return NULL_ACTION;
    }

	  @Override
	  protected InstanceInitAction getConstructorCallInitAction(final IRNode ccall) {
	    return new InstanceInitAction() {
        public void tryBefore() {
          newQuery(query.getSubAnalysisQuery(ccall));
        }
        
        public void finallyAfter() {
          restoreQuery();
        }
        
        public void afterVisit() {
          // do nothing
        }
      };
	  }
	  
	  
	  
	  @Override
	  public Void visitVariableUseExpression(final IRNode use) {
	    // See if the current variable is a primitive or not
	    final IJavaType type = binder.getJavaType(use);
	    if (type instanceof IJavaReferenceType) {
         // See if the current variable is considered to be null or not
	      final Set<IRNode> nonNull = query.getResultFor(use);
	      final IRNode varDecl = binder.getBinding(use);
        final InfoDrop drop = new InfoDrop();
        setLockResultDep(drop, use);
        drop.setCategory(NONNULL_CATEGORY);
        final String varName = VariableUseExpression.getId(use);
        if (nonNull.contains(varDecl)) {
          drop.setMessage(varName + " IS NOT null");
        } else {
          drop.setMessage(varName + " may be null");
        }
	    }
	    
	    return null;
	  }
	}
}