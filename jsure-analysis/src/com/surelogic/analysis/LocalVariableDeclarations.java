package com.surelogic.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.surelogic.util.NullList;
import com.surelogic.analysis.visitors.JavaSemanticsVisitor;
import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.ClassInitializer;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.EnumConstantClassDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.NestedClassDeclaration;
import edu.cmu.cs.fluid.java.operator.NestedEnumDeclaration;
import edu.cmu.cs.fluid.java.operator.NestedInterfaceDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.VoidTreeWalkVisitor;
import edu.cmu.cs.fluid.java.promise.ClassInitDeclaration;
import edu.cmu.cs.fluid.java.promise.InitDeclaration;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public final class LocalVariableDeclarations {
  // =========================================================================
  // == Fields 
  // =========================================================================
  
  /**
   * Variables declared local to the method.  A list of 
   * ParameterDeclaration and VariableDeclarator nodes.
   */
  private List<IRNode> local = new ArrayList<IRNode>();

  /**
   * Variables accessible in the method, but that are declared in an outer
   * context. These are only possible if the method is part of an nested class
   * declared inside another method.  A list of 
   * ParameterDeclaration and VariableDeclarator nodes.
   */
  private List<IRNode> external = new ArrayList<IRNode>();
  
  /**
   * Receivers declared on and internal to the method's flow of control.
   * This includes the receiver of the method/constructor declaration itself,
   * plus any receivers of anonymous class expressions inside the method, and
   * those along the initialization path of those anonymous  class expressions.
   */
  private List<IRNode> receivers = new ArrayList<IRNode>();
  
  /**
   * Receivers declared on classes that enclose the flow unit.
   */
  private List<IRNode> qualifiedReceivers = new ArrayList<IRNode>();
  
  
  
  // =========================================================================
  // == Constructor (private)
  // =========================================================================

  /**
   * Private; get instances using {@link #getDeclarationsFor(IRNode)}.
   */
  private LocalVariableDeclarations(
      final List<IRNode> l, final List<IRNode> e,
      final List<IRNode> r, final List<IRNode> q) {
    local = Collections.unmodifiableList(l);
    external = Collections.unmodifiableList(e);
    receivers = Collections.unmodifiableList(r);
    qualifiedReceivers = Collections.unmodifiableList(q);
  }
  
  
  
  public List<IRNode> getLocal() {
    return local;
  }
  
  public List<IRNode> getExternal() {
    return external;
  }
  
  public List<IRNode> getReceivers() {
    return receivers;
  }
  
  public List<IRNode> getQualifiedReceivers() {
    return qualifiedReceivers;
  }
  
  public Iteratable<IRNode> getAllParameterDeclarations() {
    return new FilterIterator<IRNode, IRNode>(
        new AppendIterator<IRNode>(local.iterator(), external.iterator())) {
      @Override
      protected Object select(final IRNode o) {
        return ParameterDeclaration.prototype.includes(o) ? o : IteratorUtil.noElement;
      }
    };
  }
  
  
  
  // =========================================================================
  // == Helper inner classes
  // =========================================================================

  /**
   * Crawl a method body or initializer block to find all the local variable and
   * parameter declarations. {@link #doAccept(IRNode)} must only be called with
   * a MethodDeclaration, ConstructorDeclaration, InitDeclaration, or
   * ClassInitDeclaration.
   */
  private static final class LocalDeclarationsVisitor extends JavaSemanticsVisitor {
    private final List<IRNode> declarations;
    private final List<IRNode> receivers;

    
    /**
     * Only instantiate internal to {@link #getDeclarationsFor(IRNode)}.
     */
    private LocalDeclarationsVisitor(final IRNode mdecl,
        final List<IRNode> d, final List<IRNode> r) {
      super(false, true, mdecl);
      declarations = d;
      receivers = r;
    }
    
    
    
    /**
     * Get the local variable and parameter declarations for a particular
     * method/constructor or initializer.
     * 
     * @param mdecl
     *          A MethodDeclaration, ConstructorDeclaration, InitDeclaration, or
     *          ClassInitDeclaration node.
     */
    public static void getDeclarationsFor(
        final IRNode mdecl, final List<IRNode> d, final List<IRNode> r) {
      final LocalDeclarationsVisitor v = 
          new LocalDeclarationsVisitor(mdecl, d, r);
      v.doAccept(mdecl);
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
    protected void handleLocalVariableDeclaration(final IRNode varDecl) {
      declarations.add(varDecl);
      // Need to process the initializer, if any
      doAcceptForChildren(varDecl);
    }
    
    @Override
    public void handleConstructorDeclaration(final IRNode cdecl) {
      receivers.add(JavaPromise.getReceiverNode(cdecl));
      doAcceptForChildren(cdecl);
    }
    
    @Override
    public void handleMethodDeclaration(final IRNode mdecl) {
      if (!TypeUtil.isStatic(mdecl)) {
        receivers.add(JavaPromise.getReceiverNode(mdecl));
      }
      doAcceptForChildren(mdecl);
    }
    
    @Override
    public void handleAnonClassExpression(final IRNode expr) {
      // Add the receiver of the <init> method
      receivers.add(
          JavaPromise.getReceiverNode(JavaPromise.getInitMethod(expr)));
      super.handleAnonClassExpression(expr);
    }
    
    @Override
    public void handleEnumConstantClassDeclaration(final IRNode expr) {
      // Add the receiver of the <init> method
      receivers.add(
          JavaPromise.getReceiverNode(JavaPromise.getInitMethod(expr)));
      super.handleEnumConstantClassDeclaration(expr);
    }
  }
  
  
  

  /**
   * Crawl a method body or initializer block to find all the final local
   * variable and parameter declarations. {@link #doAccept(IRNode)} must only be
   * called with a MethodDeclaration, ConstructorDeclaration, or
   * ClassInitializer.
   * 
   * <p>
   * We correctly disregard any final variable declarations that appear
   * <em>after</em> the nested type we came up from.
   * 
   * <p>
   * Here, if we are a ClassInitializer we don't care about the contents of
   * other class initializers, because they are not in scope. This means we
   * don't have to be a {@link JavaSemanticsVisitor}.
   */
  private static final class ExternalDeclarationsVisitor extends VoidTreeWalkVisitor {
    // =========================================================================
    // == Fields
    // =========================================================================
    
    /**
     * Nested class that we came up out of, A NestedInterfaceDeclaration,
     * NestedTypeDeclaration, or AnonClassExpression.
     */
    private final IRNode cameFrom;
    
    private final List<IRNode> declarations;
 
    /**
     * Whether we still care about the declarations.  We stop processing
     * local variable declarations when we hit the class declaration that we 
     * came up out of.
     */
    private boolean addDeclaration = true;
    

    
    // =========================================================================
    // == Constructor and static factory method
    // =========================================================================
    
    /**
     * Only instantiate internal to {@link #getDeclarationsFor(IRNode)}.
     */
    private ExternalDeclarationsVisitor(final IRNode type, final List<IRNode> addTo) {
      cameFrom = type;
      declarations = addTo;
    }
    
    
    
    /**
     * Get the final local variable and parameter declarations for a particular
     * method/constructor or initializer.
     * 
     * @param mdecl
     *          A MethodDeclaration, ConstructorDeclaration, or ClassInitializer
     *          node.
     */
    public static void getDeclarationsFor(
        final IRNode mdecl, final IRNode type, final List<IRNode> addTo) {
      final ExternalDeclarationsVisitor v = new ExternalDeclarationsVisitor(type, addTo);
      v.doAccept(mdecl);
    }

    
    
    // =========================================================================
    // == Visitor methods
    // =========================================================================
    
    @Override
    public Void visitParameterDeclaration(final IRNode node) {
      /* Should use TypeUtil.isEffectivelyFinal() to filter the variables but
       * that requires setting up an instance to a flow analysis that I'm not
       * able to do right now.  So we just include all the variables.  Since
       * this list is just used to build an index for flow-control lattices, 
       * this is fine.  We just end up with a larger than necessary lattice.  
       */
//      if (TypeUtil.isJSureFinal(node)) { // TODO: Really replace with isEffectivelyFinal()
        declarations.add(node);
//      }
      return null;
    }
    
    @Override
    public Void visitVariableDeclarator(final IRNode node) {
      /* Should use TypeUtil.isEffectivelyFinal() to filter the variables but
       * that requires setting up an instance to a flow analysis that I'm not
       * able to do right now.  So we just include all the variables.  Since
       * this list is just used to build an index for flow-control lattices, 
       * this is fine.  We just end up with a larger than necessary lattice.  
       */
      if (addDeclaration) { // && TypeUtil.isJSureFinal(node)) { // TODO: Really replace with isEffectivelyFinal()
        declarations.add(node);
      }
      return null;
    }
    
    @Override
    public Void visitClassDeclaration(final IRNode node) {
      /* Keep adding declarations as long as we haven't found the class we came
       * up out of.
       */
      addDeclaration = !(node == cameFrom);
      
      /* STOP: we've encountered a class declaration.  We don't want to enter
       * the method declarations of nested class definitions.
       */
      return null;
    }
  
    @Override
    public Void visitInterfaceDeclaration(final IRNode node) {
      /* Keep adding declarations as long as we haven't found the class we came
       * up out of.
       */
      addDeclaration = !(node == cameFrom);
      
      /* STOP: we've encountered a class declaration.  We don't want to enter
       * the method declarations of nested class definitions.
       */
      return null;
    }
  
    @Override
    public Void visitEnumDeclaration(final IRNode node) {
      /* Keep adding declarations as long as we haven't found the class we came
       * up out of.
       */
      addDeclaration = !(node == cameFrom);
      
      /* STOP: we've encountered a class declaration.  We don't want to enter
       * the method declarations of nested class definitions.
       */
      return null;
    }
  
    @Override
    public Void visitAnonClassExpression(final IRNode node) {
      /* Keep adding declarations as long as we haven't found the class we came
       * up out of.
       */
      addDeclaration = !(node == cameFrom);
      
      /* STOP: we've encountered a class declaration.  We don't want to enter
       * the method declarations of nested class definitions.
       */
      return null;
    }
    
    @Override
    public Void visitEnumConstantClassDeclaration(final IRNode node) {
      /* Keep adding declarations as long as we haven't found the class we came
       * up out of.
       */
      addDeclaration = !(node == cameFrom);
      
      /* STOP: we've encountered a class declaration.  We don't want to enter
       * the method declarations of nested class definitions.
       */
      return null;
    }
  }
  
  
  
  // =========================================================================
  // == Methods
  // =========================================================================
 
  /**
   * Does the parameter/variable declaration have a reference type?
   * @param binder The binder
   * @param decl a ParamterDeclaratin or VariableDeclarator node.
   * @return {@code true} if the declaration has a reference (object) type.
   */
  public static boolean hasReferenceType(final IBinder binder, final IRNode decl) {
    return binder.getJavaType(decl) instanceof IJavaReferenceType;
  }
  
  /**
   * Separates reference from non-reference typed declarations into two lists.
   * 
   * @param binder
   *          The binder
   * @param decls
   *          The input list of declarations. This list is unchanged by this
   *          method.
   * @param refs
   *          The output list of declarations with reference types. This list is
   *          modified by use of the {@link List#add()} method by this method.
   * @param prims
   *          The output list of declarations with primitive types. This list is
   *          modified by use of the {@link List#add()} method by this method.
   */
  public static void separateDeclarations(
      final IBinder binder, final List<IRNode> decls,
      final List<IRNode> refs, final List<IRNode> prims) {
    for (final IRNode decl : decls) {
      if (hasReferenceType(binder, decl)) {
        refs.add(decl);
      } else {
        prims.add(decl);
      }
    }
  }
  
  /**
   * Get the variable and parameter declarations for a particular
   * method/constructor or initializer.
   * 
   * @param mdecl
   *          A MethodDeclaration, ConstructorDeclaration, InitDeclaration, or
   *          ClassInitDeclaration node.
   */
  public static LocalVariableDeclarations getDeclarationsFor(final IRNode mdecl) {
    final List<IRNode> locals = new ArrayList<IRNode>(10);
    final List<IRNode> receivers = new ArrayList<IRNode>(5);
    LocalDeclarationsVisitor.getDeclarationsFor(mdecl, locals, receivers);
    
    final List<IRNode> externals;
    final List<IRNode> qualifiedReceivers;
    if (ClassInitDeclaration.prototype.includes(mdecl)) {
      externals = Collections.emptyList();
      qualifiedReceivers = Collections.emptyList();
    } else {
      externals = new ArrayList<IRNode>(5);
      qualifiedReceivers = new ArrayList<IRNode>(5);
      getExternallyDeclaredVariables(mdecl, externals, qualifiedReceivers);
    }    
    return new LocalVariableDeclarations(locals, externals, receivers, qualifiedReceivers);
  }
  
  /**
   * Get the externally declared variables visible for a particular
   * method/constructor or instance initializer.
   * 
   * @param mdecl
   *          A MethodDeclaration, ConstructorDeclaration, or InitDeclaration
   *          node.
   */
  public static void getExternallyDeclaredVariables(final IRNode mdecl,
      final List<IRNode> externals, final List<IRNode> qualifiedReceivers) {
    IRNode current = InitDeclaration.prototype.includes(mdecl) ?
        JavaPromise.getPromisedFor(mdecl) : JJNode.tree.getParentOrNull(mdecl);
    IRNode lastTypeDecl = null;
    /* Search upwards for containing method/constructor declarations or class
     * initializers.
     */
    while (current != null) {
      final Operator op = JJNode.tree.getOperator(current);
      if (MethodDeclaration.prototype.includes(op)) {
        ExternalDeclarationsVisitor.getDeclarationsFor(current, lastTypeDecl, externals);
      } else if (ConstructorDeclaration.prototype.includes(op)) {
        ExternalDeclarationsVisitor.getDeclarationsFor(current, lastTypeDecl, externals);
      } else if (ClassInitializer.prototype.includes(op)) {
        ExternalDeclarationsVisitor.getDeclarationsFor(current, lastTypeDecl, externals);
      } else if (NestedClassDeclaration.prototype.includes(op)) {
        lastTypeDecl = current;
      } else if (NestedInterfaceDeclaration.prototype.includes(op)) {
        lastTypeDecl = current;
      } else if (NestedEnumDeclaration.prototype.includes(op)) {
        lastTypeDecl = current;
      } else if (AnonClassExpression.prototype.includes(op)) {
        lastTypeDecl = current;
      } else if (EnumConstantClassDeclaration.prototype.includes(op)) {
        lastTypeDecl = current;
      }
      current = JJNode.tree.getParentOrNull(current); 
    }
  }
  
  public static List<IRNode> getExternallyDeclaredVariables(final IRNode mdecl) {
    final List<IRNode> result = new ArrayList<IRNode>(10);
    getExternallyDeclaredVariables(mdecl, result, NullList.<IRNode>prototype());
    return Collections.unmodifiableList(result);
  }  
}
