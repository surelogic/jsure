package com.surelogic.analysis.nullable;


import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.nullable.RawTypeLattice.Element;
import com.surelogic.util.IThunk;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.analysis.SimplifiedJavaFlowAnalysisQuery;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.ConstructorCall;
import edu.cmu.cs.fluid.java.operator.EnumConstantClassDeclaration;
import edu.cmu.cs.fluid.java.operator.ImpliedEnumConstantInitialization;
import edu.cmu.cs.fluid.java.operator.SuperExpression;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.uwm.cs.fluid.java.control.AbstractCachingSubAnalysisFactory;
import edu.uwm.cs.fluid.java.control.IJavaFlowAnalysis;
import edu.uwm.cs.fluid.java.control.JavaEvaluationTransfer;
import edu.uwm.cs.fluid.java.control.JavaForwardAnalysis;
import edu.uwm.cs.fluid.java.analysis.IntraproceduralAnalysis;


/**
 * A class that computes the "raw" type of a local variable.
 * 
 * <p>Right now we just compute the value for the receiver, but in the near
 * future we will use an array of RawTypeLattice values and compute the value
 * for all the local variables in a method.
 * 
 * <p>Because we just work on the receiver right now, this only needs to be
 * invoked on constructors.
 */
public final class RawTypeAnalysis extends IntraproceduralAnalysis<Element, RawTypeLattice, JavaForwardAnalysis<Element, RawTypeLattice>> implements IBinderClient {
  public final class Query extends SimplifiedJavaFlowAnalysisQuery<Query, Element, Element, RawTypeLattice> {
    public Query(final IThunk<? extends IJavaFlowAnalysis<Element, RawTypeLattice>> thunk) {
      super(thunk);
    }
    
    private Query(final Delegate<Query, Element, Element, RawTypeLattice> d) {
      super(d);
    }
    
    @Override
    protected RawResultFactory getRawResultFactory() {
      return RawResultFactory.ENTRY;
    }

    @Override
    protected Element processRawResult(
        final IRNode expr, final RawTypeLattice lattice, final Element rawResult) {
      return rawResult;
    }

    @Override
    protected Query newSubAnalysisQuery(final Delegate<Query, Element, Element, RawTypeLattice> d) {
      return new Query(d);
    }
  }
  
  
  
  public RawTypeAnalysis(final IBinder b) {
    super(b);
  }

  @Override
  protected JavaForwardAnalysis<Element, RawTypeLattice> createAnalysis(final IRNode flowUnit) {
    final RawTypeLattice l = new RawTypeLattice(binder.getTypeEnvironment());
    final Transfer t = new Transfer(binder, l, 0);
    return new JavaForwardAnalysis<RawTypeLattice.Element, RawTypeLattice>("Raw Types", l, t, DebugUnparser.viewer);
  }


  
  private static final class Transfer extends JavaEvaluationTransfer<RawTypeLattice, RawTypeLattice.Element> {
    public Transfer(final IBinder binder, final RawTypeLattice lattice, final int floor) {
      super(binder, lattice, new SubAnalysisFactory(), floor);
    }


    
    public Element transferComponentSource(final IRNode node) {
      // Receiver is completely raw at the start
      
      /* In the future, parameters should be initialized based on annotations */
      
      return lattice.getRaw();
    }

    @Override
    protected Element transferConstructorCall(
        final IRNode node, final boolean flag, final Element value) {
      if (flag) {
        final ITypeEnvironment typeEnv = binder.getTypeEnvironment();
        if (ConstructorCall.prototype.includes(node)) {
          if (SuperExpression.prototype.includes(ConstructorCall.getObject(node))) {
            /* Initialized up to the superclass type.  ConstructorCall expressions
             * can only appear inside of a constructor declaration, which in turn
             * can only appear in a class declaration.
             */
            final IRNode classDecl = VisitUtil.getEnclosingType(node);
            return lattice.injectClass(
                typeEnv.getSuperclass(
                    (IJavaDeclaredType) typeEnv.getMyThisType(classDecl)));
          } else { // ThisExpression
            return lattice.getNotRaw();
          }
        } else if (AnonClassExpression.prototype.includes(node)) {
          final IRNode superClassDecl =
              binder.getBinding(AnonClassExpression.getType(node));
          return lattice.injectClass(
              (IJavaDeclaredType) typeEnv.getMyThisType(superClassDecl));
        } else if (ImpliedEnumConstantInitialization.prototype.includes(node)
            && EnumConstantClassDeclaration.prototype.includes(tree.getParent(node))) {
          /* The immediately enclosing type is the EnumConstantClassDeclaration
           * node.  We need to go up another level to get the EnumDeclaration.
           */
          final IRNode superClassDecl =
              VisitUtil.getEnclosingType(VisitUtil.getEnclosingType(node));
          return lattice.injectClass(
              (IJavaDeclaredType) typeEnv.getMyThisType(superClassDecl));
        } else { // Not sure why it should get here
          return value;
        }
      } else {
        // exceptional branch: object is not initialized to anything
        return null;
      }
    }
    
