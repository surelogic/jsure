/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/locks/DummyAnalysis.java,v 1.2 2008/06/24 19:13:19 thallora Exp $*/
package com.surelogic.analysis.locks;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLFormatter;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.tree.Operator;
import edu.uwm.cs.fluid.control.BackwardAnalysis;
import edu.uwm.cs.fluid.control.FlowAnalysis;
import edu.uwm.cs.fluid.util.ChainLattice;

public final class DummyAnalysis extends
    edu.uwm.cs.fluid.java.analysis.IntraproceduralAnalysis<Integer> {
  private static final Logger LOGGER =
    Logger.getLogger("com.surelogic.analysis.locks.DummyAnalysis");
      
  static {
    try {
      final FileHandler handler = new FileHandler("%h/dummy_trace%u.log");
      LOGGER.addHandler(handler);
      LOGGER.setLevel(Level.ALL);
      handler.setLevel(Level.ALL);
      handler.setFormatter(new SLFormatter());
    } catch (final IOException e) {
      // swallow
    }
  }
  
  
  
  public DummyAnalysis(final IBinder b) {
    super(b);
  }

  
  
  // For debugging
  private static void log(
      final Level level, final String messageTemplate, final Object... args) {
    LOGGER.log(level, MessageFormat.format(messageTemplate, args));
  }
  
  
  
  @SuppressWarnings("unchecked")
  @Override
  protected FlowAnalysis<Integer> createAnalysis(final IRNode flowUnit) {
    final ChainLattice lattice = new ChainLattice(2);
    final FlowAnalysis<Integer> analysis =
      new BackwardAnalysis<Integer>("Dummy", lattice,
          new DummyTransfer(binder, lattice), DebugUnparser.viewer);
    return analysis;
  }

  /**
   * Get the matching unlock.
   * @return The IRNode of the matching unlock method call.
   */
  public Integer getDummy(final IRNode mcall) {
    final Integer value = getAnalysisResultsAfter(mcall);
    log(Level.INFO, "getUnlocksFor({0} at {1}) == {2}",
        MethodCall.getMethod(mcall), JavaNode.getSrcRef(mcall).getLineNumber(),
        value);    
    return value;
  }
  
  
  
  static final class DummyTransfer extends
      edu.uwm.cs.fluid.java.control.JavaBackwardTransfer<
          ChainLattice, Integer> {
    public DummyTransfer(final IBinder binder, final ChainLattice lattice) {
      super(binder, lattice);
    }
    
    private boolean isNormal(final Integer val) {
      return val != lattice.bottom() || val != lattice.top();
    }
    
    public Integer transferConditional(
        final IRNode node, final boolean flag, final Integer after) {
      // Bail out if input is top or bottom
      if (!isNormal(after)) {
        return after;
      }
      
      final String header = MessageFormat.format(
          "transferConditional({0} at {1}, {2}, {3})",
          DebugUnparser.toString(node), JavaNode.getSrcRef(node).getLineNumber(),
          flag, lattice.toString(after));
      log(Level.INFO, "{0} == {1} [value unchanged]", header, after);
      return after;
    }
    
    @Override
    protected Integer transferIsObject(
        final IRNode node, final boolean flag, final Integer value) {
      // Bail out if input is top or bottom
      if (!isNormal(value)) {
        return value;
      }
      
      final String header = 
        MessageFormat.format("transferIsObject({0} at {1}, {2}, {3})",
        DebugUnparser.toString(node), JavaNode.getSrcRef(node).getLineNumber(),
        flag, lattice.toString(value));        
      log(Level.INFO, "{0} == {1} [value unchanged]", header, value);
      return value;
    }

    
    @Override
    protected Integer transferCall(
        final IRNode call, final boolean flag, final Integer value) {
      // Bail out if input is top or bottom
      if (!isNormal(value)) {
        return value;
      }
      
      final Operator op = tree.getOperator(call);
      if (op instanceof MethodCall) {
        final String header = 
          MessageFormat.format("transferCall({0} at {1}, {2}, {3})",
              DebugUnparser.toString(call), JavaNode.getSrcRef(call).getLineNumber(),
              flag, lattice.toString(value));
        
        log(Level.INFO, "{0} == {1} [value unchanged]", header, value);
        return value;
      } else {
        // Constructor calls are not interesting
        return value;
      }
    }

    @Override
    protected FlowAnalysis<Integer> createAnalysis(IBinder binder) {
      return new BackwardAnalysis<Integer>(
          "Dummy", lattice, this, DebugUnparser.viewer);
    }

    @SuppressWarnings("unchecked")
    public Integer transferComponentSink(IRNode node, boolean normal) {
      final Integer initValue = Integer.valueOf(1);
      log(Level.INFO, "transferComponentSink({0}) == {1}",
          normal, initValue);
      return initValue;
    }
  }
}

