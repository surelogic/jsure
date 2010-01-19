package edu.uwm.cs.fluid.java.analysis;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import edu.cmu.cs.fluid.control.Component.WhichPort;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRNodeViewer;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.analysis.AnalysisQuery;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.CallInterface;
import edu.cmu.cs.fluid.java.operator.CatchClause;
import edu.cmu.cs.fluid.java.operator.InstanceOfExpression;
import edu.cmu.cs.fluid.java.operator.NullLiteral;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.tree.SyntaxTreeInterface;
import edu.cmu.cs.fluid.util.ImmutableList;
import edu.cmu.cs.fluid.util.ImmutableSet;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;
import edu.cmu.cs.fluid.util.Pair;
import edu.uwm.cs.fluid.control.ForwardAnalysis;
import edu.uwm.cs.fluid.java.control.JavaEvaluationTransfer;
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
public final class SimpleNonnullAnalysis extends IntraproceduralAnalysis<Pair<ImmutableList<NullInfo>,ImmutableSet<IRNode>>, SimpleNonnullAnalysis.Lattice, SimpleNonnullAnalysis.Analysis> {
  public final class Query implements AnalysisQuery<ImmutableSet<IRNode>> {
    private final Analysis a;
    
    public Query(final IRNode flowUnit) {
      a = getAnalysis(flowUnit);
    }

    private Query(final Analysis s) {
      a = s;
    }
    
    public ImmutableSet<IRNode> getResultFor(final IRNode expr) {
      return a.getAfter(expr, WhichPort.ENTRY).second();
    }

    public Query getSubAnalysisQuery() {
      final Analysis sub = a.getSubAnalysis();
      if (sub == null) {
        throw new UnsupportedOperationException();
      } else {
        return new Query(sub);
      }
    }

    public boolean hasSubAnalysisQuery() {
      return a.getSubAnalysis() != null;
    }
  }
  
//  private static final Logger LOG = SLLogger.getLogger();

  public SimpleNonnullAnalysis(IBinder b) {
    super(b);
  }

  @Override
  protected Analysis createAnalysis(IRNode flowUnit) {
    return Analysis.createAnalysis("Java.Nonnull", binder);
  }

  /**
   * Return the set of variable declarators that are guaranteed not to be null.
   * @param node node in AST to denote where to check
   * @return variables that are not null at this execution point.
   */
  public ImmutableSet<IRNode> getNonnullBefore(IRNode node, IRNode constructorContext) {
    return getAnalysisResultsBefore(node, constructorContext).second();
  }

  public Query getNonnullBeforeQuery(final IRNode flowUnit) {
    return new Query(flowUnit);
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
    
    public boolean isNormal(Pair<ImmutableList<NullInfo>,ImmutableSet<IRNode>> val) {
      return getLL().isList(val.first());
    }
  }
  
  private static final class Transfer extends JavaEvaluationTransfer<Lattice,Pair<ImmutableList<NullInfo>,ImmutableSet<IRNode>>> {

    private static final NullLattice nullLattice = NullLattice.getInstance();
    
    private static final SyntaxTreeInterface tree = JJNode.tree; 
    
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

    
    
    public Transfer(IBinder binder, Lattice lattice) {
      super(binder, lattice);
    }
    
    
    
    public Analysis getSubAnalysis() {
      return subAnalysis;
    }

    
    
