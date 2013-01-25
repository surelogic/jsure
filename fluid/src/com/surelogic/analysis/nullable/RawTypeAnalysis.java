package com.surelogic.analysis.nullable;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.LocalVariableDeclarations;
import com.surelogic.analysis.nullable.RawLattice.Element;
import com.surelogic.annotation.rules.NonNullRules;
import com.surelogic.common.Pair;
import com.surelogic.dropsea.ir.drops.nullable.RawPromiseDrop;
import com.surelogic.util.IThunk;
import com.surelogic.util.NullList;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.analysis.SimplifiedJavaFlowAnalysisQuery;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.ConstructorCall;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.DimExprs;
import edu.cmu.cs.fluid.java.operator.EnumConstantClassDeclaration;
import edu.cmu.cs.fluid.java.operator.ImpliedEnumConstantInitialization;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.SuperExpression;
import edu.cmu.cs.fluid.java.promise.InitDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.AbstractRemovelessIterator;
import edu.cmu.cs.fluid.util.ImmutableList;
import edu.uwm.cs.fluid.java.control.AbstractCachingSubAnalysisFactory;
import edu.uwm.cs.fluid.java.control.IJavaFlowAnalysis;
import edu.uwm.cs.fluid.java.control.JavaForwardAnalysis;
import edu.uwm.cs.fluid.java.control.LatticeDelegatingJavaEvaluationTransfer;
import edu.uwm.cs.fluid.java.analysis.EvaluationStackLattice;
import edu.uwm.cs.fluid.java.analysis.IntraproceduralAnalysis;
import edu.uwm.cs.fluid.util.PairLattice;


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
public final class RawTypeAnalysis extends IntraproceduralAnalysis<
    RawTypeAnalysis.Value, RawTypeAnalysis.Lattice,
    JavaForwardAnalysis<RawTypeAnalysis.Value, RawTypeAnalysis.Lattice>>
