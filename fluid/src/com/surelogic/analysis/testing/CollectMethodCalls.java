package com.surelogic.analysis.testing;

import com.surelogic.analysis.IBinderClient;
import com.surelogic.util.IThunk;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.analysis.SimplifiedJavaFlowAnalysisQuery;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;
import edu.cmu.cs.fluid.util.ImmutableSet;
import edu.uwm.cs.fluid.java.analysis.IntraproceduralAnalysis;
import edu.uwm.cs.fluid.java.control.AbstractCachingSubAnalysisFactory;
import edu.uwm.cs.fluid.java.control.IJavaFlowAnalysis;
import edu.uwm.cs.fluid.java.control.JavaForwardAnalysis;
import edu.uwm.cs.fluid.java.control.JavaForwardTransfer;
import edu.uwm.cs.fluid.util.UnionLattice;

/**
 * A control flow analysis that just collects all the method/constructor calls
 * that control passes through.  This is not very useful, but I need it for 
 * testing the correctness of the control flow graph in regression tests.  
 */

final class CollectMethodCalls extends IntraproceduralAnalysis<ImmutableSet<IRNode>, UnionLattice<IRNode>, JavaForwardAnalysis<ImmutableSet<IRNode>, UnionLattice<IRNode>>> implements IBinderClient {
  public final class Query extends SimplifiedJavaFlowAnalysisQuery<Query, ImmutableSet<IRNode>, ImmutableSet<IRNode>, UnionLattice<IRNode>> {
    private Query(final Delegate<Query, ImmutableSet<IRNode>, ImmutableSet<IRNode>, UnionLattice<IRNode>> d) {
      super(d);
    }

    public Query(final IThunk<? extends IJavaFlowAnalysis<ImmutableSet<IRNode>, UnionLattice<IRNode>>> thunk) {
      super(thunk);
    }

    @Override
    protected RawResultFactory getRawResultFactory() {
      return RawResultFactory.NORMAL_EXIT;
    }

    
    
    @Override
    protected Query newSubAnalysisQuery(final Delegate<Query, ImmutableSet<IRNode>, ImmutableSet<IRNode>, UnionLattice<IRNode>> d) {
      return new Query(d);
    }

    @Override
    protected ImmutableSet<IRNode> processRawResult(
        final IRNode expr, final UnionLattice<IRNode> lattice, 
        final ImmutableSet<IRNode> rawResult) {
      return rawResult;
    }
  }
  
  
  
  public CollectMethodCalls(final IBinder b) {
    super(b);
  }
  
  @Override
  public JavaForwardAnalysis<ImmutableSet<IRNode>, UnionLattice<IRNode>> createAnalysis(final IRNode flowUnit) {
    final UnionLattice<IRNode> l =  new UnionLattice<IRNode>();
    return new JavaForwardAnalysis<ImmutableSet<IRNode>, UnionLattice<IRNode>>(
        "method calls", l, new Transfer(binder, l), DebugUnparser.viewer);
  }

  /**
   * Get an query object tailored to a specific flow unit.
   */
  public Query getQuery(final IRNode flowUnit) {
    return new Query(getAnalysisThunk(flowUnit));
  }

  
  
  private static final class Transfer extends JavaForwardTransfer<UnionLattice<IRNode>, ImmutableSet<IRNode>> {
    public Transfer(final IBinder binder, final UnionLattice<IRNode> lattice) {
      super(binder, lattice, new SubAnalysisFactory());
    }

    
    
    @Override 
    public ImmutableSet<IRNode> transferCall(
        final IRNode node, final boolean flag, final ImmutableSet<IRNode> before) {
      // Only need to add the call once, so just do it on the normal path
      ImmutableSet<IRNode> out = before;
      if (flag) {
        out = out.union(ImmutableHashOrderSet.<IRNode>emptySet().addElement(node));
      }
      return out;
    }
        
    @Override
    public ImmutableSet<IRNode> transferImpliedNewExpression(
        final IRNode call, final boolean flag, final ImmutableSet<IRNode> before) {
      // N.B. Should be handled as a specialized case of transferCall
      
      // Only need to add the call once, so just do it on the normal path
      ImmutableSet<IRNode> out = before;
      if (flag) {
        out = out.union(
            ImmutableHashOrderSet.<IRNode>emptySet().addElement(
                JJNode.tree.getParent(call)));
      }
      return out;
  }

    public ImmutableSet<IRNode> transferComponentSource(final IRNode node) {
      return ImmutableHashOrderSet.emptySet();
    }
  }

  

  private static final class SubAnalysisFactory extends AbstractCachingSubAnalysisFactory<UnionLattice<IRNode>, ImmutableSet<IRNode>> {
    @Override
    protected JavaForwardAnalysis<ImmutableSet<IRNode>, UnionLattice<IRNode>> realCreateAnalysis(
        final IRNode caller, final IBinder binder,
        final UnionLattice<IRNode> lattice, final ImmutableSet<IRNode> initialValue,
        final boolean terminationNormal) {
      return new JavaForwardAnalysis<ImmutableSet<IRNode>, UnionLattice<IRNode>>(
          "method call (subanalysis)", lattice, new Transfer(binder, lattice), DebugUnparser.viewer);
    }    
  }



  public IBinder getBinder() {
    return binder;
  }

  public void clearCaches() {
    clear();
  }
}
