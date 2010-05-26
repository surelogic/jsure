package edu.cmu.cs.fluid.analysis.threadroles;

import java.util.Iterator;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.JavaModelException;

import com.surelogic.analysis.IAnalysisMonitor;
import com.surelogic.analysis.threadroles.TRolesFirstPass;
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

public class ThreadRoleZerothPass1 extends AbstractIRAnalysisModule {
  

  //private static ColorBDDPack cBDDPack = null;

  private static ITypeEnvironment tEnv;

  private IBinder binder;

  //private EclipseBinder eBinder;

  private QueuingSrcNotifyListener listener = new QueuingSrcNotifyListener();

  public ThreadRoleZerothPass1() {
    super(ParserNeed.NEW);
    INSTANCE = this;
    ConvertToIR.register(listener);
  }

  private static ThreadRoleZerothPass1 INSTANCE;

  private static final Logger LOG = SLLogger
      .getLogger("analysis.threadroles.managethreadroleannos");

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

        TRolesFirstPass.getInstance().trfpStart(binder);


        final Iterator<CodeInfo> it = listener.infos().iterator();
        while (it.hasNext()) {
          info = it.next();
          if (info.getProperty(CodeInfo.DONE) == Boolean.TRUE) {
            continue;
          }
          cu = info.getNode();

          TRolesFirstPass.getInstance().doImportandRenameWalks(cu, binder);
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
//    final Iterable<IRNode> reprocessThese = TRolesFirstPass.getInstance().cfpEnd();
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
    TRolesFirstPass.getInstance().resetForAFullBuild();
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
    TRolesFirstPass.getInstance().doOneCUZerothPass(cu, binder);
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

  public static ThreadRoleZerothPass1 getInstance() {
    return INSTANCE;
  }


  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.analysis.util.AbstractFluidAnalysisModule#preBuild(org.eclipse.core.resources.IProject)
   */
  @Override
  public void preBuild(IProject p) {
    TRolesFirstPass.preBuild();
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
