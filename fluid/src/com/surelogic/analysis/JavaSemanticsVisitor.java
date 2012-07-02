package com.surelogic.analysis;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.ClassBody;
import edu.cmu.cs.fluid.java.operator.ClassInitializer;
import edu.cmu.cs.fluid.java.operator.ConstructorCall;
import edu.cmu.cs.fluid.java.operator.EnumConstantClassDeclaration;
import edu.cmu.cs.fluid.java.operator.EnumConstantDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.SuperExpression;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.java.operator.VoidTreeWalkVisitor;
import edu.cmu.cs.fluid.java.promise.ClassInitDeclaration;
import edu.cmu.cs.fluid.java.promise.InitDeclaration;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Visitor that further imposes Java evaluation semantics on the visitation. In
 * particular, this means that
 * <ul>
 * <li>Instance field initializers and instance initialization blocks are
 * analyzed as part of the constructor declarations (but only when that
 * constructor calls a super constructor)
 * <li>Static initialization blocks are analyzed as part of the class's
 * <code>&lt;clinit&gt;</code> method
 * <li>Anonymous class expressions are dealt with (how??)
 * <li>Enumeration types are dealt with (how??)
 * </ul>
 * 
 * <p>
 * Also, this class allows the implementation to recursively visit or not the
 * bodies of all nested types.
 * 
 * <p>
 * More info here on how to subclass...
 * 
 * <p>
 * Visitor methods that need to overridden to implement the semantics are
 * implemented as <code>final</code> methods to prevent the behavior from being
 * damaged by subclasses. In the cases where it would usually be desirable for a
 * subclass to take action on this methods, <code>handleXXX</code> methods are
 * provided for the subclass to implement instead. (Example here.)
 * 
 * <p>The visitor tracks the current enclosing method/constructor declaration...
 * 
 * <p>The visitor tracks the current enclosing type declaration...
 * 
 */
// TODO: Expand to deal with LValue
public abstract class JavaSemanticsVisitor extends VoidTreeWalkVisitor {
  private static enum VisitTypeAction {
    ANNOTATION {
      @Override
      public void visit(final JavaSemanticsVisitor v, final IRNode typeDecl) {
        v.handleAnnotationDeclaration(typeDecl);
      }
    },
    CLASS {
      @Override
      public void visit(final JavaSemanticsVisitor v, final IRNode typeDecl) {
        v.handleClassDeclaration(typeDecl);
      }
    },
    NESTED_CLASS {
      @Override
      public void visit(final JavaSemanticsVisitor v, final IRNode typeDecl) {
        v.handleNestedClassDeclaration(typeDecl);
      }
    },
    ENUM {
      @Override
      public void visit(final JavaSemanticsVisitor v, final IRNode typeDecl) {
        v.handleEnumDeclaration(typeDecl);
      }
    },
    NESTED_ENUM {
      @Override
      public void visit(final JavaSemanticsVisitor v, final IRNode typeDecl) {
        v.handleNestedEnumDeclaration(typeDecl);
      }
    },
    INTERFACE {
      @Override
      public void visit(final JavaSemanticsVisitor v, final IRNode typeDecl) {
        v.handleInterfaceDeclaration(typeDecl);
      }
    },
    NESTED_INTERFACE {
      @Override
      public void visit(final JavaSemanticsVisitor v, final IRNode typeDecl) {
        v.handleNestedInterfaceDeclaration(typeDecl);
      }
    };
    
    public abstract void visit(JavaSemanticsVisitor v, IRNode typeDecl);
  }
  
  private enum WhichMembers {
    /**
     * Tag for visiting only the static members of a class body, that is, those
     * items that are executed by the class initializer.
     */
    STATIC {
      @Override
      public boolean acceptsMember(final IRNode bodyDecl) {
        final Operator op = JJNode.tree.getOperator(bodyDecl);
        if (EnumConstantDeclaration.prototype.includes(op)) {
          return true;
        } else {
          return (FieldDeclaration.prototype.includes(op)
              || ClassInitializer.prototype.includes(op))
            && isStatic(bodyDecl);
        }
      }
    },

    /**
     * Tag for visiting only the instance members of a class body, that is,
     * those items that are executed by the instance initializer.
     */
    INSTANCE {
      @Override
      public boolean acceptsMember(final IRNode bodyDecl) {
        final Operator op = JJNode.tree.getOperator(bodyDecl);
        return (FieldDeclaration.prototype.includes(op)
            || ClassInitializer.prototype.includes(op))
          && !isStatic(bodyDecl);
      }
    },
    
    /**
     * Tag for visiting only the declarative members of a class body, that is,
     * those items that are not executed by either the static or instance
     * initializers, and should be visited during the normal traversal of the
     * class body.
     */
    DECLARATIVE {
      @Override
      public boolean acceptsMember(final IRNode bodyDecl) {
        final Operator op = JJNode.tree.getOperator(bodyDecl);
        return !FieldDeclaration.prototype.includes(op)
            && !ClassInitializer.prototype.includes(op)
            && !EnumConstantDeclaration.prototype.includes(op);        
      }
    };

    public abstract boolean acceptsMember(IRNode bodyDecl);
    
    protected boolean isStatic(final IRNode bodyDecl) {
      return JavaNode.getModifier(bodyDecl, JavaNode.STATIC);
    }
  }

  
  
  /**
   * Should the visitation proceed into the bodies of any type declarations
   * encountered?  This affects {@link #visitClassDeclaration(IRNode)},
   * {@link #visitNestedClassDeclaration(IRNode)}, {@link #visitEnumDeclaration(IRNode)},
   * {@link #visitNestedEnumDeclaration(IRNode)}, {@link #visitInterfaceDeclaration(IRNode)},
   * {@link #visitNestedInterfaceDeclaration(IRNode)},
   * {@link #visitAnonClassExpression(IRNode)}, and 
   * {@link #visitEnumConstantClassDeclaration(IRNode)}.
   */
  private final boolean visitInsideTypes;
  
  /**
   * The current type declaration we are inside of.
   */
  private IRNode enclosingType = null;
  
  /**
   * The current method/constructor declaration that we are inside of.  May be
   * a MethodDeclaration, ConstructorDeclaration, InitDeclaration, or
   * ClassInitDeclaration node.
   */
  private IRNode enclosingDecl = null;
  
  /**
   * Whether we are inside a constructor declaration.  Also true if we 
   * are analyzing the InitDeclaration associated with the construction of
   * an anonymous class.
   */
  private boolean insideConstructor = false;

  /**
   * Whether we are inside a field declaration.  This is set by 
   * {@link #visitFieldDeclaration}, and is also reset by the cases that 
   * recursively enter a class.
   */
  private boolean insideFieldDeclaration = false;
  
  /**
   * When {@link #insideFieldDeclaration} is <code>true</code> this indicates
   * whether that field declaration is static or not. 
   */
  private boolean isStaticField = false;
  
  
  
  /**
   * The normal constructor to use, if the visitation is going to start with a
   * compilation unit or type declaration.
   * 
   * @param goInside
   *          <code>true</code> if the contents of nested types should be
   *          visited.
   */
  protected JavaSemanticsVisitor(final boolean goInside) {
    this(goInside, null, null);
  }

  /**
   * The constructor to use if the visitation is going to start at a ClassBody
   * node.
   * 
   * <p>
   * <em>Do not confuse this with the constructor
   * {@link #JavaSemanticsVisitor(boolean, IRNode)}.</em>
   * 
   * @param typeDecl
   *          The type declaration node that is the part of the ClassBody node
   *          at which visitation is going to start. Must be an
   *          AnonClassExpression, EnumConstantClassDeclaration,
   *          ClassDeclaration, NestedClassDeclaration, EnumDeclaration,
   *          NestedEnumDeclaration, InterfaceDeclaration, or
   *          NestedInterfaceDeclaration.
   * @param goInside
   *          <code>true</code> if the contents of nested types should be
   *          visited.
   */
  protected JavaSemanticsVisitor(final IRNode typeDecl, final boolean goInside) {
    this(goInside, typeDecl, null);
  }
  
  /**
   * The constructor to use if the visitation is going to start at a method or
   * constructor declaration, or a ClassInitDeclaration, or an InitDeclaration.
   * This constructor makes sure that the enclosing declaration and enclosing
   * type are properly initialized.
   * 
   * <p>
   * <em>Do not confuse this with the constructor
   * {@link #JavaSemanticsVisitor(IRNode, boolean)}.</em>
   * 
   * @param goInside
   *          <code>true</code> if the contents of nested types should be
   *          visited.
   * @param flowUnit
   *          The nearest method/constructor declaration that surrounds the node
   *          at which the visitation will start. Must not be <code>null</code>.
   *          Must be one of MethodDeclaration, ConstructorDeclaration,
   *          InitDeclaration, or ClassInitDeclaration.
   */
  protected JavaSemanticsVisitor(final boolean goInside, final IRNode flowUnit) {
    this(goInside, getInitialEnclosingType(flowUnit), flowUnit);
  }
  