    @Override
    protected Element pop(Element val) {
      // TODO: sanity check
      return val;
    }

    @Override
    protected Element popAllPending(Element val) {
      // TODO: sanity check
      return val;
//      if (!lattice.isNormal(val)) return val;
//      if (stackFloorSize == 0) {
//        return newPair(ImmutableList.<NullInfo>nil(),val.second());
//      } else {
//        ImmutableList<NullInfo> newStack = val.first();
//        while (newStack.size() > stackFloorSize) {
//          newStack = lattice.getLL().pop(newStack);
//        }
//        return newPair(newStack, val.second());
//      }
    }

    @Override
    protected Element push(Element val) {
      return val;
//      return push(val, NullInfo.MAYBENULL);
    }

//    protected Pair<ImmutableList<NullInfo>,ImmutableSet<IRNode>> push(Element val, NullInfo ni) {
//      if (!lattice.isNormal(val)) return val;
//      return newPair(lattice.getLL().push(val.first(), ni),val.second());
//    }
    
    @Override
    protected Element dup(Element val) {
      return val;
//      if (!lattice.isNormal(val)) return val;
//      NullInfo ni = lattice.getLL().peek(val.first());
//      return push(val,ni);
    }

    @Override
    protected Element popSecond(Element val) {
      return val;
//      if (!lattice.isNormal(val)) return val;
//      final ListLattice<NullLattice, NullInfo> ll = lattice.getLL();
//      NullInfo ni = ll.peek(val.first());
//      return newPair(ll.push(ll.pop(ll.pop(val.first())),ni),val.second());
    }

//    @Override
//    protected Element transferAllocation(IRNode node, Element val) {
//      return push(val,NullInfo.NOTNULL);
//    }


//    @Override
//    protected Element transferArrayCreation(IRNode node, Element val) {
//      if (!lattice.isNormal(val)) return val;
//      if (tree.getOperator(node) instanceof DimExprs) {
//        val = pop(val, tree.numChildren(node));
//      }
//      return push(val, NullInfo.NOTNULL);
//    }

//    @Override
//    protected Element transferAssignVar(IRNode use, Element val) {
//      if (!lattice.isNormal(val)) return val;
//      IRNode var = binder.getIBinding(use).getNode();
//      return transferSetVar(var, val);
//    }

//    /**
//     * Transfer an assignment of a variable to what's on the stack.  Leave stack alone.
//     * @param var
//     * @param val
//     * @return
//     */
//    private Element transferSetVar(
//        IRNode var, Element val) {
//      final ListLattice<NullLattice, NullInfo> ll = lattice.getLL();
//      NullInfo ni = ll.peek(val.first());
//      
//      if (val.second().contains(var)) { // Variable is coming in as NONNULL
//        if (!nullLattice.lessEq(ni,NullInfo.NOTNULL)) { // Value might be null
//          return newPair(val.first(),val.second().removeCopy(var)); // Now variable might be null
//        }
//        if (debug && LOG.isLoggable(Level.FINE)) LOG.fine(JJNode.getInfo(var) + " is still non null after being assigned " + ni);
//        // otherwise, do nothing: not null before, not null afterwards
//      } else { // Variable is coming in as possibly null
//        if (nullLattice.lessEq(ni,NullInfo.NOTNULL)) { // Value is not null
//          return newPair(val.first(),val.second().addCopy(var)); // Now the variable is not null
//        }
//        if (debug && LOG.isLoggable(Level.FINE)) LOG.fine(JJNode.getInfo(var) + " is still maybe null after being assigned " + ni);
//        // do nothing : maybe null before, maybe null afterwards
//      }
//      return val;
//    }

//    @Override
//    protected Element transferBox(IRNode expr, Element val) {
//      if (!lattice.isNormal(val)) return val;
//      final ListLattice<NullLattice, NullInfo> ll = lattice.getLL();
//      return newPair(ll.push(ll.pop(val.first()),NullInfo.NOTNULL),val.second());
//    }

//    @Override
//    protected Element transferDefaultInit(IRNode node, Element val) {
//      if (!lattice.isNormal(val)) return val;
//      return push(val,NullInfo.NULL);
//    }

//    @Override
//    protected Element transferEq(IRNode node, boolean flag, Element val) {
//      if (!lattice.isNormal(val)) return val;
//      final ListLattice<NullLattice, NullInfo> ll = lattice.getLL();
//      ImmutableList<NullInfo> stack = val.first();
//      ImmutableSet<IRNode> nullVars = val.second();
//      NullInfo ni2 = ll.peek(stack);
//      stack = ll.pop(stack);
//      NullInfo ni1 = ll.peek(stack);
//      stack = ll.pop(stack);
//      stack = ll.push(stack, NullInfo.MAYBENULL);
//      // don't pop the second: we don't care what the top of the stack has for primitives
//      // if the condition is impossible, we propagate bottom
//      if (nullLattice.meet(ni1,ni2) == nullLattice.bottom()) {
//        if (flag) return null; // else fall through to end
//      } else
//      // if the comparison is guaranteed true, we propagate bottom for false:
//      if (nullLattice.lessEq(ni1,NullInfo.NULL) && nullLattice.lessEq(ni2, NullInfo.NULL)) {
//        if (!flag) return null; // else fall through to end
//      } else
//      // if we have an *inequality* comparison with null:
//      if (!flag) {
//        if (nullLattice.lessEq(ni1,NullInfo.NULL)) {
//          IRNode n = tree.getChild(node, 1); // don't use EqExpression methods because this transfer is called on != also
//          if (tree.getOperator(n) instanceof VariableUseExpression) {
//            IRNode var = binder.getIBinding(n).getNode();
//            nullVars = nullVars.addCopy(var);
//          }
//        } else if (tree.getOperator(tree.getChild(node,1)) instanceof NullLiteral) {
//          // NB: it would be a little more precise if we checked for ni2 being under NULL
//          // than what we do here but then we must check for assignments of the variable
//          // so that we don't make a wrong conclusion for "x == (x = null)" which, even
//          // if false, still leaves x null.  The first branch is is OK because "(x = null) == x"
//          // doesn't have the same problem.
//          IRNode n = tree.getChild(node, 0);
//          if (tree.getOperator(n) instanceof VariableUseExpression) {
//            IRNode var = binder.getIBinding(n).getNode();
//            nullVars = nullVars.addCopy(var);
//          }
//        }
//      }
//      return newPair(stack,nullVars);
//    }

//    @Override
//    protected Element transferImplicitArrayCreation(IRNode arrayInitializer, Element val) {
//      return push(val,NullInfo.NOTNULL);
//    }

//    @Override
//    protected Element transferInitializationOfVar(IRNode node, Element val) {
//      if (!lattice.isNormal(val)) return val;
//      return pop(transferSetVar(node,val));
//    }

//    @Override
//    protected Element transferInstanceOf(IRNode node, boolean flag, Element val) {
//      if (!lattice.isNormal(val)) return val;
//      if (!flag) return val;
//      IRNode n = InstanceOfExpression.getValue(node);
//      if (tree.getOperator(n) instanceof VariableUseExpression) {
//        IRNode var = binder.getIBinding(n).getNode();
//        return newPair(val.first(),val.second().addCopy(var));
//      }
//      return val;
//    }

//    @SuppressWarnings("unused")
//    @Override
//    protected Element transferIsObject(IRNode n, boolean flag, Element val) {
//      if (!lattice.isNormal(val)) return val;
//      final ListLattice<NullLattice, NullInfo> ll = lattice.getLL();
//      ImmutableList<NullInfo> stack = val.first();
//      // need to find the receiver:
//      IRNode p = tree.getParent(n);
//      if (tree.getOperator(p) instanceof CallInterface) {
//        final CallInterface cop = ((CallInterface)tree.getOperator(p));
//        int numArgs;
//        try {
//          numArgs = tree.numChildren(cop.get_Args(p));
//        } catch (final CallInterface.NoArgs e) {
//          numArgs = 0;
//        }
//        while (numArgs > 0) {
//          stack = ll.pop(stack);
//          --numArgs;
//        }
//      }
//      NullInfo ni = ll.peek(stack);
//      if (flag && nullLattice.lessEq(ni, NullInfo.NULL)) {
//        if (debug && LOG.isLoggable(Level.FINE)) LOG.fine("Since we know " + ni + " is null, we can assume " + DebugUnparser.toString(n) + " cannot be dereferenced.");
//        return null; // lattice.bottom();
//      }
//      if (!flag && nullLattice.lessEq(ni, NullInfo.NOTNULL)) {
//        if (debug && LOG.isLoggable(Level.FINE)) LOG.fine("Since we know " + ni + " is not null, we can assume " + DebugUnparser.toString(n) + " won't throw a NPE.");
//        return null; //lattice.bottom();
//      }
//      if (flag && tree.getOperator(n) instanceof VariableUseExpression) {
//        IRNode var = binder.getIBinding(n).getNode();
//        return newPair(val.first(),val.second().addCopy(var));
//      }
//      return super.transferIsObject(n, flag, val);
//    }

//    @Override
//    protected Element transferLiteral(IRNode node, Element val) {
//      if (!lattice.isNormal(val)) return val;
//      final ListLattice<NullLattice, NullInfo> ll = lattice.getLL();
//      ImmutableList<NullInfo> stack = val.first();
//      
//      NullInfo ni;
//      if (tree.getOperator(node) instanceof NullLiteral) {
//        ni = NullInfo.NULL;
//      } else {
//        ni = NullInfo.NOTNULL; // all other literals are not null
//      }
//      return newPair(ll.push(stack, ni),val.second());
//    }

//    @Override
//    protected Element transferToString(IRNode node, Element val) {
//      if (!lattice.isNormal(val)) return val;
//      final ListLattice<NullLattice, NullInfo> ll = lattice.getLL();
//      ImmutableList<NullInfo> stack = val.first();
//      if (nullLattice.lessEq(ll.peek(stack),NullInfo.NOTNULL)) return val;
//      // otherwise, we can force not null
//      return newPair(ll.push(ll.pop(stack), NullInfo.NOTNULL),val.second());
//    }

//    @Override
//    protected Element transferUseVar(IRNode use, Element val) {
//      if (!lattice.isNormal(val)) return val;
//      final ListLattice<NullLattice, NullInfo> ll = lattice.getLL();
//      NullInfo ni;
//      IRNode var = binder.getIBinding(use).getNode();
//      if (val.second().contains(var)) {
//        ni = NullInfo.NOTNULL;
//      } else {
//        ni = NullInfo.MAYBENULL; // all other literals are not null
//      }
//      return newPair(ll.push(val.first(), ni),val.second());
//    }
    
