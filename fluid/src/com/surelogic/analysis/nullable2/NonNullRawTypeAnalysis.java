package com.surelogic.analysis.nullable2;


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
import com.surelogic.analysis.nullable2.NonNullRawLattice.ClassElement;
import com.surelogic.analysis.nullable2.NonNullRawLattice.Element;
import com.surelogic.analysis.nullable2.StackEvaluatingAnalysisWithInference;
import com.surelogic.annotation.rules.NonNullRules;
import com.surelogic.common.Pair;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.nullable.RawPromiseDrop;
import com.surelogic.util.IThunk;
import com.surelogic.util.NullList;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.analysis.SimplifiedJavaFlowAnalysisQuery;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.AssignmentInterface;
import edu.cmu.cs.fluid.java.operator.CallInterface;
import edu.cmu.cs.fluid.java.operator.CatchClause;
import edu.cmu.cs.fluid.java.operator.ConstructorCall;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.CrementExpression;
import edu.cmu.cs.fluid.java.operator.DimExprs;
import edu.cmu.cs.fluid.java.operator.EnumConstantClassDeclaration;
import edu.cmu.cs.fluid.java.operator.EnumConstantDeclaration;
import edu.cmu.cs.fluid.java.operator.ImpliedEnumConstantInitialization;
import edu.cmu.cs.fluid.java.operator.InstanceOfExpression;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.NoInitialization;
import edu.cmu.cs.fluid.java.operator.NullLiteral;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.QualifiedThisExpression;
import edu.cmu.cs.fluid.java.operator.SuperExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.promise.InitDeclaration;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.uwm.cs.fluid.java.control.AbstractCachingSubAnalysisFactory;
import edu.uwm.cs.fluid.java.control.IJavaFlowAnalysis;
import edu.uwm.cs.fluid.java.control.JavaForwardAnalysis;
import edu.uwm.cs.fluid.java.control.LatticeDelegatingJavaEvaluationTransfer;
import edu.uwm.cs.fluid.util.UnionLattice;


/**
 * TODO
 */