  /**
   * General constructor.  Only used internally.
   * 
   * @param goInside
   *          <code>true</code> if the contents of nested types should be
   *          visited.
   * @param eType
   *          The nearest type declaration that surrounds the node at which
   *          the visitation will start.
   * @param eDecl
   *          The nearest method/constructor declaration that surrounds the node
   *          at which the visitation will start. May be null if the visitation
   *          does not start inside a method/constructor. Must be one of
   *          MethodDeclaration, ConstructorDeclaration, InitDeclaration, or
   *          ClassInitDeclaration. If a MethodDeclaration or
   *          ConstructorDeclaration, then the grandparent of {@code eDecl} must
   *          be {@code eType}. If InitDeclaration or ClassInitDeclaration, then
   *          {@code JavaPromise.getPromisedFor(eDecl)} must be {@code eType}.
   */
  private JavaSemanticsVisitor(
      final boolean goInside, final IRNode eType, final IRNode eDecl) {
    super();
    visitInsideTypes = goInside;
    enclosingType = eType;
    enclosingDecl = eDecl;
  }
  
  private static IRNode getInitialEnclosingType(final IRNode flowUnit) {
    final Operator op = JJNode.tree.getOperator(flowUnit);
    if (InitDeclaration.prototype.includes(op) ||
        ClassInitDeclaration.prototype.includes(op)) {
      return JavaPromise.getPromisedFor(flowUnit);
    } else {
      // Method or constructor declaration: get the containing class
      return JJNode.tree.getParent(JJNode.tree.getParent(flowUnit));
    }
  }

  
  
  
  /**
   * Visit the immediate children of a classBody and handle the field
   * declarations and ClassInitializer blocks.
   * 
   * @param classBody
   *          A ClassBody IRNode.
   * @param isStatic
   *          <code>true</code> if we should process <code>static</code>
   *          fields and <code>static</code> initializer blocks only;
   *          <code>false</code> if we should process instance field
   *          declarations and instance initializer blocks only.
   */
  private void processClassBody(final IRNode classBody, final WhichMembers which) {
    for (final IRNode bodyDecl : ClassBody.getDeclIterator(classBody)) {
      if (which.acceptsMember(bodyDecl)) {
        this.doAccept(bodyDecl);
      }       
    }
  }

  
  
  /**
   * Get the current type declaration (if any) the visitation is inside of.
   * 
   * @return The type declaration node visitation is inside of or
   *         <code>null</code> if the visitor is not inside a type.
   */
  protected final IRNode getEnclosingType() {
    return enclosingType;
  }

  /**
   * Called whenever the visitor enters a new type declaration.  This is 
   * called after the internal record of the enclosing type has been updated, 
   * so that <code>getEnclosingType() == newType</code>.  This will eventually
   * be followed by a matched call to {@link #leavingEnclosingType} when the
   * visit of the type is completed.
   * 
   * <p>The default implementation does nothing.
   * 
   * @param newType
   *          The IRNode of the new enclosing type, either a suboperator of
   *          TypeDeclaration or an AnonClassExpression node.  Will never be
   *          <code>null</code>.
   */
  protected void enteringEnclosingType(final IRNode newType) {
    // do nothing
  }

  /**
   * Called whenever the visitor leaves a type declaration. This is called before
   * the internal record of the enclosing type has been updated, so that
   * <code>getEnclosingType() == leavingType</code>. This always follows a call to
   * {@link #enteringEnclosingType}.
   * 
   * <p>
   * The default implementation does nothing.
   * 
   * @param leavingType
   *          The IRNode of the type whose visitation was just completed, either
   *          a suboperator of TypeDeclaration or an AnonClassExpression node.
   *          Will never be <code>null</code>.
   */
  protected void leavingEnclosingType(final IRNode leavingType) {
    // do nothing
  }

  private void enterEnclosingType(final IRNode newType) {
    enclosingType = newType;
    enteringEnclosingType(newType);
  }
  
  private void leaveEnclosingType(final IRNode returningToType) {
    leavingEnclosingType(enclosingType);
    enclosingType = returningToType;
  }
  
  /**
   * Get the current method/constructor declaration, if any, the visitation is
   * inside of.
   * 
   * @return The MethodDeclaration, ConstructorDeclaration, InitDeclaration, or
   *         ClassInitDeclaration node the visitation is inside of, or
   *         <code>null</code> if the visitor is not inside a method/constructor
   *         declaration.
   */
  protected final IRNode getEnclosingDecl() {
    return enclosingDecl;
  }

  /**
   * Get whether visitation is inside a constructor or not.  Also true if the
   * visitor is visiting a init declaration on behalf of a constructor.
   */
  protected final boolean isInsideConstructor() {
    return insideConstructor;
  }
  
  /**
   * Called whenever the visitor enters a new method/constructor declaration.
   * This is called after the internal record of the enclosing declaration has
   * been updated, so that <code>getEnclosingDecl() == enteringDecl</code>. This
   * will always be followed by a call to {@link #leavingEnclosingDecl}.
   * 
   * <p>
   * The default implementation does nothing.
   * 
   * @param enteringDecl
   *          The IRNode of the new enclosing type, either a MethodDeclaration,
   *          ConstructorDeclaration, ClassInitDeclaration, or InitDeclaration.
   *          This will never be <code>null</code>.
   * @param anonClassDecl
   *          If {@code enteringDecl} is an init method from an anonymous class
   *          declaration then this is the IRNode of the AnonClassDeclaration
   *          node.  Otherwise, it is {@value null}.
   */
  protected void enteringEnclosingDecl(
      final IRNode enteringDecl, final IRNode anonClassDecl) {
    // do nothing
  }

  /**
   * Called whenever the visitor finishes visiting a method/constructor
   * declaration. This is called before the internal record of the enclosing
   * declaration has been updated, so that
   * <code>getEnclosingDecl() == leavingDecl</code>. This always follows a
   * call to {@link #enteringEnclosingDecl}.
   * 
   * <p>
   * The default implementation does nothing.
   * 
   * @param leavingDecl
   *          The IRNode of the declaration whose visitation has completed,
   *          either a MethodDeclaration, ConstructorDeclaration,
   *          ClassInitDeclaration, or InitDeclaration. This will never be
   *          <code>null</code>.
   */
  protected void leavingEnclosingDecl(final IRNode leavingDecl) {
    // do nothing
  }
  
  /*
   * If enteringDecl is an <init> method from an anonymous class declaration
   * then anonClassDecl refers to that anonymous class.  Otherwise it is null.
   */
  private void enterEnclosingDecl(final IRNode enteringDecl, final IRNode anonClassDecl) {
    enclosingDecl = enteringDecl;
    enteringEnclosingDecl(enteringDecl, anonClassDecl);
  }
  
  private void leaveEnclosingDecl(final IRNode returningToDecl) {
    leavingEnclosingDecl(enclosingDecl);
    enclosingDecl = returningToDecl;
  }
  
  
  
  /**
   * Abstract body of the methods {@link #visitClassDeclaration(IRNode)},
   * {@link #visitNestedClassDeclaration(IRNode)},
   * {@link #visitEnumDeclaration(IRNode)},
   * {@link #visitNestedEnumDeclaration(IRNode)},
   * {@link #visitInterfaceDeclaration(IRNode)}, and
   * {@link #visitNestedInterfaceDeclaration(IRNode)}. Does nothing if we should
   * not visit into types. Otherwise, it
   * <ol>
   * <li>Saves the current enclosing type and method information
   * <li>Sets the enclosing type to the new type, and calls
   * {@link #enteringEnclosingType(IRNode)}.
   * <li>Visits the new type by calling the
   * {@link VisitTypeAction#visit(JavaSemanticsVisitor, IRNode)} method of the
   * <code>action</code> parameter.
   * <li>Restores the previous enclosing method information.
   * <li>Calls {@link #leavingEnclosingType(IRNode)} and then restores the
   * previous enclosing type information.
   * </ol>
   * 
   * @param typeDecl
   *          The type declaration to visit. It is expected that this node is a
   *          ClassDeclaration, NestedClassDeclaration, EnumDeclaration,
   *          NestedEnumDeclaration, InterfaceDeclaration, or
   *          NestedInterfaceDeclaration node. In particular, it should not be
   *          an AnnotationDeclaration or TypeFormal node.
   * @param action
   *          The action to invoke if we should visit into types.
   */
  private void visitTypeDeclaration(
      final IRNode typeDecl, final VisitTypeAction action) {
    if (visitInsideTypes) {
      final IRNode prevEnclosingType = enclosingType;
      final IRNode prevEnclosingDecl = enclosingDecl;
      final boolean prevInsideConstructor = insideConstructor;
      final boolean prevInsideFieldDeclaration = insideFieldDeclaration;
      final boolean prevIsStaticField = isStaticField;
      enterEnclosingType(typeDecl);
      /* We aren't entering a method/constructor declaration here, so we
       * don't want to call enteringEnclosingDecl(). 
       */
      enclosingDecl = null;
      insideConstructor = false;
      insideFieldDeclaration = false;
      isStaticField = false;
      try {
        action.visit(this, typeDecl);
      } finally {
        /* We will have already left a method/constructor declaration before
         * getting here, so leavingEnclosingDecl() will have already been
         * called. 
         */
        isStaticField = prevIsStaticField;
        insideFieldDeclaration = prevInsideFieldDeclaration;
        insideConstructor = prevInsideConstructor;
        enclosingDecl = prevEnclosingDecl;
        leaveEnclosingType(prevEnclosingType);
      }
    }
  }

