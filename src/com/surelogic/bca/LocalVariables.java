package com.surelogic.bca;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;

import com.surelogic.analysis.IAnalysisMonitor;
import com.surelogic.analysis.JavaSemanticsVisitor;
import com.surelogic.analysis.LocalVariableDeclarations;

import edu.cmu.cs.fluid.analysis.util.AbstractWholeIRAnalysisModule;
import edu.cmu.cs.fluid.dc.IAnalysis;
import edu.cmu.cs.fluid.eclipse.Eclipse;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.sea.Category;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.IRReferenceDrop;
import edu.cmu.cs.fluid.sea.InfoDrop;

public final class LocalVariables extends AbstractWholeIRAnalysisModule {
  private static final Category LV_CATEGORY =
    Category.getInstance("LVCategory");

	static private class ResultsDepDrop extends Drop {
		// Marker class
	}

	private Drop resultDependUpon = null;

	private static LocalVariables INSTANCE;

	@SuppressWarnings("unused")
  private IBinder binder = null;

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
	public LocalVariables() {
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
	  final LV_Visitor v = new LV_Visitor();
	  v.doAccept(compUnit);
	}
	
	
	
	// TODO: Can probably be made an extension of AbstractJavaAnalysisDriver
	private final class LV_Visitor extends JavaSemanticsVisitor {
	  public LV_Visitor() {
	    super(true);
	  }
	  
	  
	  
    private void reportLocalVariables(final IRNode mdecl) {
      final LocalVariableDeclarations lvd = LocalVariableDeclarations.getDeclarationsFor(mdecl);
      final InfoDrop drop = new InfoDrop();
      setLockResultDep(drop, mdecl);
      drop.setCategory(LV_CATEGORY);
      drop.setMessage("{0}: Local {1}; External {2}", 
          JavaNames.genQualifiedMethodConstructorName(mdecl), 
          listToString(lvd.getLocal()), listToString(lvd.getExternal()));
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
    protected void enteringEnclosingDecl(
        final IRNode mdecl, final IRNode anonClassDecl) {
      System.out.println("--- Entering method/constructor " + JavaNames.genMethodConstructorName(mdecl));
    }

    
    @Override 
    protected void leavingEnclosingDecl(final IRNode mdecl) {
      System.out.println("--- Leaving method/constructor " + JavaNames.genMethodConstructorName(mdecl));
    }

    /* Need to override this to return NULL_ACTION so that we process the 
     * field inits and instance init of anon class expressions in expression
     * statements.  Also, we need to compute the local variables for the 
     * anonymous class initializer.
     */
    @Override
    protected InstanceInitAction getAnonClassInitAction(final IRNode expr) {
      return new InstanceInitAction() {
        public void tryBefore() {
          reportLocalVariables(JavaPromise.getInitMethod(expr));
        }
        
        public void finallyAfter() {
          // do nothing
        }
        
        public void afterVisit() {
          // do nothing
        }
      };
    }

    @Override
	  protected void handleConstructorDeclaration(final IRNode cdecl) {
	    reportLocalVariables(cdecl);
      super.handleConstructorDeclaration(cdecl);
	  }

    @Override
    protected void handleMethodDeclaration(final IRNode mdecl) {
      reportLocalVariables(mdecl);
      super.handleMethodDeclaration(mdecl);
    }
    
    @Override
    protected void handleNonAnnotationTypeDeclaration(final IRNode tdecl) {
      final IRNode clinit = JavaPromise.getClassInitOrNull(tdecl);
      if (clinit != null) {
        reportLocalVariables(clinit);
      }
      super.handleNonAnnotationTypeDeclaration(tdecl);
    }
	}
	
	
	private static String listToString(final List<IRNode> list) {
	  final StringBuilder sb = new StringBuilder();
	  sb.append('[');
	  final Iterator<IRNode> i = list.iterator();
	  while (i.hasNext()) {
	    sb.append(DebugUnparser.toString(i.next()));
	    if (i.hasNext()) sb.append(", ");
	  }
	  sb.append(']');
	  return sb.toString();
	}
}