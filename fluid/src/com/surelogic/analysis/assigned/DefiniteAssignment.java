package com.surelogic.analysis.assigned;


import java.util.ArrayList;
import java.util.List;

import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.LocalVariableDeclarations;
import com.surelogic.util.IThunk;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.analysis.SimplifiedJavaFlowAnalysisQuery;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.AssignmentInterface;
import edu.cmu.cs.fluid.java.operator.NoInitialization;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.tree.Operator;
import edu.uwm.cs.fluid.java.control.AbstractCachingSubAnalysisFactory;
import edu.uwm.cs.fluid.java.control.IJavaFlowAnalysis;
import edu.uwm.cs.fluid.java.control.JavaForwardAnalysis;
import edu.uwm.cs.fluid.java.control.JavaForwardTransfer;
import edu.uwm.cs.fluid.java.analysis.IntraproceduralAnalysis;


/**
 * Determines if a field is definitely assigned by the constructor.
 */
public final class DefiniteAssignment extends IntraproceduralAnalysis<Assigned[], AssignedVars, JavaForwardAnalysis<Assigned[], AssignedVars>> implements IBinderClient {
  public static final class ProvablyUnassignedQuery extends SimplifiedJavaFlowAnalysisQuery<ProvablyUnassignedQuery, ProvablyUnassignedResult, Assigned[], AssignedVars> {
    public ProvablyUnassignedQuery(final IThunk<? extends IJavaFlowAnalysis<Assigned[], AssignedVars>> thunk) {
      super(thunk);
    }
    
    private ProvablyUnassignedQuery(final Delegate<ProvablyUnassignedQuery, ProvablyUnassignedResult, Assigned[], AssignedVars> d) {
      super(d);
    }
    
    @Override
    protected RawResultFactory getRawResultFactory() {
      return RawResultFactory.NORMAL_EXIT;
    }
    
    @Override
    protected ProvablyUnassignedResult processRawResult(
        final IRNode node, final AssignedVars lattice, final Assigned[] rawResult) {
      return new ProvablyUnassignedResult(lattice, rawResult);
    }
    
    @Override
    protected ProvablyUnassignedQuery newSubAnalysisQuery(final Delegate<ProvablyUnassignedQuery, ProvablyUnassignedResult, Assigned[], AssignedVars> d) {
      return new ProvablyUnassignedQuery(d);
    }
  }
  
  public static final class ProvablyUnassignedResult {
    private final AssignedVars lattice;
    private final Assigned[] rawResult;
    
    public ProvablyUnassignedResult(final AssignedVars lat, final Assigned[] raw) {
      lattice = lat;
      rawResult = raw;
    }
    
    public boolean isProvableUnassigned(final IRNode varDecl) {
      final int index = lattice.indexOf(varDecl);
      if (index == -1) {
        return false;
      } else {
        return rawResult[index] == Assigned.PROVABLY_UNASSIGNED;
      }
    }
    
    @Override
    public String toString() {
      return lattice.toString(rawResult);
    }
  }
  
  
  
  public DefiniteAssignment(final IBinder b) {
    super(b);
  }

  
  
  @Override
  protected JavaForwardAnalysis<Assigned[], AssignedVars> createAnalysis(final IRNode flowUnit) {
    // Collect the non-final local variables that don't have an initializer
    final List<IRNode> blankVars = new ArrayList<IRNode>();
    final LocalVariableDeclarations lvd = LocalVariableDeclarations.getDeclarationsFor(flowUnit);
    for (final IRNode varOrParamDecl : lvd.getLocal()) {
      if (VariableDeclarator.prototype.includes(varOrParamDecl)) {
        if (!JavaNode.getModifier(varOrParamDecl, JavaNode.FINAL) &&
            NoInitialization.prototype.includes(VariableDeclarator.getInit(varOrParamDecl))) {
          blankVars.add(varOrParamDecl);
        }
      }
    }

    final AssignedVars l = AssignedVars.create(blankVars);
    final Transfer t = new Transfer(binder, l, 0);
    return new JavaForwardAnalysis<Assigned[], AssignedVars>("Definitely Assigned", l, t, DebugUnparser.viewer);
  }


  
  private final class Transfer extends JavaForwardTransfer<AssignedVars, Assigned[]> {
    public Transfer(final IBinder binder, final AssignedVars lattice, final int floor) {
      super(binder, lattice, new SubAnalysisFactory());
    }


    
    @Override
    public Assigned[] transferComponentSource(final IRNode node) {
      // Everything is Provably Unassigned at the start
      return lattice.getEmptyValue();
    }
    
    @Override
    protected Assigned[] transferAssignment(
        final IRNode node, final Assigned[] value) {
      if (!lattice.isNormal(value)) return value;
      
      final Operator op = tree.getOperator(node);
      final IRNode target = ((AssignmentInterface) op).getTarget(node);
      if (VariableUseExpression.prototype.includes(target)) {
        final IRNode varOrParamDecl = binder.getBinding(target);
        if (VariableDeclarator.prototype.includes(varOrParamDecl)) {
          return lattice.replaceValue(value, varOrParamDecl, Assigned.PROVABLY_ASSIGNED);
        }
      }
      return value;
    }
    
    /* Don't care about initialization because we are only tracking variables
     * that we know don't have initializers.
     */
  }
  
  
  private final class SubAnalysisFactory extends AbstractCachingSubAnalysisFactory<AssignedVars, Assigned[]> {
    @Override
    protected JavaForwardAnalysis<Assigned[], AssignedVars> realCreateAnalysis(
        final IRNode caller, final IBinder binder,
        final AssignedVars lattice,
        final Assigned[] initialValue,
        final boolean terminationNormal) {
      final Transfer t = new Transfer(binder, lattice, 0);
      return new JavaForwardAnalysis<Assigned[], AssignedVars>(
          "sub analysis", lattice, t, DebugUnparser.viewer);
    }
  }


  
  @Override
  public IBinder getBinder() {
    return binder;
  }

  @Override
  public void clearCaches() {
    // do nothing
  }



  public ProvablyUnassignedQuery getProvablyUnassignedQuery(final IRNode flowUnit) {
    return new ProvablyUnassignedQuery(getAnalysisThunk(flowUnit));
  }
}