    @Override
    protected Analysis
    createAnalysis(final IBinder binder, final boolean terminationNormal) {
      if (subAnalysis == null) {
        subAnalysis = Analysis.createAnalysis("sub analysis", binder);
      }
      return subAnalysis;
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
     * In order to make transfer functions strict, we check at the beginning of each whether
     * we have bottom or not. 
     */
    
    @Override
    protected Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> pop(Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      if (!lattice.isNormal(val)) return val;
      return newPair(lattice.getLL().pop(val.first()),val.second());
    }

    @Override
    protected Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> popAllPending(Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      if (!lattice.isNormal(val)) return val;
      return newPair(ImmutableList.<NullInfo>nil(),val.second());
    }

    @Override
    protected Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> push(Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      if (!lattice.isNormal(val)) return val;
      return newPair(lattice.getLL().push(val.first(),NullInfo.MAYBENULL),val.second());
    }

    @Override
    protected Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> dup(Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      if (!lattice.isNormal(val)) return val;
      NullInfo ni = lattice.getLL().peek(val.first());
      return newPair(lattice.getLL().push(val.first(),ni),val.second());
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
      if (!lattice.isNormal(val)) return val;
      return newPair(lattice.getLL().push(val.first(),NullInfo.NOTNULL),val.second());
    }


    @Override
    protected Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> transferArrayCreation(IRNode node, Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      if (!lattice.isNormal(val)) return val;
      return newPair(lattice.getLL().push(val.first(),NullInfo.NOTNULL),val.second());
    }

    @Override
    protected Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> transferAssignVar(IRNode use, Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      if (!lattice.isNormal(val)) return val;
      IRNode var = binder.getIBinding(use).getNode();
      return transferSetVar(var, val);
    }

    /**
     * Transfer an assignment of a variable to what's on the stack.  Leave stack alone.
     * @param var
     * @param val
     * @return
     */
    private Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> transferSetVar(
        IRNode var, Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      final ListLattice<NullLattice, NullInfo> ll = lattice.getLL();
      NullInfo ni = ll.peek(val.first());
      
      if (val.second().contains(var)) {
        if (!nullLattice.lessEq(ni,NullInfo.NOTNULL)) return newPair(val.first(),val.second().removeCopy(var));
        if (LOG.isLoggable(Level.FINE)) LOG.fine(JJNode.getInfo(var) + " is still non null after being assigned " + ni);
        // otherwise, do nothing: not null before, not null afterwards
      } else {
        if (nullLattice.lessEq(ni,NullInfo.NOTNULL)) return newPair(val.first(),val.second().addCopy(var));
        if (LOG.isLoggable(Level.FINE)) LOG.fine(JJNode.getInfo(var) + " is still maybe null after being assigned " + ni);
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
      final ListLattice<NullLattice, NullInfo> ll = lattice.getLL();
      return newPair(ll.push(val.first(),NullInfo.NULL),val.second());
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
      // don't pop the second: we don't care what the top of the stack has for primitives
      // if the condition is impossible, we propagate bottom
      if (nullLattice.meet(ni1,ni2) == nullLattice.bottom()) {
        if (flag) return null; else return val;
      }
      // if the comparison is guaranteed true, we propagate bottom for false:
      if (nullLattice.lessEq(ni1,NullInfo.NULL) && nullLattice.lessEq(ni2, NullInfo.NULL)) {
        if (flag) return val; else return null;
      }
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
      if (!lattice.isNormal(val)) return val;
      final ListLattice<NullLattice, NullInfo> ll = lattice.getLL();
      return newPair(ll.push(val.first(),NullInfo.NOTNULL),val.second());
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

    @Override
    protected Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> transferIsObject(IRNode n, boolean flag, Pair<ImmutableList<NullInfo>, ImmutableSet<IRNode>> val) {
      if (!lattice.isNormal(val)) return val;
      final ListLattice<NullLattice, NullInfo> ll = lattice.getLL();
      ImmutableList<NullInfo> stack = val.first();
      // need to find the receiver:
      IRNode p = tree.getParent(n);
      if (tree.getOperator(p) instanceof CallInterface) {
        CallInterface cop = ((CallInterface)tree.getOperator(p));
        IRNode arguments = cop.get_Args(p);
        int num = tree.numChildren(arguments);
        while (num > 0) {
          stack = ll.pop(stack);
          --num;
        }
      }
      NullInfo ni = ll.peek(stack);
      if (flag && nullLattice.lessEq(ni, NullInfo.NULL)) {
        if (LOG.isLoggable(Level.FINE)) LOG.fine("Since we know " + ni + " is null, we can assume " + DebugUnparser.toString(n) + " cannot be dereferenced.");
        return null; // lattice.bottom();
      }
      if (!flag && nullLattice.lessEq(ni, NullInfo.NOTNULL)) {
        if (LOG.isLoggable(Level.FINE)) LOG.fine("Since we know " + ni + " is not null, we can assume " + DebugUnparser.toString(n) + " won't throw a NPE.");
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
      final Operator op = tree.getOperator(var);
      if (op instanceof ReceiverDeclaration || op instanceof QualifiedReceiverDeclaration || val.second().contains(var)) {
        ni = NullInfo.NOTNULL;
      } else {
        ni = NullInfo.MAYBENULL; // all other literals are not null
      }
      return newPair(ll.push(val.first(), ni),val.second());
    }
    
    
  }
  
  public static final class Analysis extends ForwardAnalysis<Pair<ImmutableList<NullInfo>,ImmutableSet<IRNode>>, Lattice, Transfer> {

    private Analysis(String name, Lattice l, Transfer t, IRNodeViewer nv) {
      super(name, l, t, nv);
    }
    
    static Analysis createAnalysis(String name, IBinder binder) {
      Lattice l = new Lattice();
      Transfer t = new Transfer(binder,l);
      return new Analysis(name, l, t, DebugUnparser.viewer);
    }
    
    public Analysis getSubAnalysis() {
      return trans.getSubAnalysis();
    }
  }
  
  public static final class Test extends TestFlowAnalysis<Pair<ImmutableList<NullInfo>,ImmutableSet<IRNode>>, Lattice, Analysis> {

    @Override
    protected Analysis createAnalysis(IRNode flowUnit, IBinder binder) {
      return Analysis.createAnalysis("nonnll", binder);
    }
    
    public static void main(String[] args)  {
      System.out.println("Starting.");
      new Test().test(args);
    }
  }
}
