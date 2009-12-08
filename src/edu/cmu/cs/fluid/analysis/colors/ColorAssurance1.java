package edu.cmu.cs.fluid.analysis.colors;

import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.JavaModelException;

import com.surelogic.analysis.IAnalysisMonitor;
import com.surelogic.analysis.colors.ColorSecondPass;
import com.surelogic.analysis.colors.ColorStats;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.analysis.util.AbstractFluidAnalysisModule;
import edu.cmu.cs.fluid.analysis.util.AbstractWholeIRAnalysisModule;
import edu.cmu.cs.fluid.analysis.util.ConvertToIR;
import edu.cmu.cs.fluid.eclipse.Eclipse;
import edu.cmu.cs.fluid.eclipse.QueuingSrcNotifyListener;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.util.AbstractRunner;

public class ColorAssurance1 extends AbstractWholeIRAnalysisModule {

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
 
  private IBinder binder;

  static private class ResultsDepDrop extends Drop {
  }

  private Drop resultDependUpon = null;


  private static ColorAssurance1 INSTANCE;


  private QueuingSrcNotifyListener listener = new QueuingSrcNotifyListener(); 
  
  /**
   * Provides a reference to the sole object of this class.
   * 
   * @return a reference to the only object of this class
   */
  public static ColorAssurance1 getInstance() {
    return INSTANCE;
  }

  public ColorAssurance1() {
    super(ParserNeed.NEW);
    INSTANCE = this;
    ColorStats.getInstance();
    ConvertToIR.register(listener);
  }

  /**
   * Should be protected.
   */
  public void clearResults() {
    if (resultDependUpon != null) {
      resultDependUpon.invalidate();
      resultDependUpon = null;
    }
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
  protected boolean doAnalysisOnAFile(IRNode cu, IAnalysisMonitor monitor) throws JavaModelException {
    cuCount += 1;
    return false;
  }
  
  @Override
  public boolean needsAST() {
    return false;
  }  

  /**
   * @see edu.cmu.cs.fluid.javaassure.IAnalysis#analyzeEnd(org.eclipse.core.resources.IProject)
   */
  @Override
  public Iterable<IRNode> finishAnalysis(final IProject project, IAnalysisMonitor monitor) {
    AbstractFluidAnalysisModule.runInVersion(new AbstractRunner() {
      public void run() {
        LOG.info("Finishing color assurance");
        ColorSecondPass.getInstance().cspEnd(resultDependUpon, binder);
        LOG.info("Color Assurance complete.");
      }
    });

    return NONE_TO_ANALYZE;

  }

}
