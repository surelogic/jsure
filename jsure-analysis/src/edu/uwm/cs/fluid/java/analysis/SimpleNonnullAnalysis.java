package edu.uwm.cs.fluid.java.analysis;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.analysis.IBinderClient;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.util.IThunk;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.analysis.SimplifiedJavaFlowAnalysisQuery;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.CallInterface;
import edu.cmu.cs.fluid.java.operator.CatchClause;
import edu.cmu.cs.fluid.java.operator.DimExprs;
import edu.cmu.cs.fluid.java.operator.InstanceOfExpression;
import edu.cmu.cs.fluid.java.operator.NullLiteral;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.SyntaxTreeInterface;
import edu.cmu.cs.fluid.util.ImmutableList;
import edu.cmu.cs.fluid.util.ImmutableSet;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;
import edu.uwm.cs.fluid.java.control.AbstractCachingSubAnalysisFactory;
import edu.uwm.cs.fluid.java.control.IJavaFlowAnalysis;
import edu.uwm.cs.fluid.java.control.JavaForwardAnalysis;
import edu.uwm.cs.fluid.java.control.LatticeDelegatingJavaEvaluationTransfer;
import edu.uwm.cs.fluid.util.*;


/**
 * A class that does intra-procedural non-null checking.  It doesn't
 * use any information from outside the method (no annotations, ignores field, etc).
 * If one wants a smarter analysis, one should use permission analysis which takes
 * into account annotations as well as raw annotations (needed before one can trust
 * the initialization of final variables).
 * @author boyland
 */