  /**
   * Visit a type declaration that is a ClassDeclaration,
   * NestedClassDeclaration, EnumDeclaration, NestedEnumDeclaration,
   * InterfaceDeclaration, or NestedInterfaceDeclaration node. In particular,
   * this will not be called to visit an AnnotationDeclaration or TypeFormal
   * node.  This method is not called directly by any of the <code>visit*</code> 
   * methods.  Rather, it is called by the default implementations of 
   * {@link #handleClassDeclaration(IRNode)}, {@link #handleEnumDeclaration(IRNode)},
   * and {@link #handleInterfaceDeclaration(IRNode)}.
   * 
   * <p>The default implementation of this method simply visits all the 
   * children of the type declaration node by calling <code>doAcceptForChildren(typeDecl)</code>.  This method should be overridden
   * if you want to have a single visitation action for all ClassDeclaration,
   * NestedClassDeclaration, EnumDeclaration, NestedEnumDeclaration,
   * InterfaceDeclaration, and NestedInterfaceDeclaration nodes.  If you need
   * to treat the nodes differently, then you should instead override the
   * {@link #handleClassDeclaration(IRNode)}, {@link #handleNestedClassDeclaration(IRNode)}, 
   * {@link #handleEnumDeclaration(IRNode)}, {@link #handleNestedEnumDeclaration(IRNode)},
   * {@link #handleInterfaceDeclaration(IRNode)}, and {@link #handleNestedInterfaceDeclaration(IRNode)}
   * methods.
   * 
   * @param typeDecl
   */
  protected void handleNonAnnotationTypeDeclaration(final IRNode typeDecl) {
    doAcceptForChildren(typeDecl);
  }

  
  
  /**
   * Process the given node as a method call.  Called by the default
   * implementations of {@link #handleAnonClassExpression(IRNode)},
   * {@link #handleConstructorCall(IRNode)},
   * {@link #handleEnumConstantClassDeclaration(IRNode)},
   * {@link #handleNormalEnumConstantDeclaration(IRNode)},
   * {@link #handleSimpleEnumConstantDeclaration(IRNode)},
   * {@link #visitMethodCall(IRNode)}, and 
   * {@link #visitNewExpression(IRNode)}.
   * 
   * <p>Default implementation does nothing.
   */
  protected void handleAsMethodCall(final IRNode call) {
    // Does nothing
  }
  
  
  
  /**
   * Visit an annotation declaration.   Does nothing if we should not
   * visit into types.  Otherwise, it
   * <ol>
   * <li>Saves the current enclosing type and method information
   * <li>Sets the enclosing type to the new class.
   * <li>Visits the class by calling {@link #handleClassDeclaration(IRNode)}.
   * <li>Restores the previous enclosing type and method information.
   * </ol>
   * 
   * <p>The default implementation of {@link JavaSemanticsVisitor#handleAnnotationDeclaration(IRNode)}
   * does nothing.
   */
  @Override
  public final Void visitAnnotationDeclaration(final IRNode classDecl) {
    visitTypeDeclaration(classDecl, VisitTypeAction.ANNOTATION);
    return null;
  }

  /**
   * Visit an annotation declaration.  Called by {@link #visitAnnotationDeclaration(IRNode)}.
   * The default implementation does nothing.
   * 
   * <p>It is the responsibility of the subclass implementation to visit the 
   * children of this node (or not) as appropriate to the analysis. 
   * 
   * @param classDecl The class declaration node.  (May also be a 
   * NestedAnnotationDeclaration node.)
   */
  protected void handleAnnotationDeclaration(final IRNode classDecl) {
    // Do nothing
  }

  
  
  /**
   * Visit an anonymous class expression.  The most complicated of all the
   * visitations.  Does the following
   * <ol>
   *   <li>Visits the expression itself by calling {@link #handleAnonClassExpression}.
   *   <li>Calls {@link #getAnonClassInitAction} to get a helper for visiting
   *   the initialization of the anonymous class.
   *   <li>If the initialization helper is not <code>null</code>, or the visitor
   *   is set to visit inside types then the initialization
   *   of the anonymous class is recursively visited using an 
   *   {@link #InstanceInitializationVisitor}.  
   *     <ol>
   *       <li>The current enclosing type and method are saved.
   *       <li>The enclosing type is set to the anonymous class and
   *       {@link #enterEnclosingType(IRNode)}} is called.
   *       <li>If the initialization helper is not <code>null</code> we visit
   *       the instance initializers of the anonymous class:
   *         <ol>
   *           <li>The
   *           enclosing method is set to the anonymous class's <code>&lt;init&gt;</code>
   *           method as represented by an InitDeclaration node, and we
   *           record that we are inside of a constructor.
   *           <li>The {@link #enteringEnclosingDecl(IRNode)} method is called
   *           and passed the InitDeclaration node as the new enclosing 
   *           declaration.
   *           <li>The {@link InstanceInitAction#tryBefore()} method of the 
   *           initialization helper is called immediately before the recursive visit is
   *           begin.
   *           <li>We recursively visit the anonymous class to visit the instance
   *           initializers. 
   *           <li>The {@link InstanceInitAction#finallyAfter() method of the
   *           initialization helper is called immediately after the recursive visit 
   *           ends (whether normally or with an exception).
   *           <li>The {@link #leavingEnclosingDecl(IRNode)} method is called
   *           and passed the InitDeclaration node as the enclosing declaration
   *           we are leaving.
   *           <li>We restore the original enclosing method, and set
   *           that we are no longer inside a constructor.
   *           <li>The {@link InstanceInitAction#afterVisit() method of the
   *           initialization helper is called so that the results of the recursive visit
   *           can be integrated into the results of the main visit.
   *         </ol>
   *       <li>If the bodies of type declarations are supposed to be
   *       visited, then the body of the anonymous class is visited:
   *         <ol>
   *           <li>We save the original enclosing type and method.
   *           <li>We set the enclosing type to the anonymous class and clear the
   *           enclosing method.  <em>{@link #enteringEnclosingDecl(IRNode)} is not called.</em>
   *           <li>We visit the body of the anonymous class by calling 
   *           {@link #handleAnonClassAsTypeDeclaration}.
   *           <li>We restore the enclosing type and method to their original values.
   *            <em>{@link #leavingEnclosingDecl(IRNode)} is not called.</em>
   *         </ol>
   *       <li>{@link #leavingEnclosingType(IRNode)} is called and the original
   *       enclosing type is restored.
   *     </ol>
   * </ol>
   * 
   * <p>The default implementation of {@link #handleAnonClassExpression} visits
   * the arguments of the expression&mdash;and not the body&mdash;by calling
   * <code>doAccept(AnonClassExpression.getArgs(expr))</code>.  If you care about
   * the fact AnonClassExpressions are also AllocationCallExpressions, then
   * you should handle that case in your implementation of 
   * {@link #handleAnonClassExpression}; you cannot rely on your implementation
   * of {@link #visitAllocationCallExpression(IRNode)} to visit anonymous class
   * expression nodes.
   * 
   * <p>The default implementation of {@link #getAnonClassInitAction} 
   * returns <code>null</code>.
   * 
   * <p>The default implementation of {@link #handleAnonClassAsTypeDeclaration}
   * visits the class body of the anonymous class expression by calling 
   * <code>doAccept(AnonClassExpression.getBody(expr))</code>.  This class
   * does not do anything to share actions between {@link #handleNonAnnotationTypeDeclaration(IRNode)}
   * and {@link #handleAnonClassAsTypeDeclaration}: if you need these visitations
   * to be similar, your subclass must take care of that itself.
   */
  @Override
  public final Void visitAnonClassExpression(final IRNode expr) {
    // Visit the anon class expression (ignoring it's body)
    handleAnonClassExpression(expr);
  
    // Deal with the declaration
    processAnonClassExpression(expr, AnonClassExpression.getBody(expr));
    
    return null;
  }



