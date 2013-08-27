package com.surelogic.analysis.nullable2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.nullable2.NonNullRawLattice.Element;
import com.surelogic.common.Pair;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.util.*;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.nullable.RawPromiseDrop;
import com.surelogic.util.IRNodeIndexedArrayLattice;
import com.surelogic.util.IThunk;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.analysis.SimplifiedJavaFlowAnalysisQuery;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.operator.EnumConstantDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;
import edu.cmu.cs.fluid.util.ImmutableList;
import edu.cmu.cs.fluid.util.ImmutableSet;
import edu.cmu.cs.fluid.util.Triple;
import edu.uwm.cs.fluid.java.analysis.EvaluationStackLattice;
import edu.uwm.cs.fluid.java.analysis.IntraproceduralAnalysis;
import edu.uwm.cs.fluid.java.control.IJavaFlowAnalysis;
import edu.uwm.cs.fluid.java.control.JavaForwardAnalysis;
import edu.uwm.cs.fluid.util.PairLattice;
import edu.uwm.cs.fluid.util.UnionLattice;

/**
 * Specialized intraprocedural analysis whose flow analysis models an 
 * evaluation stack, additional arbitrary state, and also infers the state
 * of local variables.
 * 
 * <p>Primarily aggregates additional class declarations that are all
 * interrelated.
 * 
 * @param <I> The type of the state to be inferred for each local variable.
 * @param <T> The type of the overall value used by analysis.  
 * @param <L_I> The lattice type of the inferred variable states.
 * @param <L_T> The lattice type of the overall analysis.
 */
