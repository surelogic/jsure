package edu.cmu.cs.fluid.analysis.util;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;

import com.surelogic.analysis.IAnalysisMonitor;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.eclipse.QueuingSrcNotifyListener;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.CodeInfo;
import edu.cmu.cs.fluid.util.AbstractRunner;

/**
 * Processes the files queued by its listener on ConvertToIR
 * 
 * @author Edwin
 *
 */
public abstract class AbstractQueuedIRAnalysisModule extends AbstractIRAnalysisModule
{  
  /**
   * Logger for this class
   */
  @SuppressWarnings("unused")
  private static final Logger LOG = SLLogger.getLogger("AbstractQueuedIRAnalysisModule");
  
  protected final QueuingSrcNotifyListener listener = new QueuingSrcNotifyListener(); 
  
  public AbstractQueuedIRAnalysisModule() {
  }
  @Override
  protected boolean useAssumptions() {
    return false;
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.analysis.util.AbstractIRAnalysisModule#doAnalysisOnAFile(edu.cmu.cs.fluid.ir.IRNode)
   */
  @Override
  protected boolean doAnalysisOnAFile(IRNode cu, IAnalysisMonitor monitor) {    
    // do nothing
	  return false;
  }

  /**
   * @see edu.cmu.cs.fluid.dc.IAnalysis#analyzeEnd(org.eclipse.core.resources.IProject)
   */
  @Override
  public Iterable<IRNode> finishAnalysis(IProject project, IAnalysisMonitor monitor) {
    //System.err.println("Starting iterator for "+this);    
    final Iterator<CodeInfo> it = listener.infos();
    while (it.hasNext()) {
      try {
        final CodeInfo info = it.next();
        info.clearProperty(CodeInfo.DONE);
      } catch(ConcurrentModificationException e) {
        System.out.println("Stopped in "+AbstractQueuedIRAnalysisModule.this);
        throw e;
      }
    }
    
    runInVersion(new AbstractRunner() {
      public void run() {
        final Iterator<CodeInfo> it = listener.infos();
        while (it.hasNext()) {
          try {
            final CodeInfo info = it.next();
            if (info.getNode().identity() != IRNode.destroyedNode) {
              processInfo(info);
              doneProcessing(info.getNode());
            }
            else {
              // ignoring destroyed nodes
            }
          } catch(ConcurrentModificationException e) {
            System.err.println("Stopped in "+AbstractQueuedIRAnalysisModule.this);
            throw e;
          }
        }
        end();
      }
    });
    listener.clear();
    return NONE_TO_ANALYZE;
  }

  /**
   * @param info
   */
  protected abstract void processInfo(CodeInfo info);
  
  protected void end() {}
}