  /**
   * Visit an anonymous class declaration.  Called by {@link #visitAnonClassExpression(IRNode)}.
   * The default implementation 
   * <ol>
   * <li>Visits the arguments to the expression by
   * calling <code>doAccept(AnonClassExpression.getArgs(expr))</code>.
   * <li>Processes the expression as a method call by calling
   * {@link #handleAsMethodCall(IRNode)}.
   * </ol>
   * 
   * <p>If the analysis cares that an anonymous class expression is also an 
   * AllocationCallExpression (that is, a kind of method/constructor call) then
   * that should be dealt with in the implementation of this method, after the
   * arguments are visited.
   * 
   * <p>It is the responsibility of the subclass implementation to visit the 
   * children of this node (or not) as appropriate to the analysis.  <em>This
   * method should not visit the class body of the anonymous class expression.</em>
   * 
   * @param expr The anonymous class expression node.
   */
  protected void handleAnonClassExpression(final IRNode expr) {
    doAccept(AnonClassExpression.getArgs(expr));
    handleAsMethodCall(expr);
  }

  /**
   * Get the initialization action to use when recursively visiting the instance
   * initializers of an anonymous class expression (including those declared
   * using an EnumConstantClassDeclaration):
   * <ul>
   * <li>The {@link InstanceInitAction#tryBefore()} method of the action is
   * called immediately before the recursive visit begins, and is meant to set
   * up any contextual information needed by the analysis.
   * <li>The {@link InstanceInitAction#finallyAfter()} method of the action is
   * called immediately after the visit ends (with or without exception), and is
   * meant to cleanup/restore any contextual information needed by the analysis.
   * <li>The {@link InstanceInitAction#afterVisit()} method of the action is
   * called only if there is no exception and is meant to integrate the results
   * of the recursive visit into the main results.
   * </ul>
   * 
   * <p>
   * The argument <code>classBody</code> exists because we cannot assume that
   * <code>expr</code> is an AnonClassExpression. It could be a
   * EnumConstantClassDeclaration. The methods for getting the class body
   * differ in these two cases, so it is
   * better to have the caller provide them to us.
   * 
   * <p>
   * The default implementation returns {@link InstanceInitAction#NULL_ACTION}.
   * 
   * @param expr
   *          The anonymous class expression node or enum constant class
   *          declaration node.
   * @param classBody
   *          The ClassBody node of the declaration
   * 
   * @return The action to use. This must never return <code>null</code>.
   */
  protected InstanceInitAction getAnonClassInitAction(
      final IRNode expr, final IRNode classBody) {
    return InstanceInitAction.NULL_ACTION;
  }



  /**
   * Visit an anonymous class expression, including those declared as
   * EnumConstantClassExpressions, as if it were a type declaration.
   * Called by {@link #processAnonClassExpression(IRNode, IRNode)} only if 
   * the bodies of types are supposed to be visited.  The default implementation
   * visits the class body of the anonymous class by calling 
   * <code>doAccept(classBody)</code>.
   * 
   * <p>By default there is no relationship between {@link #handleNonAnnotationTypeDeclaration(IRNode)}
   * and this method: if you need these visitations
   * to be similar, your subclass must take care of that itself.
   * @param expr An AnonClassExpression or EnumConstantClassDeclaration node
   */
  protected void handleAnonClassAsTypeDeclaration(
      final IRNode expr, final IRNode classBody) {
    doAccept(classBody);
  }

  
  
  /**
   * Used by {@link #visitAnonClassExpression(IRNode) and {
   * @link #visitEnumConstantClassDeclaration(IRNode)} to set up the context
   * visit the initializers and body of the class they define.
   * 
   * @param expr
   *          The AnonClassExpression or EnumConstantClassExpression node
   * @param classBody
   *          The class body child of the <code>expr</code>.
   */
  private void processAnonClassExpression(
      final IRNode expr, final IRNode classBody) {
    // Prepare to recursively visit the initialization, if required
    final InstanceInitAction action = getAnonClassInitAction(expr, classBody);
  
    final IRNode prevEnclosingType = enclosingType;
    final IRNode prevEnclosingDecl = enclosingDecl;
    final boolean prevInsideConstructor = insideConstructor;
    final boolean prevInsideFieldDeclaration = insideFieldDeclaration;
    final boolean prevIsFieldStatic = isStaticField;
    
    // No longer inside a field declaration because are entering a type
    insideFieldDeclaration = false;
    isStaticField = false;
    
    // Now inside the anonymous type declaration
    enterEnclosingType(expr);
    try {
      /* Need to visit the instance initializer blocks and instance field 
       * declarations.  We rely on the fact that anonymous classes cannot 
       * have static field members or static initializer blocks.  We ignore
       * method and constructor declarations.  I know this seems odd, but
       * see Bug 1662.  Technically the initializer blocks of the anonymous
       * class expression are executed as part of the enclosing method/constructor. 
       */
      insideConstructor = true; // We are inside the constructor of the anonymous class
      enterEnclosingDecl(JavaPromise.getInitMethodOrNull(expr), expr); // Inside the <init> method
      action.tryBefore();
      try {
        processClassBody(classBody, WhichMembers.INSTANCE);
      } finally {
        action.finallyAfter();
        leaveEnclosingDecl(prevEnclosingDecl);
        insideConstructor = prevInsideConstructor;
      }
      action.afterVisit();

      // Still inside the anonymous class expression
        
      // Visit the type body if required
      if (visitInsideTypes) {
        try {
          insideConstructor = false;
          enclosingDecl = null; // We are not inside of any method or constructor -- see comments in visitNonAnnotationTypeDeclaration()
          handleAnonClassAsTypeDeclaration(expr, classBody);
        } finally {
          enclosingDecl = prevEnclosingDecl;
          insideConstructor = prevInsideConstructor;
        }
      }
    } finally {
      // Leaving the anonymous type expression
      leaveEnclosingType(prevEnclosingType);
      isStaticField = prevIsFieldStatic;
      insideFieldDeclaration = prevInsideFieldDeclaration;
    }
  }

  
  
  /**
   * Visit a ClassBody node.  Does the following things:
   *  
   * <ol>
   *   <li>Visits the expression itself by calling {@link #handleClassBody}.  
   *   (The default implementation of this method does nothing, and I cannot
   *   currently think of a good use for this hook, but it's included for
   *   completeness.)
   *   <li>Visits each child of the ClassBody node that is <em>not</em> a 
   *   FieldDeclaration, ClassInitializer, or EnumConstantDeclaration.
   *   <li>We simulate execution of the static initializer:
   *     <ol>
   *       <li>The enclosing method is set to the class initializer
   *       <code>&lt;clinit&gt;</code> method as represented by the enclosing
   *       type's ClassInitDeclaration node by calling
   *       {@link #enteringEnclosingDecl(IRNode)}.
   *       <li>We call {link {@link #handleClassInitDeclaration(IRNode)} with
   *       the ClassInitDeclaration node.
   *       <li>We visit each child of the ClassBody node that is an
   *       EnumConstantDeclaration node, a static FieldDeclaration, or a 
   *       static ClassInitializer.
   *       <li>The {@link #leavingEnclosingDecl(IRNode)} method is called
   *       and passed the ClassInitDeclaration node as the enclosing declaration
   *       we are leaving.
   *       <li>We restore the original enclosing method.
   *     </ol>
   * </ol>
   */
  @Override
  public Void visitClassBody(final IRNode classBody) {
    handleClassBody(classBody);
    
    /*
     * Don't visit FieldDeclarations, ClassInitializers, or
     * EnumConstantDeclarations. This are handled by treating instance
     * initialization and class initialization separately. Instance
     * initialization is handled when processing ConstructorCall and
     * AnonClassExpression nodes. Class initialization is handled at the end of
     * this method.
     */
    processClassBody(classBody, WhichMembers.DECLARATIVE);
    
    /* Now handle class initialization by entering the context of <clinit>
     * and visiting static field declarations, static class initializers,
     * and enumeration constant declarations.
     */
    final IRNode prevEnclosingDecl = enclosingDecl;
    final IRNode classInit = JavaPromise.getClassInitOrNull(enclosingType);
    enterEnclosingDecl(classInit, null);    
    try {
      /* Give subclass an opportunity to deal with the ClassInitDeclaration
       * itself.
       */
      handleClassInitDeclaration(classBody, classInit);
      /* Now visit all the static members of the class */
      processClassBody(classBody, WhichMembers.STATIC);
    } finally {
      leaveEnclosingDecl(prevEnclosingDecl);
    }

    return null;
  }

  /**
   * Called by {@link #visitClassBody} so that subclasses have a hook into
   * when a class body element is visited.  The default implementation does
   * nothing.  I cannot think of a good use for this method right now, but 
   * it's included for completeness.  This method is called <em>before</em>
   * any children of the class body are visited.
   * 
   * @param classBody The class body node being visited.
   */
  protected void handleClassBody(final IRNode classBody) {
    // do nothing
  }
  
  
  
  /**
   * Visit a class declaration.   Does nothing if we should not
   * visit into types.  Otherwise, it
   * <ol>
   * <li>Saves the current enclosing type and method information
   * <li>Sets the enclosing type to the new class.
   * <li>Visits the class by calling {@link #handleClassDeclaration(IRNode)}.
   * <li>Restores the previous enclosing type and method information.
   * </ol>
   * 
   * <p>The default implementation of {@link JavaSemanticsVisitor#handleClassDeclaration(IRNode)}
   * calls {@link #handleNonAnnotationTypeDeclaration(IRNode)}.
   * 
   * @see {@link #handleNonAnnotationTypeDeclaration(IRNode)} for more information
   * on the order of operations.
   */
  @Override
  public final Void visitClassDeclaration(final IRNode classDecl) {
    visitTypeDeclaration(classDecl, VisitTypeAction.CLASS);
    return null;
  }

