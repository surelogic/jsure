package edu.cmu.cs.fluid.analysis.colors;

import java.util.Iterator;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.JavaModelException;

import com.surelogic.analysis.IAnalysisMonitor;
import com.surelogic.analysis.colors.ColorFirstPass;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.analysis.util.AbstractIRAnalysisModule;
import edu.cmu.cs.fluid.analysis.util.ConvertToIR;
import edu.cmu.cs.fluid.eclipse.Eclipse;
import edu.cmu.cs.fluid.eclipse.QueuingSrcNotifyListener;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.CodeInfo;

import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.util.AbstractRunner;

public class ColorZerothPass1 extends AbstractIRAnalysisModule {
  

  //private static ColorBDDPack cBDDPack = null;

  private static ITypeEnvironment tEnv;

  private IBinder binder;

  //private EclipseBinder eBinder;

  private QueuingSrcNotifyListener listener = new QueuingSrcNotifyListener();

  public ColorZerothPass1() {
    super(ParserNeed.NEW);
    INSTANCE = this;
    ConvertToIR.register(listener);
  }

  private static ColorZerothPass1 INSTANCE;

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

        ColorFirstPass.getInstance().cfpStart(binder);


        final Iterator<CodeInfo> it = listener.infos().iterator();
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
  protected boolean doAnalysisOnAFile(IRNode cu, IAnalysisMonitor monitor) throws JavaModelException {
    ColorFirstPass.getInstance().doOneCUZerothPass(cu, binder);
    return true;
  }

  // /*
  // * (non-Javadoc)
  // *
  // * @see edu.cmu.cs.fluid.sea.ISeaListener#dropChanged(int,
  // * edu.cmu.cs.fluid.sea.IDrop)
  // */
  // public void dropChanged(int kind, IDrop drop) {
  // LOG.finer("dropChanged() - kind = " + kind + " drop is "
  // + drop.getContents());
  // if (drop.getOwner() == this) {
  // if (kind == ISeaListener.DEPONENT_INVALID) {
  // LOG.finer("DEPONENT_INVALID so invalidating my drop " + drop.getGUID());
  // ColorSea.invalidateColorDrop(drop); // dump our old result
  // }
  // }
  // }

  public static ColorZerothPass1 getInstance() {
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