public abstract class StackEvaluatingAnalysisWithInference
extends IntraproceduralAnalysis<StackEvaluatingAnalysisWithInference.EvalValue, StackEvaluatingAnalysisWithInference.EvalLattice, JavaForwardAnalysis<StackEvaluatingAnalysisWithInference.EvalValue, StackEvaluatingAnalysisWithInference.EvalLattice>> {
  protected static final ImmutableHashOrderSet<Source> EMPTY =
      ImmutableHashOrderSet.<Source>emptySet();

  
  
  protected StackEvaluatingAnalysisWithInference(final IBinder binder) {
    super(binder);
  }
  
  
  
  @Override
  protected abstract JavaForwardAnalysis<EvalValue, EvalLattice> createAnalysis(final IRNode flowUnit);

  
  
  // ======================================================================
  
    
  
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
    STRING_LITERAL(962);
    
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
    
    public IRNode getAnnotatedNode(final IBinder binder) {
      return first().getAnnotatedNode(binder, second());
    }
    
    public static String setToString(final Set<Source> sources) {
      final List<String> strings = new ArrayList<String>();
      for (final Source src : sources) {
        final Kind k = src.first();
        final IRNode where = src.second();
        final Element value = src.third();
        final Operator op = JJNode.tree.getOperator(where);
        final IJavaRef javaRef = JavaNode.getJavaRef(where);
        final int line = javaRef == null ? -1 : javaRef.getLineNumber();
        final StringBuilder sb = new StringBuilder();
        sb.append(k);
        sb.append('[');
        sb.append(op.name());
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
    
    public Element getNonNullState() { return first(); }
    public Set<Source> getSources() { return second(); } 
    
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
      return new Base(NonNullRawLattice.MAYBE_NULL, lattice2.bottom());
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
    
    public Base injectValue(final Element v) {
      return newPair(v, ImmutableHashOrderSet.<Source>emptySet());
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
  public static final class StatePairLattice
  extends PairLattice<Base[], Element[], LocalStateLattice, InferredLattice, State> {
    protected StatePairLattice(final LocalStateLattice l1, final InferredLattice l2) {
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
  public static final class EvalValue
  extends EvaluationStackLattice.EvalPair<Base, State> {
    protected EvalValue(final ImmutableList<Base> v1, final State v2) {
      super(v1, v2);
    }
  }



  /**
   * Specialization of {@link EvaluationStackLattice} for use with
   * {@link EvalValue}.
   * 
   * @param <E> The type of the stack elements.
   * @param <I> The type of the state to be inferred for each local variable
   * @param <V> The type of overall state object used during evaluation.
   * @param <R> The type of the overall value used by analysis.  
   * @param <L_E> The lattice type of the stack elements.
   * @param <L_I> The lattice type of the inferred variable states.
   * @param <L_V> The lattice type of the overall state object.
   */
  public static final class EvalLattice
  extends EvaluationStackLattice<Base, State, BaseLattice, StatePairLattice, EvalValue> {
    protected EvalLattice(final BaseLattice l1, final StatePairLattice l2) {
      super(l1, l2);
    }
   
    
    
    public final IRNode[] getInferredStateKeys() {
      return lattice2.getInferredStateKeys();
    }
    
    public final int indexOfInferred(final IRNode var) {
      return lattice2.indexOfInferred(var);
    }
    
    public final EvalValue inferVar(final EvalValue v, final int idx, final Element e, final IRNode src) {
      return newPair(v.first(), lattice2.inferVar(v.second(), idx, e, src));
    }
    
    @Override
    protected EvalValue newPair(final ImmutableList<Base> v1, final State v2) {
      return new EvalValue(v1, v2);
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
    
    public EvalValue getEmptyValue() {
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

    public EvalValue setThis(final EvalValue v, final IRNode rcvrDecl, final Base b) {
      return newPair(v.first(), lattice2.setThis(v.second(), rcvrDecl, b));
    }
    
    public Base getThis(final EvalValue v, final IRNode rcvrDecl) {
      return lattice2.getThis(v.second(), rcvrDecl);
    }
    
    public int indexOf(final IRNode var) {
      return lattice2.indexOf(var);
    }
    
    public EvalValue setVar(final EvalValue v, final int idx, final Base b) {
      return newPair(v.first(), lattice2.setVar(v.second(), idx, b));
    }
    
    public EvalValue setVarNonNullIfNotAlready(final EvalValue v, final int idx, final Kind k, final IRNode where) {
      return newPair(v.first(), lattice2.setVarNonNullIfNotAlready(v.second(), idx, k, where));
    }
    
    public Base getVar(final EvalValue v, final int idx) {
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



  // ======================================================================

  
  
  public static abstract class InferredVarStateQuery<
      SELF extends InferredVarStateQuery<SELF, R>,
      R extends Result<?>>
  extends SimplifiedJavaFlowAnalysisQuery<SELF, R, EvalValue, EvalLattice> {
    protected InferredVarStateQuery(
        final IThunk<? extends IJavaFlowAnalysis<EvalValue, EvalLattice>> thunk) {
      super(thunk);
    }
    
    protected InferredVarStateQuery(final Delegate<SELF, R, EvalValue, EvalLattice> d) {
      super(d);
    }
    
    @Override
    protected final RawResultFactory getRawResultFactory() {
      return RawResultFactory.NORMAL_EXIT;
    }
  }

  

  /**
   * Abstract result type for the the "inferred variables query"
   * {@link InferredVarStateQuery}.
   *
   * @param <I> The type of the state to be inferred for each local variable.
   * @param <L> The lattice type of the inferred variable states.
   */
  public static abstract class Result<P extends PromiseDrop<?>>
  implements Iterable<InferredVarState> {
    protected final NonNullRawLattice inferredStateLattice;
    private final IRNode[] keys;
    private final Element[] values;
    
    protected Result(final IRNode[] keys, final Element[] val, final NonNullRawLattice sl) {
      this.keys = keys;
      this.values = val;
      this.inferredStateLattice = sl;
    }
    
    @Override
    public final Iterator<InferredVarState> iterator() {
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
          return new InferredVarState(
              keys[currentIdx], inferredState);
        }
      };
    }
    
    public final boolean lessEq(final Element a, final Element b) {
      return inferredStateLattice.lessEq(a, b);
    }

    public abstract P getPromiseDrop(IRNode n);
    
    public abstract Element injectPromiseDrop(P pd);
  }
  
  
  
  public static final class InferredVarState
  extends Pair<IRNode, Element> {
    public InferredVarState(
        final IRNode varDecl, final Element state) {
      super(varDecl, state);
    }
    
    public IRNode getLocal() { return first(); }
    public Element getState() { return second(); }
  }
}
