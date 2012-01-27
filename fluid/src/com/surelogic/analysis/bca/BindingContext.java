package com.surelogic.analysis.bca;

import java.util.ArrayList;
import java.util.List;

import com.surelogic.analysis.LocalVariableDeclarations;
import com.surelogic.analysis.uniqueness.UniquenessUtils;
import com.surelogic.annotation.rules.UniquenessRules;

import edu.cmu.cs.fluid.FluidRuntimeException;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.PlainIRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.AssignExpression;
import edu.cmu.cs.fluid.java.operator.CastExpression;
import edu.cmu.cs.fluid.java.operator.ConditionalExpression;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.Initialization;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.NullLiteral;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.QualifiedThisExpression;
import edu.cmu.cs.fluid.java.operator.SuperExpression;
import edu.cmu.cs.fluid.java.operator.ThisExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.CachedSet;
import edu.cmu.cs.fluid.util.ImmutableSet;
import edu.uwm.cs.fluid.util.ArrayLattice;
import edu.uwm.cs.fluid.util.UnionLattice;

/**
 * Information about binding of locals to initial values, newly dynamically
 * allocated (unique) values, and unique fields. Essentially this data structure
 * is a pure table, indexed by a fixed set of locals for a method. The value
 * for each local is a set of objects it could be bound to. The null information
 * case is when the set is infinite; the perfect information is when the set is
 * empty. We try to avoid infinite sets; if an expression computes an unknown
 * reference, we put the expression in the set, rather than return an infinite
 * set. The elements of the set are
 * <ul>
 * <li>parameter declarations, which represent their initial values.
 * <li>receiver declarations, which represent the 'this' object.
 * <li>constructor or method calls that return unique objects, which represent
 * any instance of its evaluation during the execution of the method.
 * <li>accesses of unique fields which represents any evaluation of the given
 * FieldRef expression IRNode.
 * <li>expressions returning unknown values.
 * </ul>
 * The set can be used to determine all the parameters or new constructor calls
 * an expression could evaluate to.
 * 
 * <p>The lattice values are arrays of sets of IRNodes.  Each array index
 * corresponds to a local variable or parameter declaration in the method.  The
 * sets are initialization values that reach the variable.
 * 
 * <p>To allow an empty value to be distinguished from bottom, we actually
 * add an extra array index to each lattice that maps to fixed set of a single
 * bogus element.  Because the bottom value is an array of all empty sets, this
 * prevents our empty value, an array with all empty sets but one, from being
 * equal to bottom.  It also avoids adding any complication to the rest of the 
 * use of the lattice because by adding it to the end of the array, it's an
 * element that will never actually be used.
 */
public final class BindingContext extends ArrayLattice<UnionLattice<IRNode>, ImmutableSet<IRNode>> {
  // =========================================================================
  // == Bogus values for differentiation empty from bottom
  // =========================================================================
  
  /**
   * In order to keep our analysis transfer functions strict, we need to create
   * a dummy value that we use as a set element for a dummy array location.
   * @see #IGNORE_ME_SINGLETON_SET
   */
  private static final IRNode IGNORE_ME = new PlainIRNode();
  static {
    JJNode.setInfo(IGNORE_ME, "<ignore>");
  }
  
  /**
   * Our bogus set that we add to the end of the array to differentiate empty
   * from bottom.  We used to use a single static final field for this purpose,
   * but this fails to work because at the end of an analysis pass JSure clears
   * the CachedSet cache, and our referenced set is no longer the one that is
   * in the cache.  This would create problems when manipulating lattice members
   * in the join and meet methods, and ultimately cause {@link #isNormal} to 
   * incorrectly return {@value false}.  So now we have a single bogus set per
   * lattice.
   */
  private final ImmutableSet<IRNode> ignoreMeSingletonSet =
    CachedSet.<IRNode>getEmpty().addElement(IGNORE_ME);

  private static final IRNode EXTERNAL_VAR = new PlainIRNode();
  static {
    JJNode.setInfo(EXTERNAL_VAR, "<external variable>");
  }
  
  
  
  public static boolean isExternalVar(final IRNode node) {
    return node.equals(EXTERNAL_VAR);
  }
  
  
  
  // =========================================================================
  // == Fields
  // =========================================================================
  
  /**
   * The binder to use.
   */
  private final IBinder binder;

