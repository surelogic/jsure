package edu.cmu.cs.fluid.analysis.util;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;

import com.surelogic.annotation.rules.AnnotationRules;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.xml.XMLGenerator;

import edu.cmu.cs.fluid.eclipse.Eclipse;
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.java.CodeInfo;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IHasBinding;
import edu.cmu.cs.fluid.java.bind.IHasType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.bind.ScopedPromises;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.Category;
import edu.cmu.cs.fluid.sea.IRReferenceDrop;
import edu.cmu.cs.fluid.sea.PromiseWarningDrop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.SourceCUDrop;
import edu.cmu.cs.fluid.sea.drops.promises.LockModel;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.CustomizableHashCodeMap;
import edu.cmu.cs.fluid.version.VersionedUnitSlotInfo;

public final class PromiseScrubber extends AbstractQueuedIRAnalysisModule {
  private static PromiseScrubber INSTANCE;

  public static PromiseScrubber getInstance() {
    return INSTANCE;
  }

  private static void setInstance(PromiseScrubber me) {
    INSTANCE = me;
  }
  
  public PromiseScrubber() {
    setInstance(this);
    ConvertToIR.register(listener);
  }

  /**
   * Logger for this class
   */
  private static final Logger LOG = SLLogger.getLogger("PromiseScrubber");

  public static final boolean testBinder = IDE.testBinder;

  @Override
  protected boolean useAssumptions() {
    return false;
  }

  private IBinder binder;
  
  @Override
  public void analyzeBegin(IProject p) {
    super.analyzeBegin(p);
    binder = Eclipse.getDefault().getTypeEnv(p).getBinder();
    PromiseFramework.getInstance().setBinder(binder);
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see edu.cmu.cs.fluid.analysis.util.AbstractIRAnalysisModule#doAnalysisOnAFile(edu.cmu.cs.fluid.ir.IRNode)
   */
  @Override
  protected void doAnalysisOnAFile(IRNode cu) {
    if (testBinder) {
      for(IRNode n : JJNode.tree.bottomUp(cu)) {
        Operator op = JJNode.tree.getOperator(n);
        if (op instanceof IHasType) {
          checkIfNull("IJavaType", n, binder.getJavaType(n));
        } 
        if (op instanceof IHasBinding) {   
          checkIfNull("IBinding", n, binder.getIBinding(n));          
        }
      }
    }
    if (!AnnotationRules.useNewParser) {
      final ScopedPromises sp = ScopedPromises.getInstance();
      /*
       * final Enumeration types = VisitUtil.getTypeDecls(dropContents.cu); while
       * (types.hasNext()) { IRNode type = (IRNode) types.nextElement();
       * sp.processAssumptions(type); }
       */

      sp.processCuAssumptions(binder, cu); // TODO change to process types

      final PromiseFramework frame = PromiseFramework.getInstance();
      // Call IR scrubber within Fluid
      // send in "this" as implementing
      // *.fluid.java.analysis.ITallyhoWarningReport
      Map m = frame.pushTypeContext(cu);
      Map m2 = null;
      try {
        frame.checkAST(PromiseScrubber.this, cu);
      } finally {
        m2 = frame.popTypeContext();
        if (m != m2) {
          LOG.severe("Popping a different type context");
        }
      }

      m = frame.pushTypeContext(cu, false, true); // Only look at assumptions
      try {
        frame.checkAssumptionsOnAST(PromiseScrubber.this, cu);
      } finally {
        m2 = frame.popTypeContext();
        if (m != m2) {
          LOG.severe("Popping a different type context 2");
        }
      }
    }
    if (PromiseParser.useXMLGen) {
      try {
        XMLGenerator.generateXML(cu,false);
      } catch (Exception e) {
        e.printStackTrace();
      }
    } 
  }

  private void checkIfNull(String label, IRNode n, Object result) {
    if (result == null) {
      IRNode context = VisitUtil.getEnclosingType(n);
      if (context == null) {
        context = VisitUtil.getEnclosingCompilationUnit(n);
      }
      LOG.severe("No "+label+" for "+DebugUnparser.toString(n)+" in "+context);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see edu.cmu.cs.fluid.analysis.util.AbstractQueuedIRAnalysisModule#processInfo(edu.cmu.cs.fluid.eclipse.CodeInfo)
   */
  @Override
  protected void processInfo(CodeInfo info) {
    IRNode cu = info.getNode();

    // final CUDrop drop = BinaryCUDrop.queryCU(name);
    final CUDrop drop = CUDrop.queryCU(cu);
    processClassFile(drop);
    
    if (PromiseParser.useXMLGen) {
		try {
			XMLGenerator.generateXML(cu,false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    
  }

  private void processClassFile(CUDrop drop) {
    if (drop != null) {
      drop.analysisContext = analysisContext;
    }
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("Scrubbing IType " + drop.javaOSFileName);
    }
    PromiseFramework.getInstance().checkAST(PromiseScrubber.this, drop.cu);
  }
  
  @Override
  public Iterable<IRNode> finishAnalysis(IProject p) {
    Iterable<IRNode> rv = super.finishAnalysis(p);
    
    final ITypeEnvironment te = Eclipse.getDefault().getTypeEnv(p);
    handleWaitQueue(new IQueueHandler() {
      public void handle(String qname) {
        IRNode t   = te.findNamedType(qname);
        IRNode cu  = VisitUtil.getEnclosingCompilationUnit(t);
        CUDrop d   = CUDrop.queryCU(cu);
        if (d == null) {
          // FIX?
          return;
        }
        else if (d instanceof SourceCUDrop) {
          doAnalysisOnAFile(cu);
        } else {
          processClassFile(d);
        }
      }      
    });
    
    AnnotationRules.scrub();    
    RegionModel.purgeUnusedRegions();
    LockModel.purgeUnusedLocks();
    return rv;
  }
  
  /**
   * @see edu.cmu.cs.fluid.dc.IAnalysis#postBuild(org.eclipse.core.resources.IProject)
   */
  @Override
  public void postBuild(IProject project) {
    PromiseFramework.getInstance().printCheckOps();
    SlotInfo.printSlotInfoSizes();
    VersionedUnitSlotInfo.printVUSlotCounts();
    CustomizableHashCodeMap.printStats();
  }

  @Override
  protected IRReferenceDrop makeWarningDrop() {
    return new PromiseWarningDrop();
  }
  @Override
  protected IRReferenceDrop makeProblemDrop() {
    return new PromiseWarningDrop();
  }
  @Override
  protected Category warningCategory() {
    return JavaGlobals.PROMISE_SCRUBBER;
  }
  @Override
  protected Category problemCategory() {
    return JavaGlobals.PROMISE_SCRUBBER;
  }
}
