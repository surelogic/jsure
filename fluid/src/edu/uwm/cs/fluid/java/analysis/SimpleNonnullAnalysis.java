package edu.uwm.cs.fluid.java.analysis;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.analysis.IBinderClient;
import com.surelogic.common.Pair;
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
import edu.uwm.cs.fluid.java.control.JavaEvaluationTransfer;
import edu.uwm.cs.fluid.java.control.JavaForwardAnalysis;
import edu.uwm.cs.fluid.util.*;
import edu.uwm.cs.fluid.java.analysis.SimpleNonnullAnalysis.NullInfo;


/**
 * A class that does intra-procedural non-null checking.  It doesn't
 * use any information from outside the method (no annotations, ignores field, etc).
 * If one wants a smarter analysis, one should use permission analysis which takes
 * into account annotations as well as raw annotations (needed before one can trust
 * the initialization of final variables).
 * @author boyland
 */
public final class SimpleNonnullAnalysis extends IntraproceduralAnalysis<Pair<ImmutableList<NullInfo>,ImmutableSet<IRNode>>, SimpleNonnullAnalysis.Lattice, JavaForwardAnalysis<Pair<ImmutableList<NullInfo>,ImmutableSet<IRNode>>, SimpleNonnullAnalysis.Lattice>> implements IBinderClient {
  public final class Query extends SimplifiedJavaFlowAnalysisQuery<Query, ImmutableSet<IRNode>, Pair<ImmutableList<NullInfo>,ImmutableSet<IRNode>>, SimpleNonnullAnalysis.Lattice> {
    public Query(final IThunk<? extends IJavaFlowAnalysis<Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>>, Lattice>> thunk) {
      super(thunk);
    }
    
    private Query(final Delegate<Query, ImmutableSet<IRNode>, Pair<ImmutableList<NullInfo>,ImmutableSet<IRNode>>, SimpleNonnullAnalysis.Lattice> d) {
      super(d);
    }

