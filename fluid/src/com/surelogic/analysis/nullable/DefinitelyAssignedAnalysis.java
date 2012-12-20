package com.surelogic.analysis.nullable;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.surelogic.analysis.IBinderClient;
import com.surelogic.util.IThunk;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.analysis.SimplifiedJavaFlowAnalysisQuery;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.AssignmentInterface;
import edu.cmu.cs.fluid.java.operator.ConstructorCall;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.NoInitialization;
import edu.cmu.cs.fluid.java.operator.ThisExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableDeclarators;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.tree.Operator;
import edu.uwm.cs.fluid.java.control.AbstractCachingSubAnalysisFactory;
import edu.uwm.cs.fluid.java.control.IJavaFlowAnalysis;
import edu.uwm.cs.fluid.java.control.JavaForwardAnalysis;
import edu.uwm.cs.fluid.java.control.JavaForwardTransfer;
import edu.uwm.cs.fluid.java.analysis.IntraproceduralAnalysis;


/**
 * Determines if a field is definitely assigned by the constructor.
 */
public final class DefinitelyAssignedAnalysis extends IntraproceduralAnalysis<Assigned[], AssignedVars, JavaForwardAnalysis<Assigned[], AssignedVars>> implements IBinderClient {
  public final class NotDefinitelyAssignedQuery extends SimplifiedJavaFlowAnalysisQuery<NotDefinitelyAssignedQuery, Set<IRNode>, Assigned[], AssignedVars> {
    public NotDefinitelyAssignedQuery(final IThunk<? extends IJavaFlowAnalysis<Assigned[], AssignedVars>> thunk) {
      super(thunk);
    }
    
    private NotDefinitelyAssignedQuery(final Delegate<NotDefinitelyAssignedQuery, Set<IRNode>, Assigned[], AssignedVars> d) {
      super(d);
    }
    
    @Override
    protected RawResultFactory getRawResultFactory() {
      return RawResultFactory.NORMAL_EXIT;
    }

    @Override
    protected Set<IRNode> processRawResult(
        final IRNode expr, final AssignedVars lattice, final Assigned[] rawResult) {
      final Set<IRNode> result = new HashSet<IRNode>();
      for (int i = 0; i < rawResult.length - 1; i++) {
        if (rawResult[i] == Assigned.UNASSIGNED) result.add(lattice.getKey(i));
      }
      return result;
    }

    @Override
    protected NotDefinitelyAssignedQuery newSubAnalysisQuery(final Delegate<NotDefinitelyAssignedQuery, Set<IRNode>, Assigned[], AssignedVars> d) {
      return new NotDefinitelyAssignedQuery(d);
    }
  }
  
  
  
  public DefinitelyAssignedAnalysis(final IBinder b) {
    super(b);
  }

  @Override
  protected JavaForwardAnalysis<Assigned[], AssignedVars> createAnalysis(final IRNode flowUnit) {
    // Track all the non-final fields declared in the class
    // TODO: Just the @NonNull ones
    final List<IRNode> nonFinalFields = new ArrayList<IRNode>(10);
    for (final IRNode fdecl : VisitUtil.getClassFieldDecls(VisitUtil.getEnclosingType(flowUnit))) {
      if (!TypeUtil.isFinal(fdecl)) {
        for (final IRNode vd : VariableDeclarators.getVarIterator(FieldDeclaration.getVars(fdecl))) {
          nonFinalFields.add(vd);
        }
      }
    }
    final AssignedVars l = AssignedVars.create(nonFinalFields);
    final Transfer t = new Transfer(binder, l, 0);
    return new JavaForwardAnalysis<Assigned[], AssignedVars>("Definitely Assigned", l, t, DebugUnparser.viewer);
  }


  
  private static final class Transfer extends JavaForwardTransfer<AssignedVars, Assigned[]> {
    public Transfer(final IBinder binder, final AssignedVars lattice, final int floor) {
      super(binder, lattice, new SubAnalysisFactory());
    }


    
    @Override
    public Assigned[] transferComponentSource(final IRNode node) {
      // Everything is not definitely assigned at the start
      return lattice.getEmptyValue();
    }
    
    @Override
    protected Assigned[] transferAssignment(
        final IRNode node, final Assigned[] value) {
      if (!lattice.isNormal(value)) return value;
      
      final Operator op = tree.getOperator(node);
      final IRNode target = ((AssignmentInterface) op).getTarget(node);
      if (FieldRef.prototype.includes(target)) {
        final IRNode field = binder.getBinding(target);
        // TODO: Check that the field is @NOnNull
        if (!TypeUtil.isFinal(field)) { // lattice doesn't contain the final fields!
          return lattice.replaceValue(value, field, Assigned.ASSIGNED);
        }
      }
      return value;
    }
    
    @Override
    protected Assigned[] transferInitialization(
        final IRNode node, final Assigned[] value) {
      if (!lattice.isNormal(value)) return value;
      
      final IRNode p = tree.getParent(tree.getParent(node));
      if (FieldDeclaration.prototype.includes(tree.getOperator(p))) {
        // TODO: Check that the field is @NonNull
        if (!TypeUtil.isFinal(p)) { // lattice doesn't contain the final fields!
          final IRNode initializer = VariableDeclarator.getInit(node);
          if (!NoInitialization.prototype.includes(initializer)) {
            return lattice.replaceValue(value, node, Assigned.ASSIGNED);
          }
        }
      }
      return value;
    }
    
    @Override
    protected Assigned[] transferCall(
        final IRNode call, final boolean flag, final Assigned[] value) {
      if (!lattice.isNormal(value)) return value;
      
      /* See if this is a "this(...)" call */
      if (ConstructorCall.prototype.includes(call) && 
          ThisExpression.prototype.includes(ConstructorCall.getObject(call))) {
        if (!flag) {
          /* After abrupt termination we do not know what the state of the
           * fields is.
           */
          return lattice.getEmptyValue();
        } else {
          /* After "this(...)" we get to assume that all the fields are 
           * definitely assigned.
           */
          return lattice.getAllAssigned();
        }
      } else {
        /* Normal method and super(...) calls don't affect the state.
         * (Well not true, we rely on the analysis engine to take the path
         * through the field initializations when a super(...) call is 
         * encountered, but the call itself is not interesting to us.)
         */
        return value;
      }
    }

  }
  
  
  private static final class SubAnalysisFactory extends AbstractCachingSubAnalysisFactory<AssignedVars, Assigned[]> {
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



  public NotDefinitelyAssignedQuery getNotDefinitelyAssignedQuery(final IRNode flowUnit) {
    return new NotDefinitelyAssignedQuery(getAnalysisThunk(flowUnit));
  }
}
