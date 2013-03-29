package com.surelogic.analysis.nullable;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.surelogic.analysis.AnalysisUtils;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.InstanceInitAction;
import com.surelogic.analysis.JavaSemanticsVisitor;
import com.surelogic.analysis.LocalVariableDeclarations;
import com.surelogic.analysis.StackEvaluatingAnalysisWithInference;
import com.surelogic.analysis.StackEvaluatingAnalysisWithInference.EvalValue;
import com.surelogic.analysis.StackEvaluatingAnalysisWithInference.EvalLattice;
import com.surelogic.analysis.StackEvaluatingAnalysisWithInference.StatePair;
import com.surelogic.analysis.StackEvaluatingAnalysisWithInference.StatePairLattice;
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
import edu.cmu.cs.fluid.java.operator.QualifiedThisExpression;
import edu.cmu.cs.fluid.java.operator.SuperExpression;
import edu.cmu.cs.fluid.java.promise.InitDeclaration;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.ImmutableList;
import edu.uwm.cs.fluid.java.control.AbstractCachingSubAnalysisFactory;
import edu.uwm.cs.fluid.java.control.IJavaFlowAnalysis;
import edu.uwm.cs.fluid.java.control.JavaForwardAnalysis;
import edu.uwm.cs.fluid.java.control.LatticeDelegatingJavaEvaluationTransfer;


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
public final class RawTypeAnalysis 
extends StackEvaluatingAnalysisWithInference<
    Element, RawTypeAnalysis.Value,
    RawLattice, RawTypeAnalysis.Lattice>
