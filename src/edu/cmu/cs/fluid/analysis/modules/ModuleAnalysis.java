/*
 * Created on Oct 21, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.analysis.modules;

import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import com.surelogic.analysis.IAnalysisMonitor;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.analysis.util.AbstractWholeIRAnalysisModule;
import edu.cmu.cs.fluid.eclipse.Eclipse;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.analysis.ModuleAnalysisAndVisitor;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.sea.Drop;


public class ModuleAnalysis extends AbstractWholeIRAnalysisModule {
  
  static private class ResultsDepDrop extends Drop { /* marker drop */ }
  
  private static ResultsDepDrop resultDependUpon = null;
  
  ITypeEnvironment tEnv = null;
  
  IBinder binder = null;
  

  private static final Logger LOG = SLLogger
      .getLogger("edu.cmu.cs.fluid.Modules");
  
  
  
  
  @Override
  public void analyzeBegin(IProject project) {
    LOG.info("analyzeBegin()");
    super.analyzeBegin(project);
    
    // Setup some fluid analysis stuff (Check that this is correct)
    tEnv = Eclipse.getDefault().getTypeEnv(project);
    binder = tEnv.getBinder();
    

    //Hashtable options = JavaCore.getOptions();
    Map options = getJavaProject().getOptions(true);
    String compiler   = (String) options.get(JavaCore.COMPILER_COMPLIANCE);
    if (!compiler.equals("1.5")) {
      reportProblem("@module declarations will not be found, since compiler is set to "+compiler+", instead of 1.5", null);
    }
//  Init the drop that all Module assurance results link to
    if (resultDependUpon != null) {
      resultDependUpon.invalidate();
      resultDependUpon = new ResultsDepDrop();
    } else {
      resultDependUpon = new ResultsDepDrop();
    }
    

//    runInVersion(new AbstractRunner() {
//
//      public void run() {
        ModuleAnalysisAndVisitor.getInstance().maStart(resultDependUpon);
//      }
//    });
   }
  
  @Override
  protected boolean doAnalysisOnAFile(IRNode cu, IAnalysisMonitor monitor) throws JavaModelException {
    ModuleAnalysisAndVisitor.getInstance().doOneCU(cu, binder);
    return true;
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.analysis.util.AbstractIRAnalysisModule#finishAnalysis(org.eclipse.core.resources.IProject)
   */
  @Override
  public void postBuild(IProject project) {
    ModuleAnalysisAndVisitor.getInstance().maEnd();
    super.postBuild(project);
  }
  
  

}