implements IBinderClient {
  public final class Query extends SimplifiedJavaFlowAnalysisQuery<Query, Element[], Value, Lattice> {
    public Query(final IThunk<? extends IJavaFlowAnalysis<Value, Lattice>> thunk) {
      super(thunk);
    }
    
    private Query(final Delegate<Query, Element[], Value, Lattice> d) {
      super(d);
    }
    
    @Override
    protected RawResultFactory getRawResultFactory() {
      return RawResultFactory.ENTRY;
    }

    @Override
    protected Element[] processRawResult(
        final IRNode expr, final Lattice lattice, final Value rawResult) {
      return rawResult.second().first();
    }

    @Override
    protected Query newSubAnalysisQuery(final Delegate<Query, Element[], Value, Lattice> d) {
      return new Query(d);
    }
  }
  
  
  
  public final class InferredRawQuery extends SimplifiedJavaFlowAnalysisQuery<InferredRawQuery, Inferred, Value, Lattice> {
    public InferredRawQuery(final IThunk<? extends IJavaFlowAnalysis<Value, Lattice>> thunk) {
      super(thunk);
    }
    
    private InferredRawQuery(final Delegate<InferredRawQuery, Inferred, Value, Lattice> d) {
      super(d);
    }
    
    @Override
    protected RawResultFactory getRawResultFactory() {
      return RawResultFactory.NORMAL_EXIT;
    }

    @Override
    protected Inferred processRawResult(
        final IRNode expr, final Lattice lattice, final Value rawResult) {
      return new Inferred(
          lattice.getInferredLattice(), rawResult.second().second());
    }

    @Override
    protected InferredRawQuery newSubAnalysisQuery(final Delegate<InferredRawQuery, Inferred, Value, Lattice> d) {
      return new InferredRawQuery(d);
    }
  }
  
  public final class Inferred implements Iterable<Pair<IRNode, Element>> {
    private final RawVariables lattice;
    private final Element[] values;
    
    private Inferred(final RawVariables lat, final Element[] val) {
      lattice = lat;
      values = val;
    }
    
    @Override
    public Iterator<Pair<IRNode, Element>> iterator() {
      return new AbstractRemovelessIterator<Pair<IRNode, Element>>() {
        private int idx = 0;
        
        @Override
        public boolean hasNext() {
          return idx < lattice.getRealSize();
        }

        @Override
        public Pair<IRNode, Element> next() {
          final int currentIdx = idx++;
          return new Pair<IRNode, Element>(
              lattice.getKey(currentIdx), values[currentIdx]);
        }
      };
    }
    
    public Element injectAnnotation(final RawPromiseDrop pd) {
      return lattice.getBaseLattice().injectPromiseDrop(pd);
    }

    public boolean lessEq(final Element a, final Element b) {
      return lattice.getBaseLattice().lessEq(a, b);
    }
  }
  
  
  
  public final class DebugQuery extends SimplifiedJavaFlowAnalysisQuery<DebugQuery, String, Value, Lattice> {
    public DebugQuery(final IThunk<? extends IJavaFlowAnalysis<Value, Lattice>> thunk) {
      super(thunk);
    }
    
    private DebugQuery(final Delegate<DebugQuery, String, Value, Lattice> d) {
      super(d);
    }
    
    @Override
    protected RawResultFactory getRawResultFactory() {
      return RawResultFactory.NORMAL_EXIT;
    }

    @Override
    protected String processRawResult(
        final IRNode expr, final Lattice lattice, final Value rawResult) {
      return lattice.toString(rawResult);
    }

    @Override
    protected DebugQuery newSubAnalysisQuery(final Delegate<DebugQuery, String, Value, Lattice> d) {
      return new DebugQuery(d);
    }
  }
  
  
  
  public RawTypeAnalysis(final IBinder b) {
    super(b);
  }

  
  
  @Override
  protected JavaForwardAnalysis<Value, Lattice> createAnalysis(final IRNode flowUnit) {
    final LocalVariableDeclarations lvd = LocalVariableDeclarations.getDeclarationsFor(flowUnit);
    final List<IRNode> refVars = new ArrayList<IRNode>(
        lvd.getLocal().size() + lvd.getExternal().size() + 1);
    // Receiver is always the first item in the array, if it exists
    final Operator flowUnitOp = JJNode.tree.getOperator(flowUnit);
    if (InitDeclaration.prototype.includes(flowUnitOp) || 
        ConstructorDeclaration.prototype.includes(flowUnitOp) ||
        (MethodDeclaration.prototype.includes(flowUnitOp) && !TypeUtil.isStatic(flowUnit))) {
      refVars.add(JavaPromise.getReceiverNode(flowUnit));
    }
    // Add all reference-typed variables in scope
    LocalVariableDeclarations.separateDeclarations(
        binder, lvd.getLocal(), refVars, NullList.<IRNode>prototype());
    LocalVariableDeclarations.separateDeclarations(
        binder, lvd.getExternal(), refVars, NullList.<IRNode>prototype());
    
    // Get the local variables that are annotated with @Raw
    // N.B. Non-ref types variables cannot be @Raw, so we don't have to test for them
    final List<IRNode> inferred = new ArrayList<IRNode>(lvd.getLocal().size());
    for (final IRNode v : lvd.getLocal()) {
      if (!ParameterDeclaration.prototype.includes(v)) {
        if (NonNullRules.getRaw(v) != null) inferred.add(v);
      }
    }
    
    final RawLattice rawLattice = new RawLattice(binder.getTypeEnvironment());
    final RawVariables rawVariables = RawVariables.create(refVars, rawLattice);
    final RawVariables inferredLattice = RawVariables.create(inferred, rawLattice);
    final StateLattice stateLattice = new StateLattice(rawVariables, inferredLattice);
    final Lattice lattice = new Lattice(rawLattice, stateLattice);
    final Transfer t = new Transfer(binder, lattice, 0);
    return new JavaForwardAnalysis<Value, Lattice>("Raw Types", lattice, t, DebugUnparser.viewer);
  }
  
    

  /* The analysis state is two association lists.  The first is a map from all
   * the reference-valued variables in scope to the current raw state of the
   * variable.  The second is a map from all the annotated local variable 
   * declarations (not including parameter declarations) to the inferred
   * annotation for the variable.  This is used to check against any actual 
   * annotation on the variable, which must be greater than the inferred
   * annotation.
   */
  private static final class State extends Pair<Element[], Element[]> {
    public State(final Element[] vars, final Element[] inferred) {
      super(vars, inferred);
    }
  }
  
  private static final class StateLattice extends PairLattice<
      Element[], Element[], RawVariables, RawVariables, State> {
    /**
     * When present, the receiver is always the first element in the associative
     * array.
     */
    private static final int THIS = 0;

    
       
    public StateLattice(final RawVariables l1, final RawVariables l2) {
      super(l1, l2);
    }

    public RawVariables getInferredLattice() {
      return lattice2;
    }
    
    @Override
    protected State newPair(final Element[] v1, final Element[] v2) {
      return new State(v1, v2);
    }
    
    public State getEmptyValue() {
      return new State(lattice1.getEmptyValue(), lattice2.getEmptyValue());
    }
    
    
    
    public int getNumVariables() {
      return lattice1.getRealSize();
    }
    
    public IRNode getVariable(final int i) {
      return lattice1.getKey(i);
    }
    
    public Element injectClass(final IJavaDeclaredType t) {
      return lattice1.getBaseLattice().injectClass(t);
    }
    
    public Element injectPromiseDrop(final RawPromiseDrop pd) {
      return lattice1.getBaseLattice().injectPromiseDrop(pd);
    }
    
    public boolean containsThis() {
      return ReceiverDeclaration.prototype.includes(lattice1.getKey(THIS));
    }
    
    public State setThis(final State v, final Element e) {
      return newPair(lattice1.replaceValue(v.first(), THIS, e), v.second()); 
    }
    
    public Element getThis(final State v) {
      return v.first()[THIS];
    }
    
    public int indexOf(final IRNode var) {
      return lattice1.indexOf(var);
    }
    
    public State setVar(final State v, final int idx, final Element e) {
      return newPair(lattice1.replaceValue(v.first(), idx, e), v.second());
    }
    
    public Element getVar(final State v, final int idx) {
      return v.first()[idx];
    }
    
    public int indexOfInferred(final IRNode var) {
      return lattice2.indexOf(var);
    }
    
    public State inferVar(final State v, final int idx, final Element e) {
      final Element current = v.second()[idx];
      final Element joined = lattice2.getBaseLattice().join(current, e);
      return newPair(v.first(), lattice2.replaceValue(v.second(), idx, joined));
      
    }
  }
  
  public static final class Value extends EvaluationStackLattice.Pair<Element, State> {
    public Value(final ImmutableList<Element> v1, final State v2) {
      super(v1, v2);
    }
  }
  
  public static final class Lattice extends EvaluationStackLattice<
      Element, State, RawLattice, StateLattice, Value> {
    protected Lattice(final RawLattice l1, final StateLattice l2) {
      super(l1, l2);
    }

    public RawVariables getInferredLattice() {
      return lattice2.getInferredLattice();
    }
    
    @Override
    protected Value newPair(final ImmutableList<Element> v1, final State v2) {
      return new Value(v1, v2);
    }

    @Override
    public Element getAnonymousStackValue() {
      return RawLattice.NOT_RAW;
    }
    
    
    
    public int getNumVariables() {
      return lattice2.getNumVariables();
    }
    
    public IRNode getVariable(final int i) {
      return lattice2.getVariable(i);
    }
    
    public Value getEmptyValue() {
      return newPair(ImmutableList.<Element>nil(), lattice2.getEmptyValue());
    }
    
    public Element injectClass(final IJavaDeclaredType t) {
      return lattice2.injectClass(t);
    }
    
    public Element injectPromiseDrop(final RawPromiseDrop pd) {
      return lattice2.injectPromiseDrop(pd);
    }
    
    public boolean containsThis() {
      return lattice2.containsThis();
    }
    
    public Value setThis(final Value v, final Element e) {
      return newPair(v.first(), lattice2.setThis(v.second(), e)); 
    }
    
    public Element getThis(final Value v) {
      return lattice2.getThis(v.second());
    }
    
    public int indexOf(final IRNode var) {
      return lattice2.indexOf(var);
    }
    
    public Value setVar(final Value v, final int idx, final Element e) {
      return newPair(v.first(), lattice2.setVar(v.second(), idx, e));
    }
    
    public Element getVar(final Value v, final int idx) {
      return lattice2.getVar(v.second(), idx);
    }
    
    public int indexOfInferred(final IRNode var) {
      return lattice2.indexOfInferred(var);
    }
    
    public Value inferVar(final Value v, final int idx, final Element e) {
      return newPair(v.first(), lattice2.inferVar(v.second(), idx, e));
    }
  }


  
  private static final class Transfer extends LatticeDelegatingJavaEvaluationTransfer<Lattice, Value, Element> {
    public Transfer(final IBinder binder, final Lattice lattice, final int floor) {
      super(binder, lattice, new SubAnalysisFactory(), floor);
    }


    
    @Override
    public Value transferComponentSource(final IRNode node) {
      Value value = lattice.getEmptyValue();

      if (lattice.containsThis()) {
        /* Receiver is completely raw at the start of constructors
         * and instance initializer blocks.  Receiver is based on the
         * annotation at the start of non-static methods.
         */
        if (MethodDeclaration.prototype.includes(node)) {
          final RawPromiseDrop pd = NonNullRules.getRaw(lattice.getVariable(0));
          if (pd != null) {
            value = lattice.setThis(value, lattice.injectPromiseDrop(pd));
          }
        } else {
          value = lattice.setThis(value, RawLattice.RAW);
        }
      }
      
      /* Parameters are initialized based on annotations */
      for (int idx = 0; idx < lattice.getNumVariables(); idx++) {
        final IRNode v = lattice.getVariable(idx);
        if (ParameterDeclaration.prototype.includes(v)) {
          final RawPromiseDrop pd = NonNullRules.getRaw(v);
          if (pd != null) {
            value = lattice.setVar(value, idx, lattice.injectPromiseDrop(pd));
          }
        }
      }

      return value;
    }

    @Override
    protected Value pushMethodReturnValue(final IRNode node, final Value val) {
      // push the value based on the annotation of the method's return node
      final IRNode methodDecl = binder.getBinding(node);
      final IRNode returnNode = JavaPromise.getReturnNode(methodDecl);
      if (returnNode != null) {
        final RawPromiseDrop pd = NonNullRules.getRaw(returnNode);
        if (pd != null) {
          return push(val, lattice.injectPromiseDrop(pd));
        }
      }
      // Void return or no annotatioN: not raw
      return push(val, RawLattice.NOT_RAW);
    }
    
    /*
     * In order to make transfer functions strict, we check at the beginning of
     * each whether we have bottom or not.
     */

    @Override
    protected Value transferAllocation(final IRNode node, final Value val) {
      // new expressions always return fully initialized values.
      return push(val, RawLattice.NOT_RAW);
    }
    
    @Override
    protected Value transferArrayCreation(final IRNode node, Value val) {
      if (!lattice.isNormal(val)) return val;
      
      // new arrays are always fully initialized values
      if (DimExprs.prototype.includes(tree.getOperator(node))) {
        val = pop(val, tree.numChildren(node));
      }
      return push(val, RawLattice.NOT_RAW);
    }
    
    @Override
    protected Value transferAssignVar(final IRNode use, final Value val) {
      if (!lattice.isNormal(val)) return val;
 
      // transfer the state of the stack into the variable
      return setVar(binder.getIBinding(use).getNode(), val);
    }

    private Value setVar(final IRNode varDecl, final Value val) {
      final int idx = lattice.indexOf(varDecl);
      if (idx != -1) {
        final Element rawState = lattice.peek(val);
        Value newValue = lattice.setVar(val, idx, rawState);
        final int inferredIdx = lattice.indexOfInferred(varDecl);
        if (inferredIdx != -1) {
          newValue = lattice.inferVar(newValue, inferredIdx, rawState);
        }
        return newValue;
      } else {
        return val;
      }
    }
    
    @Override
    protected Value transferConstructorCall(
        final IRNode node, final boolean flag, final Value value) {
      if (!lattice.isNormal(value)) return value;
      
      if (flag) {
        final ITypeEnvironment typeEnv = binder.getTypeEnvironment();
        if (ConstructorCall.prototype.includes(node)) {
          if (SuperExpression.prototype.includes(ConstructorCall.getObject(node))) {
            /* Initialized up to the superclass type.  ConstructorCall expressions
             * can only appear inside of a constructor declaration, which in turn
             * can only appear in a class declaration.
             */
            final IRNode classDecl = VisitUtil.getEnclosingType(node);
            final Element rcvrState = lattice.injectClass(
                typeEnv.getSuperclass(
                    (IJavaDeclaredType) typeEnv.getMyThisType(classDecl)));
            return lattice.setThis(value, rcvrState);
          } else { // ThisExpression
            return lattice.setThis(value, RawLattice.NOT_RAW);
          }
        } else if (AnonClassExpression.prototype.includes(node)) {
          final IRNode superClassDecl =
              binder.getBinding(AnonClassExpression.getType(node));
          final Element rcvrState= lattice.injectClass(
              (IJavaDeclaredType) typeEnv.getMyThisType(superClassDecl));
          return lattice.setThis(value, rcvrState);
        } else if (ImpliedEnumConstantInitialization.prototype.includes(node)
            && EnumConstantClassDeclaration.prototype.includes(tree.getParent(node))) {
          /* The immediately enclosing type is the EnumConstantClassDeclaration
           * node.  We need to go up another level to get the EnumDeclaration.
           */
          final IRNode superClassDecl =
              VisitUtil.getEnclosingType(VisitUtil.getEnclosingType(node));
          final Element rcvrState = lattice.injectClass(
              (IJavaDeclaredType) typeEnv.getMyThisType(superClassDecl));
          return lattice.setThis(value, rcvrState);
        } else { // Not sure why it should ever get here
          throw new IllegalStateException(
              "transferConstructorCall() called with a " +
                  JJNode.tree.getOperator(node).name() + " node");
        }
      } else {
        // exceptional branch: object is not initialized to anything
        return null;
      }
    }

    /*
     * Consider for later: transferEq may be interesting. On the equal branch we
     * know that both sides refer to the same object, so we can use the most
     * specific raw type (MEET of the two values).
     */

    @Override
    protected Value transferInitializationOfVar(final IRNode node, final Value val) {
      if (!lattice.isNormal(val)) return val;
      return pop(setVar(node, val));
    }
    
    // Start with considering transferInstanceOf()
    
    @Override
    protected Value transferUseReceiver(final IRNode use, final Value val) {
      if (!lattice.isNormal(val)) return val;
      return lattice.push(val, lattice.getThis(val));
    }
    
    @Override
    protected Value transferUseQualifiedReceiver(
        final IRNode use, final IRNode binding, final Value val) {
      if (!lattice.isNormal(val)) return val;
      
      // Qualified receiver is always fully initialized
      return lattice.push(val, RawLattice.NOT_RAW);
    }
    
    @Override
    protected Value transferUseVar(final IRNode use, final Value val) {
      if (!lattice.isNormal(val)) return val;
      
      // Push the variable state onto the stack
      final IRNode var = binder.getIBinding(use).getNode();
      final int idx = var == null ? -1 : lattice.indexOf(var);
      if (idx != -1) {
        return lattice.push(val, lattice.getVar(val, idx));
      } else {
        return lattice.push(val, RawLattice.NOT_RAW);
      }
    }
  }
  
  
  
  private static final class SubAnalysisFactory extends AbstractCachingSubAnalysisFactory<Lattice, Value> {
    @Override
    protected JavaForwardAnalysis<Value, Lattice> realCreateAnalysis(
        final IRNode caller, final IBinder binder,
        final Lattice lattice,
        final Value initialValue,
        final boolean terminationNormal) {
      final Transfer t = new Transfer(binder, lattice, 0);
      return new JavaForwardAnalysis<Value, Lattice>("sub analysis", lattice, t, DebugUnparser.viewer);
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



  public Query getRawTypeQuery(final IRNode flowUnit) {
    return new Query(getAnalysisThunk(flowUnit));
  }
  
  public InferredRawQuery getInferredRawQuery(final IRNode flowUnit) {
    return new InferredRawQuery(getAnalysisThunk(flowUnit));
  }
  
  public DebugQuery getDebugQuery(final IRNode flowUnit) {
    return new DebugQuery(getAnalysisThunk(flowUnit));
  }
}
