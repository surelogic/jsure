/*$Header: /cvs/fluid/fluid/src/edu/uwm/cs/fluid/java/analysis/CachedFlowAnalysis.java,v 1.5 2008/06/24 19:13:13 thallora Exp $*/
package edu.uwm.cs.fluid.java.analysis;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.control.BlankOutputPort;
import edu.cmu.cs.fluid.control.Component;
import edu.cmu.cs.fluid.control.ControlEdge;
import edu.cmu.cs.fluid.control.ControlEdgeIterator;
import edu.cmu.cs.fluid.control.ControlNode;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaComponentFactory;
import edu.cmu.cs.fluid.java.analysis.CachedProceduralAnalysis;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.FlowUnit;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.uwm.cs.fluid.control.IFlowAnalysis;
import edu.uwm.cs.fluid.util.Lattice;

/**
 * Side-effecting analysis using lattice ADT.
 * @author boyland
 */
public abstract class CachedFlowAnalysis<T, L extends Lattice<T>, R extends CachedFlowAnalysis.FlowAnalysisResults<T,L>> extends
    CachedProceduralAnalysis<T, R> {
  private static final Logger LOG = SLLogger.getLogger("fluid.java.analysis");

  public CachedFlowAnalysis(IBinder b) {
    super(b);
  }

  public CachedFlowAnalysis(IBinder b, int cacheSize) {
    super(b, cacheSize);
  }

  @Override
  protected R computeResults(IRNode procedure) {
	  FlowUnit op = (FlowUnit) JJNode.tree.getOperator(procedure);
	  SideEffectingFlowAnalysis<T,L,R> fa = createAnalysis(procedure);
	  final JavaComponentFactory factory = JavaComponentFactory.startUse();
	  try {
		  fa.initialize(op.getSource(procedure, factory));
		  fa.initialize(op.getNormalSink(procedure, factory));
		  fa.initialize(op.getAbruptSink(procedure, factory));
		  fa.performAnalysis();
		  R results = createResults(fa);
		  fa.collectResults(results);
		  fa.reworkAll();
		  return results;
	  } finally {
		  JavaComponentFactory.finishUse(factory);
	  }    
  }

  protected abstract R createResults(SideEffectingFlowAnalysis<T,L,R> analysis);
  
  protected T getAfter(IRNode node) {
    IRNode proc = getProcedure(node);
    return getResults(proc).getAfter(node);
  }

  protected T getAfterError(IRNode node) {
    IRNode proc = getProcedure(node);
    return getResults(proc).getAfterError(node);
  }

  /**
   * Create the appropriate flow analysis instance. 
   * Initialization will be done using the transfer function.
   */
  protected abstract SideEffectingFlowAnalysis<T,L,R> createAnalysis(IRNode procedure);

  public static interface SideEffectingFlowAnalysis<T, L extends Lattice<T>, R extends FlowAnalysisResults<T, L>> extends IFlowAnalysis<T, L> {
    /**
     * Indicate that this analysis should start collecting results.
     */
    public void collectResults(R results);
  }
  
  /**
   * Cache flow analysis results and provide ways to query that information.
   *XXX It might be better to copy the information out into offline arrays
   * so the analysis could be garbage collected.
   * @author boyland
   */
  static class FlowAnalysisResults<T, L extends Lattice<T>> implements Results<T> {
    final IRNode procedure;
    final SideEffectingFlowAnalysis<T,L,FlowAnalysisResults<T, L>> analysis;
    
    public FlowAnalysisResults(IRNode proc, SideEffectingFlowAnalysis<T,L,FlowAnalysisResults<T,L>> fa) {
      procedure = proc;
      analysis = fa;
    }
    
    public IRNode getProcedure() {
      return procedure;
    }

    public T getSlotValue(IRNode node) {
      return getBefore(node);
    }

    public boolean valueExists(IRNode node) {
      return true;
    }
    
    public T getBefore(IRNode node) {
      return getPortResult(node,0);
    }
    
    public T getAfter(IRNode node) {
      return getPortResult(node,1);
    }
    
    public T getAfterError(IRNode node) {
      return getPortResult(node,2);
    }
    
    protected T getPortResult(IRNode node, int port) {
      // TODO is this bracketing right?
      final JavaComponentFactory factory = JavaComponentFactory.startUse();
      try {
      Component comp = factory.getComponent(node, true);
      if (comp == null)
        return null;
      ControlNode cn;
      switch (port) {
        case 0 :
          cn = comp.getEntryPort();
          break;
        case 1 :
          cn = comp.getNormalExitPort();
          break;
        case 2 :
          cn = comp.getAbruptExitPort();
          break;
        default :
          throw new FluidError("unknown port designator: " + port);
      }
      if (cn instanceof BlankOutputPort)
        return null;
      Lattice<T> lattice = analysis.getLattice();
      T val = null;
      try {
        for (ControlEdgeIterator outs = cn.getOutputs();
          outs.hasNext();
          ) {
          ControlEdge e = outs.nextControlEdge();
          T next = analysis.getInfo(e);
          if (val == null)
            val = next;
          else if (next != null)
            val = lattice.join(val,next);
        }
      } catch (Exception ex) {
        LOG.log(
          Level.SEVERE,
          "Exception occurred for " + DebugUnparser.toString(node),
          ex);
        ex.printStackTrace();
      }
      if (val == null)
        val = lattice.bottom();
      return val;
      } finally {
    	  JavaComponentFactory.finishUse(factory);
      }
    }

  }
}