  /**
   * Visit a class declaration.  Called by {@link #visitClassDeclaration(IRNode)}.
   * The default implementation calls {@link #handleNonAnnotationTypeDeclaration(IRNode)}
   * This method is called by the default implementation of 
   * {@link #handleNestedClassDeclaration(IRNode)}.
   * 
   * <p>It is the responsibility of the subclass implementation to visit the 
   * children of this node (or not) as appropriate to the analysis. 
   * 
   * @param classDecl The class declaration node.  (May also be a 
   * NestedClassDeclaration node.)
   */
  protected void handleClassDeclaration(final IRNode classDecl) {
    handleNonAnnotationTypeDeclaration(classDecl);
  }
  
  /**
   * Visit the promise node representing the static initialization method
   * <code>clinit()</code>.  We only get here if the original node passed to
   * {@link #visit(IRNode)} is a ClassInitDeclaration.   The order of operations is
   * <ol>
   *   <li>Call {@link #handleClassInitDeclaration(IRNode)}.
   *   <li>Set the enclosing method to class init declaration.
   *   <li>Call {@link #enteringEnclosingDecl(IRNode)} with the ClassInitDeclaration node as the enclosing declaration
   *   we are entering.
   *   <li>For each <code>static</code> initializer and <code>static</code>
   *   field declaration in the class associated with the ClassInitDeclaration:
   *   <ol>
   *     <li>Recursively visit <em>the children</em> of the static ClassInitializer
   *     and VariableDeclarator nodes.
   *   </ol>
   *   <li>Call {@link #leavingEnclosingDecl(IRNode)} with the ClassInitDeclaration node as the enclosing declaration
   *   we are leaving.
   *   <li>Reset the enclosing method declaration to <code>null</code>
   * </ol>
   * 
   */
  @Override
  public final Void visitClassInitDeclaration(final IRNode node) {
    /* Give subclass an opportunity to deal with the ClassInitDeclaration
     * itself.
     */
    handleClassInitDeclaration(null, node);
    
    enterEnclosingDecl(node, null);
    try {
      /* Recursively find all the static initializer blocks and static field
       * initializations in the class. 
       */
      final IRNode classDecl = JavaPromise.getPromisedFor(node);
      processClassBody(VisitUtil.getClassBody(classDecl), WhichMembers.STATIC);
    } finally {
      leaveEnclosingDecl(null);
    }
    
    // Done
    return null;
  }

  /**
   * Called by {@link #visitClassInitDeclaration(IRNode)} to handle the
   * visitation of the class init declaration node itself. This is called before
   * any of the static initializers and static field initializations are
   * visited. The enclosing decl is correctly set to the ClassInitDeclaration
   * when this method is called.
   * 
   * <p>The ClassBody node is also provided because when doing control flow
   * analysis, the static class initialization flow results are on the exit
   * edge of the ClassBody.
   * 
   * <p>
   * The default implementation does nothing.
   * 
   * @param classBody
   *          The ClassBody node being traversed to generate the traversal of
   *          the ClassInitDeclaration node, or <code>null</code> if the
   *          ClassInitDeclaration node is being directly visited.
   * @param node
   *          The ClassInitDeclaration node
   */
  protected void handleClassInitDeclaration(final IRNode classBody, final IRNode node) {
    // do nothing
  }
  
  /**
   * Visit a static or instance initializer block.
   * 
   * <p>If the initializer is <code>static</code>, we
   * <ol>
   *   <li>Set the enclosing method to the
   *   class initialization method of the current class.
   *   <li>Call {@link #enteringEnclosingDecl(IRNode)} with the ClassInitDeclaration node as the enclosing declaration
   *   we are entering.
   *   <li>Visit the contents
   *   of the initializer by calling {@link #handleStaticInitializer}.
   *   <li>Call {@link #leavingEnclosingDecl(IRNode)} with the ClassInitDeclaration node as the enclosing declaration
   *   we are leaving.
   *   <li>Reset the enclosing method declaration to <code>null</code>
   * </ol>
   * 
   * <p>Otherwise, we have two scenarios: (1) we are being recursively
   * analyzing on behalf of a constructor, or (2) we are being visited by the
   * top-level visitation.  In the case of (2), we just return because we only
   * want to process instance initializers recursively.  In the case of (1), 
   * we simply call the method {@link #handleInstanceInitializer}.  We do not have 
   * to set the enclosing declaration because this will have been set by the 
   * surrounded call context in {@link #visitAnonClassExpression(IRNode)},
   * {@link #visitInitDeclaration(IRNode)}, or 
   * {@link #visitConstructorDeclaration(IRNode)}.
   * 
   * <p>The default implementation of {@link #handleStaticInitializer(IRNode)}
   * simply visits the children of the node.
   * 
   * <p>The default implementation of {@link #handleInstanceInitiailizer(IRNode)}
   * simply visits the children of the node.
   */
  @Override
  public final Void visitClassInitializer(final IRNode expr) {
    /*
     * 2010-10-20: We only get here now if we are being explicitly visited as
     * part of a constructor or as part of a class initializer visitation. The
     * enclosing declaration context is already going to be set.
     */
    if (TypeUtil.isStatic(expr)) {
      handleStaticInitializer(expr);
    } else {
      handleInstanceInitializer(expr);
    }
    return null;

//    if (TypeUtil.isStatic(expr)) {
//      enterEnclosingDecl(ClassInitDeclaration.getClassInitMethod(enclosingType), null);
//      try {
//        handleStaticInitializer(expr);
//      } finally {
//        leaveEnclosingDecl(null);
//      }
//    } else {
//      if (insideConstructor) { // case (2)
//        handleInstanceInitializer(expr);
//      }
//    }
//    return null;
  }
  
  /**
   * Visit a static initializer.  Called by {@link #visitClassInitializer(IRNode)}.
   * The default implementation simply visits the children of the node by calling
   * <code>doAcceptForChildren(init)</code>.
   * 
   * <p>It is the responsibility of the subclass implementation to visit the 
   * children of this node (or not) as appropriate to the analysis. 
   * 
   * @param init The static initializer.
   */
  protected void handleStaticInitializer(final IRNode init) {
    doAcceptForChildren(init);
  }
  
  /**
   * Visit an instance initializer.  Called by {@link #visitClassInitializer(IRNode)}.
   * The default implementation simply visits the children of the node by calling
   * <code>doAcceptForChildren(init)</code>.
   * 
   * <p>It is the responsibility of the subclass implementation to visit the 
   * children of this node (or not) as appropriate to the analysis. 
   * 
   * @param init The instance initializer.
   */
  protected void handleInstanceInitializer(final IRNode init) {
    doAcceptForChildren(init);
  }
 
  /**
   * Visit a constructor call: <code>super(&hellip;)</code> or <code>this(&hellip)</code>.
   * Order of operations is as follows:
   * <ol>
   *   <li>Visit the node itself via a call to {@link #handleConstructorCall}
   *   <li>Get an instance initialization helper by calling 
   *   {@link #getConstructorCallInitAction(IRNode)}.
   *   <li>Visit the instance initializers of the current class if the
   *   call is of the form <code>super(&hellip;)</code>.
   *     <ol>
   *       <li>Call {@link InstanceInitAction#tryBefore()} immediately before
   *   beginning the recursive traversal to visit the instance initializers.
   *       <li>Recursively visit the instance initializers.
   *       <li>Call {@link InstanceInitAction#finallyAfter()} immediately after
   *   finishing the recursive traversal (with or without exception).
   *       <li>Call {@link InstanceInitAction#afterVisit()} to integrate
   *       the results into the main results.
   *     </ol>
   * </ol>
   * 
   * <p>The default implementation of {@link #handleConstructorCall} simply
   * visits the children of the node.
   * 
   * <p>The default implementation of {@link #getConstructorCallInitAction(IRNode)}
   * returns an action that does nothing.
   */
  @Override
  public final Void visitConstructorCall(final IRNode expr) {
    // 1. Visit the call itself
    handleConstructorCall(expr);
    
    final IRNode conObject = ConstructorCall.getObject(expr);
    final Operator conObjectOp = JJNode.tree.getOperator(conObject);
    if (SuperExpression.prototype.includes(conObjectOp)) {
      // 2.  Make sure we account for the super class's field initializers, etc
      final InstanceInitAction action = getConstructorCallInitAction(expr);
      action.tryBefore();      
      try {
        processClassBody(JJNode.tree.getParent(enclosingDecl), WhichMembers.INSTANCE);
      } finally {
        action.finallyAfter();
      }
      action.afterVisit();
    }
    return null;
  }

