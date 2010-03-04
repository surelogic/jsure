package com.surelogic.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.ClassInitializer;
import edu.cmu.cs.fluid.java.operator.ConstructorCall;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.NestedClassDeclaration;
import edu.cmu.cs.fluid.java.operator.NestedEnumDeclaration;
import edu.cmu.cs.fluid.java.operator.NestedInterfaceDeclaration;
import edu.cmu.cs.fluid.java.operator.SuperExpression;
import edu.cmu.cs.fluid.java.operator.VoidTreeWalkVisitor;
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
  
  
  
  // =========================================================================
  // == Helper inner classes
  // =========================================================================

  /**
   * Crawl a method body or initializer block to find all the local variable and
   * parameter declarations. {@link #doAccept(IRNode)} must only be called with
   * a MethodDeclaration, ConstructorDeclaration, InitDeclaration, or
   * ClassInitDeclaration.
   */
  private static class LocalDeclarationsVisitor extends VoidTreeWalkVisitor {
    // =========================================================================
    // == Helper inner class for the inner class!
    // =========================================================================
    
    private final class InitializationVisitor extends VoidTreeWalkVisitor {
      /**
       * The outer visitation.
       */
      private final LocalDeclarationsVisitor outer;
  
      /** 
       * Are we matching static initializers?  If <code>false</code> we match
       * instance initializers.
       */
      private final boolean isStatic; 
  
  
      
      public InitializationVisitor(
          final LocalDeclarationsVisitor fdv, final boolean matchStatic) {
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
    
    /**
     * Only instantiate internal to {@link #getDeclarationsFor(IRNode)}.
     */
    private LocalDeclarationsVisitor() {
      super();
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
      final LocalDeclarationsVisitor v = new LocalDeclarationsVisitor();
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
  
  
  

  /**
   * Crawl a method body or initializer block to find all the final local variable and
   * parameter declarations. {@link #doAccept(IRNode)} must only be called with
   * a MethodDeclaration, ConstructorDeclaration, or ClassInitializer. 
   * 
   * <p>Right now this returns too many variables, because it will return
   * those declarations that appear <em>after</em> the nested type we came
   * up from, when in reality, those declarations are not accessible to it.
   */
  private static class ExternalDeclarationsVisitor extends VoidTreeWalkVisitor {
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
   * Get the local variable and parameter declarations for a particular
   * method/constructor or initializer.
   * 
   * @param mdecl
   *          A MethodDeclaration, ConstructorDeclaration, InitDeclaration, or
   *          ClassInitDeclaration node.
   */
  public static LocalVariableDeclarations getDeclarationsFor(final IRNode mdecl) {
    /* First get the variables and parameters declared in the method.
     */
    final List<IRNode> local = LocalDeclarationsVisitor.getDeclarationsFor(mdecl);
    
    /* Then get the variables declared in outer contexts. Search upwards for
     * containing method/constructor declarations or class initializers.
     */
    final List<IRNode> external = new ArrayList<IRNode>();
    IRNode current = JJNode.tree.getParentOrNull(mdecl);
    IRNode lastTypeDecl = null;
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
    
    return new LocalVariableDeclarations(local, external);
  }
}
