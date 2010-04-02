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
import edu.cmu.cs.fluid.java.operator.ClassBody;
import edu.cmu.cs.fluid.java.operator.ClassInitializer;
import edu.cmu.cs.fluid.java.operator.ConstructorCall;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.NestedClassDeclaration;
import edu.cmu.cs.fluid.java.operator.NestedEnumDeclaration;
import edu.cmu.cs.fluid.java.operator.NestedInterfaceDeclaration;
import edu.cmu.cs.fluid.java.operator.SuperExpression;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.java.operator.VoidTreeWalkVisitor;
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
    // == Helper method
    // =========================================================================

    private enum WhichMembers {
      STATIC {
        @Override
        protected boolean acceptsModifier(final boolean isStatic) {
          return isStatic;
        }
      },
      INSTANCE {
        @Override
        protected boolean acceptsModifier(final boolean isStatic) {
          return !isStatic;
        }
      };
      
      protected abstract boolean acceptsModifier(boolean isStatic);
      
      public final boolean acceptsMember(final IRNode bodyDecl) {
        return acceptsModifier(JavaNode.getModifier(bodyDecl, JavaNode.STATIC));
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
        final Operator op = JJNode.tree.getOperator(bodyDecl);
        if (FieldDeclaration.prototype.includes(op) ||
            ClassInitializer.prototype.includes(op)) {
          if (which.acceptsMember(bodyDecl)) {
            this.doAcceptForChildren(bodyDecl);
          }
        }       
      }
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
      // Add the declaration if it is a local variable and not a field
      if (!FieldDeclaration.prototype.includes(
          JJNode.tree.getOperator(
              JJNode.tree.getParentOrNull(
                  JJNode.tree.getParentOrNull(node))))) {
        declarations.add(node);
      }
      
      // Need to process the initializer, if any
      doAcceptForChildren(node);
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
      /* Visit the argument list because we may have embedded anonymous classes
       * there too.
       */
      doAcceptForChildren(AnonClassExpression.getArgs(node));
      
      /* Need to visit the instance initializer blocks and instance field 
       * declarations.  We rely on the fact that anonymous classes cannot 
       * have static field members or static initializer blocks.  We ignore
       * method and constructor declarations.  I know this seems odd, but
       * see Bug 1662.  Technically the initializer blocks of the anonymous
       * class expression are executed as part of the enclosing method/constructor. 
       */
      processClassBody(AnonClassExpression.getBody(node), WhichMembers.INSTANCE);
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
      final IRNode classDecl = JavaPromise.getPromisedFor(node);
      final IRNode classBody =
        AnonClassExpression.prototype.includes(classDecl) ?
            AnonClassExpression.getBody(classDecl) :
              TypeDeclaration.getBody(classDecl);
      processClassBody(classBody, WhichMembers.STATIC);
      return null;
    }
    
    @Override
    public Void visitInitDeclaration(final IRNode node) {
      /*
       * The intent is that we only get here when we are looking at the instance
       * initializer of an anonymous class expression. But, UniqueAnalysis is
       * messed up and confused and can get us here for regular classes too.
       * Need to check for that.
       */
      /* XXX: Hopefully, this will be fixed when
       * UniquenessAnalysis is replaced.
       */
      
      final IRNode classDecl = JavaPromise.getPromisedFor(node);
      final IRNode classBody;
      // Again, this really should be the only case
      if (AnonClassExpression.prototype.includes(classDecl)) {
        classBody = AnonClassExpression.getBody(classDecl);
      } else {
        classBody = TypeDeclaration.getBody(classDecl);
      }
      processClassBody(classBody, WhichMembers.INSTANCE);
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
      // Process the argument list in case it contains anonymous classes
      doAcceptForChildren(ConstructorCall.getArgs(node));
      
      final IRNode conObject = ConstructorCall.getObject(node);
      final Operator conObjectOp = JJNode.tree.getOperator(conObject);
      if (SuperExpression.prototype.includes(conObjectOp)) {
        // Visit the initializers.
        processClassBody(JJNode.tree.getParent(currentConstructor), WhichMembers.INSTANCE);
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
    IRNode current = InitDeclaration.prototype.includes(mdecl) ? JavaPromise.getPromisedFor(mdecl) : JJNode.tree.getParentOrNull(mdecl);
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
