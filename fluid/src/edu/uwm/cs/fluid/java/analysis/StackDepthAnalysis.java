/*$Header: /cvs/fluid/fluid/src/edu/uwm/cs/fluid/java/analysis/StackDepthAnalysis.java,v 1.5 2007/07/10 22:16:35 aarong Exp $*/
package edu.uwm.cs.fluid.java.analysis;

import java.io.File;
import java.io.IOException;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.uwm.cs.fluid.java.control.AbstractCachingSubAnalysisFactory;
import edu.uwm.cs.fluid.java.control.JavaEvaluationTransfer;
import edu.uwm.cs.fluid.java.control.JavaForwardAnalysis;
import edu.uwm.cs.fluid.util.FlatLattice;


/**
 * A class to test the default transfer functions of {@link edu.uwm.cs.fluid.java.control.JavaEvaluationTransfer}
 * @author boyland
 */
public class StackDepthAnalysis extends JavaForwardAnalysis<Object, FlatLattice> {
  /**
   * @param name
   * @param l
   * @param t
   */
  public StackDepthAnalysis(final FlatLattice lattice, final StackDepthTransfer t) {
    super("Stack depth", lattice, t, DebugUnparser.viewer);
  }
  
  public static StackDepthAnalysis create(final IBinder binder) {
    return new StackDepthAnalysis(FlatLattice.prototype,
        new StackDepthTransfer(
            binder, FlatLattice.prototype, Integer.valueOf(0)));
  }
  
  public static StackDepthAnalysis create(
      final IBinder binder, final FlatLattice lattice, final int floor) {
    return new StackDepthAnalysis(lattice,
        new StackDepthTransfer(binder, lattice, floor));
  }
  
  
  public static class StackDepthTransfer extends JavaEvaluationTransfer<FlatLattice, Object> {
    /**
     * @param binder
     * @param lattice
     */
    private StackDepthTransfer(
        final IBinder binder, final FlatLattice lattice, final int floor) {
      super(binder, lattice, new SubAnalysisFactory(), floor);
    }
    
    /* (non-Javadoc)
     * @see edu.uwm.cs.fluid.java.control.JavaEvaluationTransfer#pop(T)
     */
    @Override
    protected Object pop(Object val) {
      if (val instanceof Integer) {
        return ((Integer)val)-1;
      }
      return val;
    }
    
    /* (non-Javadoc)
     * @see edu.uwm.cs.fluid.java.control.JavaEvaluationTransfer#push(T)
     */
    @Override
    protected Object push(Object val) {
      if (val instanceof Integer) {
        return ((Integer)val)+1;
      }
      return val;
    }
    
    /* (non-Javadoc)
     * @see edu.uwm.cs.fluid.java.control.JavaEvaluationTransfer#popAllPending(T)
     */
    @Override
    protected Object popAllPending(Object val) {
      return stackFloorSize;
    }
    
    /* (non-Javadoc)
     * @see edu.uwm.cs.fluid.control.ForwardTransfer#transferComponentSource(edu.cmu.cs.fluid.ir.IRNode)
     */
    public Object transferComponentSource(IRNode node) {
      return 0;
    }

    @Override
    protected Object transferUseQualifiedRcvr(
        final IRNode qThis, final IRNode qRcvr, final Object val) {
      // Results in a reference pushed on the stack
      return push(val);
    }
  }
  
  public static final class SubAnalysisFactory extends AbstractCachingSubAnalysisFactory<FlatLattice, Object> {
    @Override
    protected StackDepthAnalysis realCreateAnalysis(
        final IRNode caller, final IBinder binder, FlatLattice lattice,
        final Object initialValue, final boolean terminationNormal) {
      final int floor = (initialValue instanceof Integer) ? ((Integer) initialValue).intValue() : 0; 
      return StackDepthAnalysis.create(binder, lattice, floor);
    }    
  }
}

class TestStackDepthAnalysis extends TestFlowAnalysis<Object, FlatLattice, StackDepthAnalysis> {
  @Override
  protected StackDepthAnalysis createAnalysis(IRNode ignored, IBinder binder) {
    return StackDepthAnalysis.create(binder);
  }
  
  public static void main(String[] files) throws IOException {
    TestStackDepthAnalysis test = new TestStackDepthAnalysis();
    for (String file : files) {
      IRNode cu = test.addCompilatioUnit(new File(file));
      test.analyzeCompilationUnit(cu);
    }
  }  
 
}