  /**
   * Called by {@link #visitConstructorCall(IRNode)} to handle the visitation of
   * the constructor call itself. This is called before the instance
   * initializers are visited.
   * 
   * <p>
   * The default implementation 
   * <ol>
   * <li>Visits the children of node by calling 
   * <code>doAcceptForChildren(ccall)</code>. 
   * <li>Processes the expression as a method call by calling
   * {@link #handleAsMethodCall(IRNode)}.
   * </ol>
   * 
   * <p>In most 
   * cases a reimplementation should first visit the children of the node
   * to handle the parameters, and then process the constructor call node itself. 
   * 
   * @param ccall
   *          The constructor call node.
   */
  protected void handleConstructorCall(final IRNode ccall) {
    doAcceptForChildren(ccall);
    handleAsMethodCall(ccall);
  }
  
  /**
   * Get the initialization action to use when recursively visiting the instance
   * initializers based on a constructor call node.
   * <ul>
   * <li>The {@link InstanceInitAction#tryBefore()} method of the action is
   * called immediately before the recursive visit begins, and is meant to set
   * up any contextual information needed by the analysis.
   * <li>The {@link InstanceInitAction#finallyAfter()} method of the action is
   * called immediately after the visit ends (with or without exception), and is
   * meant to cleanup/restore any contextual information needed by the analysis.
   * <li>The {@link InstanceInitAction#afterVisit()} method of the action is
   * called only if there is no exception and is meant to integrate the results
   * of the recursive visit into the main results.
   * </ul>
   * 
   * <p>The default implementation returns an action that does nothing.
   * 
   * @param ccall
   *          The constructor call node.  This node always represents a 
   *          <code>super(&hellip;)</code> expression.
   * 
   * @return The action to use.  This method must not return <code>null</code>.
   */
  protected InstanceInitAction getConstructorCallInitAction(final IRNode ccall) {
    return InstanceInitAction.NULL_ACTION;
  }
 
  /**
   * Visit a constructor declaration. The order of operations is
   * <ol>
   *   <li>Record that we are inside a constructor, and the identity of that constructor.
   *   <li>Call {@link #enteringEnclosingDecl(IRNode)} with 
   *   <code>cdecl</code> as the new enclosing declaration.
   *   <li>Visit the constructor declaration node itself by calling
   *   {@link #handleConstructorDeclaration}.
   *   <li>Record that we are no longer inside of a constructor.
   *   <li>Call {@link #leavingEnclosingDecl(IRNode)} with 
   *   <code>cdecl</code> as the enclosing declaration we are leaving.
   * </ol>
   * 
   * <p>The default implementation of {@link #handleConstructorDeclaration}
   * simply visits the children of the node.
   */
  @Override
  public final Void visitConstructorDeclaration(final IRNode cdecl) {
    // 1. Record that we are inside a constructor
    insideConstructor = true;
    enterEnclosingDecl(cdecl, null);
    try {
      // 2. Process the constructor declaration
      handleConstructorDeclaration(cdecl);
    } finally {
      // 3. Record we are no longer in a constructor
      leaveEnclosingDecl(null);
      insideConstructor = false;
    }
    return null;
  }

  /**
   * Called by {@link #visitConstructorDeclaration(IRNode)} to handle the visitation of
   * a constructor declaration. 
   * 
   * <p>
   * The default implementation simply visits the children of node by calling 
   * <code>doAcceptForChildren(cdecl)</code>.  In most 
   * cases a reimplementation should first process the constructor declaration
   * itself, and then visit the children of the node.
   * 
   * @param cdecl
   *          The constructor declaration node.
   */
  protected void handleConstructorDeclaration(final IRNode cdecl) {
    doAcceptForChildren(cdecl);
  }
  
  @Override
  public final Void visitEnumConstantClassDeclaration(final IRNode decl) {
    /* Like a static final field declaration whose initializer is an
     * AnonClassExpression.
     * 
     * So we base this off of visitFieldDeclaration assuming that the field
     * is static together with visitANonClassExpression.
     */
  
    // (1) Deal with the static field declaration
    insideFieldDeclaration = true;
    isStaticField = true;
//    // Set the enclosing declaration to the static initializer
//    enterEnclosingDecl(ClassInitDeclaration.getClassInitMethod(enclosingType), null);    
//    try {
      // Handle the constant declaration, ignoring the class body
      handleEnumConstantClassDeclaration(decl);
      
      // (2) Deal with the class body
      processAnonClassExpression(decl, EnumConstantClassDeclaration.getBody(decl));
//    } finally {
//      // Reset the enclosing declaration
//      leaveEnclosingDecl(null);
//    }
    isStaticField = false;
    insideFieldDeclaration = false;
  
    return null;
  }

  /**
   * Visit an enumeration constant class declaration.  Called by {@link #visitEnumConstantClassDeclaration(IRNode)}.
   * The default implementation 
   * <ol>
   * <li>Visits the arguments to the expression by
   * calling <code>doAccept(AnonClassExpression.getArgs(expr))</code>.
   * <li>Processes the expression as a method call by calling
   * {@link #handleAsMethodCall(IRNode)}.
   * </ol>
   * 
   * <p>It is the responsibility of the subclass implementation to visit the 
   * children of this node (or not) as appropriate to the analysis.  <em>This
   * method should not visit the class body of the enumeration constant class
   * declaration.</em>
   * 
   * @param expr The anonymous class expression node.
   */
  protected void handleEnumConstantClassDeclaration(final IRNode decl) {
    doAccept(EnumConstantClassDeclaration.getArgs(decl));
    handleAsMethodCall(decl);
  }

  /**
   * Visit an enumeration declaration.   Does nothing if we should not
   * visit into types.  Otherwise, it
   * <ol>
   * <li>Saves the current enclosing type and method information
   * <li>Sets the enclosing type to the new enumeration.
   * <li>Visits the enumeration by calling {@link #handleEnumDeclaration(IRNode)}.
   * <li>Restores the previous enclosing type and method information.
   * </ol>
   * 
   * <p>The default implementation of {@link JavaSemanticsVisitor#handleEnumDeclaration(IRNode)}
   * calls {@link #handleNonAnnotationTypeDeclaration(IRNode)}.
   * 
   * @see {@link #handleNonAnnotationTypeDeclaration(IRNode)} for more information
   * on the order of operations.
   */
  @Override
  public final Void visitEnumDeclaration(final IRNode enumDecl) {
    visitTypeDeclaration(enumDecl, VisitTypeAction.ENUM);
    return null;
  }

  /**
   * Visit an enumeration declaration.  Called by {@link #visitEnumDeclaration(IRNode)}.
   * The default implementation calls {@link #handleNonAnnotationTypeDeclaration(IRNode)}
   * This method is called by the default implementation of 
   * {@link #handleNestedEnumDeclaration(IRNode)}.
   * 
   * <p>It is the responsibility of the subclass implementation to visit the 
   * children of this node (or not) as appropriate to the analysis. 
   * 
   * @param enumDecl The enumeration declaration node.  (May also be a 
   * NestedEnumDeclaration node.)
   */
  protected void handleEnumDeclaration(final IRNode enumDecl) {
    handleNonAnnotationTypeDeclaration(enumDecl);
  }

  /**
   * Visit a field declaration.  We check to see whether
   * the visitation is currently inside a constructor or whether the declaration
   * is <code>static</code>.  If so, we then
   * <ol>
   *   <li>Set {@link #insideFieldDeclaration} to <code>true</code>.
   *   <li>Set the enclosing method to the class initializer of the current type
   *   and call {@link #enteringEnclosingDecl(IRNode)} with 
   *   the ClassInitDeclaration as the new enclosing declaration if the field
   *   is <code>static</code>.
   *   <li>Visit the children of the declaration by calling {@link #handleFieldDeclaration(IRNode)}.
   *   <li>Call {@link #leavingEnclosingDecl(IRNode)} with the ClassInitDeclaration
   *   as the enclosing declaration we are leaving and clear the enclosing method
   *   if the field is <code>static</code>.
   *   <li>Set {@link #insideFieldDeclaration} to <code>false</code>.
   * </ol>
   */
  @Override
  public final Void visitFieldDeclaration(final IRNode fieldDecl) {
    /*
     * 2010-10-20: We only get here now if we are being explicitly visited as
     * part of a constructor or as part of a class initializer visitation. The
     * enclosing declaration context is already going to be set.
     */
    insideFieldDeclaration = true;
    isStaticField = TypeUtil.isStatic(fieldDecl);
    try {
      handleFieldDeclaration(fieldDecl);
    } finally {
      isStaticField = false;
      insideFieldDeclaration = false;
    }    
    return null;

//    final boolean isStatic = TypeUtil.isStatic(fieldDecl);
//    
//    /* Only visit the children if we are inside a constructor or if the
//     * declaration is static.
//     */
//    if (insideConstructor || isStatic) {
//      insideFieldDeclaration = true;
//      isStaticField = isStatic;
//      // If static, set the enclosing declaration to the static initializer
//      if (isStatic) {
//        enterEnclosingDecl(ClassInitDeclaration.getClassInitMethod(enclosingType), null);
//      }
//      try {
//        handleFieldDeclaration(fieldDecl);
//      } finally {
//        // If static, reset the enclosing declaration
//        if (isStatic) {
//          leaveEnclosingDecl(null);
//        }
//        isStaticField = false;
//        insideFieldDeclaration = false;
//      }
//    }    
//    return null;
  }
  