  /**
   * The method or constructor declaration being analyzed.  If a static class
   * initializer is being analyzed this refers to a ClassInitDeclaration node.
   * If the an instance initializer is being analyzed, then this should be the
   * constructor declaration of the constructor that is invoking the initializer,
   * unless the initializer is part of an anonymous class expression, in which
   * case this will be an InitDeclaration node.
   */
  private final IRNode methodDecl;
  
  /**
   * The VariableDeclarator and ParameterDeclaration nodes of all the parameters
   * and local variables declared in the method/constructor.  The position of a
   * declaration in this array is used to index into appropriate array lattice
   * position.  If we are ignoring primitively typed variables, then they are
   * not including in this list, but are included in the {@link #ignore} 
   * list.
   */
  private final IRNode[] locals;
  
  /**
   * The VariableDeclaration and ParamterDeclaration nodes of all the variables
   * visible in the scope, but not being tracked by analysis.  This includes
   * two classes of variables: 
   * <ul>
   * <li>The final
   * parameters and local variables declared in external contexts that are
   * visible within the method/constructor being analyzed.  These will
   * exist if the method/constructor is part of a nested class declared within
   * another method/constructor.
   * <li>All the primitively typed parameters and local variables if we are not
   * tracking them.
   * </ul>  
   */
  private final IRNode[] ignore;
  
  private final boolean[] isExternal;
  

  
  // =========================================================================
  // == Constructor and static factory method
  // =========================================================================
  
  /** Create a new BindingContext lattice for a particular method. */
  @SuppressWarnings("unchecked")
  private BindingContext(
      final IRNode md, final IRNode[] locals, final IRNode[] ignore, 
      final boolean[] isExternal, final IBinder binder) {
    // We add one to the # of locals to make room for our bogus element
    super(new UnionLattice<IRNode>(), locals.length + 1, new ImmutableSet[0]);
    this.methodDecl = md;
    this.locals = locals;
    this.ignore = ignore;
    this.isExternal = isExternal;
    this.binder = binder;
  }

  
  
  /**
   * Create a new BindingContext lattice for a particular flow unit.
   * The flow unit needs to be a MethodDeclaration, ConstructorDeclaration,
   * ClassInitDeclaration, or InitDeclaration.
   */
  public static BindingContext createForFlowUnit(final boolean ignorePrimitives,
      final IRNode flowUnit, final IBinder binder) {
    final LocalVariableDeclarations lvd = LocalVariableDeclarations.getDeclarationsFor(flowUnit);
    final List<IRNode> localsOfInterest = new ArrayList<IRNode>(lvd.getLocal().size());
    final List<IRNode> ignore = new ArrayList<IRNode>(lvd.getExternal());
    if (ignorePrimitives) {
      LocalVariableDeclarations.separateDeclarations(binder, lvd.getLocal(), localsOfInterest, ignore);
    }
    final IRNode[] localArray = new IRNode[localsOfInterest.size()];
    final IRNode[] ignoreArray = new IRNode[ignore.size()];
    final boolean[] isExternal = new boolean[ignore.size()];
    for (int i = 0; i < lvd.getExternal().size(); i++) isExternal[i] = true;
    
    return new BindingContext(flowUnit,
        localsOfInterest.toArray(localArray),
        ignore.toArray(ignoreArray), isExternal,
        binder);
  }
  

  
  // =========================================================================
  // == Extra lattice value methods
  // =========================================================================

  /**
   * Get the empty value for the lattice.  This is value has the last index
   * refer to {@link #IGNORE_ME_SINGLETON_SET}, and the rest of the values 
   * refer to the empty set.
   */
  @SuppressWarnings("unchecked")
  public ImmutableSet<IRNode>[] getEmptyValue() {
    final ImmutableSet<IRNode>[] empty = new ImmutableSet[locals.length + 1];
    for (int i = 0; i < locals.length; i++) {
      empty[i] = CachedSet.<IRNode>getEmpty();
    }
    empty[locals.length] = ignoreMeSingletonSet;
    return empty;
  }
  
  /**
   * Get the initial value; that is, the lattice value for use on entry to
   * the method.
   */
  public ImmutableSet<IRNode>[] getInitialValue() {
    ImmutableSet<IRNode>[] initValue = getEmptyValue();
    for (int i = 0; i < locals.length; i++) {
      final IRNode local = locals[i];
      if (ParameterDeclaration.prototype.includes(JJNode.tree.getOperator(local))) {
        initValue = this.updateDeclaration(initValue, local,
            CachedSet.<IRNode>getEmpty().addElement(local));
      }
    }
    return initValue;
  }
  
