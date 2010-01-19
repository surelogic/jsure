/*$Header: /cvs/fluid/fluid/src/edu/uwm/cs/fluid/java/analysis/LiveVariableAnalysis.java,v 1.9 2007/08/23 22:04:15 boyland Exp $*/
package edu.uwm.cs.fluid.java.analysis;

import java.io.File;
import java.io.IOException;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IndependentIRNode;
import edu.cmu.cs.fluid.ir.SlotUndefinedException;
import edu.uwm.cs.fluid.java.control.JavaBackwardTransfer;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.AssignmentInterface;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;
import edu.cmu.cs.fluid.util.ImmutableSet;
import edu.uwm.cs.fluid.util.UnionLattice;
import edu.uwm.cs.fluid.control.BackwardAnalysis;


/**
 * The simplest flow analysis -- live variables.
 * @author boyland
 */
public class LiveVariableAnalysis extends BackwardAnalysis<ImmutableSet<IRNode>, UnionLattice<IRNode>, LiveVariableAnalysis.Transfer> {
  /**
   * In order to keep our analysis transfer functions strict, 
   * we put this node in when initializing.  It should be ignored when getting information
   * about the nodes.
   */
  public static final IRNode ignoreMe = new IndependentIRNode();
  static {
    JJNode.setInfo(ignoreMe,"<ignore>");
  }
  
  /**
   * Create a LiveVariable analysis
   * @param l
   * @param t
   */
  private LiveVariableAnalysis(UnionLattice<IRNode> l, Transfer t) {
    super("Live variables",l,t, DebugUnparser.viewer);
  }

  public static LiveVariableAnalysis create(IBinder b) {
    UnionLattice<IRNode> l = new UnionLattice<IRNode>() {

      @Override
      public String toString(ImmutableSet<IRNode> v) {
        StringBuilder sb = null;
        for (IRNode d : v) {
          if (sb == null) sb = new StringBuilder("{");
          else sb.append(',');
          //if (d == ignoreMe) continue;
          try {
            sb.append(JJNode.getInfo(d));
          } catch (SlotUndefinedException e) {
            sb.append(d);
          }
        }
        if (sb == null) return "{}";
        sb.append('}');
        return sb.toString();
      }
      
    };
    Transfer t = new Transfer(l,b);
    return new LiveVariableAnalysis(l,t);
  }
  
  public static class Transfer extends JavaBackwardTransfer<UnionLattice<IRNode>,ImmutableSet<IRNode>> {
    /**
     * We cache the subanalysis we create so that both normal and abrupt paths
     * are stored in the same analysis. Plus this puts more force behind an
     * assumption made by
     * {@link JavaTransfer#runClassInitializer(IRNode, IRNode, T, boolean)}.
     * 
     * <p>
     * <em>Warning: reusing analysis objects won't work if we have smart worklists.</em>
     */
    private LiveVariableAnalysis subAnalysis = null;

    public ImmutableSet<IRNode> transferConditional(IRNode node, boolean flag,
        ImmutableSet<IRNode> after) {
      // System.out.println("Doing transfer conditional on " + DebugUnparser.toString(node));
      return after;
    }

    private Transfer(UnionLattice<IRNode> l, IBinder b) {
      super(b,l);
    }
    @Override 
    protected ImmutableSet<IRNode> transferUse(IRNode node, Operator op, ImmutableSet<IRNode> usedAfter) {
      if (op instanceof VariableUseExpression) {
        IRNode decl = binder.getBinding(node);
        return usedAfter.addCopy(decl);
      } else {
        return usedAfter;
      }
    }
    @Override 
    protected ImmutableSet<IRNode> transferAssignment(IRNode assign, ImmutableSet<IRNode> usedAfter) {
      IRNode target = ((AssignmentInterface)tree.getOperator(assign)).getTarget(assign);
      if (VariableUseExpression.prototype.includes(tree.getOperator(target))) {
        IRNode decl = binder.getBinding(target);
        return usedAfter.removeCopy(decl);
      } else {
        return usedAfter;
      }
    }
    @Override 
    protected ImmutableSet<IRNode> transferInitialization(IRNode init, ImmutableSet<IRNode> usedAfter) {
      if (tree.getOperator(init) instanceof VariableDeclarator) {
        return usedAfter.removeCopy(init);
      } else {
        return usedAfter;
      }
    }
    
    /**
     * In order to preserv strictness of the analysis functions,
     * we can't return the empty set here.  Instead we return a set with
     * one, uninteresting node, in it.
     */
    public ImmutableSet<IRNode> transferComponentSink(IRNode node, boolean normal) {
      LOG.fine("initializing live variables from end");
      return new ImmutableHashOrderSet<IRNode>(new IRNode[]{ignoreMe});
    }
    
    /* (non-Javadoc)
     * @see edu.uwm.cs.fluid.java.control.JavaTransfer#createAnalysis(edu.cmu.cs.fluid.java.bind.IBinder)
     */
    @Override
    protected LiveVariableAnalysis createAnalysis(IBinder binder, boolean terminationNormal) {
      if (subAnalysis == null) {
        subAnalysis = new LiveVariableAnalysis(
            lattice, new Transfer(lattice, binder));
      }
      return subAnalysis;
    }
  }
}

class TestLiveVariables extends TestFlowAnalysis<ImmutableSet<IRNode>, UnionLattice<IRNode>, LiveVariableAnalysis> {

  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.java.analysis.TestFlowAnalysis#createAnalysis(edu.cmu.cs.fluid.java.bind.IBinder)
   */
  @Override
  protected LiveVariableAnalysis createAnalysis(IRNode ignored, IBinder binder) {
    return LiveVariableAnalysis.create(binder);
  }
  
  public static void main(String[] files) throws IOException {
    TestLiveVariables test = new TestLiveVariables();
    for (String file : files) {
      IRNode cu = test.addCompilatioUnit(new File(file));
      test.analyzeCompilationUnit(cu);
    }
  }  
}