  /**
   * Called by {@link #visitFieldDeclaration(IRNode)} to handle the visitation of
   * a field declaration. 
   * 
   * <p>
   * The default implementation simply visits the children of the node by calling 
   * <code>doAcceptForChildren(cdecl)</code>.  In most 
   * cases a reimplementation should first process the field declaration
   * itself, and then visit the children of the node.
   * 
   * @param fieldDecl
   *          The field declaration node.
   */
  protected void handleFieldDeclaration(final IRNode fieldDecl) {
    doAcceptForChildren(fieldDecl);
  }
  
  /**
   * Visit an InitDeclaration.  <em>We should never get there because
   * of the way we handle anonymous class expressions and constructors.</em>
   * Unfortunately, we do get here because the way that UniquenssAnalysis 
   * handles anonymous class expressions is broken.  So to reiterate,
   * we only get here when the InitDeclaration is from an AnonClassExpression or EnumConstantClassDeclaration
   * being visited by UniqueAnalysis.  
   * 
   * <P>We handle this similarly to an anon class expression, except we are already
   * inside the AnonClassExpression.
   */
  @Override
  public final Void visitInitDeclaration(final IRNode initDecl) {
    // Prepare to recursively visit the initialization, if required
    final InstanceInitAction action = getInitDeclarationAction(initDecl);
  
    final IRNode prevEnclosingDecl = enclosingDecl;
    final boolean prevInsideConstructor = insideConstructor;

    /* Need to visit the instance initializer blocks and instance field 
     * declarations.  We rely on the fact that anonymous classes cannot 
     * have static field members or static initializer blocks.  We ignore
     * method and constructor declarations.  I know this seems odd, but
     * see Bug 1662.  Technically the initializer blocks of the anonymous
     * class expression are executed as part of the enclosing method/constructor. 
     */
    insideConstructor = true; // We are inside the constructor of the anonymous class
    final IRNode anonClassDecl = JavaPromise.getPromisedFor(initDecl);
    enterEnclosingDecl(initDecl, anonClassDecl); // Inside the <init> method
    action.tryBefore();
    try {
      if (AnonClassExpression.prototype.includes(anonClassDecl)) {
        processClassBody(AnonClassExpression.getBody(anonClassDecl), WhichMembers.INSTANCE);
      } else {
        processClassBody(EnumConstantClassDeclaration.getBody(anonClassDecl), WhichMembers.INSTANCE);
      }
    } finally {
      action.finallyAfter();
      leaveEnclosingDecl(prevEnclosingDecl);
      insideConstructor = prevInsideConstructor;
    }
    action.afterVisit();

    return null;
  }
  
  protected InstanceInitAction getInitDeclarationAction(final IRNode initDecl) {
    return InstanceInitAction.NULL_ACTION;
  }
  
  /**
   * Visit an interface declaration.   Does nothing if we should not
   * visit into types.  Otherwise, it
   * <ol>
   * <li>Saves the current enclosing type and method information
   * <li>Sets the enclosing type to the new interface.
   * <li>Visits the interface by calling {@link #handleInterfaceDeclaration(IRNode)}.
   * <li>Restores the previous enclosing type and method information.
   * </ol>
   * 
   * <p>The default implementation of {@link JavaSemanticsVisitor#handleInterfaceDeclaration(IRNode)}
   * calls {@link #handleNonAnnotationTypeDeclaration(IRNode)}.
   * 
   * @see {@link #handleNonAnnotationTypeDeclaration(IRNode)} for more information
   * on the order of operations.
   */
  @Override
  public final Void visitInterfaceDeclaration(final IRNode intDecl) {
    visitTypeDeclaration(intDecl, VisitTypeAction.INTERFACE);
    return null;
  }

  /**
   * Visit an interface declaration.  Called by {@link #visitInterfaceDeclaration(IRNode)}.
   * The default implementation calls {@link #handleNonAnnotationTypeDeclaration(IRNode)}
   * This method is called by the default implementation of 
   * {@link #handleNestedInterfaceDeclaration(IRNode)}.
   * 
   * <p>It is the responsibility of the subclass implementation to visit the 
   * children of this node (or not) as appropriate to the analysis. 
   * 
   * @param intDecl The interface declaration node.  (May also be a 
   * NestedInterfaceDeclaration node.)
   */
  protected void handleInterfaceDeclaration(final IRNode intDecl) {
    handleNonAnnotationTypeDeclaration(intDecl);
  }
  
  /**
   * Visit a method call.  Delegates to {@link #handleMethodCall}.
   */
  @Override 
  public final Void visitMethodCall(final IRNode expr) {
    handleMethodCall(expr);
    return null;
  }
  
  /**
   * Visit a method call.  The default implementation 
   * <ol>
   * <li>Visits the children of node by calling 
   * <code>doAcceptForChildren(expr)</code>. 
   * <li>Processes the expression as a method call by calling
   * {@link #handleAsMethodCall(IRNode)}.
   * </ol>
   */
  protected void handleMethodCall(final IRNode expr) {
    doAcceptForChildren(expr);
    handleAsMethodCall(expr);
  }

  /**
   * Visit a method declaration. The order of operations is
   * <ol>
   *   <li>Record that we are inside a method, and the identity of that method.
   *   <li>Call {@link #enteringEnclosingDecl(IRNode)} with 
   *   <code>mdecl</code> as the new enclosing declaration.
   *   <li>Visit the method declaration node itself by calling
   *   {@link #handleMethodDeclaration}.
   *   <li>Call {@link #leavingEnclosingDecl(IRNode)} with 
   *   <code>mdecl</code> as the enclosing declaration we are leaving.
   *   <li>Record that we are no longer inside of a method.
   * </ol>
   * 
   * <p>The default implementation of {@link #handleMethodDeclaration} simply
   * visits the children of the node.
   */
  @Override
  public final Void visitMethodDeclaration(final IRNode mdecl) {
    // 1. Record we are inside a method
    enterEnclosingDecl(mdecl, null);
    try {
      // 2. Visit the method declaration
      handleMethodDeclaration(mdecl);
    } finally {
      // 3. Record we are no longer inside a method
      leaveEnclosingDecl(null);
    }
    return null;
  }

  /**
   * Called by {@link #visitMethodDeclaration(IRNode)} to handle the visitation of
   * a method declaration.
   * 
   * <p>
   * The default implementation simply visits the children of node by calling
   * <code>doAcceptForChildren(mdecl)</code>.  In most 
   * cases a reimplementation should first process the method declaration
   * itself, and then visit the children of the node.
   * 
   * @param mdecl
   *          The method declaration node.
   */
  protected void handleMethodDeclaration(final IRNode mdecl) {
    doAcceptForChildren(mdecl);
  }

  /**
   * Visit a nested class declaration.   Does nothing if we should not
   * visit into types.  Otherwise, it
   * <ol>
   * <li>Saves the current enclosing type and method information
   * <li>Sets the enclosing type to the new nested class.
   * <li>Visits the class by calling {@link #handleNestedClassDeclaration(IRNode)}.
   * <li>Restores the previous enclosing type and method information.
   * </ol>
   * 
   * <p>The default implementation of {@link JavaSemanticsVisitor#handleNestedClassDeclaration(IRNode)}
   * calls {@link #handleClassDeclaration(IRNode)}.
   * 
   * @see {@link #handleNonAnnotationTypeDeclaration(IRNode)} for more information
   * on the order of operations.
   */
  @Override
  public final Void visitNestedClassDeclaration(final IRNode classDecl) {
    visitTypeDeclaration(classDecl, VisitTypeAction.NESTED_CLASS);
    return null;
  }
  
  /**
   * Visit a nested class declaration.  Called by {@link #visitNestedClassDeclaration(IRNode)}.
   * The default implementation calls {@link #handleClassDeclaration(IRNode)}.
   * 
   * <p>It is the responsibility of the subclass implementation to visit the 
   * children of this node (or not) as appropriate to the analysis. 
   * 
   * @param classDecl The nested class declaration node.
   */
  protected void handleNestedClassDeclaration(final IRNode classDecl) {
    handleClassDeclaration(classDecl);
  }