    @Override
    protected Element transferUseReceiver(
        final IRNode use, 
        final Element val) {
      return val;
//      if (!lattice.isNormal(val)) return val;
//      // Receiver is always non-null
//      return newPair(lattice.getLL().push(val.first(), NullInfo.NOTNULL),val.second());
    }
    
    @Override
    protected Element transferUseQualifiedReceiver(
        final IRNode use, final IRNode binding, 
        final Element val) {
      return val;
//      if (!lattice.isNormal(val)) return val;
//      // Qualified receiver is always non-null
//      return newPair(lattice.getLL().push(val.first(), NullInfo.NOTNULL),val.second());
    }
    
    
//    @Override
//    protected Element transferConcat(
//        IRNode node, Element val) {
//      if (!lattice.isNormal(val)) return val;
//      // pop the values of the stack and push a non-null
//      final ListLattice<NullLattice, NullInfo> ll = lattice.getLL();
//      ImmutableList<NullInfo> stack = val.first();
//      stack = ll.pop(stack);
//      stack = ll.pop(stack);
//      stack = ll.push(stack, NullInfo.NOTNULL);
//      return newPair(stack, val.second());
//    }
  }
  
  
  
  private static final class SubAnalysisFactory extends AbstractCachingSubAnalysisFactory<RawTypeLattice, Element> {
    @Override
    protected JavaForwardAnalysis<Element, RawTypeLattice> realCreateAnalysis(
        final IRNode caller, final IBinder binder,
        final RawTypeLattice lattice,
        final Element initialValue,
        final boolean terminationNormal) {
//      final int floor = initialValue.first().size();
      final Transfer t = new Transfer(binder, lattice, 0);
      return new JavaForwardAnalysis<Element, RawTypeLattice>("sub analysis", lattice, t, DebugUnparser.viewer);
    }
  }


  
  public IBinder getBinder() {
    return binder;
  }

  public void clearCaches() {
    clear();
  }



  public Query getRawTypeQuery(final IRNode flowUnit) {
    return new Query(getAnalysisThunk(flowUnit));
  }
}