    @Override
    protected RawResultFactory getRawResultFactory() {
      return RawResultFactory.ENTRY;
    }


    
    @Override
    protected Query newSubAnalysisQuery(final Delegate<Query, ImmutableSet<IRNode>, Pair<ImmutableList<NullInfo>,ImmutableSet<IRNode>>, SimpleNonnullAnalysis.Lattice> d) {
      return new Query(d);
    }


    
    @Override
    protected ImmutableSet<IRNode> processRawResult(final IRNode expr,
        final Lattice lattice,
        final Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> rawResult) {
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
  protected JavaForwardAnalysis<Pair<ImmutableList<NullInfo>,ImmutableSet<IRNode>>, Lattice> createAnalysis(IRNode flowUnit) {
    final Lattice l = new Lattice();
    final Transfer t = new Transfer(binder,l, 0);
    return new JavaForwardAnalysis<Pair<ImmutableList<NullInfo>,ImmutableSet<IRNode>>, Lattice>("Java.Nonnull", l, t, DebugUnparser.viewer);
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

    public NullInfo bottom() {
      return NullInfo.IMPOSSIBLE;
    }

    public NullInfo join(NullInfo v1, NullInfo v2) {
      return NullInfo.values()[v1.ordinal() | v2.ordinal()];
    }

    public boolean lessEq(NullInfo v1, NullInfo v2) {
      return (v1.ordinal() | v2.ordinal()) == v2.ordinal();
    }

    public NullInfo meet(NullInfo v1, NullInfo v2) {
      return NullInfo.values()[v1.ordinal() & v2.ordinal()];
    }

    public NullInfo top() {
      return NullInfo.MAYBENULL;
    }
    
  }
  
  private static Pair<ImmutableList<NullInfo>,ImmutableSet<IRNode>> newPair(ImmutableList<NullInfo> o1, ImmutableSet<IRNode> o2) {
    return new Pair<ImmutableList<NullInfo>,ImmutableSet<IRNode>>(o1,o2);
  }
  
  public static final class Lattice extends PairLattice<ImmutableList<NullInfo>,ImmutableSet<IRNode>> {

    private Lattice() {
      super(new ListLattice<NullLattice,NullInfo>(NullLattice.getInstance()), new IntersectionLattice<IRNode>(){

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
    
    @SuppressWarnings("unchecked")
    public ListLattice<NullLattice,NullInfo> getLL() { 
      return (ListLattice<NullLattice, NullInfo>) lattice1; 
    }
    
    @Override
    @SuppressWarnings("unused")
    public Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> join(
        Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> v1,
        Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> v2) {
      final String s1 = toString(v1);
      final String s2 = toString(v2);

      Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> join = super.join(v1, v2);
      if (isNormal(join)) return join;
      if (join.equals(top())) return join;
      if (join.equals(bottom())) return join;
      System.out.println("Found a non-normal non bottom/top: " + toString(join));
      
      Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> jj = super.join(v1, v2);
      final boolean n = isNormal(jj);
      
      if (v1.first() == getLL().top()) return top();
      else if (v1.first() == getLL().bottom()) return bottom();
      System.out.println("Internal assertion error: " + toString(join));
      return top();
    }
    
    public boolean isNormal(Pair<ImmutableList<NullInfo>,ImmutableSet<IRNode>> val) {
      return getLL().isList(val.first());
    }
  }
  
  private static final class Transfer extends JavaEvaluationTransfer<Lattice,Pair<ImmutableList<NullInfo>,ImmutableSet<IRNode>>> {

    private static final NullLattice nullLattice = NullLattice.getInstance();
    
    private static final SyntaxTreeInterface tree = JJNode.tree; 
    
    
    
    public Transfer(IBinder binder, Lattice lattice, int floor) {
      super(binder, lattice, new SubAnalysisFactory(), floor);
    }


    
    public Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> transferComponentSource(IRNode node) {
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
      return newPair(ImmutableList.<NullInfo>nil(),initSet);
    }

    /*
     * In order to make transfer functions strict, we check at the beginning of
     * each whether we have bottom or not.
     */
    
    @Override
    protected Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> pop(Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      if (!lattice.isNormal(val)) return val;
      return newPair(lattice.getLL().pop(val.first()),val.second());
    }

    @Override
    protected Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> popAllPending(Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      if (!lattice.isNormal(val)) return val;
      if (stackFloorSize == 0) {
        return newPair(ImmutableList.<NullInfo>nil(),val.second());
      } else {
        ImmutableList<NullInfo> newStack = val.first();
        while (newStack.size() > stackFloorSize) {
          newStack = lattice.getLL().pop(newStack);
        }
        return newPair(newStack, val.second());
      }
    }

    @Override
    protected Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> push(Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      return push(val, NullInfo.MAYBENULL);
    }

    protected Pair<ImmutableList<NullInfo>,ImmutableSet<IRNode>> push(Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val, NullInfo ni) {
      if (!lattice.isNormal(val)) return val;
      return newPair(lattice.getLL().push(val.first(), ni),val.second());
    }
    
    @Override
    protected Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> dup(Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      if (!lattice.isNormal(val)) return val;
      NullInfo ni = lattice.getLL().peek(val.first());
      return push(val,ni);
    }

    @Override
    protected Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> popSecond(Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      if (!lattice.isNormal(val)) return val;
      final ListLattice<NullLattice, NullInfo> ll = lattice.getLL();
      NullInfo ni = ll.peek(val.first());
      return newPair(ll.push(ll.pop(ll.pop(val.first())),ni),val.second());
    }

    @Override
    protected Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> transferAllocation(IRNode node, Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      return push(val,NullInfo.NOTNULL);
    }


    @Override
    protected Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> transferArrayCreation(IRNode node, Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      if (!lattice.isNormal(val)) return val;
      if (tree.getOperator(node) instanceof DimExprs) {
        val = pop(val, tree.numChildren(node));
      }
      return push(val, NullInfo.NOTNULL);
    }

    @Override
    protected Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> transferAssignVar(IRNode use, Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
//      if (!lattice.isNormal(val)) return val;
      IRNode var = binder.getIBinding(use).getNode();
      return transferSetVar(var, val);
    }

    /**
     * Transfer an assignment of a variable to what's on the stack.  Leave stack alone.
     * @param var
     * @param val
     * @return
     */
    @SuppressWarnings("unused")
    private Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> transferSetVar(
        IRNode var, Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      final ListLattice<NullLattice, NullInfo> ll = lattice.getLL();
      NullInfo ni = ll.peek(val.first());
      
      if (val.second().contains(var)) { // Variable is coming in as NONNULL
        if (!nullLattice.lessEq(ni,NullInfo.NOTNULL)) { // Value might be null
          return newPair(val.first(),val.second().removeCopy(var)); // Now variable might be null
        }
        if (debug && LOG.isLoggable(Level.FINE)) LOG.fine(JJNode.getInfo(var) + " is still non null after being assigned " + ni);
        // otherwise, do nothing: not null before, not null afterwards
      } else { // Variable is coming in as possibly null
        if (nullLattice.lessEq(ni,NullInfo.NOTNULL)) { // Value is not null
          return newPair(val.first(),val.second().addCopy(var)); // Now the variable is not null
        }
        if (debug && LOG.isLoggable(Level.FINE)) LOG.fine(JJNode.getInfo(var) + " is still maybe null after being assigned " + ni);
        // do nothing : maybe null before, maybe null afterwards
      }
      return val;
    }

    @Override
    protected Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> transferBox(IRNode expr, Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      if (!lattice.isNormal(val)) return val;
      final ListLattice<NullLattice, NullInfo> ll = lattice.getLL();
      return newPair(ll.push(ll.pop(val.first()),NullInfo.NOTNULL),val.second());
    }

    @Override
    protected Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> transferDefaultInit(IRNode node, Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      if (!lattice.isNormal(val)) return val;
      return push(val,NullInfo.NULL);
    }

    @Override
    protected Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> transferEq(IRNode node, boolean flag, Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      if (!lattice.isNormal(val)) return val;
      final ListLattice<NullLattice, NullInfo> ll = lattice.getLL();
      ImmutableList<NullInfo> stack = val.first();
      ImmutableSet<IRNode> nullVars = val.second();
      NullInfo ni2 = ll.peek(stack);
      stack = ll.pop(stack);
      NullInfo ni1 = ll.peek(stack);
      stack = ll.pop(stack);
      stack = ll.push(stack, NullInfo.MAYBENULL);
      // don't pop the second: we don't care what the top of the stack has for primitives
      // if the condition is impossible, we propagate bottom
      if (nullLattice.meet(ni1,ni2) == nullLattice.bottom()) {
        if (flag) return null; // else fall through to end
      } else
      // if the comparison is guaranteed true, we propagate bottom for false:
      if (nullLattice.lessEq(ni1,NullInfo.NULL) && nullLattice.lessEq(ni2, NullInfo.NULL)) {
        if (!flag) return null; // else fall through to end
      } else
      // if we have an *inequality* comparison with null:
      if (!flag) {
        if (nullLattice.lessEq(ni1,NullInfo.NULL)) {
          IRNode n = tree.getChild(node, 1); // don't use EqExpression methods because this transfer is called on != also
          if (tree.getOperator(n) instanceof VariableUseExpression) {
            IRNode var = binder.getIBinding(n).getNode();
            nullVars = nullVars.addCopy(var);
          }
        } else if (tree.getOperator(tree.getChild(node,1)) instanceof NullLiteral) {
          // NB: it would be a little more precise if we checked for ni2 being under NULL
          // than what we do here but then we must check for assignments of the variable
          // so that we don't make a wrong conclusion for "x == (x = null)" which, even
          // if false, still leaves x null.  The first branch is is OK because "(x = null) == x"
          // doesn't have the same problem.
          IRNode n = tree.getChild(node, 0);
          if (tree.getOperator(n) instanceof VariableUseExpression) {
            IRNode var = binder.getIBinding(n).getNode();
            nullVars = nullVars.addCopy(var);
          }
        }
      }
      return newPair(stack,nullVars);
    }

    @Override
    protected Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> transferImplicitArrayCreation(IRNode arrayInitializer, Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      return push(val,NullInfo.NOTNULL);
    }

    @Override
    protected Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> transferInitializationOfVar(IRNode node, Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      if (!lattice.isNormal(val)) return val;
      return pop(transferSetVar(node,val));
    }

    @Override
    protected Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> transferInstanceOf(IRNode node, boolean flag, Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      if (!lattice.isNormal(val)) return val;
      if (!flag) return val;
      IRNode n = InstanceOfExpression.getValue(node);
      if (tree.getOperator(n) instanceof VariableUseExpression) {
        IRNode var = binder.getIBinding(n).getNode();
        return newPair(val.first(),val.second().addCopy(var));
      }
      return val;
    }

    @SuppressWarnings("unused")
    @Override
    protected Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> transferIsObject(IRNode n, boolean flag, Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      if (!lattice.isNormal(val)) return val;
      final ListLattice<NullLattice, NullInfo> ll = lattice.getLL();
      ImmutableList<NullInfo> stack = val.first();
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
          stack = ll.pop(stack);
          --numArgs;
        }
      }
      NullInfo ni = ll.peek(stack);
      if (flag && nullLattice.lessEq(ni, NullInfo.NULL)) {
        if (debug && LOG.isLoggable(Level.FINE)) LOG.fine("Since we know " + ni + " is null, we can assume " + DebugUnparser.toString(n) + " cannot be dereferenced.");
        return null; // lattice.bottom();
      }
      if (!flag && nullLattice.lessEq(ni, NullInfo.NOTNULL)) {
        if (debug && LOG.isLoggable(Level.FINE)) LOG.fine("Since we know " + ni + " is not null, we can assume " + DebugUnparser.toString(n) + " won't throw a NPE.");
        return null; //lattice.bottom();
      }
      if (flag && tree.getOperator(n) instanceof VariableUseExpression) {
        IRNode var = binder.getIBinding(n).getNode();
        return newPair(val.first(),val.second().addCopy(var));
      }
      return super.transferIsObject(n, flag, val);
    }

    @Override
    protected Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> transferLiteral(IRNode node, Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      if (!lattice.isNormal(val)) return val;
      final ListLattice<NullLattice, NullInfo> ll = lattice.getLL();
      ImmutableList<NullInfo> stack = val.first();
      
      NullInfo ni;
      if (tree.getOperator(node) instanceof NullLiteral) {
        ni = NullInfo.NULL;
      } else {
        ni = NullInfo.NOTNULL; // all other literals are not null
      }
      return newPair(ll.push(stack, ni),val.second());
    }

    @Override
    protected Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> transferToString(IRNode node, Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      if (!lattice.isNormal(val)) return val;
      final ListLattice<NullLattice, NullInfo> ll = lattice.getLL();
      ImmutableList<NullInfo> stack = val.first();
      if (nullLattice.lessEq(ll.peek(stack),NullInfo.NOTNULL)) return val;
      // otherwise, we can force not null
      return newPair(ll.push(ll.pop(stack), NullInfo.NOTNULL),val.second());
    }

    @Override
    protected Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> transferUseVar(IRNode use, Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      if (!lattice.isNormal(val)) return val;
      final ListLattice<NullLattice, NullInfo> ll = lattice.getLL();
      NullInfo ni;
      IRNode var = binder.getIBinding(use).getNode();
      if (val.second().contains(var)) {
        ni = NullInfo.NOTNULL;
      } else {
        ni = NullInfo.MAYBENULL; // all other literals are not null
      }
      return newPair(ll.push(val.first(), ni),val.second());
    }
    
    @Override
    protected Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> transferUseReceiver(
        final IRNode use, 
        final Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      if (!lattice.isNormal(val)) return val;
      // Receiver is always non-null
      return newPair(lattice.getLL().push(val.first(), NullInfo.NOTNULL),val.second());
    }
    
    @Override
    protected Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> transferUseQualifiedReceiver(
        final IRNode use, final IRNode binding, 
        final Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      if (!lattice.isNormal(val)) return val;
      // Qualified receiver is always non-null
      return newPair(lattice.getLL().push(val.first(), NullInfo.NOTNULL),val.second());
    }
    
    
    @Override
    protected Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> transferConcat(
        IRNode node, Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      if (!lattice.isNormal(val)) return val;
      // pop the values of the stack and push a non-null
      final ListLattice<NullLattice, NullInfo> ll = lattice.getLL();
      ImmutableList<NullInfo> stack = val.first();
      stack = ll.pop(stack);
      stack = ll.pop(stack);
      stack = ll.push(stack, NullInfo.NOTNULL);
      return newPair(stack, val.second());
    }
  }
  
  
  
  private static final class SubAnalysisFactory extends AbstractCachingSubAnalysisFactory<Lattice, Pair<ImmutableList<NullInfo>,ImmutableSet<IRNode>>> {
    @Override
    protected JavaForwardAnalysis<Pair<ImmutableList<NullInfo>,ImmutableSet<IRNode>>, Lattice> realCreateAnalysis(
        final IRNode caller, final IBinder binder,
        final Lattice lattice,
        final Pair<ImmutableList<NullInfo>,ImmutableSet<IRNode>> initialValue,
        final boolean terminationNormal) {
      final int floor = initialValue.first().size();
      final Transfer t = new Transfer(binder, lattice, floor);
      return new JavaForwardAnalysis<Pair<ImmutableList<NullInfo>,ImmutableSet<IRNode>>, Lattice>("sub analysis", lattice, t, DebugUnparser.viewer);
    }
  }


  
  public static final class Test extends TestFlowAnalysis<Pair<ImmutableList<NullInfo>,ImmutableSet<IRNode>>, Lattice, JavaForwardAnalysis<Pair<ImmutableList<NullInfo>,ImmutableSet<IRNode>>, Lattice>> {
    @Override
    protected JavaForwardAnalysis<Pair<ImmutableList<NullInfo>,ImmutableSet<IRNode>>, Lattice> createAnalysis(IRNode flowUnit, IBinder binder) {
      final Lattice l = new Lattice();
      final Transfer t = new Transfer(binder,l, 0);
      return new JavaForwardAnalysis<Pair<ImmutableList<NullInfo>,ImmutableSet<IRNode>>, Lattice>("nonnll", l, t, DebugUnparser.viewer);
    }
    
    public static void main(String[] args)  {
      System.out.println("Starting.");
      new Test().test(args);
    }
  }



  public IBinder getBinder() {
    return binder;
  }

  public void clearCaches() {
    clear();
  }
}
