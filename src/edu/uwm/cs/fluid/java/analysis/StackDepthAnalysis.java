/*$Header: /cvs/fluid/fluid/src/edu/uwm/cs/fluid/java/analysis/StackDepthAnalysis.java,v 1.5 2007/07/10 22:16:35 aarong Exp $*/
package edu.uwm.cs.fluid.java.analysis;

import java.io.File;
import java.io.IOException;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.uwm.cs.fluid.control.FlowAnalysis;
import edu.uwm.cs.fluid.control.ForwardAnalysis;
import edu.uwm.cs.fluid.control.ForwardTransfer;
import edu.uwm.cs.fluid.java.control.JavaEvaluationTransfer;
import edu.uwm.cs.fluid.util.FlatLattice;


/**
 * A class to test the default transfer functions of {@link edu.uwm.cs.fluid.java.control.JavaEvaluationTransfer}
 * @author boyland
 */
public class StackDepthAnalysis extends ForwardAnalysis<Object> {
  /**
   * @param name
   * @param l
   * @param t
   */
  public StackDepthAnalysis(ForwardTransfer<Object> t) {
    super("Stack depth",FlatLattice.prototype,t,DebugUnparser.viewer);
  }
  
  public static StackDepthAnalysis create(IBinder binder) {
    return new StackDepthAnalysis(new StackDepthTransfer(binder));
  }
  
  
  static class StackDepthTransfer extends JavaEvaluationTransfer<FlatLattice,Object> {
    
    /**
     * @param binder
     * @param lattice
     */
    public StackDepthTransfer(IBinder binder) {
      super(binder,FlatLattice.prototype);
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
      return 0;
    }
    
    /* (non-Javadoc)
     * @see edu.uwm.cs.fluid.java.control.JavaTransfer#createAnalysis(edu.cmu.cs.fluid.java.bind.IBinder)
     */
    @Override
    protected FlowAnalysis<Object> createAnalysis(IBinder binder) {
      return StackDepthAnalysis.create(binder);
    }
    
    /* (non-Javadoc)
     * @see edu.uwm.cs.fluid.control.ForwardTransfer#transferComponentSource(edu.cmu.cs.fluid.ir.IRNode)
     */
    public Object transferComponentSource(IRNode node) {
      return 0;
    }
  }
}

class TestStackDepthAnalysis extends TestFlowAnalysis {
  @Override
  protected FlowAnalysis<Object> createAnalysis(IRNode ignored, IBinder binder) {
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