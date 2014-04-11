package com.surelogic.analysis.nullable;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.surelogic.analysis.AnalysisUtils;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.LocalVariableDeclarations;
import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.nullable.NonNullRawLattice.ClassElement;
import com.surelogic.analysis.nullable.NonNullRawLattice.Element;
import com.surelogic.analysis.nullable.NullableUtils.Method;
import com.surelogic.analysis.visitors.InstanceInitAction;
import com.surelogic.analysis.visitors.JavaSemanticsVisitor;
import com.surelogic.annotation.rules.NonNullRules;
import com.surelogic.common.Pair;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.util.AbstractRemovelessIterator;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.nullable.NonNullPromiseDrop;
import com.surelogic.dropsea.ir.drops.nullable.RawPromiseDrop;
import com.surelogic.util.IRNodeIndexedArrayLattice;
import com.surelogic.util.IThunk;
import com.surelogic.util.NullList;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.analysis.SimplifiedJavaFlowAnalysisQuery;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.operator.AllocationExpression;
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
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.ImpliedEnumConstantInitialization;
import edu.cmu.cs.fluid.java.operator.Initialization;
import edu.cmu.cs.fluid.java.operator.InstanceOfExpression;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.NoInitialization;
import edu.cmu.cs.fluid.java.operator.NullLiteral;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.QualifiedThisExpression;
import edu.cmu.cs.fluid.java.operator.ReferenceType;
import edu.cmu.cs.fluid.java.operator.ReturnStatement;
import edu.cmu.cs.fluid.java.operator.SuperExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.promise.InitDeclaration;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;
import edu.cmu.cs.fluid.util.ImmutableList;
import edu.cmu.cs.fluid.util.ImmutableSet;
import edu.cmu.cs.fluid.util.Triple;
import edu.uwm.cs.fluid.java.analysis.EvaluationStackLattice;
import edu.uwm.cs.fluid.java.analysis.IntraproceduralAnalysis;
import edu.uwm.cs.fluid.java.control.AbstractCachingSubAnalysisFactory;
import edu.uwm.cs.fluid.java.control.IJavaFlowAnalysis;
import edu.uwm.cs.fluid.java.control.JavaForwardAnalysis;
import edu.uwm.cs.fluid.java.control.LatticeDelegatingJavaEvaluationTransfer;
import edu.uwm.cs.fluid.util.PairLattice;
import edu.uwm.cs.fluid.util.UnionLattice;


/**
 * TODO
 */
public final class NonNullRawTypeAnalysis 
extends IntraproceduralAnalysis<
    NonNullRawTypeAnalysis.Value,
    NonNullRawTypeAnalysis.Lattice,
    JavaForwardAnalysis<NonNullRawTypeAnalysis.Value,
    NonNullRawTypeAnalysis.Lattice>>
