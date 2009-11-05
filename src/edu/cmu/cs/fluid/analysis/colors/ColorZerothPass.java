/*
 * Created on Apr 15, 2003
 */
package edu.cmu.cs.fluid.analysis.colors;

import java.util.Iterator;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.JavaModelException;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.analysis.util.AbstractIRAnalysisModule;
import edu.cmu.cs.fluid.analysis.util.ConvertToIR;
import edu.cmu.cs.fluid.eclipse.Eclipse;
import edu.cmu.cs.fluid.eclipse.QueuingSrcNotifyListener;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.CodeInfo;
import edu.cmu.cs.fluid.java.analysis.AnalysisContext;
import edu.cmu.cs.fluid.java.analysis.ColorFirstPass;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.util.AbstractRunner;

/**
 * @author dfsuther
 * 
 * ManageColorAnnos is an eclipse extension point. Its purpose is to examines
 * the project's AST for color annotations and generates drops for them. The
 * drops that come from user-written annotations are distinct from other color
 * drops.
 * 
 * ManagerColorAnnos establishes the following invariant.
 * 
 * 
 * 
 * Discussion:
 * 
 * Classes that implement eclipse extension points can not use the Singleton
 * pattern since eclipse insists on creating an instance. Such classes typically
 * act as modified Singleton classes: They provide a getInstance() method that
 * only works after eclipse and major domo have initialized the analysis. So
 * users have to be careful. This points hould be made in the javadoc comments
 * for majordomo and the relevant getInstance methods. Another point that should
 * be clear, if it is not already, is that majordomo may call analyzeEnd more
 * than once. TODO: Update AbstractAnalysisModule.getInstance comments if
 * needed.
 * 
 * ColorPromises.isColorRelevant is the wrong notion. We want its dual
 * ColorPromises.isColorTransparant. TODO: Replace isColorRelevant with
 * isColorTransparant.
 * 
 */

public final class ColorZerothPass extends AbstractIRAnalysisModule {

  //private static ColorBDDPack cBDDPack = null;

  private static ITypeEnvironment tEnv;

  private IBinder binder;

  //private EclipseBinder eBinder;

  private QueuingSrcNotifyListener listener = new QueuingSrcNotifyListener();

  public ColorZerothPass() {
    INSTANCE = this;
    ConvertToIR.register(listener);
  }

  private static ColorZerothPass INSTANCE;

  private static final Logger LOG = SLLogger
      .getLogger("analysis.colors.managecolorannos");

  /*
   * (non-Javadoc)
   * 
   * @see edu.cmu.cs.fluid.analysis.IAnalysis#analyzeBegin()
   */
  @Override
  public void analyzeBegin(IProject project) {
    LOG.info("analyzeBegin()");
    super.analyzeBegin(project);

    // Setup some fluid analysis stuff (Check that this is correct)
    tEnv = Eclipse.getDefault().getTypeEnv(project);
    binder = tEnv.getBinder();

    runInVersion(new AbstractRunner() {

      public void run() {
        CodeInfo info;
        IRNode cu;

        AnalysisContext ac = AnalysisContext.getContext(binder);
        ColorFirstPass.getInstance().cfpStart(ac);


        final Iterator<CodeInfo> it = listener.infos();
        while (it.hasNext()) {
          info = it.next();
          if (info.getProperty(CodeInfo.DONE) == Boolean.TRUE) {
            continue;
          }
          cu = info.getNode();

          ColorFirstPass.getInstance().doImportandRenameWalks(cu, binder);
        }
      }
    });
    listener.clear();
   }

//  /*
//   * (non-Javadoc)
//   * 
//   * @see edu.cmu.cs.fluid.dc.IAnalysis#analyzeEnd(org.eclipse.core.resources.IProject)
//   */
//  @Override
//  public Iterable<IRNode> finishAnalysis(IProject project) {
//    final Iterable<IRNode> reprocessThese = ColorFirstPass.getInstance().cfpEnd();
//
//  
//      return reprocessThese;
//    
//  }

  /*
   * (non-Javadoc)
   * 
   * @see edu.cmu.cs.fluid.dc.IAnalysis#resetForAFullBuild(org.eclipse.core.resources.IProject)
   */
  @Override
  public void resetForAFullBuild(IProject project) {
    ColorFirstPass.getInstance().resetForAFullBuild();
  }

  @Override
  public boolean needsAST() {
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see edu.cmu.cs.fluid.analysis.util.AbstractIRAnalysisModule#doAnalysisOnAFile(edu.cmu.cs.fluid.ir.IRNode)
   */
  @Override
  protected void doAnalysisOnAFile(IRNode cu) throws JavaModelException {
    ColorFirstPass.getInstance().doOneCUZerothPass(cu, binder);
  }

   public static ColorZerothPass getInstance() {
    return INSTANCE;
  }


  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.analysis.util.AbstractFluidAnalysisModule#preBuild(org.eclipse.core.resources.IProject)
   */
  @Override
  public void preBuild(IProject p) {
    ColorFirstPass.preBuild();
    super.preBuild(p);
  }

  /*
   * (non-Javadoc)
   * 
   * @see edu.cmu.cs.fluid.dc.IAnalysis#analyzeResource(org.eclipse.core.resources.IResource,
   *      int)
   */
  @Override
  public boolean analyzeResource(IResource resource, int kind) {
    constructionOfIRAnalysisNeeded = false;
    super.analyzeResource(resource, kind);
    // we want to continue analyzing compunits, so...
    return false;
  }

}