  /**
   * Visit a nested enumeration declaration.   Does nothing if we should not
   * visit into types.  Otherwise, it
   * <ol>
   * <li>Saves the current enclosing type and method information
   * <li>Sets the enclosing type to the new nested enumeration.
   * <li>Visits the enumeration by calling {@link #handleNestedEnumDeclaration(IRNode)}.
   * <li>Restores the previous enclosing type and method information.
   * </ol>
   * 
   * <p>The default implementation of {@link JavaSemanticsVisitor#handleNestedEnumDeclaration(IRNode)}
   * calls {@link #handleEnumDeclaration(IRNode)}.
   * 
   * @see {@link #handleNonAnnotationTypeDeclaration(IRNode)} for more information
   * on the order of operations.
   */
  @Override
  public final Void visitNestedEnumDeclaration(final IRNode enumDecl) {
    visitTypeDeclaration(enumDecl, VisitTypeAction.NESTED_ENUM);
    return null;
  }

  /**
   * Visit a nested enumeration declaration.  Called by {@link #visitNestedEnumDeclaration(IRNode)}.
   * The default implementation calls {@link #handleEnumDeclaration(IRNode)}.
   * 
   * <p>It is the responsibility of the subclass implementation to visit the 
   * children of this node (or not) as appropriate to the analysis. 
   * 
   * @param enumDecl The nested class declaration node.
   */
  protected void handleNestedEnumDeclaration(final IRNode enumDecl) {
    handleEnumDeclaration(enumDecl);
  }

  /**
   * Visit a nested interface declaration.   Does nothing if we should not
   * visit into types.  Otherwise, it
   * <ol>
   * <li>Saves the current enclosing type and method information
   * <li>Sets the enclosing type to the new nested interface.
   * <li>Visits the interface by calling {@link #handleNestedInterfaceDeclaration(IRNode)}.
   * <li>Restores the previous enclosing type and method information.
   * </ol>
   * 
   * <p>The default implementation of {@link JavaSemanticsVisitor#handleNestedInterfaceDeclaration(IRNode)}
   * calls {@link #handleInterfaceDeclaration(IRNode)}.
   * 
   * @see {@link #handleNonAnnotationTypeDeclaration(IRNode)} for more information
   * on the order of operations.
   */
  @Override
  public final Void visitNestedInterfaceDeclaration(final IRNode intDecl) {
    visitTypeDeclaration(intDecl, VisitTypeAction.NESTED_INTERFACE);
    return null;
  }

  /**
   * Visit a nested interface declaration.  Called by {@link #visitNestedInterfaceDeclaration(IRNode)}.
   * The default implementation calls {@link #handleInterfaceDeclaration(IRNode)}.
   * 
   * <p>It is the responsibility of the subclass implementation to visit the 
   * children of this node (or not) as appropriate to the analysis. 
   * 
   * @param intDecl The nested class declaration node.
   */
  protected void handleNestedInterfaceDeclaration(final IRNode intDecl) {
    handleInterfaceDeclaration(intDecl);
  }

  /**
   * Visit a new expression. Delegates to {@link #handleNewExpression}.
   */
  @Override
  public final Void visitNewExpression(final IRNode expr) {
    handleNewExpression(expr);
    return null;
  }
  
  /**
   * Visit a new expression call.  The default implementation 
   * <ol>
   * <li>Visits the children of node by calling 
   * <code>doAcceptForChildren(expr)</code>. 
   * <li>Processes the expression as a method call by calling
   * {@link #handleAsMethodCall(IRNode)}.
   * </ol>
   */
  protected void handleNewExpression(final IRNode expr) {
    doAcceptForChildren(expr);
    handleAsMethodCall(expr);
  }
  
  @Override
  public final Void visitNormalEnumConstantDeclaration(final IRNode decl) {
    /* Like a static final field declaration whose initializer is a
     * ConstructorCall.  We don't deal with the fact that this is like a
     * constructor call here.  The implementation of
     * handleSimpleEnumConstantDeclaration() takes care of that.
     * 
     * So we base this off of visitFieldDeclaration assuming that the field
     * is static.
     */

    insideFieldDeclaration = true;
    isStaticField = true;
//    // Set the enclosing declaration to the static initializer
//    enterEnclosingDecl(ClassInitDeclaration.getClassInitMethod(enclosingType), null);    
//    try {
      handleNormalEnumConstantDeclaration(decl);
//    } finally {
//      // Reset the enclosing declaration
//      leaveEnclosingDecl(null);
//    }
    isStaticField = false;
    insideFieldDeclaration = false;

    return null;
  }

  /**
   * Called by {@link #visitNormalEnumConstantDeclaration(IRNode)} to handle 
   * the declaration itself. 
   * 
   * <p>
   * The default implementation 
   * <ol>
   * <li>Visits the children of node by calling 
   * <code>doAcceptForChildren(decl)</code>. 
   * <li>Processes the expression as a method call by calling
   * {@link #handleAsMethodCall(IRNode)}.
   * </ol>
   * 
   * <p>In most 
   * cases a reimplementation should first visit the children of the node
   * to handle the parameters, and then process the declaration node itself. 
   * 
   * @param decl
   *          The normal enumeration constant declaration
   */
  protected void handleNormalEnumConstantDeclaration(final IRNode decl) {
    doAcceptForChildren(decl);
    handleAsMethodCall(decl);
  }

  @Override
  public final Void visitSimpleEnumConstantDeclaration(final IRNode decl) {
    /* Like a static final field declaration whose initializer is a
     * ConstructorCall.  We don't deal with the fact that this is like a
     * constructor call here.  The implementation of
     * handleSimpleEnumConstantDeclaration() takes care of that.
     * 
     * So we base this off of visitFieldDeclaration assuming that the field
     * is static.
     */
  
    insideFieldDeclaration = true;
    isStaticField = true;
//    // Set the enclosing declaration to the static initializer
//    enterEnclosingDecl(ClassInitDeclaration.getClassInitMethod(enclosingType), null);    
//    try {
      handleSimpleEnumConstantDeclaration(decl);
//    } finally {
//      // Reset the enclosing declaration
//      leaveEnclosingDecl(null);
//    }
    isStaticField = false;
    insideFieldDeclaration = false;
  
    return null;
  }

  /**
   * Called by {@link #visitSimpleEnumConstantDeclaration(IRNode)} to handle 
   * the declaration itself. 
   * 
   * <p>
   * The default implementation processes the expression as a method call by calling
   * {@link #handleAsMethodCall(IRNode)}.
   * 
   * @param decl
   *          The normal enumeration constant declaration
   */
  protected void handleSimpleEnumConstantDeclaration(final IRNode decl) {
    handleAsMethodCall(decl);
  }

  /**
   * Visit a variable declaration. We could be inside a local variable
   * declaration, or inside a field declaration.
   * 
   * <p>
   * If we are inside a field declaration, then we only get here if the
   * visitation is currently inside a constructor or if the field is
   * <code>static</code>. If so, we visit the declaration by calling
   * {@link #handleFieldInitialization(IRNode, boolean)}.
   * 
   * <p>
   * If we are inside a local variable declaration then we visit the declaration
   * by calling {@link #handleLocalVariableDeclarator}.
   */
  @Override
  public final Void visitVariableDeclarator(final IRNode varDecl) {
    /*
     * If this is inside a FieldDeclaration, then we only get here if we are
     * being executed on behalf of the instance initialization or if we are part
     * of a static field declaration.
     * 
     * If this inside a DeclStatement, then we always get here.
     */
    if (insideFieldDeclaration) {
      handleFieldInitialization(varDecl, isStaticField);
    } else {
      handleLocalVariableDeclaration(varDecl);
    }
    return null;
  }

  /**
   * Called by {@link #visitVariableDeclarator(IRNode)} to handle the visitation
   * of a variable declaration that is part of a FieldDeclaration.
   * 
   * <p>
   * The default implementation simply visits the children of node by calling
   * <code>doAcceptForChildren(varDecl)</code>.  In most 
   * cases a reimplementation should first process the variable declaration
   * itself, and then visit the children of the node.
   * 
   * @param varDecl
   *          The variable declaration node.
   * @param isStatic
   *          whether the declaration is for a static field or not. When
   *          <code>true</code> {@link #insideConstructor} will be
   *          <code>false</code> and {@link #enclosingDecl} will refer a
   *          ClassInitDeclaration node. When <code>false</code>,
   *          {@link #insideConstructor} will be <code>true</code> and
   *          {@link #enclosingDecl} will refer to a ConstructorDeclaration
   *          node, or a InitDeclaration node in the case of an anonymous class
   *          expression.
   */
  protected void handleFieldInitialization(final IRNode varDecl, final boolean isStatic) {
    doAcceptForChildren(varDecl);
  }

  /**
   * Called by {@link #visitVariableDeclarator(IRNode)} to handle the visitation of
   * a variable declaration that is a local variable (that is, not a field). 
   * 
   * <p>
   * The default implementation simply visits the children of node by 
   * calling <code>doAcceptForChildren(varDecl)</code>.  In most 
   * cases a reimplementation should first process the variable declaration
   * itself, and then visit the children of the node.
   * 
   * @param varDecl
   *          The variable declaration node.
   */
  protected void handleLocalVariableDeclaration(final IRNode varDecl) {
    doAcceptForChildren(varDecl);
  }
}