  /**
   * Is the lattice value normal, that is, neither top nor bottom?
   */
  public boolean isNormal(final ImmutableSet<IRNode>[] value) {
    /* Value is good as long as the bogus element is the special ignore set. */
    final boolean result = value[locals.length] == ignoreMeSingletonSet;
    return result;
  }
  
  /**
   * Search the list of local variable declarations and return the index of the
   * given declaration.
   * 
   * @param local
   *          The declaration to look for.
   * @return The index of the declaration in {@link #locals} or <code>-1</code>
   *         if the declaration is not found.
   */
  private int findLocal(final IRNode decl) {
    for (int i = 0; i < locals.length; ++i) {
      if (locals[i].equals(decl)) return i;
    }
    return -1;
  }
  
  /**
   * Search the list of ignored variable declarations and return the index of
   * the given declaration.
   * 
   * @param local
   *          The declaration to look for.
   * @return The index of the declaration in {@link #ignore} or <code>-1</code>
   *         if the declaration is not found.
   */
  private int findIgnored(final IRNode decl) {
    for (int i = 0; i < ignore.length; ++i) {
      if (ignore[i].equals(decl)) return i;
    }
    return -1;
  }
  
  /**
   * Update the values associated with a particular variable declaration.
   */
  ImmutableSet<IRNode>[] updateDeclaration(final ImmutableSet<IRNode>[] oldValue,
      final IRNode decl, ImmutableSet<IRNode> objects) {
    /* If findLocal() == -1, then we have two cases (1) the variables is 
     * being ignored by analysis, or (2) we have an error.
     */
    final int localIdx = findLocal(decl);
    if (localIdx != -1) {
      return replaceValue(oldValue, findLocal(decl), objects);
    } else {
      if (findIgnored(decl) != -1) {
        return oldValue;
      } else {
        throw new FluidRuntimeException("Variable declaration " + DebugUnparser.toString(decl) + " is unknown in lattice");
      }
    }
  }

  private ImmutableSet<IRNode> localObjects(
      final ImmutableSet<IRNode>[] value, final IRNode decl) {
    /* First check the local declarations, and if we find it, use the
     * value stored in the lattice.  Otherwise, check the external declarations.
     * If we find a match there, we return the empty set.  Otherwise we 
     * throw an exception.   
     */
    final int localIdx = findLocal(decl);
    if (localIdx != -1) {
      return value[localIdx];
    } else {
      final int ignoredIdx = findIgnored(decl);
      if (ignoredIdx != -1) {
        if (isExternal[ignoredIdx]) {
          return CachedSet.<IRNode>getEmpty().addElement(EXTERNAL_VAR);
        } else {
          return CachedSet.<IRNode>getEmpty();
        }
      } else {
        throw new FluidRuntimeException("Variable declaration " + DebugUnparser.toString(decl) + " is unknown in lattice");
      }
    }
  }
  
