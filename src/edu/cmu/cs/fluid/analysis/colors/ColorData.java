package edu.cmu.cs.fluid.analysis.colors;

import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.JavaModelException;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.analysis.util.AbstractFluidAnalysisModule;
import edu.cmu.cs.fluid.analysis.util.AbstractIRAnalysisModule;
import edu.cmu.cs.fluid.eclipse.Eclipse;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.analysis.ColorStats;
import edu.cmu.cs.fluid.java.analysis.DataColoring;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.sea.Drop;
/**
 * @author dfsuther
 * 
 */
public final class ColorData
  extends AbstractIRAnalysisModule {


  /**
	 * Log4j logger for this class
	 */
  @SuppressWarnings("unused")
  private static final Logger LOG = SLLogger.getLogger("Data Coloring");



  private static ITypeEnvironment tEnv;

 
  @SuppressWarnings("unused")
  private IBinder binder;

  static private class ResultsDepDrop extends Drop {
  }

  private Drop resultDependUpon = null;


  private static ColorData INSTANCE;
  


  /**
	 * Provides a reference to the sole object of this class.
	 * 
	 * @return a reference to the only object of this class
	 */
  public static ColorData getInstance() {
    return INSTANCE;
  }

  public ColorData() {
    INSTANCE = this;
    ColorStats.getInstance();
  }

  /**
   * Should be protected.
   */
  public void clearResults() {
    resultDependUpon.invalidate();
    resultDependUpon = null;
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
    
    // Setup some fluid analysis stuff (Check that this is correct)
    tEnv = Eclipse.getDefault().getTypeEnv(project);
    binder = tEnv.getBinder();
  }

  /**
	 * @see edu.cmu.cs.fluid.dc.IAnalysis#analyzeResource(org.eclipse.core.resources.IResource,
	 *      int)
	 */
  @Override
  public boolean analyzeResource(IResource resource, int kind) {
    constructionOfIRAnalysisNeeded = false;
    return false; // please call my analyzeCompilationUnit()
  }

  /**
   * @see edu.cmu.cs.fluid.analysis.util.AbstractIRAnalysisModule#doAnalysisOnAFile(edu.cmu.cs.fluid.ir.IRNode)
   */
  @Override
  protected void doAnalysisOnAFile(IRNode cu) throws JavaModelException {
    DataColoring.getInstance().doDataColoringforOneCU(cu);
  }
  
  @Override
  public boolean needsAST() {
    return false;
  }  

  /**
	 * @see edu.cmu.cs.fluid.javaassure.IAnalysis#analyzeEnd(org.eclipse.core.resources.IProject)
	 */
  @Override
  public Iterable<IRNode> finishAnalysis(IProject project) {
    AbstractFluidAnalysisModule.runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
      public void run() {
        //ColorSecondPass.getInstance().cspEnd(resultDependUpon, binder);
      }
    });

    return NONE_TO_ANALYZE;

  }
}