package com.surelogic.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.ClassInitializer;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
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
import edu.cmu.cs.fluid.util.AppendIterator;
import edu.cmu.cs.fluid.util.FilterIterator;
import edu.cmu.cs.fluid.util.Iteratable;
import edu.cmu.cs.fluid.util.IteratorUtil;

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
  
  
  
  // =========================================================================
  // == Constructor (private)
  // =========================================================================

  /**
   * Private; get instances using {@link #getDeclarationsFor(IRNode)}.
   */
  private LocalVariableDeclarations(final List<IRNode> l, final List<IRNode> e) {
    local = Collections.unmodifiableList(l);
    external = Collections.unmodifiableList(e);
  }
  
  
  
  public List<IRNode> getLocal() {
    return local;
  }
  
  public List<IRNode> getExternal() {
    return external;
  }
  
  public Iteratable<IRNode> getAllDeclarations() {
    return new AppendIterator<IRNode>(local.iterator(), external.iterator());
  }
  
  public Iteratable<IRNode> getAllParameterDeclarations() {
    return new FilterIterator<IRNode, IRNode>(getAllDeclarations()) {
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
    private final List<IRNode> declarations = new LinkedList<IRNode>();
 

    
    /**
     * Only instantiate internal to {@link #getDeclarationsFor(IRNode)}.
     */
    private LocalDeclarationsVisitor(final IRNode mdecl) {
      super(false, mdecl);
    }
    
    
    
    /**
     * Get the local variable and parameter declarations for a particular
     * method/constructor or initializer.
     * 
     * @param mdecl
     *          A MethodDeclaration, ConstructorDeclaration, InitDeclaration, or
     *          ClassInitDeclaration node.
     */
    public static List<IRNode> getDeclarationsFor(final IRNode mdecl) {
      final LocalDeclarationsVisitor v = new LocalDeclarationsVisitor(mdecl);
      v.doAccept(mdecl);
      return v.declarations;
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
      if (TypeUtil.isFinal(node)) {
        declarations.add(node);
      }
      return null;
    }
    
    @Override
    public Void visitVariableDeclarator(final IRNode node) {
      if (addDeclaration && TypeUtil.isFinal(node)) {
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
    final List<IRNode> local = LocalDeclarationsVisitor.getDeclarationsFor(mdecl);
    final List<IRNode> external;
    if (ClassInitDeclaration.prototype.includes(mdecl)) {
      external = Collections.emptyList();
    } else {
      external = getExternallyDeclaredVariables(mdecl);
    }    
    return new LocalVariableDeclarations(local, external);
  }
  
  /**
   * Get the externally declared variables visible for a particular
   * method/constructor or instance initializer.
   * 
   * @param mdecl
   *          A MethodDeclaration, ConstructorDeclaration, or InitDeclaration
   *          node.
   */
  public static List<IRNode> getExternallyDeclaredVariables(final IRNode mdecl) {
    final List<IRNode> external = new ArrayList<IRNode>();
    IRNode current = InitDeclaration.prototype.includes(mdecl) ?
        JavaPromise.getPromisedFor(mdecl) : JJNode.tree.getParentOrNull(mdecl);
    IRNode lastTypeDecl = null;
    /* Search upwards for containing method/constructor declarations or class
     * initializers.
     */
    while (current != null) {
      final Operator op = JJNode.tree.getOperator(current);
      if (MethodDeclaration.prototype.includes(op)) {
        ExternalDeclarationsVisitor.getDeclarationsFor(current, lastTypeDecl, external);
      } else if (ConstructorDeclaration.prototype.includes(op)) {
        ExternalDeclarationsVisitor.getDeclarationsFor(current, lastTypeDecl, external);
      } else if (ClassInitializer.prototype.includes(op)) {
        ExternalDeclarationsVisitor.getDeclarationsFor(current, lastTypeDecl, external);
      } else if (NestedClassDeclaration.prototype.includes(op)) {
        lastTypeDecl = current;
      } else if (NestedInterfaceDeclaration.prototype.includes(op)) {
        lastTypeDecl = current;
      } else if (NestedEnumDeclaration.prototype.includes(op)) {
        lastTypeDecl = current;
      } else if (AnonClassExpression.prototype.includes(op)) {
        lastTypeDecl = current;
      }
      current = JJNode.tree.getParentOrNull(current); 
    }
    return external;
  }
}
