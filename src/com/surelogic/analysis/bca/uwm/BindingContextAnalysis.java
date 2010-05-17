package com.surelogic.analysis.bca.uwm;

import com.surelogic.util.IThunk;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.analysis.AbstractJavaFlowAnalysisQuery;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.AssignmentInterface;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.ImmutableSet;
import edu.uwm.cs.fluid.java.analysis.IntraproceduralAnalysis;
import edu.uwm.cs.fluid.java.control.AbstractCachingSubAnalysisFactory;
import edu.uwm.cs.fluid.java.control.IJavaFlowAnalysis;
import edu.uwm.cs.fluid.java.control.JavaForwardAnalysis;
import edu.uwm.cs.fluid.java.control.JavaForwardTransfer;

/**
 * This class tracks bindings of locals within a method. It associates a
 * binding context with each control-flow point. This context binds locals to a
 * set of IR nodes. the following kinds of nodes can appear in the sets:
 * <ul>
 * <li>parameter declarations, which represent their initial values.
 * <li>constructor or method calls that return unique objects, which represent
 * any instance of its evaluation during the execution of the method.
 * <li>accesses of unique fields which represents any evaluation of the
 * given FieldRef expression IRNode.
 * </ul>
 */

public class BindingContextAnalysis extends IntraproceduralAnalysis<ImmutableSet<IRNode>[], BindingContext, JavaForwardAnalysis<ImmutableSet<IRNode>[], BindingContext>> {
  public final class Query extends AbstractJavaFlowAnalysisQuery<Query, ImmutableSet<IRNode>, ImmutableSet<IRNode>[], BindingContext> {
    private Query(final Delegate<Query, ImmutableSet<IRNode>, ImmutableSet<IRNode>[], BindingContext> d) {
      super(d);
    }

    public Query(final IThunk<? extends IJavaFlowAnalysis<ImmutableSet<IRNode>[], BindingContext>> thunk) {
      super(thunk);
    }

    @Override
    protected RawResultFactory getRawResultFactory() {
      return RawResultFactory.NORMAL_EXIT;
    }

    
    
    @Override
    protected Query newSubAnalysisQuery(final Delegate<Query, ImmutableSet<IRNode>, ImmutableSet<IRNode>[], BindingContext> d) {
      return new Query(d);
    }

    @Override
    protected ImmutableSet<IRNode> processRawResult(
        final IRNode expr, final BindingContext lattice, 
        final ImmutableSet<IRNode>[] rawResult) {
      return lattice.expressionObjects(rawResult, expr);
    }
  }
  
  
  
  /**
   * Whether primitively typed variables should be tracked.  Normally we don't
   * care about them, but we could used BCA as a use-def analysis, and then
   * we might care about primitively typed variables.
   */
  private final boolean ignorePrimitives;
  
  
  
  public BindingContextAnalysis(final IBinder b, final boolean ignoreP) {
    super(b);
    ignorePrimitives = ignoreP;
  }
  
  @Override
  public JavaForwardAnalysis<ImmutableSet<IRNode>[], BindingContext> createAnalysis(final IRNode flowUnit) {
    final BindingContext bc = BindingContext.createForFlowUnit(ignorePrimitives, flowUnit, binder);
    return new JavaForwardAnalysis<ImmutableSet<IRNode>[], BindingContext>("BCA", bc, new Transfer(binder, bc), DebugUnparser.viewer);
  }

  /**
   * Get an query object tailored to a specific flow unit.
   */
  public Query getExpressionObjectsQuery(final IRNode flowUnit) {
    return new Query(getAnalysisThunk(flowUnit));
  }

  
  
  private static final class Transfer extends JavaForwardTransfer<BindingContext, ImmutableSet<IRNode>[]> {
    public Transfer(final IBinder binder, final BindingContext lattice) {
      super(binder, lattice, new SubAnalysisFactory());
    }

    
    
    public ImmutableSet<IRNode>[] transferComponentSource(final IRNode node) {
      return lattice.getInitialValue();
    }
    
    @Override
    public ImmutableSet<IRNode>[] transferAssignment(
        final IRNode node, final ImmutableSet<IRNode>[] before) {
//      System.out.println(MessageFormat.format("*********************\n***** transferAssignment(\"{0}\")", DebugUnparser.toString(node)));
//      System.out.println("before lattice:");
//      System.out.println(lattice.toString(before));
      
      // Be strict
      if (!lattice.isNormal(before)) {
//        System.out.println("Lattice is not normal, skip");
        return before;
      }
      
      ImmutableSet<IRNode>[] out = before;
      
      final AssignmentInterface op = (AssignmentInterface) tree.getOperator(node);
      final IRNode lhs = op.getTarget(node);
      final Operator lhsOp = tree.getOperator(lhs);
      if (VariableUseExpression.prototype.includes(lhsOp)) {
        final IRNode decl = binder.getBinding(lhs);
        final ImmutableSet<IRNode> rhsObjects = lattice.expressionObjects(before, op.getSource(node));
        out = lattice.updateDeclaration(before, decl, rhsObjects);
      }
      
//      System.out.println("after lattice:");
//      System.out.println(lattice.toString(out));
      
      return out;
    }
    
    @Override
    public ImmutableSet<IRNode>[] transferInitialization(
        final IRNode node, final ImmutableSet<IRNode>[] before) {
//      System.out.println(MessageFormat.format("*********************\n***** transferInitialization(\"{0}\")", DebugUnparser.toString(node)));
//      System.out.println("before lattice:");
//      System.out.println(lattice.toString(before));

      // Be strict
      if (!lattice.isNormal(before)) {
//        System.out.println("Lattice is not normal, skip");
        return before;
      }
      
      ImmutableSet<IRNode>[] out = before;
      
      if (VariableDeclarator.prototype.includes(tree.getOperator(node))) {
        // Make sure it's NOT a field initialization
        if (!FieldDeclaration.prototype.includes(
            JJNode.tree.getOperator(
                JJNode.tree.getParentOrNull(
                    JJNode.tree.getParentOrNull(node))))) {
          final ImmutableSet<IRNode> initObjects =
            lattice.expressionObjects(before, VariableDeclarator.getInit(node));
          out = lattice.updateDeclaration(before, node, initObjects);
        }
      }
      
//      System.out.println("after lattice:");
//      System.out.println(lattice.toString(out));

      return out;
    }
  }

  

  private static final class SubAnalysisFactory extends AbstractCachingSubAnalysisFactory<BindingContext, ImmutableSet<IRNode>[]> {
    @Override
    protected JavaForwardAnalysis<ImmutableSet<IRNode>[], BindingContext> realCreateAnalysis(
        final IRNode caller, final IBinder binder,
        final BindingContext lattice, final ImmutableSet<IRNode>[] initialValue,
        final boolean terminationNormal) {
      return new JavaForwardAnalysis<ImmutableSet<IRNode>[], BindingContext>(
          "BCA (subanalysis)", lattice, new Transfer(binder, lattice), DebugUnparser.viewer);
    }
    
  }
}
