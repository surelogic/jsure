package edu.cmu.cs.fluid.analysis.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;

import com.surelogic.analysis.IAnalysisMonitor;
import com.surelogic.ast.java.operator.ICompilationUnitNode;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.jsure.core.Eclipse;
import com.surelogic.jsure.core.EclipseCodeFile;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.SourceCUDrop;

/**
 * A module that contains the basics that most IR-based analyses need (e.g.,
 * AnalysisContext)
 * 
 * @author chance
 */
public abstract class AbstractIRAnalysisModule extends
    AbstractFluidAnalysisModule<Void> { 
  protected enum ParserNeed {
    EITHER, OLD, NEW 
  }
  
  private static final Logger LOG = SLLogger.getLogger("dcf");

  protected static final Iterable<IRNode> NONE_TO_ANALYZE = Collections.emptyList();
  
  protected final boolean usesEitherParser;
  protected final boolean needsNewParser;
  
  protected IProject lastProjectAnalyzed = null;
  protected boolean constructionOfIRAnalysisNeeded = true;

  protected final boolean useAssumptions = useAssumptions();

  protected IBinder binder;

  protected CompilationUnit f_ast;
  
  private String msgPrefix = null;
  
  protected AbstractIRAnalysisModule(ParserNeed parserNeed) {
    switch (parserNeed) {
      default:
      case EITHER:
        usesEitherParser = true;
        needsNewParser   = false;
        break;
      case OLD:
        usesEitherParser = false;
        needsNewParser   = false;
        break;
      case NEW: 
        usesEitherParser = false;
        needsNewParser   = true;
        break;
    }
  }
  
  protected AbstractIRAnalysisModule() {
    this(ParserNeed.EITHER); 
  }
  
  /**
   * Indicates if the usual assumptions support is desired.
   * 
   * @return <code>true</code> if intending to use usual assumptions support,
   *         <code>false</code> otherwise.
   */
  protected boolean useAssumptions() {
    return true;
  }

  @Override
  public void analyzeBegin(IProject p) {
    super.analyzeBegin(p);
    binder = null; //Eclipse.getDefault().getTypeEnv(p).getBinder();
    msgPrefix = "Running " + getLabel() + " on file: ";
    
    if (lastProjectAnalyzed != p) {
    	constructionOfIRAnalysisNeeded = true;
    }
  }

  @Override
  public final boolean analyzeCompilationUnit(final ICompilationUnit file,
      CompilationUnit ast, IAnalysisMonitor monitor) {
    if (IDE.getInstance().isCancelled()) {
      return false;
    }

    // save the file to report results
    javaFile = file;
    f_ast = ast;

    if (constructionOfIRAnalysisNeeded) {
      constructionOfIRAnalysisNeeded = false;
      runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {

        public void run() {
          constructIRAnalysis();
        }
      });
    }
    final CUDrop drop      = SourceCUDrop.queryCU(new EclipseCodeFile(file));    
    final boolean isLoaded = (drop != null) ? true : Eclipse.getDefault().getJavaFileLocator().isLoaded(file.getHandleIdentifier());
    boolean rv = analyzeCUDrop(drop, isLoaded, monitor);

    javaFile = null;
    f_ast = null;
    return rv;
  }

  private boolean analyzeCUDrop(final CUDrop drop, final boolean isLoaded, 
		  final IAnalysisMonitor monitor) {
    Object rv = runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
      public void run() {
    	result = null;
        try {
          if (drop != null) {
            if (LOG.isLoggable(Level.FINE) && javaFile != null) {
              LOG.fine(msgPrefix + javaFile.getElementName());
            }
            PromiseFramework frame = PromiseFramework.getInstance();

            if (useAssumptions) {
              frame.pushTypeContext(drop.cu);
              try {
                doAnalysisOnCUDrop(drop, monitor);
                doneProcessing(drop.cu);
              } finally {
                frame.popTypeContext();
              }
            } else {
              doAnalysisOnCUDrop(drop, monitor);
            }
          } else if (isLoaded && javaFile != null) {
            LOG.warning("No IR drop found for " + javaFile.getElementName());
          }
        } catch (JavaModelException e) {
          if (javaFile != null) {
            LOG.log(Level.SEVERE, getLabel()
              + " skipped due to problem finding IR for "
              + javaFile.getElementName(), e);
          }
        }
      }

      private void doAnalysisOnCUDrop(final CUDrop drop, IAnalysisMonitor monitor) throws JavaModelException {
        if (!usesEitherParser && needsNewParser != true) {
          String msg = AbstractIRAnalysisModule.this.getClass().getSimpleName()+
                       " is incompatible with the "+
                       (needsNewParser ? "old" : "new")+" promise parser code";
          reportProblem(msg, drop.cu);
          throw new Error(msg);
        }
        if (drop.lines == 0) {
        	return; // Skip empty files
        }
        result = doAnalysisOnAFile(drop, monitor);
      }
    });
    return rv == Boolean.TRUE;
  }

  /**
   * Sets up for IR analysis within the Fluid code. Called after creating an
   * analysis context.
   */
  protected void constructIRAnalysis() {
    // do nothing
  }

  protected boolean useTypedASTs() {
    return false;
  }
  
  protected boolean doAnalysisOnAFile(final CUDrop drop, IAnalysisMonitor monitor) throws JavaModelException {
      if (useTypedASTs()) {            
          return doAnalysisOnAFile(drop.cu, drop.cun, monitor);
      } else {
          return doAnalysisOnAFile(drop.cu, monitor);
      }
  }
  
  /**
   * Does IR analysis within the Fluid code within a runInVersion() call.
   * 
   * @param file
   *          the Java compilation unit to perform analysis on
   */
  protected abstract boolean doAnalysisOnAFile(IRNode cu, IAnalysisMonitor monitor)
      throws JavaModelException;
  
  protected boolean doAnalysisOnAFile(IRNode n, ICompilationUnitNode cu, IAnalysisMonitor monitor)
  throws JavaModelException 
  {    
    return doAnalysisOnAFile(n, monitor);
  }
  /*
  @Override
  protected void doneProcessing(IRNode cu) {
    super.doneProcessing(cu);
    alreadyAnalyzed.add(cu);
  }
  */
  /**
   * Allows us to insert code before the analysis ends
   */
  @Override
  public final IResource[] analyzeEnd(IProject project, IAnalysisMonitor monitor) {       
    if (msgPrefix == null) {
      LOG.severe("Probably forgot to call super.analyzeBegin() for "+this.getClass().getCanonicalName());
    }
    
    // Process cached files from ConvertToIR
    // Also make sure that we don't reprocess files
    List<IResource> resources = null;
    Iterable<IRNode> iAble = finishAnalysis(project, monitor);
    if (iAble == null) {
      return null; // analyze everything again
    }
    boolean notEmpty = false;
    for (IRNode cu : iAble) {
      notEmpty = true;

      CUDrop d = CUDrop.queryCU(cu);
      if (d instanceof SourceCUDrop) {
        if (resources == null) {
          resources = new ArrayList<IResource>();
        }
        ICompilationUnit icu = (ICompilationUnit) d.hostEnvResource;
        try {
          resources.add(icu.getCorrespondingResource());
        } catch (JavaModelException e) {
          e.printStackTrace();
          LOG.severe("Couldn't get resource: ignoring "+icu.getElementName());
        }
      } else { // simulate
        analyzeCUDrop(d, true, null);
      }
    }
    if (resources != null) {
      return resources.toArray(NONE_FURTHER);
    } else if (notEmpty) { // need to call finishAnalysis
      return analyzeEnd(project, monitor);
    }
    //IDE.getInstance().setDefaultClassPath(null);
    return NONE_FURTHER;
  }
  
  /** 
   * @return IRNodes returned may have be previously processed
   */
  protected Iterable<IRNode> finishAnalysis(IProject project, IAnalysisMonitor monitor) {
    return NONE_TO_ANALYZE;
  }
}