  /**
   * TODO: Update this javadoc
   * 
   * TODO: Set breakpoints for field ref and method call, and the final return,
   * see if we ever actually care.
   * 
   * Return a set of objects that this expression could evaluate too. An
   * infinite set is used when the set may include unknown objects. The binding
   * context <tt>this</tt> is used to evaluate local variables.
   * <p>
   * This code relies on the fact that in Java, it is not possible to execute a
   * side-effect reference assignment after computing a reference value to
   * return.
   */
  public ImmutableSet<IRNode> expressionObjects(
      final ImmutableSet<IRNode>[] value, final IRNode expr) {
    final Operator op = JJNode.tree.getOperator(expr);
    
    if (Initialization.prototype.includes(op)) {
      return expressionObjects(value, Initialization.getValue(expr));
    } else if (VariableUseExpression.prototype.includes(op)) {
      /* Don't check for null, if we cannot bind the use, we have big problems */
      return localObjects(value, binder.getBinding(expr));
    } else if (ConditionalExpression.prototype.includes(op)) {
      final ImmutableSet<IRNode> trueObjects =
        expressionObjects(value, ConditionalExpression.getIftrue(expr));
      final ImmutableSet<IRNode> falseObjects =
        expressionObjects(value, ConditionalExpression.getIffalse(expr));
      return trueObjects.union(falseObjects);
    } else if (CastExpression.prototype.includes(op)) {
      return expressionObjects(value, CastExpression.getExpr(expr));
    } else if (NullLiteral.prototype.includes(op)) {
      return CachedSet.<IRNode>getEmpty();
    } else if (FieldRef.prototype.includes(op)) {
      /* XXX: John says "I find this dubious, but currently it doesn't do
       * anything because even if the field is NOT unique, the same thing is
       * returned."
       */
      /* Don't check for null, if we cannot bind the use, we have big problems */
      if (UniquenessUtils.isFieldUnique(binder.getBinding(expr))) {
        return CachedSet.<IRNode>getEmpty().addElement(expr);
      }
    } else if (AssignExpression.prototype.includes(op)) {
      return expressionObjects(value, AssignExpression.getOp2(expr));
    } else if (MethodCall.prototype.includes(op)) {
      /* XXX: John says "I find this dubious, but currently it doesn't do
       * anything because even if the field is NOT unique, the same thing is
       * returned."
       */
      /* Don't check for null, if we cannot bind the use, we have big problems */
      IRNode b = binder.getBinding(expr);
      IRNode returnNode = JavaPromise.getReturnNodeOrNull(b);
      /*
      if (returnNode == null) {
    	  System.out.println("No return node on "+DebugUnparser.toString(b));
      }
      */
      if (returnNode != null && UniquenessRules.isUnique(returnNode)) {
        return CachedSet.<IRNode>getEmpty().addElement(expr);
      }
    } else if (ThisExpression.prototype.includes(op)) {
      final IRNode rec = JavaPromise.getReceiverNode(methodDecl);
      return CachedSet.<IRNode>getEmpty().addElement(rec);
    } else if (SuperExpression.prototype.includes(op)) {
      final IRNode rec = JavaPromise.getReceiverNode(methodDecl);
      return CachedSet.<IRNode>getEmpty().addElement(rec);
    } else if (QualifiedThisExpression.prototype.includes(op)) {
      final IRNode qualifiedRec =
        JavaPromise.getQualifiedReceiverNodeByName(methodDecl,
            binder.getBinding(QualifiedThisExpression.getType(expr)));
      return CachedSet.<IRNode>getEmpty().addElement(qualifiedRec);
    }

    /* By returning the expression rather than returning an infinite
     * set, this analysis can be used to track values within a method
     * (unique and limited, for instance).  In essence, the analysis
     * can be used in the place of reaching definitions analysis.
     */
    return CachedSet.<IRNode>getEmpty().addElement(expr);
    // or -- return CachedSet.getUniverse();
  }

  
  
  
  // =========================================================================
  // == To String Methods
  // =========================================================================
  
  private static String elemToString(final Object o) {
    if (o instanceof IRNode) {
      if (EXTERNAL_VAR.equals(o)) {
        return "<external variable>";
      } else {
        return DebugUnparser.toString((IRNode)o);
      }
    } else {
      return o.toString();
    }
  }

  public static String setToString(ImmutableSet<IRNode> s) {
    final StringBuilder sb = new StringBuilder();
    if (s.isInfinite()) {
      sb.append("~");
      s = s.invertCopy();
    }
    sb.append('{');
    final java.util.Iterator<IRNode> i = s.iterator();
    while (i.hasNext()) {
      sb.append(elemToString(i.next()));
      if (i.hasNext()) sb.append(", ");
    }
    sb.append('}');
    return sb.toString();
  }

  // TODO: Probably need to deal with qualified receiver here too
  private static String localToString(final Object l) {
    if (l instanceof IRNode) {
      final IRNode n = (IRNode) l;
      final Operator op = JJNode.tree.getOperator(n);
      if (VariableDeclarator.prototype.includes(op)) {
        return VariableDeclarator.getId(n);
      } else if (ParameterDeclaration.prototype.includes(op)) {
        return ParameterDeclaration.getId(n);
      } else if (ReceiverDeclaration.prototype.includes(op)) {
        return "this";
      } else if (ReturnValueDeclaration.prototype.includes(op)) {
        return "return";
      } else {
        return "<" + op.name() + ">";
      }
    }
    return l.toString();
  }

  @Override
  public String toString(final ImmutableSet<IRNode>[] value) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < locals.length; ++i) {
      sb.append(i).append(' ').append(localToString(locals[i])).append(' ').append(setToString(value[i])).append('\n');
    }
    return sb.toString();
  }

  // TODO: Implement toString for the lattice itself?
}
