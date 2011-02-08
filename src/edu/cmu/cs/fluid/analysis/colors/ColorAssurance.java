package edu.cmu.cs.fluid.analysis.colors;

import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.JavaModelException;

import com.surelogic.analysis.IAnalysisMonitor;
import com.surelogic.analysis.threadroles.TRoleSecondPass;
import com.surelogic.analysis.threadroles.TRoleStats;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.jsure.core.Eclipse;
import com.surelogic.jsure.core.QueuingSrcNotifyListener;
import com.surelogic.jsure.core.driver.AbstractFluidAnalysisModule;
import com.surelogic.jsure.core.driver.AbstractWholeIRAnalysisModule;
import com.surelogic.jsure.core.listeners.ClearProjectListener;
import com.surelogic.jsure.core.listeners.IClearProjectHelper;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.util.AbstractRunner;

public final class ColorAssurance
  extends AbstractWholeIRAnalysisModule 
  implements IClearProjectHelper {

  /**
	 * @author dfsuther
	 * 
	 * To change the template for this generated type comment go to Window -
	 * Preferences - Java - Code Generation - Code and Comments
	 */
  /**
	 * Log4j logger for this class
	 */
  private static final Logger LOG = SLLogger.getLogger("ColorAssurance");


  private static int cuCount = 0;

  private static ITypeEnvironment tEnv;

 
  static private class ResultsDepDrop extends Drop {
  }

  private Drop resultDependUpon = null;


  private static ColorAssurance INSTANCE;


  private QueuingSrcNotifyListener listener = new QueuingSrcNotifyListener(); 
  
  /**
	 * Provides a reference to the sole object of this class.
	 * 
	 * @return a reference to the only object of this class
	 */
  public static ColorAssurance getInstance() {
    return INSTANCE;
  }

  public ColorAssurance() {
    INSTANCE = this;
    TRoleStats.getInstance();
    //ConvertToIR.register(listener);
    ClearProjectListener.addHelper(this);
  }

  /**
   * Should be protected.
   */
  public void clearResults(boolean clearAll) {
    if (resultDependUpon != null) {
      resultDependUpon.invalidate();
      resultDependUpon = null;
    }
  }

  /**
	 * @see com.surelogic.jsure.core.driver.IAnalysis#analyzeBegin(org.eclipse.core.resources.IProject)
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
    
    // Setup some fluid analysis stuff (Check that this is correct)
    tEnv = null; // Eclipse.getDefault().getTypeEnv(project);
  }

  /**
	 * @see com.surelogic.jsure.core.driver.IAnalysis#analyzeResource(org.eclipse.core.resources.IResource,
	 *      int)
	 */
  @Override
  public boolean analyzeResource(IResource resource, int kind) {
    constructionOfIRAnalysisNeeded = false;
    return false; // please call my analyzeCompilationUnit()
  }

  /**
   * @see com.surelogic.jsure.core.driver.AbstractIRAnalysisModule#doAnalysisOnAFile(edu.cmu.cs.fluid.ir.IRNode)
   */
  @Override
  protected boolean doAnalysisOnAFile(IRNode cu, IAnalysisMonitor monitor) throws JavaModelException {
    cuCount += 1;
    return false;
  }
  
  @Override
  public boolean needsAST() {
    return false;
  }  

  /**
	 * @see com.surelogic.jsure.core.driver.javaassure.IAnalysis#analyzeEnd(org.eclipse.core.resources.IProject)
	 */
  @Override
  public Iterable<IRNode> finishAnalysis(final IProject project, IAnalysisMonitor monitor) {
    AbstractFluidAnalysisModule.runInVersion(new AbstractRunner() {
      public void run() {
        LOG.info("Finishing color assurance");
        TRoleSecondPass.getInstance().cspEnd(resultDependUpon, binder);
        LOG.info("Color Assurance complete.");
      }
    });

    return NONE_TO_ANALYZE;

  }
}