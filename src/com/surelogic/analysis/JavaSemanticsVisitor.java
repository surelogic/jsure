package com.surelogic.analysis;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.analysis.InstanceInitializationVisitor;
import edu.cmu.cs.fluid.java.analysis.InstanceInitializationVisitor.Action;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.NoInitialization;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VoidTreeWalkVisitor;
import edu.cmu.cs.fluid.java.promise.ClassInitDeclaration;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;

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
 * 
 */
// TODO: Expand to deal with LValue
public abstract class JavaSemanticsVisitor extends VoidTreeWalkVisitor {
  private static enum VisitTypeAction {
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
  
  
  
  protected static interface InstanceInitAction extends InstanceInitializationVisitor.Action {
    public void afterVisit();
  }
  
  protected static final InstanceInitAction NULL_ACTION = new InstanceInitAction() {
    public void tryBefore() { /* do nothing */ }
    public void finallyAfter() { /* do nothing */ }
    public void afterVisit() { /* do nothing */ }
  };
  
  
  
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
   * Should the visitation proceed into the bodies of any type declarations
   * encountered?  This affects {@link #visitClassDeclaration(IRNode)},
   * {@link #visitNestedClassDeclaration(IRNode)}, {@link #visitEnumDeclaration(IRNode)},
   * {@link #visitNestedEnumDeclaration(IRNode)}, {@link #visitInterfaceDeclaration(IRNode)},
   * {@link #visitNestedInterfaceDeclaration(IRNode)},
   * and {@link #visitAnonClassExpression(IRNode)}.
   */
  private final boolean visitInsideTypes;
  
  
  
  public JavaSemanticsVisitor(final boolean goInside) {
    super();
    visitInsideTypes = goInside;
  }
  
  
  
  /**
   * Get the current type declaration (if any) the visitation is inside of.
   * 
   * @return The type declaration node visitation is inside of or
   *         <code>null</code> if the visitor is not inside a type.
   */
  protected IRNode getEnclosingType() {
    return enclosingType;
  }

//  /**
//   * Called whenever the visitor enters a new type declaration.  This is 
//   * called after the internal record of the enclosing type has updated, 
//   * so that <code>getEnclosingType() == newType</code>.  This will eventually
//   * be followed by a matched call to {@link #leavingEnclosingType} when the
//   * visit of the type is completed.
//   * 
//   * <p>The default implementation does nothing.
//   * 
//   * @param newType
//   *          The IRNode of the new enclosing type, either a suboperator of
//   *          TypeDeclaration or an AnonClassExpression node.  Will never be
//   *          <code>null</code>.
//   */
//  protected void enteringEnclosingType(final IRNode newType) {
//    // do nothing
//  }
//
//  /**
//   * Called whenever the visitor leaves a type declaration. This is called after
//   * the internal record of the enclosing type has updated, so that
//   * <code>getEnclosingType() == newType</code>. This always follows a call to
//   * {@link #enteringEnclosingType}.
//   * 
//   * <p>
//   * The default implementation does nothing.
//   * 
//   * @param leavingType
//   *          The IRNode of the type whose visitation was just completed, either
//   *          a suboperator of TypeDeclaration or an AnonClassExpression node.
//   *          Will never be <code>null</code>.
//   * @param newType
//   *          The IRNode of the type to which visitation is resuming, if the
//   *          previous type was a nested type, or <code>null</code> if the
//   *          previous type was a top-level type.
//   */
//  protected void leavingEnclosingType(final IRNode leavingType, final IRNode newType) {
//    // do nothing
//  }
//
//  private void enterEnclosingType(final IRNode newType) {
//    enclosingType = newType;
//    enteringEnclosingType(newType);
//  }
//  
//  private void leaveEnclosingType(final IRNode leavingType, final IRNode newType) {
//    enclosingType = newType;
//    leavingEnclosingType(leavingType, newType);
//  }
  
  /**
   * Get the current method/constructor declaration, if any, the visitation is
   * inside of.
   * 
   * @return The MethodDeclaration, ConstructorDeclaration, InitDeclaration, or
   *         ClassInitDeclaration node the visitation is inside of, or
   *         <code>null</code> if the visitor is not inside a method/constructor
   *         declaration.
   */
  protected IRNode getEnclosingDecl() {
    return enclosingDecl;
  }

  /**
   * Called whenever the visitor enters a new method/constructor declaration.
   * This is called after the internal record of the enclosing declaration has
   * been updated, so that <code>getEnclosingDecl() == enteringDecl</code>.  This
   * will always be followed by a call to {@link #leavingEnclosingDecl}.
   * 
   * <p>
   * The default implementation does nothing.
   * 
   * @param enteringDecl
   *          The IRNode of the new enclosing type, either a MethodDeclaration,
   *          ConstructorDeclaration, ClassInitDeclaration, or InitDeclaration.
   *          This will never be <code>null</code>.
   */
  protected void enteringEnclosingDecl(final IRNode enteringDecl) {
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
  
  private void enterEnclosingDecl(final IRNode enteringDecl) {
    enclosingDecl = enteringDecl;
    enteringEnclosingDecl(enteringDecl);
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
   * <li>Sets the enclosing type to the new type.
   * <li>Visits the new type by calling the {@link VisitTypeAction#visit(JavaSemanticsVisitor, IRNode)} method of the
   * <code>action</code> parameter.
   * <li>Restores the previous enclosing type and method information.
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
  private void visitNonAnnotationTypeDeclaration(
      final IRNode typeDecl, final VisitTypeAction action) {
    if (visitInsideTypes) {
      final IRNode prevEnclosingType = enclosingType;
      final IRNode prevEnclosingDecl = enclosingDecl;
      final boolean prevInsideConstructor = insideConstructor;
      try {
        enclosingType = typeDecl;
        /* We aren't entering a method/constructor declaration here, so we
         * don't want to call enteringEnclosingDecl(). 
         */
        enclosingDecl = null;
        insideConstructor = false;
        action.visit(this, typeDecl);
      } finally {
        /* We will have already left a method/constructor declaration before
         * getting here, so leavingEnclosingDecl() will have already been
         * called. 
         */
        enclosingDecl = prevEnclosingDecl;
        insideConstructor = prevInsideConstructor;
        enclosingType = prevEnclosingType;
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
   * Visit an anonymous class expression.  The most complicated of all the
   * visitations.  Does the following
   * <ol>
   *   <li>Visits the expression itself by calling {@link #handleAnonClassExpression}.
   *   <li>Calls {@link #getAnonClassInitAction} to get a helper for visiting
   *   the initialization of the anonymous class.
   *   <li>If {@link #getAnonClassInitAction} is not null, then the initialization
   *   of the anonymous class is recursively visited using an 
   *   {@link #InstanceInitializationVisitor}.  
   *     <ol>
   *       <li>The current enclosing type and method are saved.
   *       <li>The enclosing type is set to the anonymous class, the
   *       enclosing method is set to the anonymous class's <code>&lt;init&gt;</code>
   *       method as represented by an InitDeclaration node, and we
   *       record that we are inside of a constructor.
   *       <li>The {@link InstanceInitAction#tryBefore()} method of the 
   *       init helper is called immediately before the recursive visit is
   *       begin.
   *       <li>We recursively visit the anonymous class to visit the instance
   *       initializers. 
   *       <li>The {@link InstanceInitAction#finallyAfter() method of the
   *       init helper is called immediately after the recursive visit 
   *       ends (whether normally or with an exception).
   *       <li>We restore the original enclosing type and method, and set
   *       that we are no longer inside a constructor.
   *       <li>The {@link InstanceInitAction#afterVisit() method of the
   *       init helper is called so that the results of the recursive visit
   *       can be integrated into the results of the main visit.
   *     </ol>
   *   <li>Finally, if the bodies of type declarations are supposed to be
   *   visited, then the body of the anonymous class is visited:
   *     <ol>
   *       <li>We save the original enclosing type and method.
   *       <li>We set the enclosing type to the anonymous class and clear the
   *       enclosing method.
   *       <li>We visit the body of the anonymous class by calling 
   *       {@link #handleAnonClassAsTypeDeclaration}.
   *       <li>We restore the enclosing type and method to their original values.
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
   * <p>The default implementaton of {@link #handleAnonClassAsTypeDeclaration}
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
  
    // Prepare to recursively visit the initialization, if required
    final InstanceInitAction action = getAnonClassInitAction(expr);
    final IRNode prevEnclosingType = enclosingType;
    final IRNode prevEnclosingDecl = enclosingDecl;
    final boolean prevInsideConstructor = insideConstructor;
  
    // Should we recursively visit?
    if (action != null) {
      InstanceInitializationVisitor.processAnonClassExpression(expr, this,
          new Action() {
            public void tryBefore() {
              enclosingType = expr; // Now inside the anonymous type declaration
              enterEnclosingDecl(JavaPromise.getInitMethodOrNull(expr)); // Inside the <init> method
              insideConstructor = true; // We are inside the constructor of the anonymous class
              action.tryBefore();
            }
  
            public void finallyAfter() {
              action.finallyAfter();
              enclosingType = prevEnclosingType;
              leaveEnclosingDecl(prevEnclosingDecl);
              insideConstructor = prevInsideConstructor;
            }
          });
       action.afterVisit();
    }
    
    // Visit the type body if required
    if (visitInsideTypes) {
      try {
        enclosingType = expr;
        enclosingDecl = null; // We are not inside of any method or constructor -- see comments in visitNonAnnotationTypeDeclaration()
        insideConstructor = false;
        handleAnonClassAsTypeDeclaration(expr);
      } finally {
        enclosingType = prevEnclosingType;
        enclosingDecl = prevEnclosingDecl;
        insideConstructor = prevInsideConstructor;
      }
    }
    return null;
  }



  /**
   * Visit an anonymous class declaration.  Called by {@link #visitAnonClassExpression(IRNode)}.
   * The default implementation visits the arguments to the expression by
   * calling <code>doAccept(AnonClassExpression.getArgs(expr))</code>.
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
  }



  /**
   * Get the initialization action to use when recursively visiting the instance
   * initializers of an anonymous class expression.
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
   * <p>The default implementation returns null.
   * 
   * @param expr
   *          The anonymous class expression node.
   * 
   * @return The action to use, or <code>null</code> if the instance
   *         initializers of the anonymous class should not be visited.
   */
  protected InstanceInitAction getAnonClassInitAction(final IRNode expr) {
    return null;
  }



  /**
   * Visit an anonymous class expression as if it were a type declaration.
   * Called by {@link #visitAnonClassExpression(IRNode)} only if 
   * the bodies of types are supposed to be visited.  The default implementation
   * visits the class body of the anonymous class by calling 
   * <code>doAccept(AnonClassExpression.getBody(expr))</code>.
   * 
   * <p>By default there is no relationship between {@link #handleNonAnnotationTypeDeclaration(IRNode)}
   * and this method: if you need these visitations
   * to be similar, your subclass must take care of that itself.
   * @param expr
   */
  protected void handleAnonClassAsTypeDeclaration(final IRNode expr) {
    doAccept(AnonClassExpression.getBody(expr));
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
   */
  @Override
  public final Void visitClassDeclaration(final IRNode classDecl) {
    visitNonAnnotationTypeDeclaration(classDecl, VisitTypeAction.CLASS);
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
   * If the initializer is <code>static</code>, we
   * <ol>
   *   <li>Set the enclosing method to the
   * class initialization method of the current class.
   *   <li>Visit the contents
   * of the initializer by calling {@link #handleStaticInitializer}.
   * </ol>
   * 
   * <p>Otherwise, we do nothing: the contents of the
   * instance initializer will be recursively visited when we visit a 
   * constructor call node.
   * 
   * <p>The default implementation of {@link #handleStaticInitializer(IRNode)}
   * simply visits the children of the node.
   */
  @Override
  public final Void visitClassInitializer(final IRNode expr) {
    if (TypeUtil.isStatic(expr)) {
      enterEnclosingDecl(ClassInitDeclaration.getClassInitMethod(enclosingType));
//      enclosingDecl = ClassInitDeclaration.getClassInitMethod(enclosingType);
      try {
        handleStaticInitializer(expr);
      } finally {
        leaveEnclosingDecl(null);
//        enclosingDecl = null;
      }
    } else {
      /* XXX Should have a handleInstanceInitialzer() too, but the InstanceInitializationVisitor
       * currently bypasses this node and visits its children directly.  This is
       * necessary because of legacy considerations in the classes that use to
       * use InstanceInitVisitor.
       * 
       * if (insideConstructor) {
       *   handleInstanceInitializer(expr);
       * }
       * 
       * where we have 
       * 
       *   protected void handleStaticInitializer(final IRNode init) {
       *     doAcceptForChildren(init);
       *   }
       */
    }
    return null;
  }
  
  /**
   * Visit a static initializer.  Called by {@link #visitClassInitializer(IRNode)}.
   * The default implementation simply visits the children of the node by calling
   * <code>doAcceptForChildren(init)</code>.
   * 
   * <p>It is the responsibility of the subclass implementation to visit the 
   * children of this node (or not) as appropriate to the analysis. 
   * 
   * 
   * 
   * @param init The static initializer.
   */
  protected void handleStaticInitializer(final IRNode init) {
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
    
    // 2.  Make sure we account for the super class's field initializers, etc
    final InstanceInitAction action = getConstructorCallInitAction(expr);
    InstanceInitializationVisitor.processConstructorCall(
        expr, TypeDeclaration.getBody(enclosingType), this, action);
    action.afterVisit();
    return null;
  }

  /**
   * Called by {@link #visitConstructorCall(IRNode)} to handle the visitation of
   * the constructor call itself. This is called before the instance
   * initializers are visited.
   * 
   * <p>
   * The default implementation simply visits the children of node by calling 
   * <code>doAcceptForChildren(ccall)</code>.  In most 
   * cases a reimplementation should first visit the children of the node
   * to handle the parameters, and then process the constructor call node itself. 
   * 
   * @param ccall
   *          The constructor call node.
   */
  protected void handleConstructorCall(final IRNode ccall) {
    doAcceptForChildren(ccall);
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
    return NULL_ACTION;
  }
 
  /**
   * Visit a constructor declaration. The order of operations is
   * <ol>
   *   <li>Record that we are inside a constructor, and the identity of that constructor.
   *   <li>Visit the constructor declaration node itself by calling
   *   {@link #handleConstructorDeclaration}.
   *   <li>Record that we are no longer inside of a constructor.
   * </ol>
   * 
   * <p>The default implementation of {@link #handleConstructorDeclaration}
   * simply visits the children of the node.
   */
  @Override
  public final Void visitConstructorDeclaration(final IRNode cdecl) {
    // 1. Record that we are inside a constructor
    insideConstructor = true;
    enterEnclosingDecl(cdecl);
//    enclosingDecl = cdecl;
    try {
      // 2. Process the constructor declaration
      handleConstructorDeclaration(cdecl);
    } finally {
      // 3. Record we are no longer in a constructor
      leaveEnclosingDecl(null);
      enclosingDecl = null;
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
   */
  @Override
  public final Void visitEnumDeclaration(final IRNode enumDecl) {
    visitNonAnnotationTypeDeclaration(enumDecl, VisitTypeAction.ENUM);
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
   */
  @Override
  public final Void visitInterfaceDeclaration(final IRNode intDecl) {
    visitNonAnnotationTypeDeclaration(intDecl, VisitTypeAction.INTERFACE);
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
   * Visit a method declaration. The order of operations is
   * <ol>
   *   <li>Record that we are inside a method, and the identity of that method.
   *   <li>Visit the method declaration node itself by calling
   *   {@link #handleMethodDeclaration}.
   *   <li>Record that we are no longer inside of a method.
   * </ol>
   * 
   * <p>The default implementation of {@link #handleMethodDeclaration} simply
   * visits the children of the node.
   */
  @Override
  public final Void visitMethodDeclaration(final IRNode mdecl) {
    // 1. Record we are inside a method
    enterEnclosingDecl(mdecl);
//    enclosingDecl = mdecl;
    try {
      // 2. Visit the method declaration
      handleMethodDeclaration(mdecl);
    } finally {
      // 3. Record we are no longer inside a method
      leaveEnclosingDecl(null);
//      enclosingDecl = null;
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
   */
  @Override
  public final Void visitNestedClassDeclaration(final IRNode classDecl) {
    visitNonAnnotationTypeDeclaration(classDecl, VisitTypeAction.NESTED_CLASS);
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
   */
  @Override
  public final Void visitNestedEnumDeclaration(final IRNode enumDecl) {
    visitNonAnnotationTypeDeclaration(enumDecl, VisitTypeAction.NESTED_ENUM);
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
   */
  @Override
  public final Void visitNestedInterfaceDeclaration(final IRNode intDecl) {
    visitNonAnnotationTypeDeclaration(intDecl, VisitTypeAction.NESTED_INTERFACE);
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
   * Visit a variable declaration.  This is tricky because we could be inside a
   * local variable declaration, or inside a field declaration.
   * 
   * <p>If we are inside a field declaration, then we check to see whether
   * the visitation is currently inside a constructor or whether the field is
   * <code>static</code>.  If so, we then check whether the field has an
   * initializer at all: <code>!NoInitialization.prototype.includes(VariableDeclaration.getInit(varDecl))</code>.  If the
   * field has an initializer, we then
   * <ol>
   *   <li>If the field is <code>static</code> set the enclosing method to the class initializer of the current type.
   *   <li>Visit the declaration by calling {@link #handleFieldInitialization(IRNode, boolean)}.
   *   <li>If the field is <code>static</code> clear the enclosing method.
   * </ol>
   * 
   * <p>If we are 
   * inside a local variable declaration then the order of operations is
   * <ol>
   *   <li>Visit the declaration by calling {@link #handleLocalVariableDeclarator}.
   * </ol>
   */
  @Override
  public final Void visitVariableDeclarator(final IRNode varDecl) {
    /*
     * If this is inside a FieldDeclaration, then we only want to run if we are
     * being executed on behalf of the instance initialization or if we are part
     * of a static field declaration.
     * 
     * If this inside a DeclStatement, then we always want to run, and we don't
     * do anything special at all. (I would like to avoid having to climb up the
     * parse tree, but I don't have a choice because
     * InstanceInitializationVisitor does not call back into FieldDeclaration,
     * but into the children of FieldDeclaration.)
     * 
     * XXX: Should change InstanceInitializationVisitor to call visitFieldDeclaration,
     * but that would break things in old classes (coloring) that need it to
     * behave like the old InstanceInitVisitor.
     */
    if (FieldDeclaration.prototype.includes(
        JJNode.tree.getOperator(
            JJNode.tree.getParentOrNull(
                JJNode.tree.getParentOrNull(varDecl))))) {      
      /* Analyze the field initialization if we are inside a constructor or
       * visiting a static field.
       */
      final boolean isStaticDeclaration = TypeUtil.isStatic(varDecl);
      if (insideConstructor || isStaticDeclaration) {
        /* At this point we know we are inside a field declaration that is
         * being analyzed on behalf of a constructor or a static initializer.
         */
        final IRNode init = VariableDeclarator.getInit(varDecl);
        // Don't worry about uninitialized fields
        if (!NoInitialization.prototype.includes(JJNode.tree.getOperator(init))) {
          /* If the initialization is static, we have to update the enclosing 
           * method to the class initialization declaration. 
           */
          if (isStaticDeclaration) {
            enterEnclosingDecl(ClassInitDeclaration.getClassInitMethod(enclosingType));
//            enclosingDecl = ClassInitDeclaration.getClassInitMethod(enclosingType);
          }
          try {
            handleFieldInitialization(varDecl, isStaticDeclaration);
          } finally {
            if (isStaticDeclaration) {
              leaveEnclosingDecl(null);
              enclosingDecl = null;
            }
          }
        }
      }
    } else {
      /* Not a field declaration: so we are in a local variable declaration.
       * Always analyze its contents.
       */
      handleLocalVariableDeclaration(varDecl);
    }
    return null;
  }

  /**
   * Called by {@link #visitVariableDeclarator(IRNode)} to handle the visitation
   * of a variable declaration that is part of a FieldDeclaration. Only called
   * if the field declaration has an initializer, that is, when
   * <code>!NoInitialization.prototype.includes(VariableDeclaration.getInit(varDecl))</code>.
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