public final class NonNullRawTypeAnalysis 
extends StackEvaluatingAnalysisWithInference
implements IBinderClient {
  public final class StackQuery extends SimplifiedJavaFlowAnalysisQuery<StackQuery, StackQueryResult, EvalValue, EvalLattice> {
    public StackQuery(final IThunk<? extends IJavaFlowAnalysis<EvalValue, EvalLattice>> thunk) {
      super(thunk);
    }
    
    private StackQuery(final Delegate<StackQuery, StackQueryResult, EvalValue, EvalLattice> d) {
      super(d);
    }

    @Override
    protected RawResultFactory getRawResultFactory() {
      return RawResultFactory.NORMAL_EXIT;
    }


    
    @Override
    protected StackQuery newSubAnalysisQuery(final Delegate<StackQuery, StackQueryResult, EvalValue, EvalLattice> d) {
      return new StackQuery(d);
    }


    
    @Override
    protected StackQueryResult processRawResult(final IRNode expr,
        final EvalLattice lattice, final EvalValue rawResult) {
      return new StackQueryResult(
          lattice.getStackElementLattice().getElementLattice(),
          lattice.peek(rawResult),
          lattice.getLocalStateLattice(),
          rawResult.second().first());
    }    
  }
  
  
  public static final class StackQueryResult {
    private final NonNullRawLattice nonNullLattice;
    private final Base value;
    
    private final LocalStateLattice localLattice;
    private final Base locals[];
    
    
    
    private StackQueryResult(
        final NonNullRawLattice l, final Base v,
        final LocalStateLattice lsl, final Base[] lcls) {
      nonNullLattice = l;
      value = v;
      localLattice = lsl;
      locals = lcls;
    }
    
    public NonNullRawLattice getLattice() { return nonNullLattice; }
    public Element getValue() { return value.first(); }
    public Set<Source> getSources() { return value.second(); }
    
    public Base lookupVar(final IRNode decl) {
      return locals[localLattice.indexOf(decl)];
    }
  }
  
  
  
  public final class Query extends SimplifiedJavaFlowAnalysisQuery<Query, Pair<EvalLattice, Base[]>, EvalValue, EvalLattice> {
    public Query(final IThunk<? extends IJavaFlowAnalysis<EvalValue, EvalLattice>> thunk) {
      super(thunk);
    }
    
    private Query(final Delegate<Query, Pair<EvalLattice, Base[]>, EvalValue, EvalLattice> d) {
      super(d);
    }
    
    @Override
    protected RawResultFactory getRawResultFactory() {
      return RawResultFactory.ENTRY;
    }

    @Override
    protected Pair<EvalLattice, Base[]> processRawResult(
        final IRNode expr, final EvalLattice lattice, final EvalValue rawResult) {
      return new Pair<EvalLattice, Base[]>(lattice, rawResult.second().first());
    }

    @Override
    protected Query newSubAnalysisQuery(final Delegate<Query, Pair<EvalLattice, Base[]>, EvalValue, EvalLattice> d) {
      return new Query(d);
    }
  }
  
  
  
  public final class QualifiedThisQuery extends SimplifiedJavaFlowAnalysisQuery<QualifiedThisQuery, Element, EvalValue, EvalLattice> {
    public QualifiedThisQuery(final IThunk<? extends IJavaFlowAnalysis<EvalValue, EvalLattice>> thunk) {
      super(thunk);
    }
    
    private QualifiedThisQuery(final Delegate<QualifiedThisQuery, Element, EvalValue, EvalLattice> d) {
      super(d);
    }
    
    @Override
    protected RawResultFactory getRawResultFactory() {
      return RawResultFactory.NORMAL_EXIT;
    }

    @Override
    protected Element processRawResult(
        final IRNode expr, final EvalLattice lattice, final EvalValue rawResult) {
      /* Look at the top value on the stack, that is, get the value that 
       * was pushed for handling the QualifiedThisExpression. 
       */
      return lattice.peek(rawResult).first();
    }

    @Override
    protected QualifiedThisQuery newSubAnalysisQuery(final Delegate<QualifiedThisQuery, Element, EvalValue, EvalLattice> d) {
      return new QualifiedThisQuery(d);
    }
  }
  
  
  
  public final class DebugQuery extends SimplifiedJavaFlowAnalysisQuery<DebugQuery, String, EvalValue, EvalLattice> {
    public DebugQuery(final IThunk<? extends IJavaFlowAnalysis<EvalValue, EvalLattice>> thunk) {
      super(thunk);
    }
    
    private DebugQuery(final Delegate<DebugQuery, String, EvalValue, EvalLattice> d) {
      super(d);
    }
    
    @Override
    protected RawResultFactory getRawResultFactory() {
      return RawResultFactory.NORMAL_EXIT;
    }

    @Override
    protected String processRawResult(
        final IRNode expr, final EvalLattice lattice, final EvalValue rawResult) {
      return rawResult == null ? "null" : lattice.toString(rawResult);
    }

    @Override
    protected DebugQuery newSubAnalysisQuery(final Delegate<DebugQuery, String, EvalValue, EvalLattice> d) {
      return new DebugQuery(d);
    }
  }
  
  
  
  public final class Inferred
  extends Result<PromiseDrop<?>> {
    protected Inferred(
        final IRNode[] keys, final Element[] val,
        final NonNullRawLattice sl) {
      super(keys, val, sl);
    }

    @Override
    public PromiseDrop<?> getPromiseDrop(final IRNode n) {
      PromiseDrop<?> pd = NonNullRules.getRaw(n);
      if (pd == null) pd = NonNullRules.getNonNull(n);
      if (pd == null) pd = NonNullRules.getNullable(n);
      return pd;
    }
    
    @Override
    public Element injectPromiseDrop(final PromiseDrop<?> pd) {
      return inferredStateLattice.injectPromiseDrop(pd);
    }
  }
  
  
  
  public final class InferredQuery
  extends InferredVarStateQuery<InferredQuery, Inferred> {
    protected InferredQuery(
        final IThunk<? extends IJavaFlowAnalysis<EvalValue, EvalLattice>> thunk) {
      super(thunk);
    }
    
    protected InferredQuery(
        final Delegate<InferredQuery, Inferred, EvalValue, EvalLattice> d) {
      super(d);
    }
    
    @Override
    protected Inferred processRawResult(
        final IRNode expr, final EvalLattice lattice, final EvalValue rawResult) {
      return new Inferred(
          lattice.getInferredStateKeys(),
          rawResult.second().second(),
          lattice.getInferredStateLattice());
    }

    @Override
    protected InferredQuery newSubAnalysisQuery(
        final Delegate<InferredQuery, Inferred, EvalValue, EvalLattice> delegate) {
      return new InferredQuery(delegate);
    }
  }
  
  
  
  public NonNullRawTypeAnalysis(final IBinder b) {
    super(b);
  }

  
  
  @Override
  protected JavaForwardAnalysis<EvalValue, EvalLattice> createAnalysis(final IRNode flowUnit) {
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
    
    // Get the local variables that are annotated with @Raw or @NonNull
    // N.B. Non-ref types variables cannot be @Raw or @NonNull, so we don't have to test for them
    final List<IRNode> varsToInfer = new ArrayList<IRNode>(lvd.getLocal().size());
    for (final IRNode v : lvd.getLocal()) {
      if (!ParameterDeclaration.prototype.includes(v)) {
        varsToInfer.add(v);
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
    
    final NonNullRawLattice rawLattice = new NonNullRawLattice(binder.getTypeEnvironment());
    final BaseLattice baseLattice = new BaseLattice(rawLattice, new UnionLattice<Source>());
    final LocalStateLattice rawVariables = LocalStateLattice.create(refVars, baseLattice, uses);
    final StatePairLattice stateLattice = new StatePairLattice(rawVariables, new InferredLattice(rawLattice, varsToInfer));
    final EvalLattice lattice = new EvalLattice(baseLattice, stateLattice);
    final Transfer t = new Transfer(flowUnit, binder, lattice, 0);
    return new JavaForwardAnalysis<EvalValue, EvalLattice>("NonNull and Raw Types", lattice, t, DebugUnparser.viewer);
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
//  public static final class Value extends EvalValue {
//    public Value(final ImmutableList<Base> v1, final State v2) {
//      super(v1, v2);
//    }
//  }
  
//  public static final class Lattice extends EvalLattice {
//    protected Lattice(final BaseLattice l1,
//        final NonNullRawTypeAnalysis.StatePairLattice l2) {
//      super(l1, l2);
//    }
//    
//    @Override
//    protected Value newPair(final ImmutableList<Base> v1, final State v2) {
//      return new Value(v1, v2);
//    }
//
//    @Override
//    public Base getAnonymousStackValue() {
//      return lattice1.getBaseLattice().getEmpty();
//    }
//    
//    public BaseLattice getStackElementLattice() {
//      return lattice1.getBaseLattice();
//    }
//    
//    public LocalStateLattice getLocalStateLattice() {
//      return lattice2.getLocalStateLattice();
//    }
//    
//    public NonNullRawLattice getInferredStateLattice() {
//      return lattice2.getInferredStateLattice();
//    }
//    
//    
//    public int getNumVariables() {
//      return lattice2.getNumVariables();
//    }
//    
//    public IRNode getVariable(final int i) {
//      return lattice2.getVariable(i);
//    }
//    
//    public Value getEmptyValue() {
//      return newPair(ImmutableList.<Base>nil(), lattice2.getEmptyValue());
//    }
//    
//    public Base baseValue(final Element e, final Kind k, final IRNode where) {
//      return lattice1.getBaseLattice().newPair(e, EMPTY.addElement(new Source(k, where, e)));
//    }
//    
//    public Element injectClass(final IJavaDeclaredType t) {
//      return lattice2.injectClass(t);
//    }
//    
//    public Element injectPromiseDrop(final RawPromiseDrop pd) {
//      return lattice2.injectPromiseDrop(pd);
//    }
//
//    public Value setThis(final Value v, final IRNode rcvrDecl, final Base b) {
//      return newPair(v.first(), lattice2.setThis(v.second(), rcvrDecl, b));
//    }
//    
//    public Base getThis(final Value v, final IRNode rcvrDecl) {
//      return lattice2.getThis(v.second(), rcvrDecl);
//    }
//    
//    public int indexOf(final IRNode var) {
//      return lattice2.indexOf(var);
//    }
//    
//    public Value setVar(final Value v, final int idx, final Base b) {
//      return newPair(v.first(), lattice2.setVar(v.second(), idx, b));
//    }
//    
//    public Value setVarNonNullIfNotAlready(final Value v, final int idx, final Kind k, final IRNode where) {
//      return newPair(v.first(), lattice2.setVarNonNullIfNotAlready(v.second(), idx, k, where));
//    }
//    
//    public Base getVar(final Value v, final int idx) {
//      return lattice2.getVar(v.second(), idx);
//    }
//    
//    public boolean isInterestingQualifiedThis(final IRNode use) {
//      return lattice2.isInterestingQualifiedThis(use);
//    }
//    
//    // For debugging
//    public String qualifiedThisToString() {
//      return lattice2.qualifiedThisToString();
//    }
//  }


  
  private static final class Transfer extends LatticeDelegatingJavaEvaluationTransfer<EvalLattice, EvalValue, Base> {
    private final IRNode flowUnit;
    
    public Transfer(final IRNode fu, final IBinder binder, final EvalLattice lattice, final int floor) {
      super(binder, lattice, new SubAnalysisFactory(fu), floor);
      flowUnit = fu;
    }


    
    @Override
    public EvalValue transferComponentSource(final IRNode node) {
      /* 
       * Everything is MAYBE_NULL, but we reset them below to capture
       * source information.
       */
      EvalValue value = lattice.getEmptyValue(); 

      /* Receiver is completely raw at the start of constructors
       * and instance initializer blocks.  Receiver is based on the
       * annotation at the start of non-static methods.  Otherwise it 
       * is NOT_NULL.
       */
      final Operator op = JJNode.tree.getOperator(node);
      if (ConstructorDeclaration.prototype.includes(op) ||
          InitDeclaration.prototype.includes(op)) {
        value = lattice.setThis(
            value, JavaPromise.getReceiverNode(node),
            lattice.baseValue(NonNullRawLattice.RAW, Kind.RECEIVER_RAW_OBJECT, node));
      } else if (MethodDeclaration.prototype.includes(op) && !TypeUtil.isStatic(node)) {
        final IRNode rcvr = JavaPromise.getReceiverNode(node);
        final RawPromiseDrop pd = NonNullRules.getRaw(rcvr);
        if (pd != null) {
          value = lattice.setThis(value, rcvr, 
              lattice.baseValue(lattice.injectPromiseDrop(pd), Kind.RECEIVER_METHOD, rcvr));
        } else {
          value = lattice.setThis(value, rcvr, 
              lattice.baseValue(NonNullRawLattice.NOT_NULL, Kind.RECEIVER_METHOD, rcvr));
        }
      }

      /* 
       * Parameters are initialized based on annotations.
       * 
       * Caught exceptions—also parameter declarations—are always NOT_NULL.
       */
      for (int idx = 0; idx < lattice.getNumVariables(); idx++) {
        final IRNode v = lattice.getVariable(idx);
        if (ParameterDeclaration.prototype.includes(v)) {
          final IRNode parent = JJNode.tree.getParent(v);
          if (CatchClause.prototype.includes(parent)) {
            value = lattice.setVar(value, idx,
                lattice.baseValue(NonNullRawLattice.NOT_NULL, Kind.CAUGHT_EXCEPTION, v));
          } else { // normal parameter
            // N.B. Parameter cannot have both @Raw and @NonNull annotations
            final RawPromiseDrop pd = NonNullRules.getRaw(v);
            if (pd != null) {
              value = lattice.setVar(value, idx, 
                  lattice.baseValue(lattice.injectPromiseDrop(pd), Kind.FORMAL_PARAMETER, v));
            } else if (NonNullRules.getNonNull(v) != null) {
              value = lattice.setVar(value, idx,
                  lattice.baseValue(NonNullRawLattice.NOT_NULL, Kind.FORMAL_PARAMETER, v));
            } else { // no annotation or @Nullable
              value = lattice.setVar(value, idx,
                  lattice.baseValue(NonNullRawLattice.MAYBE_NULL, Kind.FORMAL_PARAMETER, v));
            }
          }
        }
      }

      return value;
    }

    @Override
    protected EvalValue pushMethodReturnValue(final IRNode node, final EvalValue val) {
      // push the value based on the annotation of the method's return node
      final IRNode methodDecl = binder.getBinding(node);
      final IRNode returnNode = JavaPromise.getReturnNode(methodDecl);
      if (returnNode != null) {
        // NB. Either @Raw or @NonNull but never both
        if (NonNullRules.getNonNull(returnNode) != null) {
          return push(val, lattice.baseValue(NonNullRawLattice.NOT_NULL, Kind.METHOD_RETURN, node));
        }

        final RawPromiseDrop pd = NonNullRules.getRaw(returnNode);
        if (pd != null) {
          return push(val, lattice.baseValue(lattice.injectPromiseDrop(pd), Kind.METHOD_RETURN, node));
        }
      }
      // Void return or no annotatioN: not raw
      /* N.B. If the method is void return then returnNode is null.  This is
       * okay because the "return value" from the method will be thrown away
       * by the flow analysis. 
       */
      return push(val, lattice.baseValue(NonNullRawLattice.MAYBE_NULL, Kind.METHOD_RETURN, node));
    }
    
    /*
     * In order to make transfer functions strict, we check at the beginning of
     * each whether we have bottom or not.
     */

    @Override
    protected EvalValue transferAllocation(final IRNode node, final EvalValue val) {
      // new expressions always return fully initialized values.
      // XXX: What operators do we have here?  I think NewExpression and AnonClassExpression
      // final Operator op = JJNode.tree.getOperator(node)
      return push(val, lattice.baseValue(NonNullRawLattice.NOT_NULL, Kind.NEW_OBJECT, node));
    }
    
    @Override
    protected EvalValue transferArrayCreation(final IRNode node, EvalValue val) {
      if (!lattice.isNormal(val)) return val;
      
      // new arrays are always fully initialized values
      IRNode arrayCreation = node;
      if (DimExprs.prototype.includes(tree.getOperator(node))) {
        val = pop(val, tree.numChildren(node));
        arrayCreation = tree.getParent(node);
      }
      // XXX: What operators do we have here?  I think ArrayCreationExpression
      // final Operator op = JJNode.tree.getOperator(node)
      return push(val, lattice.baseValue(NonNullRawLattice.NOT_NULL, Kind.NEW_ARRAY, arrayCreation));
    }
    
    @Override
    protected EvalValue transferAssignVar(final IRNode use, final EvalValue val) {
      if (!lattice.isNormal(val)) return val;
 
      // transfer the state of the stack into the variable
      return setVar(binder.getIBinding(use).getNode(), val,
          getAssignmentSource(use));
    }

    private IRNode getAssignmentSource(final IRNode e) {
      IRNode current = e;
      Operator op = null;
      do {
        current = JJNode.tree.getParent(current);
        op = JJNode.tree.getOperator(current);
      } while (!(op instanceof AssignmentInterface));
      return ((AssignmentInterface) op).getSource(current);
    }
    
    private EvalValue setVar(final IRNode varDecl, final EvalValue val, final IRNode src) {
      final int idx = lattice.indexOf(varDecl);
      if (idx != -1) {
        final Base stackState = lattice.peek(val);
        EvalValue newValue = lattice.setVar(val, idx, stackState);
        final int inferredIdx = lattice.indexOfInferred(varDecl);
        if (inferredIdx != -1) {
          newValue = lattice.inferVar(newValue, inferredIdx, stackState.first(), src);
        }
        return newValue;
      } else {
        return val;
      }
    }
    
    @Override
    protected EvalValue transferBox(final IRNode expr, final EvalValue val) {
      if (!lattice.isNormal(val)) return val;
      return lattice.push(lattice.pop(val),
          lattice.baseValue(NonNullRawLattice.NOT_NULL, Kind.BOX, expr));
    }

    @Override
    protected EvalValue transferConcat(final IRNode node, final EvalValue val) {
      if (!lattice.isNormal(val)) return val;
      
      // pop the values of the stack and push a non-null
      EvalValue newValue = lattice.pop(val);
      newValue = lattice.pop(newValue);
      newValue = lattice.push(newValue, 
          lattice.baseValue(NonNullRawLattice.NOT_NULL, Kind.STRING_CONCAT, node));
      return newValue;
    }

    @Override
    protected EvalValue transferConstructorCall(
        final IRNode node, final boolean flag, final EvalValue value) {
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
            return lattice.setThis(value, rcvrDecl, 
                lattice.baseValue(rcvrState, Kind.RECEIVER_CONSTRUCTOR_CALL, node));
          } else { // ThisExpression
            return lattice.setThis(value, rcvrDecl,
                lattice.baseValue(NonNullRawLattice.NOT_NULL, Kind.RECEIVER_CONSTRUCTOR_CALL, node));
          }
        } else if (AnonClassExpression.prototype.includes(node)) {
          final IRNode superClassDecl =
              binder.getBinding(AnonClassExpression.getType(node));
          final Element rcvrState = lattice.injectClass(
              (IJavaDeclaredType) typeEnv.getMyThisType(superClassDecl));
          final IRNode rcvrDecl =
              JavaPromise.getReceiverNode(JavaPromise.getInitMethod(node));
          return lattice.setThis(value, rcvrDecl, 
              lattice.baseValue(rcvrState, Kind.RECEIVER_ANON_CLASS, node));
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
          return lattice.setThis(value, rcvrDecl,
              lattice.baseValue(rcvrState, Kind.RECEIVER_ANON_CLASS, tree.getParent(node)));
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

    @Override
    protected EvalValue transferCrement(
        final IRNode node, final Operator op, final EvalValue val) {
      /*
       * This turns out to be a weird method because of the way John puts the
       * control flow graph together. It works like you would expect for prefix
       * operations. But for postfix operations, this actually gets called
       * twice: Once before the variable is assigned, and once after the
       * variable is assigned. In the first call, "op" is PreIncrementExpression
       * or PreDecrementExpression corresponding to the actual
       * PostIncrementExpression or PostDecrementExpression, respectively. In
       * the second call, "op" is the inverse operation, PreDecrementExpression
       * or PreIncrementExpression, to reverse the effects of the first call for
       * the actual value returned by the expression. (That is, in the way our
       * flow graph models things, the increment/decrement always happens first,
       * then the variable is updated, and then, if necessary, the value is
       * corrected.
       */
      final Operator tt = JJNode.tree.getOperator(node);
      if (tt.equals(op)) { // Definitely a prefix operation
        /*
         * Prefix expressions yield NOT_NULL always because the value is either
         * primitive or a newly boxed value.
         */
        return lattice.push(lattice.pop(val), 
            lattice.baseValue(NonNullRawLattice.NOT_NULL, Kind.BOXED_CREMENT, node));
      } else { // Definitely a postfix operation
        // Is this the first call, before the variable assignment?
        if (((CrementExpression) tt).baseOp().equals(op)) {
          /*
           * NOT_NULL always because the value after assignment is either
           * primitive or a newly boxed value.
           * 
           * We do not POP the stack, because we want to preserve the state
           * of the value of the variable so that we can use it in the second
           * corrective call.
           */
          return lattice.push(val, 
              lattice.baseValue(NonNullRawLattice.NOT_NULL, Kind.BOXED_CREMENT, node));
        } else { // It's the second, corrective call
          /*
           * We want to return the state of the value of the variable before the
           * original operation. Need to retrieve this from the stack: It's 
           * the second item on the stack, under the value return by the 
           * assignment expression.  We don't care about the result of the assignment,
           * we want the state of things before the expression was evaluated,
           * so we pop the stack to expose the state saved in the first call.
           */
          return lattice.pop(val);
        }
      }
    }

    @Override
    protected EvalValue transferDefaultInit(final IRNode node, final EvalValue val) {
      if (!lattice.isNormal(val)) return val;
      return push(val, lattice.baseValue(NonNullRawLattice.NULL, Kind.UNITIALIZED, node));
    }

    /*
     * Consider for later: transferEq may be interesting. On the equal branch we
     * know that both sides refer to the same object, so we can use the most
     * specific raw type (MEET of the two values).
     */
    
    @Override
    protected EvalValue transferEq(
        final IRNode node, final boolean flag, final EvalValue val) {
      if (!lattice.isNormal(val)) return val;

      EvalValue newValue = val;
//      final Element ni2 = lattice.peek(newValue).first();
      newValue = lattice.pop(newValue);
      final Element ni1 = lattice.peek(newValue).first();
      newValue = lattice.pop(newValue);
      /* The source expression doesn't matter here because the result of an
       * equality expression is a primitive boolean type.
       */
      newValue = lattice.push(newValue, 
          lattice.baseValue(NonNullRawLattice.MAYBE_NULL, Kind.EQUALITY_NOTNULL, node));

      /* Used to short circuit impossible flow paths.   But we don't want to 
       * do this because it leaves spots in the flow graph where we do not have
       * analysis results.
       */

//      final Element meet = ni1.meet(ni2);
//      
//      // if the condition is impossible, we propagate bottom
//      if (meet == NonNullRawLattice.IMPOSSIBLE) {
//        if (flag) return null; // else fall through to end
//      }
//      // if the comparison is guaranteed true, we propagate bottom for false:
//      else if (ni1.lessEq(NonNullRawLattice.NULL) && ni2.lessEq(NonNullRawLattice.NULL)) {
//        if (!flag) return null; // else fall through to end
//      }
      
      /*
       * 
       * IF we have an *equality* comparison with null, we mark the variable
       * as being null.
       * 
       * If we have an *inequality* comparison with null, then we can consider
       * the variable being tested as non-null, but only if it isn't already
       * RAW or NOT_NULL.
       */
      if (ni1.lessEq(NonNullRawLattice.NULL)) {
        final IRNode n = tree.getChild(node, 1); // don't use EqExpression methods because this transfer is called on != also
        if (VariableUseExpression.prototype.includes(n)) {
          final int idx = lattice.indexOf(binder.getIBinding(n).getNode());
          if (flag) {
            newValue = lattice.setVar(newValue, idx,
                lattice.baseValue(NonNullRawLattice.NULL, Kind.EQUALITY_NULL, node));
          } else {
            newValue = lattice.setVarNonNullIfNotAlready(
                newValue, idx, Kind.EQUALITY_NOTNULL, node);
          }
        }
      } else if (NullLiteral.prototype.includes(tree.getChild(node,1))) {
        /*
         * NB: it would be a little more precise if we checked for ni2 being
         * under NULL than what we do here but then we must check for
         * assignments of the variable so that we don't make a wrong
         * conclusion for "x == (x = null)" which, even if false, still leaves
         * x null. The first branch is OK because "(x = null) == x" doesn't
         * have the same problem.
         */
        final IRNode n = tree.getChild(node, 0);
        if (VariableUseExpression.prototype.includes(tree.getOperator(n))) {
          final int idx = lattice.indexOf(binder.getIBinding(n).getNode());
          if (flag) {
            newValue = lattice.setVar(newValue, idx,
                lattice.baseValue(NonNullRawLattice.NULL, Kind.EQUALITY_NULL, node));
          } else {
            newValue = lattice.setVarNonNullIfNotAlready(
                newValue, idx, Kind.EQUALITY_NOTNULL, node);
          }
        }
      } else {
        // TRUE BRANCH: can update variables to be the meet of the two sides
      }
      return newValue;
    }    
    
    @Override
    protected EvalValue transferImplicitArrayCreation(
        final IRNode arrayInitializer, final EvalValue val) {
      return push(val, 
          lattice.baseValue(NonNullRawLattice.NOT_NULL, Kind.NEW_ARRAY, arrayInitializer));
    }

    @Override
    protected EvalValue transferInitializationOfVar(final IRNode node, final EvalValue val) {
      if (!lattice.isNormal(val)) return val;
      /* 
       * Locals without initializers are "not definitely assigned".  They are
       * not NULL (unlike fields without initializers).  The Java compiler
       * will reject any uses of the variable if the variable is "not definitely
       * assigned" at the point of the use.  So we do not set the value of the
       * variable in our model if the initializer is the default one.
       * 
       */
      final IRNode init = VariableDeclarator.getInit(node);
      if (NoInitialization.prototype.includes(init)) {
        /*
         * Just pop the stack: transferDefaultInit() pushes a value that we want
         * to ignore
         */
        return pop(val);
      } else {
        return pop(setVar(node, val, init));
      }
    }

    @Override
    protected EvalValue transferInstanceOf(
        final IRNode node, final boolean flag, final EvalValue val) {
      if (!lattice.isNormal(val)) return val;
      
      if (!flag) return val;
      
      /* TRUE branch: The value cannot be null because otherwise it would
       * not be an instance of something.  If the value of a variable is being
       * tested, we can update its state to reflect this fact, but only if 
       * it isn't already known to be raw or non-null.  NEVER change raw
       * to non-null because that will alter how the fields of the referenced
       * object are treated.
       */
      final IRNode n = InstanceOfExpression.getValue(node);
      if (VariableUseExpression.prototype.includes(n)) {
        final int idx = lattice.indexOf(binder.getIBinding(n).getNode());
        return lattice.setVarNonNullIfNotAlready(val, idx, Kind.INSTANCEOF, node);
      }
      return val;
    }

    @Override
    protected EvalValue transferIsObject(
        final IRNode n, final boolean flag, final EvalValue val) {
      if (!lattice.isNormal(val)) return val;

      /*
       * If the operation is a method call, pop the arguments to access the
       * state of the receiver. Use a *copy* of the lattice value to do this.
       */
      EvalValue newValue = val;
      final IRNode p = tree.getParent(n);
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
      
      /* Used to short circuit impossible flow paths.   But we don't want to 
       * do this because it leaves spots in the flow graph where we do not have
       * analysis results.
       */
//      /*
//       * Impossible situations: (1) We know the object is null, but we are
//       * testing the true (object is not null) path. (2) We know the object is
//       * not null, but we are testing the false (object is null) path.
//       */
//      final Element ni = lattice.peek(newValue).first();
//      if (flag && ni.lessEq(NonNullRawLattice.NULL)) {
//        return null; // lattice.bottom();
//      }
//      if (!flag && ni.lessEq(NonNullRawLattice.RAW)) {
//        return null; //lattice.bottom();
//      }
      
      /*
       * If we are on the true (object is not null) path and the expression
       * being tested is a variable use, then we can mark the variable as
       * not-null, if it isn't already RAW or NOT_NULL.
       */
      if (flag && VariableUseExpression.prototype.includes(n)) {
        final IRNode varDecl = binder.getIBinding(n).getNode();
        final int idx = lattice.indexOf(varDecl);
        return lattice.setVarNonNullIfNotAlready(val, idx, Kind.IS_OBJECT, n);
      }
      return super.transferIsObject(n, flag, val);
    }

    @Override
    protected EvalValue transferLiteral(final IRNode node, final EvalValue val) {
      if (!lattice.isNormal(val)) return val;
      
      if (NullLiteral.prototype.includes(node)) {
        return lattice.push(val, lattice.baseValue(
            NonNullRawLattice.NULL, Kind.NULL_LITERAL, node));
      } else {
        return lattice.push(val, lattice.baseValue(
            NonNullRawLattice.NOT_NULL, Kind.STRING_LITERAL, node));
      }
    }
    
    @Override
    protected EvalValue transferUseField(final IRNode fref, EvalValue val) {
      if (!lattice.isNormal(val)) return val;
      
      /* if the field reference is part of a ++ or += operation, we have to
       * duplicate the reference for the subsequent write operation. 
       */
      if (isBothLhsRhs(fref)) val = dup(val);
      
      // pop the object reference
      final Element refState = lattice.peek(val).first();
      val = pop(val);

      final IRNode fieldDecl = binder.getBinding(fref);
      /*
       * If the field is actually an enumeration constant then it is always
       * @NonNull.  We could instead use virtual @NonNull annotations on
       * EnumConstantDeclaration nodes, but Tim thinks that is overkill.
       */
      if (EnumConstantDeclaration.prototype.includes(fieldDecl)) {
        // Always @NonNull
        val = push(val, lattice.baseValue(NonNullRawLattice.NOT_NULL, Kind.ENUM_CONSTANT, fieldDecl));
      }
      /*
       * If the field is @NonNull, then we push NOT_NULL, unless the object
       * reference is RAW.  In that case, we have to check to see if the 
       * field is initialized yet.  If so, we push NOT_NULL, otherwise we must
       * push MAYBE_NULL.  If the field is not annotated, we push MAYBE_NULL.
       */
      else if (NonNullRules.getNonNull(fieldDecl) != null) {
        if (refState == NonNullRawLattice.RAW) {
          // No fields are initialized
          val = push(val, lattice.baseValue(NonNullRawLattice.MAYBE_NULL, Kind.RAW_FIELD_REF, fref));
        } else if (refState instanceof ClassElement) {
          // Partially initialized class
          final IJavaDeclaredType initializedThrough =
              ((ClassElement) refState).getType();
          
          /* If the field is declared in a proper subtype of the type named
           * in the RAW declaration, then the field is not yet initialized.
           */
          final ITypeEnvironment typeEnvironment = binder.getTypeEnvironment();
          final IRNode fieldDeclaredIn = VisitUtil.getEnclosingType(fieldDecl);
          final IJavaType fieldIsFrom = typeEnvironment.getMyThisType(fieldDeclaredIn);
          if (typeEnvironment.isSubType(fieldIsFrom, initializedThrough) &&
              !fieldIsFrom.equals(initializedThrough)) {
            val = push(val, lattice.baseValue(NonNullRawLattice.MAYBE_NULL, Kind.RAW_FIELD_REF, fref));
          } else {
            val = push(val, lattice.baseValue(NonNullRawLattice.NOT_NULL, Kind.FIELD_REF, fref));
          }
        } else {
          val = push(val, lattice.baseValue(NonNullRawLattice.NOT_NULL, Kind.FIELD_REF, fref));
        }
      } else {
        val = push(val, lattice.baseValue(NonNullRawLattice.MAYBE_NULL, Kind.FIELD_REF, fref));
      }
      return val;
    }
    
    @Override
    protected EvalValue transferUseReceiver(final IRNode use, final EvalValue val) {
      if (!lattice.isNormal(val)) return val;
      final IRNode receiverNode = AnalysisUtils.getReceiverNodeAtExpression(use, flowUnit);
      final Element nullState = lattice.getThis(val, receiverNode).first();
      return lattice.push(val, lattice.baseValue(nullState, Kind.THIS_EXPR, use));
    }
    
    @Override
    protected EvalValue transferUseQualifiedReceiver(
        final IRNode use, final IRNode binding, final EvalValue val) {
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
            lattice.baseValue(
                  lattice.injectClass(
                      qualifyingType.getSuperclass(binder.getTypeEnvironment())),
                  Kind.QUALIFIED_THIS, use));
      } else {
        return lattice.push(val, 
            lattice.baseValue(NonNullRawLattice.NOT_NULL, Kind.QUALIFIED_THIS, use));
      }
    }
    
    @Override
    protected EvalValue transferUseVar(final IRNode use, final EvalValue val) {
      if (!lattice.isNormal(val)) return val;
      
      // Push the variable state onto the stack
      final IRNode var = binder.getIBinding(use).getNode();
      final int idx = var == null ? -1 : lattice.indexOf(var);
      if (idx != -1) {
        return lattice.push(val, 
            lattice.baseValue(lattice.getVar(val, idx).first(), Kind.VAR_USE, use));
      } else {
        // N.B. primitively typed variable
        return lattice.push(val,
            lattice.baseValue(NonNullRawLattice.MAYBE_NULL, Kind.VAR_USE, use));
      }
    }
  }
  
  
  
  private static final class SubAnalysisFactory extends AbstractCachingSubAnalysisFactory<EvalLattice, EvalValue> {
    private final IRNode flowUnit;
    
    private SubAnalysisFactory(final IRNode fu) {
      flowUnit = fu;
    }
    
    @Override
    protected JavaForwardAnalysis<EvalValue, EvalLattice> realCreateAnalysis(
        final IRNode caller, final IBinder binder,
        final EvalLattice lattice,
        final EvalValue initialValue,
        final boolean terminationNormal) {
      final Transfer t = new Transfer(flowUnit, binder, lattice, 0);
      return new JavaForwardAnalysis<EvalValue, EvalLattice>("sub analysis", lattice, t, DebugUnparser.viewer);
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
  
  public InferredQuery getInferredQuery(final IRNode flowUnit) {
    return new InferredQuery(getAnalysisThunk(flowUnit));
  }
}
