package com.surelogic.analysis.bca.uwm;

import edu.cmu.cs.fluid.control.Component.WhichPort;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.analysis.AnalysisQuery;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.AssignmentInterface;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.ImmutableSet;
import edu.uwm.cs.fluid.control.ForwardAnalysis;
import edu.uwm.cs.fluid.java.analysis.IntraproceduralAnalysis;

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

public class BindingContextAnalysis extends IntraproceduralAnalysis<ImmutableSet<IRNode>[], BindingContext, BindingContextAnalysis.Analysis> {
  public final class Query implements AnalysisQuery<ImmutableSet<IRNode>> {
    private final Analysis analysis;
    private final BindingContext bc;
    
    private Query(final Analysis a) {
      analysis = a;
      bc = a.getLattice();
    }
    
    public Query(final IRNode flowUnit) {
      this(getAnalysis(flowUnit));
    }
    
    public ImmutableSet<IRNode> getResultFor(final IRNode expr) {
      return bc.expressionObjects(
          analysis.getAfter(expr, WhichPort.NORMAL_EXIT), expr);
    }

    public Query getSubAnalysisQuery(final IRNode caller) {
      final Analysis sub = analysis.getSubAnalysis();
      if (sub == null) {
        throw new UnsupportedOperationException();
      } else {
        return new Query(sub);
      }
    }

    public boolean hasSubAnalysisQuery(final IRNode caller) {
      return analysis.getSubAnalysis() != null;
    }
  }
  
  

  public static final class Analysis extends ForwardAnalysis<ImmutableSet<IRNode>[], BindingContext, Transfer> {
    private Analysis(
        final String name, final BindingContext bc, final Transfer t) {
      super(name, bc, t, DebugUnparser.viewer);
    }
    
    public Analysis getSubAnalysis() {
      return trans.getSubAnalysis();
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
  public Analysis createAnalysis(final IRNode flowUnit) {
    final BindingContext bc = BindingContext.createForFlowUnit(ignorePrimitives, flowUnit, binder);
    return new Analysis("BCA", bc, new Transfer(binder, bc));
  }

  /**
   * Get an query object tailored to a specific flow unit.
   */
  public Query getExpressionObjectsQuery(final IRNode flowUnit) {
    return new Query(flowUnit);
  }


  
  private static final class Transfer extends edu.uwm.cs.fluid.java.control.JavaForwardTransfer<BindingContext, ImmutableSet<IRNode>[]> {
    /**
     * We cache the subanalysis we create so that both normal and abrupt paths
     * are stored in the same analysis. Plus this puts more force behind an
     * assumption made by
     * {@link JavaTransfer#runClassInitializer(IRNode, IRNode, T, boolean)}.
     * 
     * <p>
     * <em>Warning: reusing analysis objects won't work if we have smart worklists.</em>
     */
    private Analysis subAnalysis = null;

    
    
    public Transfer(final IBinder binder, final BindingContext lattice) {
      super(binder, lattice);
      // TODO Auto-generated constructor stub
    }
    
    
    
    public Analysis getSubAnalysis() {
      return subAnalysis;
    }



    @Override
    protected Analysis createAnalysis(IRNode caller,
        IBinder binder, ImmutableSet<IRNode>[] initialValue, boolean terminationNormal) {
      if (subAnalysis == null) {
        subAnalysis = new Analysis("BCA (subanalysis)", lattice,
            new Transfer(binder, lattice));
      }
      return subAnalysis;
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
}