implements IBinderClient {
  protected static final ImmutableHashOrderSet<Source> EMPTY =
      ImmutableHashOrderSet.<Source>emptySet();

  
  
  public static final class StackQuery extends SimplifiedJavaFlowAnalysisQuery<StackQuery, StackQueryResult, Value, Lattice> {
    public StackQuery(final IThunk<? extends IJavaFlowAnalysis<Value, Lattice>> thunk) {
      super(thunk);
    }
    
    private StackQuery(final Delegate<StackQuery, StackQueryResult, Value, Lattice> d) {
      super(d);
    }

    @Override
    protected RawResultFactory getRawResultFactory() {
      return RawResultFactory.NORMAL_EXIT;
    }


    
    @Override
    protected StackQuery newSubAnalysisQuery(final Delegate<StackQuery, StackQueryResult, Value, Lattice> d) {
      return new StackQuery(d);
    }


    
    @Override
    protected StackQueryResult processRawResult(final IRNode expr,
        final Lattice lattice, final Value rawResult) {
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
  
  
  
  public static final class Query extends SimplifiedJavaFlowAnalysisQuery<Query, Pair<Lattice, Base[]>, Value, Lattice> {
    public Query(final IThunk<? extends IJavaFlowAnalysis<Value, Lattice>> thunk) {
      super(thunk);
    }
    
    private Query(final Delegate<Query, Pair<Lattice, Base[]>, Value, Lattice> d) {
      super(d);
    }
    
    @Override
    protected RawResultFactory getRawResultFactory() {
      return RawResultFactory.ENTRY;
    }

    @Override
    protected Pair<Lattice, Base[]> processRawResult(
        final IRNode expr, final Lattice lattice, final Value rawResult) {
      return new Pair<Lattice, Base[]>(lattice, rawResult.second().first());
    }

    @Override
    protected Query newSubAnalysisQuery(final Delegate<Query, Pair<Lattice, Base[]>, Value, Lattice> d) {
      return new Query(d);
    }
  }
  
  
  
  public static final class QualifiedThisQuery extends SimplifiedJavaFlowAnalysisQuery<QualifiedThisQuery, Element, Value, Lattice> {
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
      return lattice.peek(rawResult).first();
    }

    @Override
    protected QualifiedThisQuery newSubAnalysisQuery(final Delegate<QualifiedThisQuery, Element, Value, Lattice> d) {
      return new QualifiedThisQuery(d);
    }
  }
  
  
  
  public static final class DebugQuery extends SimplifiedJavaFlowAnalysisQuery<DebugQuery, String, Value, Lattice> {
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
      return rawResult == null ? "null" : lattice.toString(rawResult);
    }

    @Override
    protected DebugQuery newSubAnalysisQuery(final Delegate<DebugQuery, String, Value, Lattice> d) {
      return new DebugQuery(d);
    }
  }

  

  public static final class Inferred implements Iterable<InferredVarState> {
    protected final NonNullRawLattice inferredStateLattice;
  
    private final IRNode[] keys;
  
    private final Element[] values;
  
    protected Inferred(
        final IRNode[] keys, final Element[] val,
        final NonNullRawLattice sl) {
      this.keys = keys;
      this.values = val;
      this.inferredStateLattice = sl;
    }
  
    @Override
    public Iterator<InferredVarState> iterator() {
      return new AbstractRemovelessIterator<InferredVarState>() {
        private int idx = 0;
  
        @Override
        public boolean hasNext() {
          return idx < keys.length;
        }
  
        @Override
        public InferredVarState next() {
          final int currentIdx = idx++;
          final Element inferredState = values[currentIdx];
          return new InferredVarState(keys[currentIdx], inferredState);
        }
      };
    }
  
    public boolean lessEq(final Element a, final Element b) {
      return inferredStateLattice.lessEq(a, b);
    }
  
    public PromiseDrop<?> getPromiseDrop(final IRNode n) {
      PromiseDrop<?> pd = NonNullRules.getRaw(n);
      if (pd == null) pd = NonNullRules.getNonNull(n);
      if (pd == null) pd = NonNullRules.getNullable(n);
      return pd;
    }
    
    public Element injectPromiseDrop(final PromiseDrop<?> pd) {
      return inferredStateLattice.injectPromiseDrop(pd);
    }
  }


  public static final class InferredQuery
  extends SimplifiedJavaFlowAnalysisQuery<InferredQuery, Inferred, Value, Lattice> {
    protected InferredQuery(
        final IThunk<? extends IJavaFlowAnalysis<Value, Lattice>> thunk) {
      super(thunk);
    }
    
    protected InferredQuery(
        final Delegate<InferredQuery, Inferred, Value, Lattice> d) {
      super(d);
    }

    @Override
    protected final RawResultFactory getRawResultFactory() {
      return RawResultFactory.NORMAL_EXIT;
    }
    
    @Override
    protected Inferred processRawResult(
        final IRNode expr, final Lattice lattice, final Value rawResult) {
      return new Inferred(
          lattice.getInferredStateKeys(),
          rawResult.second().second(),
          lattice.getInferredStateLattice());
    }
  
    @Override
    protected InferredQuery newSubAnalysisQuery(
        final Delegate<InferredQuery, Inferred, Value, Lattice> delegate) {
      return new InferredQuery(delegate);
    }
  }


  public static final class InferredVarState extends Pair<IRNode, Element> {
    public InferredVarState(final IRNode varDecl, final Element state) {
      super(varDecl, state);
    }

    public IRNode getLocal() {
      return first();
    }

    public Element getState() {
      return second();
    }
  }  
  
  
  
  public NonNullRawTypeAnalysis(final IBinder b) {
    super(b);
  }

  
  
  @Override
  protected JavaForwardAnalysis<Value, Lattice> createAnalysis(final IRNode flowUnit) {
    final LocalVariableDeclarations lvd = LocalVariableDeclarations.getDeclarationsFor(flowUnit);
    final List<IRNode> refVars = new ArrayList<IRNode>(
        lvd.getLocal().size() + lvd.getExternal().size() +
        lvd.getReceivers().size());

    // Add the receivers
    refVars.addAll(lvd.getReceivers());
    // Add all reference-typed variables in scope
    LocalVariableDeclarations.separateDeclarations(
        binder, lvd.getLocal(), refVars, NullList.<IRNode>prototype());
    LocalVariableDeclarations.separateDeclarations(
        binder, lvd.getExternal(), refVars, NullList.<IRNode>prototype());
    
    
    // Get the local variables that are annotated with @Raw or @NonNull
    // N.B. Non-ref types variables cannot be @Raw or @NonNull, so we don't have to test for them
    final List<IRNode> varsToInfer = new ArrayList<IRNode>(lvd.getLocal().size() + 1);
    for (final IRNode v : lvd.getLocal()) {
      if (!ParameterDeclaration.prototype.includes(v)) {
        varsToInfer.add(v);
      }
    }
    /* If the flow unit is a method with a return value, add the return value 
     * node.  Only add if reference-type return value
     */
    if (MethodDeclaration.prototype.includes(flowUnit) &&
      ReferenceType.prototype.includes(MethodDeclaration.getReturnType(flowUnit))) {
      varsToInfer.add(JavaPromise.getReturnNode(flowUnit));
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
    final StateLattice stateLattice = new StateLattice(rawVariables, new InferredLattice(rawLattice, varsToInfer));
    final Lattice lattice = new Lattice(baseLattice, stateLattice);
    final Transfer t = new Transfer(flowUnit, binder, lattice, 0);
    return new JavaForwardAnalysis<Value, Lattice>("NonNull and Raw Types", lattice, t, DebugUnparser.viewer, true);
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
      super(false, true, cdecl);
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
  
  

  public enum Kind {
    VAR_USE(-1) {
      @Override
      public IRNode bind(final IRNode expr,
          final IBinder b, final ThisExpressionBinder teb) {
        return b.getBinding(expr);
      }
    },
    THIS_EXPR(-1) {
      @Override
      public IRNode bind(final IRNode expr,
          final IBinder b, final ThisExpressionBinder teb) {
        return teb.bindThisExpression(expr);
      }
    },

    RECEIVER_ANON_CLASS(940),
    NEW_ARRAY(941),
    BOX(942),
    METHOD_RETURN(943) {
      @Override
      public IRNode getAnnotatedNode(final IBinder binder, final IRNode where) {
        final IRNode mdecl = binder.getBinding(where);
        return JavaPromise.getReturnNode(mdecl);
      }
    },
    CAUGHT_EXCEPTION(944),
    RECEIVER_CONSTRUCTOR_CALL(945),
    RECEIVER_RAW_OBJECT(946),
    BOXED_CREMENT(947),
    RECEIVER_ENUM_CLASS(948),
    EQUALITY_NOTNULL(949) {
      @Override
      public String unparse(final IRNode w) {
        IRNode n = tree.getChild(w,  0);
        if (!VariableUseExpression.prototype.includes(n)) n = tree.getChild(w, 1);
        return VariableUseExpression.getId(n);
      }
    },
    EQUALITY_NULL(964) {
      @Override
      public String unparse(final IRNode w) {
        IRNode n = tree.getChild(w,  0);
        if (!VariableUseExpression.prototype.includes(n)) n = tree.getChild(w, 1);
        return VariableUseExpression.getId(n);
      }
    },
    FIELD_REF(950) {
      @Override
      public IRNode getAnnotatedNode(final IBinder binder, final IRNode where) {
        return binder.getBinding(where);
      }
      
      @Override
      public String unparse(final IRNode w) {
        return FieldRef.getId(w);
      }
    },
    RAW_FIELD_REF(963) {
      @Override
      public IRNode getAnnotatedNode(final IBinder binder, final IRNode where) {
        return binder.getBinding(where);
      }
      
      @Override
      public String unparse(final IRNode w) {
        return FieldRef.getId(w);
      }
    },
    INSTANCEOF(951),
    NEW_OBJECT(952),
    UNITIALIZED(953),
    NULL_LITERAL(954),
    FORMAL_PARAMETER(955) {
      @Override
      public IRNode getAnnotatedNode(final IBinder binder, final IRNode where) {
        return where;
      }
      
      @Override
      public String unparse(final IRNode w) {
        return ParameterDeclaration.getId(w);
      }
    },
    QUALIFIED_THIS(956),
    INTERESTING_QUALIFIED_THIS(957),
    RECEIVER_METHOD(958) {
      @Override
      public IRNode getAnnotatedNode(final IBinder binder, final IRNode where) {
        return where;
      }
    },
    STRING_CONCAT(959),
    IS_OBJECT(960),
    ENUM_CONSTANT(961) {
      @Override
      public IRNode getAnnotatedNode(final IBinder binder, final IRNode where) {
        return binder.getBinding(where);
      }
      
      @Override
      public String unparse(final IRNode w) {
        return EnumConstantDeclaration.getId(w);
      }
    },
    STRING_LITERAL(962),
    VAR_ARGS(965),
    CAST_TO_NULLABLE(967) {
      @Override
      public IRNode getAnnotatedNode(final IBinder binder, final IRNode where) {
        return where;
      }
    },
    CAST_TO_NONNULL(968) {
      @Override
      public IRNode getAnnotatedNode(final IBinder binder, final IRNode where) {
        return where;
      }
    },
    FINAL_INIT_FIELD(969) {
      @Override
      public IRNode getAnnotatedNode(final IBinder binder, final IRNode where) {
        return binder.getBinding(where);
      }
      
      @Override
      public String unparse(final IRNode w) {
        return FieldRef.getId(w);
      }
    },
    
    NO_VALUE(966) {
      @Override
      public String unparse(final IRNode w) {
        return "";
      }
    };
    
    private final int msg;
    
    Kind(final int m) {
      msg = m;
    }
    
    /* A cleaner implementation would separate out VAR_USE and THIS_EXPR
     * from the rest of the elements. But because Java lacks a typecase
     * operation, doing so would necessitate the use of a typecast in the 
     * testChain(), buildNewChain(), and buildWarningResults() methods
     * NonNullTypeChecker.  Not clear that requiring that would be any cleaner
     * than being sloppy like we are doing now.
     */
    
    // N.B. Not needed by VAR_USE or THIS_EXPR
    public final int getMessage() {
      return msg;
    }
    
    // N.B. Only needed by VAR_USE and THIS_EXPR
    public IRNode bind(
        final IRNode expr, final IBinder b, final ThisExpressionBinder teb) {
      return null;
    }
    
    public String unparse(final IRNode w) {
      return DebugUnparser.toString(w);
    }
    
    public IRNode getAnnotatedNode(final IBinder binder, final IRNode where) {
      return null;
    }
  }

  
  
  public static final class Source extends Triple<Kind, IRNode, Element> {
    public Source(final Kind k, final IRNode where, final Element value) {
      super(k, where, value);
    }
    
    public static String setToString(final Set<Source> sources) {
      final List<String> strings = new ArrayList<String>();
      for (final Source src : sources) {
        final Kind k = src.first();
        final IRNode where = src.second();
        final Element value = src.third();
        final Operator op = where == null ? null : JJNode.tree.getOperator(where);
        final IJavaRef javaRef = JavaNode.getJavaRef(where);
        final int line = javaRef == null ? -1 : javaRef.getLineNumber();
        final StringBuilder sb = new StringBuilder();
        sb.append(k);
        sb.append('[');
        sb.append(op == null ? "none" : op.name());
        sb.append('@');
        sb.append(line);
        sb.append(", ");
        sb.append(value);
        sb.append("]");
        strings.add(sb.toString());
      }
      Collections.sort(strings);
      
      final StringBuilder sb = new StringBuilder("{ ");
      for (final String s : strings) {
        sb.append(s);
        sb.append(' ');
      }
      sb.append('}');
      return sb.toString();
    }
  }
  
  
  
  /**
   * Lattice for array of inferred variable states.
   *
   * @param <I> The type of the state to be inferred for each local variable.
   * @param <L> The lattice type for the inferred values.
   */
  public static final class InferredLattice
  extends IRNodeIndexedArrayLattice<NonNullRawLattice, Element> {
    private final Element[] empty;
    
    public InferredLattice(final NonNullRawLattice base, final List<IRNode> keys) {
      super(base, keys);
      empty = createEmptyValue();
    }
    
    @Override
    protected final Element getEmptyElementValue() {
      return baseLattice.bottom();
    }
  
    @Override
    public final Element[] getEmptyValue() {
      return empty;
    }
  
    @Override
    protected Element[] newArray() {
      return new Element[size];
    }    
  
    @Override
    protected final void indexToString(final StringBuilder sb, final IRNode index) {
      final Operator op = JJNode.tree.getOperator(index);
      if (ParameterDeclaration.prototype.includes(op)) {
        sb.append(ParameterDeclaration.getId(index));
      } else if (ReturnValueDeclaration.prototype.includes(op)) {
        sb.append("<return>");
      } else { // VariableDeclarator
        sb.append(VariableDeclarator.getId(index));
      }
    }
    
    public final IRNode[] cloneKeys() {
      return indices.clone();
    }
    
    /**
     * Set the inferred state of a local variable at the given index.
     */
    public final Element[] inferVar(
        final Element[] current, final int idx, final Element v, final IRNode src) {
      return replaceValue(current, idx, baseLattice.join(current[idx], v));
    }
  }


  /**
   * Base value for the analysis, a pair of non-null state and a set of IRNodes
   * representing the possible source expressions of the value. Each IRNode in
   * the set is either a VariableUseExpresssion that binds to a
   * ReceiverDeclaration, ParameterDeclaration, a NewExpression, an
   * AnonClassExpression, an ArrayCreationExpression, a ReturnValueDeclaration,
   * BoxExpression, StringConcat, CrementExpression, NoInitialization,
   * NullLiteral, ArrayInitializer,
   * or a FieldRef. The set portion is managed by a UnionLattice.
   * 
   * TODO: Rename this later.
   */
  public static final class Base extends Pair<Element, ImmutableSet<Source>> {
    public Base(final Element nonNullState, final ImmutableSet<Source> sources) {
      super(nonNullState, sources);
    }
    
    public Base(final Element nonNullState, final Kind k, final IRNode where) {
      this(nonNullState, EMPTY.addElement(new Source(k, where, nonNullState)));
    }
    
    @Override
    protected String secondToString(final ImmutableSet<Source> sources) {
      return Source.setToString(sources);
    }
  }
  
  static final class BaseLattice extends PairLattice<Element, ImmutableSet<Source>, NonNullRawLattice, UnionLattice<Source>, Base> {
    public BaseLattice(final NonNullRawLattice l1, final UnionLattice<Source> l2) {
      super(l1, l2);
    }

    @Override
    protected Base newPair(final Element v1, final ImmutableSet<Source> v2) {
      return new Base(v1, v2);
    }
    
    public Base getEmpty() {
      return new Base(NonNullRawLattice.MAYBE_NULL, Kind.NO_VALUE, null);
//      return new Base(NonNullRawLattice.MAYBE_NULL, lattice2.bottom());
    }

    public NonNullRawLattice getElementLattice() { 
      return lattice1;
    }
    
    public Element injectClass(final IJavaDeclaredType t) {
      return lattice1.injectClass(t);
    }
    
    public Element injectPromiseDrop(final RawPromiseDrop pd) {
      return lattice1.injectPromiseDrop(pd);
    }
    
    @Override
    public String toString(final Base b) {
      return b.toString();
    }
  }
  
  
  /**
   * Pair value for the evaluation state.
   *
   * @param <S> The type of the arbitrary analysis state.
   * @param <I> The type of the state to be inferred for each local variable.
   */
  public static final class State
  extends com.surelogic.common.Pair<Base[], Element[]> {
    public State(final Base[] s, final Element[] i) {
      super(s, i);
    }
  }
  
  
  
  /**
   * Lattice type for the state component of the evaluation. 
   *
   * @param <S> The type of the arbitrary analysis state.
   * @param <I> The type of the state to be inferred for each local variable.
   * @param <V> The type of overall state object used during evaluation.
   * @param <L_S> The lattice type for the arbitrary analysis state.
   * @param <L_I> The lattice type for the inferred variable states.
   */
  public static final class StateLattice
  extends PairLattice<Base[], Element[], LocalStateLattice, InferredLattice, State> {
    protected StateLattice(final LocalStateLattice l1, final InferredLattice l2) {
      super(l1, l2);
    }
    
    
    
    public final IRNode[] getInferredStateKeys() {
      return lattice2.cloneKeys();
    }
    
    public final Element[] getEmptyInferredValue() {
      return lattice2.getEmptyValue();
    }
    
    /**
     * Get the array index of the given variable in the array of inferred
     * variables.
     * @return The array index of the variable, or -1 if the variable is not 
     * in the array.
     */
    public final int indexOfInferred(final IRNode var) {
      return lattice2.indexOf(var);
    }
    
    /**
     * Set the inferred state of a local variable at the given index.
     */
    public final State inferVar(final State state, final int idx, final Element v, final IRNode src) {
      return newPair(state.first(), lattice2.inferVar(state.second(), idx, v, src));
    }
    
    @Override
    protected State newPair(final Base[] v1, final Element[] v2) {
      return new State(v1, v2);
    }
    
    public State getEmptyValue() {
      return new State(lattice1.getEmptyValue(), getEmptyInferredValue());
    }
    
    
    
    public LocalStateLattice getLocalStateLattice() {
      return lattice1;
    }
        
    public NonNullRawLattice getInferredStateLattice() {
      return lattice2.getBaseLattice();
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

    public State setThis(final State v, final IRNode rcvrDecl, final Base b) {
      return newPair(lattice1.replaceValue(v.first(), rcvrDecl, b), v.second());
    }
    
    public Base getThis(final State v, final IRNode rcvrDecl) {
      return v.first()[lattice1.indexOf(rcvrDecl)];
    }
    
    public int indexOf(final IRNode var) {
      return lattice1.indexOf(var);
    }
    
    public State setVar(final State v, final int idx, final Base b) {
      return newPair(lattice1.replaceValue(v.first(), idx, b), v.second());
    }
    
    public State setVarNonNullIfNotAlready(
        final State v, final int idx, final Kind k, final IRNode where) {
      if (getVar(v, idx).first().lessEq(NonNullRawLattice.RAW)) {
        return v;
      } else {
        return setVar(v, idx, new Base(NonNullRawLattice.NOT_NULL, k, where));
      }
    }
    
    public Base getVar(final State v, final int idx) {
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

  
  
  // ======================================================================


  
  /**
   * Value type for the overall value pushed around by the analysis.
   * First component is the evaluation stack, and the second component
   * is a pair of arbitrary stack, and an array of inferred local variable
   * states.
   * 
   * @param <E> The type of stack elements.
   * @param <S> The type of the arbitrary analysis state.
   * @param <I> The type of the state to be inferred for each local variable.
   * @param <V> The overall type of the state component for the EvaluationStackLattice.
   */
  public static final class Value
  extends EvaluationStackLattice.EvalPair<Base, State> {
    protected Value(final ImmutableList<Base> v1, final State v2) {
      super(v1, v2);
    }
  }



  /**
   * Specialization of {@link EvaluationStackLattice} for use with
   * {@link Value}.
   * 
   * @param <E> The type of the stack elements.
   * @param <I> The type of the state to be inferred for each local variable
   * @param <V> The type of overall state object used during evaluation.
   * @param <R> The type of the overall value used by analysis.  
   * @param <L_E> The lattice type of the stack elements.
   * @param <L_I> The lattice type of the inferred variable states.
   * @param <L_V> The lattice type of the overall state object.
   */
  public static final class Lattice
  extends EvaluationStackLattice<Base, State, BaseLattice, StateLattice, Value> {
    protected Lattice(final BaseLattice l1, final StateLattice l2) {
      super(l1, l2);
    }
   
    
    
    public final IRNode[] getInferredStateKeys() {
      return lattice2.getInferredStateKeys();
    }
    
    public final int indexOfInferred(final IRNode var) {
      return lattice2.indexOfInferred(var);
    }
    
    public final Value inferVar(final Value v, final int idx, final Element e, final IRNode src) {
      return newPair(v.first(), lattice2.inferVar(v.second(), idx, e, src));
    }
    
    @Override
    protected Value newPair(final ImmutableList<Base> v1, final State v2) {
      return new Value(v1, v2);
    }

    @Override
    public Base getAnonymousStackValue() {
      return lattice1.getBaseLattice().getEmpty();
    }
    
    public BaseLattice getStackElementLattice() {
      return lattice1.getBaseLattice();
    }
    
    public LocalStateLattice getLocalStateLattice() {
      return lattice2.getLocalStateLattice();
    }
    
    public NonNullRawLattice getInferredStateLattice() {
      return lattice2.getInferredStateLattice();
    }
    
    
    public int getNumVariables() {
      return lattice2.getNumVariables();
    }
    
    public IRNode getVariable(final int i) {
      return lattice2.getVariable(i);
    }
    
    public Value getEmptyValue() {
      return newPair(ImmutableList.<Base>nil(), lattice2.getEmptyValue());
    }
    
    public Base baseValue(final Element e, final Kind k, final IRNode where) {
      return lattice1.getBaseLattice().newPair(e, EMPTY.addElement(new Source(k, where, e)));
    }
    
    public Element injectClass(final IJavaDeclaredType t) {
      return lattice2.injectClass(t);
    }
    
    public Element injectPromiseDrop(final RawPromiseDrop pd) {
      return lattice2.injectPromiseDrop(pd);
    }

    public Value setThis(final Value v, final IRNode rcvrDecl, final Base b) {
      return newPair(v.first(), lattice2.setThis(v.second(), rcvrDecl, b));
    }
    
    public Base getThis(final Value v, final IRNode rcvrDecl) {
      return lattice2.getThis(v.second(), rcvrDecl);
    }
    
    public int indexOf(final IRNode var) {
      return lattice2.indexOf(var);
    }
    
    public Value setVar(final Value v, final int idx, final Base b) {
      return newPair(v.first(), lattice2.setVar(v.second(), idx, b));
    }
    
    public Value setVarNonNullIfNotAlready(final Value v, final int idx, final Kind k, final IRNode where) {
      return newPair(v.first(), lattice2.setVarNonNullIfNotAlready(v.second(), idx, k, where));
    }
    
    public Base getVar(final Value v, final int idx) {
      return lattice2.getVar(v.second(), idx);
    }
    
    public boolean isInterestingQualifiedThis(final IRNode use) {
      return lattice2.isInterestingQualifiedThis(use);
    }
  }



  
  private static final class Transfer extends LatticeDelegatingJavaEvaluationTransfer<Lattice, Value, Base> {
    private final IRNode flowUnit;
    
    public Transfer(final IRNode fu, final IBinder binder, final Lattice lattice, final int floor) {
      super(binder, lattice, new SubAnalysisFactory(fu), floor);
      flowUnit = fu;
    }


    
    @Override
    public Value transferComponentSource(final IRNode node) {
      /* 
       * Everything is MAYBE_NULL, but we reset them below to capture
       * source information.
       */
      Value value = lattice.getEmptyValue(); 

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
       * Caught exceptions, also parameter declarations, are always NOT_NULL.
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
    protected Value pushMethodReturnValue(final IRNode node, final Value val) {
      // push the value based on the annotation of the method's return node
      final IRNode methodDecl = binder.getBinding(node);
      final IRNode returnNode = JavaPromise.getReturnNode(methodDecl);
      if (returnNode != null) {
        final Method whichCast = NullableUtils.isCastMethod(methodDecl);
        if (whichCast != null) {
          if (whichCast == Method.TO_NON_NULL) {
            return push(val, lattice.baseValue(NonNullRawLattice.NOT_NULL, Kind.CAST_TO_NONNULL, node));
          } else { // must be @Nullable
            return push(val, lattice.baseValue(NonNullRawLattice.MAYBE_NULL, Kind.CAST_TO_NULLABLE, node));
          }
        } else {
          // NB. Either @Raw or @NonNull but never both
          if (NonNullRules.getNonNull(returnNode) != null) {
            return push(val, lattice.baseValue(NonNullRawLattice.NOT_NULL, Kind.METHOD_RETURN, node));
          }

          final RawPromiseDrop pd = NonNullRules.getRaw(returnNode);
          if (pd != null) {
            return push(val, lattice.baseValue(lattice.injectPromiseDrop(pd), Kind.METHOD_RETURN, node));
          }
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
    protected Value transferAllocation(final IRNode node, final Value val) {
      // new expressions always return fully initialized values.
      // XXX: What operators do we have here?  I think NewExpression and AnonClassExpression
      // final Operator op = JJNode.tree.getOperator(node)
      return push(val, lattice.baseValue(NonNullRawLattice.NOT_NULL, Kind.NEW_OBJECT, node));
    }
    
    @Override
    protected Value transferArrayCreation(final IRNode node, Value val) {
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
    protected Value transferAssignVar(final IRNode use, final Value val) {
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
    
    private Value setVar(final IRNode varDecl, final Value val, final IRNode src) {
      final int idx = lattice.indexOf(varDecl);
      if (idx != -1) {
        final Base stackState = lattice.peek(val);
        Value newValue = lattice.setVar(val, idx, stackState);
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
    protected Value transferBox(final IRNode expr, final Value val) {
      if (!lattice.isNormal(val)) return val;
      return lattice.push(lattice.pop(val),
          lattice.baseValue(NonNullRawLattice.NOT_NULL, Kind.BOX, expr));
    }

    @Override
    protected Value transferConcat(final IRNode node, final Value val) {
      if (!lattice.isNormal(val)) return val;
      
      // pop the values of the stack and push a non-null
      Value newValue = lattice.pop(val);
      newValue = lattice.pop(newValue);
      newValue = lattice.push(newValue, 
          lattice.baseValue(NonNullRawLattice.NOT_NULL, Kind.STRING_CONCAT, node));
      return newValue;
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
    protected Value transferCrement(
        final IRNode node, final Operator op, final Value val) {
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
    protected Value transferDefaultInit(final IRNode node, final Value val) {
      if (!lattice.isNormal(val)) return val;
      return push(val, lattice.baseValue(NonNullRawLattice.NULL, Kind.UNITIALIZED, node));
    }

    /*
     * Consider for later: transferEq may be interesting. On the equal branch we
     * know that both sides refer to the same object, so we can use the most
     * specific raw type (MEET of the two values).
     */
    
    @Override
    protected Value transferEq(
        final IRNode node, final boolean flag, final Value val) {
      if (!lattice.isNormal(val)) return val;

      Value newValue = val;
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
    protected Value transferImplicitArrayCreation(
        final IRNode arrayInitializer, final Value val) {
      return push(val, 
          lattice.baseValue(NonNullRawLattice.NOT_NULL, Kind.NEW_ARRAY, arrayInitializer));
    }

    @Override
    protected Value transferInitializationOfVar(final IRNode node, final Value val) {
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
    protected Value transferInstanceOf(
        final IRNode node, final boolean flag, final Value val) {
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
    protected Value transferIsObject(
        final IRNode n, final boolean flag, final Value val) {
      if (!lattice.isNormal(val)) return val;

      /*
       * If the operation is a method call, pop the arguments to access the
       * state of the receiver. Use a *copy* of the lattice value to do this.
       */
      Value newValue = val;
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
    protected Value transferLiteral(final IRNode node, final Value val) {
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
    protected Value transferReturn(final IRNode node, final Value val) {
      if (!lattice.isNormal(val)) return val;

      final int inferredIdx = lattice.indexOfInferred(JavaPromise.getReturnNode(flowUnit));
      if (inferredIdx != -1) { // void methods and methods with primitive return types are not in the inferred list
        final Base stackState = lattice.peek(val);
        return lattice.inferVar(pop(val), inferredIdx, stackState.first(), ReturnStatement.getValue(node));
      } else {
        return pop(val);
      }
    }
    
    @Override
    protected Value transferUseField(final IRNode fref, Value val) {
      if (!lattice.isNormal(val)) return val;
      
      /* if the field reference is part of a ++ or += operation, we have to
       * duplicate the reference for the subsequent write operation. 
       */
      if (isBothLhsRhs(fref)) val = dup(val);
      
      // pop the object reference
      final Element refState = lattice.peek(val).first();
      val = pop(val);

      final IRNode varDecl = binder.getBinding(fref);
      final NonNullPromiseDrop nonNullPD = NonNullRules.getNonNull(varDecl);
      
      /*
       * If the field is actually an enumeration constant then it is always
       * @NonNull.  We could instead use virtual @NonNull annotations on
       * EnumConstantDeclaration nodes, but Tim thinks that is overkill.
       */
      if (EnumConstantDeclaration.prototype.includes(varDecl)) {
        // Always @NonNull
        val = push(val, lattice.baseValue(NonNullRawLattice.NOT_NULL, Kind.ENUM_CONSTANT, varDecl));
      }
      /*
       * If the field is @NonNull, then we push NOT_NULL, unless the object
       * reference is RAW.  In that case, we have to check to see if the 
       * field is initialized yet.  If so, we push NOT_NULL, otherwise we must
       * push MAYBE_NULL. 
       */
      else if (nonNullPD != null && !nonNullPD.isVirtual()) {
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
          final IRNode fieldDeclaredIn = VisitUtil.getEnclosingType(varDecl);
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
      }
      /*
       * The field is unannotated.  If the field is final and initialized in
       * the field declaration to a new object, then the field is NOT_NULL.
       * Otherwise it is MAYBE_NULL.
       */
      else {
        final IRNode init = VariableDeclarator.getInit(varDecl);
        if (TypeUtil.isFinal(varDecl) &&
            Initialization.prototype.includes(init) &&
            AllocationExpression.prototype.includes(Initialization.getValue(init))) {
          val = push(val, lattice.baseValue(NonNullRawLattice.NOT_NULL, Kind.FINAL_INIT_FIELD, fref));
        } else {
          val = push(val, lattice.baseValue(NonNullRawLattice.MAYBE_NULL, Kind.FIELD_REF, fref));
        }
      }
      return val;
    }
    
    @Override
    protected Value transferUseReceiver(final IRNode use, final Value val) {
      if (!lattice.isNormal(val)) return val;
      final IRNode receiverNode = AnalysisUtils.getReceiverNodeAtExpression(use, flowUnit);
      final Element nullState = lattice.getThis(val, receiverNode).first();
      return lattice.push(val, lattice.baseValue(nullState, Kind.THIS_EXPR, use));
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
    protected Value transferUseVar(final IRNode use, final Value val) {
      if (!lattice.isNormal(val)) return val;
      
      // Push the variable state onto the stack
      final IRNode var = binder.getIBinding(use).getNode();
      final int idx = var == null ? -1 : lattice.indexOf(var);
      if (idx != -1) {
    	/*
        final VouchFieldIsPromiseDrop vouch = LockRules.getVouchFieldIs(var);
        if (vouch != null) {
          if (vouch.isNullable()) {
            return lattice.push(val,
                lattice.baseValue(NonNullRawLattice.MAYBE_NULL, Kind.VOUCH_NULLLABLE, use));
          } else if (vouch.isNonNull()) {
            return lattice.push(val,
                lattice.baseValue(NonNullRawLattice.NOT_NULL, Kind.VOUCH_NONNULL, use));
          }
        }
        */
        return lattice.push(val, 
            lattice.baseValue(lattice.getVar(val, idx).first(), Kind.VAR_USE, use));
      } else {
        // N.B. primitively typed variable
        return lattice.push(val,
            lattice.baseValue(NonNullRawLattice.MAYBE_NULL, Kind.VAR_USE, use));
      }
    }

    @Override
    protected Value transferVarArgs(final IRNode node, Value val) {
      if (!lattice.isNormal(val)) return val;
      
      // pop the array elements
      val = pop(val, JJNode.tree.numChildren(node));
      
      /* Push a new non-null value indicating we have an implicitly 
       * create array. 
       */
      return lattice.push(val,
          lattice.baseValue(NonNullRawLattice.NOT_NULL, Kind.VAR_ARGS, node));
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
      return new JavaForwardAnalysis<Value, Lattice>("sub analysis", lattice, t, DebugUnparser.viewer, true);
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