implements IBinderClient {
  public final class StackQuery extends SimplifiedJavaFlowAnalysisQuery<StackQuery, Element, Value, Lattice> {
    public StackQuery(final IThunk<? extends IJavaFlowAnalysis<Value, Lattice>> thunk) {
      super(thunk);
    }
    
    private StackQuery(final Delegate<StackQuery, Element, Value, Lattice> d) {
      super(d);
    }

    @Override
    protected RawResultFactory getRawResultFactory() {
      return RawResultFactory.NORMAL_EXIT;
    }


    
    @Override
    protected StackQuery newSubAnalysisQuery(final Delegate<StackQuery, Element, Value, Lattice> d) {
      return new StackQuery(d);
    }


    
    @Override
    protected Element processRawResult(final IRNode expr,
        final Lattice lattice, final Value rawResult) {
      return lattice.peek(rawResult);
    }    
  }
  
  
  
  public final class Query extends SimplifiedJavaFlowAnalysisQuery<Query, Pair<Lattice, Element[]>, Value, Lattice> {
    public Query(final IThunk<? extends IJavaFlowAnalysis<Value, Lattice>> thunk) {
      super(thunk);
    }
    
    private Query(final Delegate<Query, Pair<Lattice, Element[]>, Value, Lattice> d) {
      super(d);
    }
    
    @Override
    protected RawResultFactory getRawResultFactory() {
      return RawResultFactory.ENTRY;
    }

    @Override
    protected Pair<Lattice, Element[]> processRawResult(
        final IRNode expr, final Lattice lattice, final Value rawResult) {
      return new Pair<Lattice, Element[]>(lattice, rawResult.second().first());
    }

    @Override
    protected Query newSubAnalysisQuery(final Delegate<Query, Pair<Lattice, Element[]>, Value, Lattice> d) {
      return new Query(d);
    }
  }
  
  
  
  public final class QualifiedThisQuery extends SimplifiedJavaFlowAnalysisQuery<QualifiedThisQuery, Element, Value, Lattice> {
    public QualifiedThisQuery(final IThunk<? extends IJavaFlowAnalysis<Value, Lattice>> thunk) {
      super(thunk);
    }
    
    private QualifiedThisQuery(final Delegate<QualifiedThisQuery, Element, Value, Lattice> d) {
      super(d);
    }
    
    @Override
    protected RawResultFactory getRawResultFactory() {
      return RawResultFactory.NORMAL_EXIT;
    }

    @Override
    protected Element processRawResult(
        final IRNode expr, final Lattice lattice, final Value rawResult) {
      /* Look at the top value on the stack, that is, get the value that 
       * was pushed for handling the QualifiedThisExpression. 
       */
      return lattice.peek(rawResult);
    }

    @Override
    protected QualifiedThisQuery newSubAnalysisQuery(final Delegate<QualifiedThisQuery, Element, Value, Lattice> d) {
      return new QualifiedThisQuery(d);
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
  
  
  
  public final class InferredRaw
  extends Result<RawLattice.Element, RawLattice, RawPromiseDrop> {
    protected InferredRaw(
        final IRNode[] keys, final InferredPair<Element>[] val,
        final RawLattice sl) {
      super(keys, val, sl);
    }

    @Override
    public RawPromiseDrop getPromiseDrop(final IRNode n) {
      return NonNullRules.getRaw(n);
    }
    
    @Override
    public Element injectPromiseDrop(final RawPromiseDrop pd) {
      return inferredStateLattice.injectPromiseDrop(pd);
    }
  }
  
  
  
  public final class InferredRawQuery
  extends InferredVarStateQuery<InferredRawQuery, RawLattice.Element, Value, RawLattice, Lattice, InferredRaw> {
    protected InferredRawQuery(
        final IThunk<? extends IJavaFlowAnalysis<Value, Lattice>> thunk) {
      super(thunk);
    }
    
    protected InferredRawQuery(
        final Delegate<InferredRawQuery, InferredRaw, Value, Lattice> d) {
      super(d);
    }
    
    @Override
    protected InferredRaw processRawResult(
        final IRNode expr, final Lattice lattice, final Value rawResult) {
      return new InferredRaw(
          lattice.getInferredStateKeys(),
          rawResult.second().second(),
          lattice.getInferredStateLattice());
    }

    @Override
    protected InferredRawQuery newSubAnalysisQuery(
        final Delegate<InferredRawQuery, InferredRaw, Value, Lattice> delegate) {
      return new InferredRawQuery(delegate);
    }
  }
  
  
  
  public RawTypeAnalysis(final IBinder b) {
    super(b);
  }

  
  
  @Override
  protected JavaForwardAnalysis<Value, Lattice> createAnalysis(final IRNode flowUnit) {
    final LocalVariableDeclarations lvd = LocalVariableDeclarations.getDeclarationsFor(flowUnit);
    final List<IRNode> refVars = new ArrayList<IRNode>(
        lvd.getLocal().size() + lvd.getExternal().size() + lvd.getReceivers().size());

    // Add the receivers
    refVars.addAll(lvd.getReceivers());

    // Add all reference-typed variables in scope
    LocalVariableDeclarations.separateDeclarations(
        binder, lvd.getLocal(), refVars, NullList.<IRNode>prototype());
    LocalVariableDeclarations.separateDeclarations(
        binder, lvd.getExternal(), refVars, NullList.<IRNode>prototype());
    
    // Get the local variables that are annotated with @Raw
    // N.B. Non-ref types variables cannot be @Raw, so we don't have to test for them
    final List<IRNode> varsToInfer = new ArrayList<IRNode>(lvd.getLocal().size());
    for (final IRNode v : lvd.getLocal()) {
      if (!ParameterDeclaration.prototype.includes(v)) {
        if (NonNullRules.getRaw(v) != null) varsToInfer.add(v);
      }
    }
    
    /* If the flow unit is a constructor C(), get all the uses of the  
     * qualified receiver "C.this" that appear along the initialization control
     * path within anonymous classes.
     */
    final Set<IRNode> uses;
    if (ConstructorDeclaration.prototype.includes(flowUnit)) {
      uses = QualifiedThisVisitor.getUses(flowUnit, getBinder());
    } else {
      uses = Collections.<IRNode>emptySet();
    }
    
    final RawLattice rawLattice = new RawLattice(binder.getTypeEnvironment());
    final RawVariables rawVariables = RawVariables.create(refVars, rawLattice, uses);
    final StateLattice stateLattice = new StateLattice(rawVariables, rawLattice, varsToInfer);
    final Lattice lattice = new Lattice(rawLattice, stateLattice);
    final Transfer t = new Transfer(flowUnit, binder, lattice, 0);
    return new JavaForwardAnalysis<Value, Lattice>("Raw Types", lattice, t, DebugUnparser.viewer);
  }
  
  
  
  /**
   * Used to visit a constructor declaration from class <code>C</code> and finds
   * all the uses of the qualified receiver <code>C.this</code> that appears in
   * the initialization of any anonymous class expressions that appear along the
   * flow of control of the constructor. These uses are interesting because they
   * capture the object in a raw state: RAW(X) where X is the superclass of
   * <code>C</code>. Otherwise uses of qualified receivers are uninteresting.
   */
  private final static class QualifiedThisVisitor extends JavaSemanticsVisitor {
    private final IBinder binder;
    private final IRNode qualifyingTypeDecl;
    private final Set<IRNode> uses;
    private int depth;
    
    private QualifiedThisVisitor(final IRNode cdecl, final IBinder b) {
      super(false, cdecl);
      binder = b;
      qualifyingTypeDecl = getEnclosingType(); // initialized in super constructor
      uses = new HashSet<IRNode>();
      depth = 0;
    }
    
    public static Set<IRNode> getUses(final IRNode cdecl, final IBinder b) {
      final QualifiedThisVisitor v = new QualifiedThisVisitor(cdecl, b);
      v.doAccept(cdecl);
      return v.uses;
    }
    
    @Override
    protected InstanceInitAction getAnonClassInitAction(
        final IRNode expr, final IRNode classBody) {
      return new InstanceInitAction() {
        @Override
        public void tryBefore() { depth += 1; }

        @Override
        public void finallyAfter() { depth -=1; }
        
        @Override
        public void afterVisit() { /* empty */ }
      };
    }
    
    @Override
    public Void visitQualifiedThisExpression(final IRNode node) {
      if (depth > 0) {
        final IRNode outerType =
            binder.getBinding(QualifiedThisExpression.getType(node));
        if (outerType.equals(qualifyingTypeDecl)) {
          uses.add(node);
        }
      }
      return null;
    }
  }
    
  

  /* The analysis state is two association lists.  The first is a map from all
   * the reference-valued variables in scope to the current raw state of the
   * variable.  The second is a map from all the annotated local variable 
   * declarations (not including parameter declarations) to the inferred
   * annotation for the variable.  This is used to check against any actual 
   * annotation on the variable, which must be greater than the inferred
   * annotation.
   */
  static final class State extends StatePair<Element[], Element> {
    public State(final Element[] vars, final InferredPair<Element>[] inferred) {
      super(vars, inferred);
    }
  }
  
  static final class StateLattice extends StatePairLattice<
      Element[], Element, State, RawVariables, RawLattice> {
    public StateLattice(final RawVariables l1, final RawLattice l2,
        final List<IRNode> keys) {
      super(l1, l2, keys);
    }
    
    @Override
    protected State newPair(final Element[] v1, final InferredPair<Element>[] v2) {
      return new State(v1, v2);
    }
    
    public State getEmptyValue() {
      return new State(lattice1.getEmptyValue(), getEmptyInferredValue());
    }
    
    
    
    public int getNumVariables() {
      return lattice1.getSize();
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

    public State setThis(final State v, final IRNode rcvrDecl, final Element e) {
      return newPair(lattice1.replaceValue(v.first(), rcvrDecl, e), v.second());
    }
    
    public Element getThis(final State v, final IRNode rcvrDecl) {
      return v.first()[lattice1.indexOf(rcvrDecl)];
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
    
    public boolean isInterestingQualifiedThis(final IRNode use) {
      return lattice1.isInterestingQualifiedThis(use);
    }
    
    // For debugging
    public String qualifiedThisToString() {
      return lattice1.qualifiedThisToString();
    }
  }
  
  public static final class Value extends EvalValue<
      Element, Element[], Element, State> {
    public Value(final ImmutableList<Element> v1, final State v2) {
      super(v1, v2);
    }
  }
  
  public static final class Lattice extends EvalLattice<
      Element, Element, State, Value,
      RawLattice, RawLattice, StateLattice> {
    protected Lattice(final RawLattice l1, final RawTypeAnalysis.StateLattice l2) {
      super(l1, l2);
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
    
    public Value setThis(final Value v, final IRNode rcvrDecl, final Element e) {
      return newPair(v.first(), lattice2.setThis(v.second(), rcvrDecl, e));
    }
    
    public Element getThis(final Value v, final IRNode rcvrDecl) {
      return lattice2.getThis(v.second(), rcvrDecl);
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
    
    public boolean isInterestingQualifiedThis(final IRNode use) {
      return lattice2.isInterestingQualifiedThis(use);
    }
    
    // For debugging
    public String qualifiedThisToString() {
      return lattice2.qualifiedThisToString();
    }
  }


  
  private static final class Transfer extends LatticeDelegatingJavaEvaluationTransfer<Lattice, Value, Element> {
    private final IRNode flowUnit;
    
    public Transfer(final IRNode fu, final IBinder binder, final Lattice lattice, final int floor) {
      super(binder, lattice, new SubAnalysisFactory(fu), floor);
      flowUnit = fu;
    }


    
    @Override
    public Value transferComponentSource(final IRNode node) {
      Value value = lattice.getEmptyValue(); // everything is NOT_RAW

      /* Receiver is completely raw at the start of constructors
       * and instance initializer blocks.  Receiver is based on the
       * annotation at the start of non-static methods.
       */
      final Operator op = JJNode.tree.getOperator(node);
      if (ConstructorDeclaration.prototype.includes(op) ||
          InitDeclaration.prototype.includes(op)) {
        value = lattice.setThis(value, JavaPromise.getReceiverNode(node), RawLattice.RAW);
      } else if (MethodDeclaration.prototype.includes(op) && !TypeUtil.isStatic(node)) {
        final IRNode rcvr = JavaPromise.getReceiverNode(node);
        final RawPromiseDrop pd = NonNullRules.getRaw(rcvr);
        if (pd != null) {
          value = lattice.setThis(value, rcvr, lattice.injectPromiseDrop(pd));
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
      return setVar(binder.getIBinding(use).getNode(), val, use);
    }

    private Value setVar(final IRNode varDecl, final Value val, final IRNode src) {
      final int idx = lattice.indexOf(varDecl);
      if (idx != -1) {
        final Element rawState = lattice.peek(val);
        Value newValue = lattice.setVar(val, idx, rawState);
        final int inferredIdx = lattice.indexOfInferred(varDecl);
        if (inferredIdx != -1) {
          newValue = lattice.inferVar(newValue, inferredIdx, rawState, src);
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
          final IRNode rcvrDecl = AnalysisUtils.getReceiverNodeAtExpression(node, flowUnit);
          if (SuperExpression.prototype.includes(ConstructorCall.getObject(node))) {
            /* Initialized up to the superclass type.  ConstructorCall expressions
             * can only appear inside of a constructor declaration, which in turn
             * can only appear in a class declaration.
             */
            final IRNode classDecl = VisitUtil.getEnclosingType(node);
            final Element rcvrState = lattice.injectClass(
                typeEnv.getSuperclass(
                    (IJavaDeclaredType) typeEnv.getMyThisType(classDecl)));
            return lattice.setThis(value, rcvrDecl, rcvrState);
          } else { // ThisExpression
            return lattice.setThis(value, rcvrDecl, RawLattice.NOT_RAW);
          }
        } else if (AnonClassExpression.prototype.includes(node)) {
          final IRNode superClassDecl =
              binder.getBinding(AnonClassExpression.getType(node));
          final Element rcvrState = lattice.injectClass(
              (IJavaDeclaredType) typeEnv.getMyThisType(superClassDecl));
          final IRNode rcvrDecl =
              JavaPromise.getReceiverNode(JavaPromise.getInitMethod(node));
          return lattice.setThis(value, rcvrDecl, rcvrState);
        } else if (ImpliedEnumConstantInitialization.prototype.includes(node)
            && EnumConstantClassDeclaration.prototype.includes(tree.getParent(node))) {
          /* The immediately enclosing type is the EnumConstantClassDeclaration
           * node.  We need to go up another level to get the EnumDeclaration.
           */
          final IRNode superClassDecl =
              VisitUtil.getEnclosingType(VisitUtil.getEnclosingType(node));
          final Element rcvrState = lattice.injectClass(
              (IJavaDeclaredType) typeEnv.getMyThisType(superClassDecl));
          final IRNode rcvrDecl =
              JavaPromise.getReceiverNode(JavaPromise.getInitMethod(
                  tree.getParent(node)));
          return lattice.setThis(value, rcvrDecl, rcvrState);
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
      return pop(setVar(node, val, node));
    }
    
    // Start with considering transferInstanceOf()
    
    @Override
    protected Value transferUseReceiver(final IRNode use, final Value val) {
      if (!lattice.isNormal(val)) return val;
      return lattice.push(val, lattice.getThis(val,
          AnalysisUtils.getReceiverNodeAtExpression(use, flowUnit)));
    }
    
    @Override
    protected Value transferUseQualifiedReceiver(
        final IRNode use, final IRNode binding, final Value val) {
      if (!lattice.isNormal(val)) return val;
      
      /* Qualified receiver is fully initialized, unless it appears in the 
       * initialization of an anonymous class created during the initialization
       * of the class itself, in which case it is Raw(X), where X is the super
       * class of the class under initialization.
       */
      if (lattice.isInterestingQualifiedThis(use)) {
        final IJavaDeclaredType qualifyingType =
            QualifiedReceiverDeclaration.getJavaType(binder, binding);
        return lattice.push(val,
            lattice.injectClass(
                qualifyingType.getSuperclass(binder.getTypeEnvironment())));
      } else {
        return lattice.push(val, RawLattice.NOT_RAW);
      }
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
    private final IRNode flowUnit;
    
    private SubAnalysisFactory(final IRNode fu) {
      flowUnit = fu;
    }
    
    @Override
    protected JavaForwardAnalysis<Value, Lattice> realCreateAnalysis(
        final IRNode caller, final IBinder binder,
        final Lattice lattice,
        final Value initialValue,
        final boolean terminationNormal) {
      final Transfer t = new Transfer(flowUnit, binder, lattice, 0);
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


  
  public StackQuery getStackQuery(final IRNode flowUnit) {
    return new StackQuery(getAnalysisThunk(flowUnit));
  }
  
  public Query getRawTypeQuery(final IRNode flowUnit) {
    return new Query(getAnalysisThunk(flowUnit));
  }
  
  public QualifiedThisQuery getQualifiedThisQuery(final IRNode flowUnit) {
    return new QualifiedThisQuery(getAnalysisThunk(flowUnit));
  }
  
  public DebugQuery getDebugQuery(final IRNode flowUnit) {
    return new DebugQuery(getAnalysisThunk(flowUnit));
  }
  
  public InferredRawQuery getInferredRawQuery(final IRNode flowUnit) {
    return new InferredRawQuery(getAnalysisThunk(flowUnit));
  }
}