public final class SimpleNonnullAnalysis extends IntraproceduralAnalysis<
    SimpleNonnullAnalysis.Value, SimpleNonnullAnalysis.Lattice,
    JavaForwardAnalysis<SimpleNonnullAnalysis.Value, SimpleNonnullAnalysis.Lattice>> implements IBinderClient {
  public final class Query extends SimplifiedJavaFlowAnalysisQuery<Query, ImmutableSet<IRNode>, Value, Lattice> {
    public Query(final IThunk<? extends IJavaFlowAnalysis<Value, Lattice>> thunk) {
      super(thunk);
    }
    
    private Query(final Delegate<Query, ImmutableSet<IRNode>, Value, Lattice> d) {
      super(d);
    }

    @Override
    protected RawResultFactory getRawResultFactory() {
      return RawResultFactory.ENTRY;
    }


    
    @Override
    protected Query newSubAnalysisQuery(final Delegate<Query, ImmutableSet<IRNode>, Value, Lattice> d) {
      return new Query(d);
    }


    
    @Override
    protected ImmutableSet<IRNode> processRawResult(final IRNode expr,
        final Lattice lattice, final Value rawResult) {
      return rawResult.second();
    }    
  }
  
  
  
  private static final boolean debug = false;
  @SuppressWarnings("unused")
  private static final Logger LOG = SLLogger.getLogger("FLUID.control.java.simpleNonNull");

  
  
  public SimpleNonnullAnalysis(IBinder b) {
    super(b);
  }

  @Override
  protected JavaForwardAnalysis<Value, Lattice> createAnalysis(IRNode flowUnit) {
    final Lattice l = new Lattice();
    final Transfer t = new Transfer(binder,l, 0);
    return new JavaForwardAnalysis<Value, Lattice>("Java.Nonnull", l, t, DebugUnparser.viewer);
  }

  public Query getNonnullBeforeQuery(final IRNode flowUnit) {
    return new Query(getAnalysisThunk(flowUnit));
  }
  

  public static enum NullInfo {
    IMPOSSIBLE, NOTNULL, NULL, MAYBENULL;
  }
  
  public static final class NullLattice extends AbstractLattice<NullInfo> {
    private static final NullLattice instance = new NullLattice();

    public static NullLattice getInstance() {
      return instance;
    }

    @Override
    public NullInfo bottom() {
      return NullInfo.IMPOSSIBLE;
    }

    @Override
    public NullInfo join(NullInfo v1, NullInfo v2) {
      return NullInfo.values()[v1.ordinal() | v2.ordinal()];
    }

    @Override
    public boolean lessEq(NullInfo v1, NullInfo v2) {
      return (v1.ordinal() | v2.ordinal()) == v2.ordinal();
    }

    @Override
    public NullInfo meet(NullInfo v1, NullInfo v2) {
      return NullInfo.values()[v1.ordinal() & v2.ordinal()];
    }

    @Override
    public NullInfo top() {
      return NullInfo.MAYBENULL;
    }
    
  }
  

  
  public static final class Value extends EvaluationStackLattice.EvalPair<
      NullInfo, ImmutableSet<IRNode>> {
    public Value(final ImmutableList<NullInfo> v1, final ImmutableSet<IRNode> v2) {
      super(v1, v2);
    }
  }
  
  public static final class Lattice extends EvaluationStackLattice<
      NullInfo, ImmutableSet<IRNode>,
      NullLattice, IntersectionLattice<IRNode>,
      Value> { 
    private Lattice() {
      super(NullLattice.getInstance(),
          new IntersectionLattice<IRNode>(){
        @Override
        public String toString(ImmutableSet<IRNode> v) {
          StringBuilder sb = new StringBuilder();
          if (v.isInfinite()) {
            sb.append('~');
            v = v.invertCopy();
          }
          sb.append('{');
          boolean first = true;
          for (IRNode n : v) {
            if (first) first = false; else sb.append(',');
            try {
              sb.append(JJNode.getInfo(n));
            } catch (RuntimeException e) {
              sb.append(n);
            }
          }
          sb.append('}');
          return sb.toString();
        }
      });
    }

    @Override
    protected Value newPair(final ImmutableList<NullInfo> v1, final ImmutableSet<IRNode> v2) {
      return new Value(v1, v2);
    }

    @Override
    public NullInfo getAnonymousStackValue() {
      return NullInfo.MAYBENULL;
    }

    public Value addNonNull(final Value val, final IRNode var) {
      return newPair(val.first(), val.second().addCopy(var));
    }
    
    public Value removeNonNull(final Value val, final IRNode var) {
      return newPair(val.first(), val.second().removeCopy(var));
    }
    
    public boolean mustBeNonNull(final Value val, final IRNode var) {
      return val.second().contains(var);
    }
  }

  
  
  private static final class Transfer extends LatticeDelegatingJavaEvaluationTransfer<Lattice, Value, NullInfo> {
    private static final NullLattice nullLattice = NullLattice.getInstance();
    private static final SyntaxTreeInterface tree = JJNode.tree; 
    
    
    
    public Transfer(IBinder binder, Lattice lattice, int floor) {
      super(binder, lattice, new SubAnalysisFactory(), floor);
    }


    
    @Override
    public Value transferComponentSource(final IRNode node) {
      Set<IRNode> caughtVars = null;
      for (IRNode n : tree.bottomUp(node)) {
        if (tree.getOperator(n) instanceof CatchClause) {
          if (caughtVars == null) caughtVars = new HashSet<IRNode>();
          caughtVars.add(CatchClause.getParam(n));
        }
      }      
      ImmutableHashOrderSet<IRNode> initSet = ImmutableHashOrderSet.<IRNode>emptySet();
      if (caughtVars != null) {
        initSet = new ImmutableHashOrderSet<IRNode>(caughtVars);
      }
      return new Value(ImmutableList.<NullInfo>nil(), initSet);
    }

    /*
     * In order to make transfer functions strict, we check at the beginning of
     * each whether we have bottom or not.
     */

    @Override
    protected Value transferAllocation(final IRNode node, final Value val) {
      return push(val,NullInfo.NOTNULL);
    }

    @Override
    protected Value transferArrayCreation(final IRNode node, Value val) {
      if (!lattice.isNormal(val)) return val;
      if (tree.getOperator(node) instanceof DimExprs) {
        val = pop(val, tree.numChildren(node));
      }
      return push(val, NullInfo.NOTNULL);
    }

    @Override
    protected Value transferAssignVar(final IRNode use, final Value val) {
      IRNode var = binder.getIBinding(use).getNode();
      return transferSetVar(var, val);
    }

    /**
     * Transfer an assignment of a variable to what's on the stack.  Leave stack alone.
     * @param var
     * @param val
     * @return
     */
    @SuppressWarnings("unused") // for the "debug" flag
    private Value transferSetVar(final IRNode var, final Value val) {
      final NullInfo ni = lattice.peek(val);
      
      if (val.second().contains(var)) { // Variable is coming in as NONNULL
        if (!nullLattice.lessEq(ni, NullInfo.NOTNULL)) { // Value might be null
          return lattice.removeNonNull(val, var);
        }
        if (debug && LOG.isLoggable(Level.FINE)) LOG.fine(JJNode.getInfo(var) + " is still non null after being assigned " + ni);
        // otherwise, do nothing: not null before, not null afterwards
      } else { // Variable is coming in as possibly null
        if (nullLattice.lessEq(ni, NullInfo.NOTNULL)) { // Value is not null
          return lattice.addNonNull(val, var);
        }
        if (debug && LOG.isLoggable(Level.FINE)) LOG.fine(JJNode.getInfo(var) + " is still maybe null after being assigned " + ni);
        // do nothing : maybe null before, maybe null afterwards
      }
      return val;
    }

    @Override
    protected Value transferBox(final IRNode expr, final Value val) {
      if (!lattice.isNormal(val)) return val;
      return lattice.push(lattice.pop(val), NullInfo.NOTNULL);
    }

    @Override
    protected Value transferDefaultInit(final IRNode node, final Value val) {
      if (!lattice.isNormal(val)) return val;
      return push(val, NullInfo.NULL);
    }

    @Override
    protected Value transferEq(
        final IRNode node, final boolean flag, final Value val) {
      if (!lattice.isNormal(val)) return val;

      Value newValue = val;
      final NullInfo ni2 = lattice.peek(newValue);
      newValue = lattice.pop(newValue);
      final NullInfo ni1 = lattice.peek(newValue);
      newValue = lattice.pop(newValue);
      newValue = lattice.push(newValue, NullInfo.MAYBENULL);
      
      // don't pop the second: we don't care what the top of the stack has for primitives
      // if the condition is impossible, we propagate bottom
      if (nullLattice.meet(ni1, ni2) == nullLattice.bottom()) {
        if (flag) return null; // else fall through to end
      } else
      // if the comparison is guaranteed true, we propagate bottom for false:
      if (nullLattice.lessEq(ni1, NullInfo.NULL) && nullLattice.lessEq(ni2, NullInfo.NULL)) {
        if (!flag) return null; // else fall through to end
      } else
      // if we have an *inequality* comparison with null:
      if (!flag) {
        if (nullLattice.lessEq(ni1, NullInfo.NULL)) {
          final IRNode n = tree.getChild(node, 1); // don't use EqExpression methods because this transfer is called on != also
          if (VariableUseExpression.prototype.includes(n)) {
            final IRNode var = binder.getIBinding(n).getNode();
            newValue = lattice.addNonNull(newValue, var);
          }
        } else if (NullLiteral.prototype.includes(tree.getChild(node,1))) {
          // NB: it would be a little more precise if we checked for ni2 being under NULL
          // than what we do here but then we must check for assignments of the variable
          // so that we don't make a wrong conclusion for "x == (x = null)" which, even
          // if false, still leaves x null.  The first branch is is OK because "(x = null) == x"
          // doesn't have the same problem.
          final IRNode n = tree.getChild(node, 0);
          if (VariableUseExpression.prototype.includes(tree.getOperator(n))) {
            final IRNode var = binder.getIBinding(n).getNode();
            newValue = lattice.addNonNull(newValue, var);
          }
        }
      }
      return newValue;
    }

    @Override
    protected Value transferImplicitArrayCreation(
        final IRNode arrayInitializer, final Value val) {
      return push(val, NullInfo.NOTNULL);
    }

    @Override
    protected Value transferInitializationOfVar(final IRNode node, final Value val) {
      if (!lattice.isNormal(val)) return val;
      
      return pop(transferSetVar(node, val));
    }

    @Override
    protected Value transferInstanceOf(
        final IRNode node, final boolean flag, final Value val) {
      if (!lattice.isNormal(val)) return val;
      
      if (!flag) return val;
      final IRNode n = InstanceOfExpression.getValue(node);
      if (VariableUseExpression.prototype.includes(n)) {
        final IRNode var = binder.getIBinding(n).getNode();
        return lattice.addNonNull(val, var);
      }
      return val;
    }

    @SuppressWarnings("unused") // for the debug flag
    @Override
    protected Value transferIsObject(
        final IRNode n, final boolean flag, final Value val) {
      if (!lattice.isNormal(val)) return val;

      Value newValue = val;
      // need to find the receiver:
      IRNode p = tree.getParent(n);
      if (tree.getOperator(p) instanceof CallInterface) {
        final CallInterface cop = ((CallInterface)tree.getOperator(p));
        int numArgs;
        try {
          numArgs = tree.numChildren(cop.get_Args(p));
        } catch (final CallInterface.NoArgs e) {
          numArgs = 0;
        }
        while (numArgs > 0) {
          newValue = lattice.pop(newValue);
          --numArgs;
        }
      }
      final NullInfo ni = lattice.peek(newValue);
      if (flag && nullLattice.lessEq(ni, NullInfo.NULL)) {
        if (debug && LOG.isLoggable(Level.FINE)) LOG.fine("Since we know " + ni + " is null, we can assume " + DebugUnparser.toString(n) + " cannot be dereferenced.");
        return null; // lattice.bottom();
      }
      if (!flag && nullLattice.lessEq(ni, NullInfo.NOTNULL)) {
        if (debug && LOG.isLoggable(Level.FINE)) LOG.fine("Since we know " + ni + " is not null, we can assume " + DebugUnparser.toString(n) + " won't throw a NPE.");
        return null; //lattice.bottom();
      }
      if (flag && VariableUseExpression.prototype.includes(n)) {
        final IRNode var = binder.getIBinding(n).getNode();
        return lattice.addNonNull(newValue, var);
      }
      return super.transferIsObject(n, flag, val);
    }

    @Override
    protected Value transferLiteral(final IRNode node, final Value val) {
      if (!lattice.isNormal(val)) return val;
      
      final NullInfo ni;
      if (NullLiteral.prototype.includes(node)) {
        ni = NullInfo.NULL;
      } else {
        ni = NullInfo.NOTNULL; // all other literals are not null
      }
      return lattice.push(val, ni);
    }

    @Override
    protected Value transferToString(final IRNode node, final Value val) {
      if (!lattice.isNormal(val)) return val;
      
      if (nullLattice.lessEq(lattice.peek(val), NullInfo.NOTNULL)) return val;
      // otherwise, we can force not null
      return lattice.push(lattice.pop(val), NullInfo.NOTNULL);
    }

    @Override
    protected Value transferUseVar(final IRNode use, final Value val) {
      if (!lattice.isNormal(val)) return val;
      
      final NullInfo ni;
      final IRNode var = binder.getIBinding(use).getNode();
      if (lattice.mustBeNonNull(val, var)) {
        ni = NullInfo.NOTNULL;
      } else {
        ni = NullInfo.MAYBENULL; // all other literals are not null
      }
      return lattice.push(val, ni);
    }
    
    @Override
    protected Value transferUseReceiver(final IRNode use, final Value val) {
      if (!lattice.isNormal(val)) return val;
      
      // Receiver is always non-null
      return lattice.push(val, NullInfo.NOTNULL);
    }
    
    @Override
    protected Value transferUseQualifiedReceiver(
        final IRNode use, final IRNode binding, final Value val) {
      if (!lattice.isNormal(val)) return val;
      
      // Qualified receiver is always non-null
      return lattice.push(val, NullInfo.NOTNULL);
    }
    
    
    @Override
    protected Value transferConcat(final IRNode node, final Value val) {
      if (!lattice.isNormal(val)) return val;
      
      // pop the values of the stack and push a non-null
      Value newValue = lattice.pop(val);
      newValue = lattice.pop(newValue);
      newValue = lattice.push(newValue, NullInfo.NOTNULL);
      return newValue;
    }
  }
  
  
  
  private static final class SubAnalysisFactory extends AbstractCachingSubAnalysisFactory<Lattice, Value> {
    @Override
    protected JavaForwardAnalysis<Value, Lattice> realCreateAnalysis(
        final IRNode caller, final IBinder binder,
        final Lattice lattice,
        final Value initialValue,
        final boolean terminationNormal) {
      final int floor = initialValue.first().size();
      final Transfer t = new Transfer(binder, lattice, floor);
      return new JavaForwardAnalysis<Value, Lattice>("sub analysis", lattice, t, DebugUnparser.viewer);
    }
  }


  
  public static final class Test extends TestFlowAnalysis<Value, Lattice, JavaForwardAnalysis<Value, Lattice>> {
    @Override
    protected JavaForwardAnalysis<Value, Lattice> createAnalysis(IRNode flowUnit, IBinder binder) {
      final Lattice l = new Lattice();
      final Transfer t = new Transfer(binder,l, 0);
      return new JavaForwardAnalysis<Value, Lattice>("nonnll", l, t, DebugUnparser.viewer);
    }
    
    public static void main(String[] args)  {
      System.out.println("Starting.");
      new Test().test(args);
    }
  }



  @Override
  public IBinder getBinder() {
    return binder;
  }

  @Override
  public void clearCaches() {
    clear();
  }
}
