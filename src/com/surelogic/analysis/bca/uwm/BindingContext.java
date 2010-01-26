package com.surelogic.analysis.bca.uwm;

import java.util.LinkedList;
import java.util.List;

import com.surelogic.annotation.rules.UniquenessRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.AssignExpression;
import edu.cmu.cs.fluid.java.operator.CastExpression;
import edu.cmu.cs.fluid.java.operator.ConditionalExpression;
import edu.cmu.cs.fluid.java.operator.ConstructorCall;
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
import edu.cmu.cs.fluid.java.operator.VoidTreeWalkVisitor;
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
 */
final class BindingContext extends ArrayLattice<UnionLattice<IRNode>, ImmutableSet<IRNode>> {
  // =========================================================================
  // == Helper inner classes
  // =========================================================================
  
  /**
   * Crawl a method declaration to find all the local variable and parameter
   * declarations. {@link #doAccept(IRNode)} must only be called with a
   * MethodDeclaration, ConstructorDeclaration, InitDeclaration, or
   * ClassInitDeclaration.
   */
  private static class FindDeclarationsVisitor extends VoidTreeWalkVisitor {
    // =========================================================================
    // == Helper inner class for the inner class!
    // =========================================================================
    
    private final class InitializationVisitor extends VoidTreeWalkVisitor {
      /**
       * The outer visitation.
       */
      private final FindDeclarationsVisitor outer;
  
      /** 
       * Are we matching static initializers?  If <code>false</code> we match
       * instance initializers.
       */
      private final boolean isStatic; 
  
  
      
      public InitializationVisitor(
          final FindDeclarationsVisitor fdv, final boolean matchStatic) {
        outer = fdv;
        isStatic = matchStatic;
      }
      
      
      
      @Override
      public Void visitTypeDeclaration(final IRNode node) {
        /* STOP: we've encountered a type declaration.  We don't want to enter
         * the method declarations of nested class definitions.
         */
        return null;
      }
      
      @Override 
      public Void visitAnonClassExpression(final IRNode expr) {
        // Traverse into the arguments, but *not* the body
        doAccept(AnonClassExpression.getArgs(expr));
        return null;
      }
  
      @Override
      public Void visitMethodDeclaration(final IRNode node) {
        /* STOP: we've encountered a method declaration. 
         */
        return null;
      }
  
      @Override
      public Void visitConstructorDeclaration(final IRNode node) {
        /* STOP: we've encountered a method declaration. 
         */
        return null;
      }
      
      @Override
      public Void visitClassInitializer(final IRNode expr) {
        /* Only go inside an initializer if it matches the kind of initializer
         * we are looking for.
         */
        if (JavaNode.getModifier(expr, JavaNode.STATIC) == isStatic) {
          outer.doAcceptForChildren(expr);
        }
        return null;
      }
    }
 

    
    // =========================================================================
    // == Fields
    // =========================================================================
    
    private final List<IRNode> declarations = new LinkedList<IRNode>();
    
    private IRNode currentConstructor = null;
 

    
    // =========================================================================
    // == Constructor and static factory method
    // =========================================================================
    
    private FindDeclarationsVisitor() {
      super();
    }
    
    
    
    /**
     * @param mdecl A MethodDeclaration, ConstructorDeclaration, 
     * InitDeclaration, or ClassInitDeclaration node.
     */
    public static IRNode[] getDeclarationsFor(final IRNode mdecl) {
      final FindDeclarationsVisitor v = new FindDeclarationsVisitor();
      v.doAccept(mdecl);
      final IRNode[] result = new IRNode[v.declarations.size()];
      return v.declarations.toArray(result);
    }

    
    
    // =========================================================================
    // == Visitor methods
    // =========================================================================
    
    @Override
    public Void visitParameterDeclaration(final IRNode node) {
      declarations.add(node);
      return null;
    }
    
    @Override
    public Void visitVariableDeclarator(final IRNode node) {
      declarations.add(node);
      return null;
    }
    
    @Override
    public Void visitClassDeclaration(final IRNode node) {
      /* STOP: we've encountered a class declaration.  We don't want to enter
       * the method declarations of nested class definitions.
       */
      return null;
    }
  
    @Override
    public Void visitInterfaceDeclaration(final IRNode node) {
      /* STOP: we've encountered a class declaration.  We don't want to enter
       * the method declarations of nested class definitions.
       */
      return null;
    }
  
    @Override
    public Void visitEnumDeclaration(final IRNode node) {
      /* STOP: we've encountered a class declaration.  We don't want to enter
       * the method declarations of nested class definitions.
       */
      return null;
    }
  
    @Override
    public Void visitAnonClassExpression(final IRNode node) {
      /* STOP: we've encountered a class declaration.  We don't want to enter
       * the method declarations of nested class definitions.
       */
      return null;
    }
    
    @Override
    public Void visitClassInitializer(final IRNode node) {
      // Ignore, these are handled recursively
      return null;
    }
    
    @Override
    public Void visitClassInitDeclaration(final IRNode node) {
      /* We have the node for the static initializer, recursively find
       * all the static initializer blocks in the class. 
       */
      final InitializationVisitor iv = new InitializationVisitor(this, true);
      /* Must use accept for children because InitializationVisitor doesn't do anything
       * for ClassDeclaration nodes.  It's better this way anyhow because we only care
       * about the children of the class declaration to begin with.
       */ 
      iv.doAcceptForChildren(JavaPromise.getPromisedFor(node));
      return null;
    }
    
    @Override
    public Void visitInitDeclaration(final IRNode node) {
      /* We have the node for the instance initializer because we are executing
       * on behalf of an anonymous class expression; recursively find
       * all the instance initializer blocks in the class. 
       */
      final InitializationVisitor iv = new InitializationVisitor(this, false);
      /* Must use accept for children because InitializationVisitor doesn't do anything
       * for ClassDeclaration nodes.  It's better this way anyhow because we only care
       * about the children of the class declaration to begin with.
       */ 
      iv.doAcceptForChildren(JavaPromise.getPromisedFor(node));
      return null;
    }
  
    @Override
    public Void visitConstructorDeclaration(final IRNode node) {
      currentConstructor = node;
      try {
        doAcceptForChildren(node);
      } finally {
        currentConstructor = null;
      }
      return null;
    }
    
    @Override
    public Void visitConstructorCall(final IRNode node) {
      // Would process the call itself first, but we don't care about it
      
      final IRNode conObject = ConstructorCall.getObject(node);
      final Operator conObjectOp = JJNode.tree.getOperator(conObject);
      if (SuperExpression.prototype.includes(conObjectOp)) {
        // Visit the initializers.
        final InitializationVisitor helper = new InitializationVisitor(this, false);
        helper.doAcceptForChildren(JJNode.tree.getParent(currentConstructor));
      }
      return null;
    }
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
   * position.
   */
  private final IRNode[] locals;
  

  
  // =========================================================================
  // == Constructor and static factory method
  // =========================================================================
  
  /** Create a new BindingContext lattice for a particular method. */
  @SuppressWarnings("unchecked")
  private BindingContext(
      final IRNode md, final IRNode[] locals, final IBinder binder) {
    super(new UnionLattice<IRNode>(), locals.length, new ImmutableSet[0]);
    this.methodDecl = md;
    this.locals = locals;
    this.binder = binder;
  }

  
  
  /**
   * Create a new BindingContext lattice for a particular flow unit.
   * The flow unit needs to be a MethodDeclaration, ConstructorDeclaration,
   * ClassInitDeclaration, or InitDeclaration.
   */
  public static BindingContext createForFlowUnit(
      final IRNode flowUnit, final IBinder binder) {
    final IRNode[] locals = FindDeclarationsVisitor.getDeclarationsFor(flowUnit);
    return new BindingContext(flowUnit, locals, binder);
  }
  

  
  // =========================================================================
  // == Extra lattice value methods
  // =========================================================================

  /**
   * Search the list of local variable declarations and return the index of the
   * given declaration.
   * 
   * @param local
   *          The declaration to look for.
   * @return The index of the declaration in {@link #locals} or <code>-1</code>
   *         if the declaration is not found.
   */
  private int findLocal(final IRNode local) {
    for (int i = 0; i < locals.length; ++i) {
      if (locals[i].equals(local)) return i;
    }
    return -1;
  }
  
  /**
   * Update the values associated with a particular variable declaration.
   */
  ImmutableSet<IRNode>[] updateDeclaration(final ImmutableSet<IRNode>[] oldValue,
      final IRNode decl, ImmutableSet<IRNode> objects) {
    /* We don't trap for -1 from findLocal.  We want analysis to die if the
     * variable is not found because it indicates a serious problem.
     */
    return replaceValue(oldValue, findLocal(decl), objects);
  }

  private ImmutableSet<IRNode> localObjects(
      final ImmutableSet<IRNode>[] value, final IRNode decl) {
    /* We don't trap for -1 from findLocal.  We want analysis to die if the
     * variable is not found because it indicates a serious problem.
     */
    return value[findLocal(decl)];
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
      return CachedSet.getEmpty();
    } else if (FieldRef.prototype.includes(op)) {
      /* XXX: John says "I find this dubious, but currently it doesn't do
       * anything because even if the field is NOT unique, the same thing is
       * returned."
       */
      /* Don't check for null, if we cannot bind the use, we have big problems */
      if (UniquenessRules.isUnique(binder.getBinding(expr))) {
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
      if (UniquenessRules.isUnique(JavaPromise.getReturnNode(binder.getBinding(expr)))) {
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
      return DebugUnparser.toString((IRNode)o);
    } else {
      return o.toString();
    }
  }

  private static String setToString(ImmutableSet<IRNode> s) {
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
