// $Header: /var/cvs/fluid/code/fluid/java/bind/TypeUtil.java,v 1.4 2002/07/25
// 15:14:57 chance Exp $
package edu.cmu.cs.fluid.java.util;

import java.util.logging.Logger;

import com.surelogic.analysis.assigned.DefiniteAssignment.ProvablyUnassignedQuery;
import com.surelogic.analysis.visitors.JavaSemanticsVisitor;
import com.surelogic.annotation.rules.*;
import com.surelogic.common.SLUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.ir.drops.VouchFieldIsPromiseDrop;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.CommonStrings;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaOperator;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaCaptureType;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaIntersectionType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;
import edu.cmu.cs.fluid.java.bind.IJavaWildcardType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.ClassInitDeclaration;
import edu.cmu.cs.fluid.java.promise.InitDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.version.VersionedSlotFactory;

public class TypeUtil implements JavaGlobals {
  // No instances please
  private TypeUtil() {
    super();
  }
  
  /**
	 * Logger for this class
	 */
  static final Logger LOG = SLLogger.getLogger("FLUID.java.util");

  @SuppressWarnings("unchecked")
  public static <T> SlotInfo<T> getVersionedSlotInfo(
    String slotName,
    IRType<T> ty,
    T val) {
    try {
      LOG.fine("Allocating slotinfo " + slotName); // , new Throwable());
      return VersionedSlotFactory.makeSlotInfo(slotName, ty, val);
    } catch (SlotAlreadyRegisteredException e) {
      LOG.warning("SlotInfo '" + slotName + "' requested for creation again.");
      try {
        return (SlotInfo<T>) SlotInfo.findSlotInfo(slotName);
      } catch (SlotNotRegisteredException e2) {
        throw new FluidError("Inconsistency in registering slot info's!");
      }
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> SlotInfo<T> getSimpleSlotInfo(
    String slotName,
    IRType<T> ty,
    T val) {
    try {
      LOG.fine("Allocating slotinfo " + slotName); // , new Throwable());
      return SimpleSlotFactory.prototype.newAttribute(slotName, ty, val);
    } catch (SlotAlreadyRegisteredException e) {
      LOG.warning("SlotInfo '" + slotName + "' requested for creation again.");
      try {
        return (SlotInfo<T>) SlotInfo.findSlotInfo(slotName);
      } catch (SlotNotRegisteredException e2) {
        throw new FluidError("Inconsistency in registering slot info's!");
      }
    }
  }

  /// typeToString
  public static String typeToString(IRNode binding) {
    Operator op = jtree.getOperator(binding);
    if (op instanceof TypeDeclInterface) {
      return JavaNames.getFullTypeName(binding);
    } else if (op instanceof ArrayDeclaration) {
      return typeToString(ArrayDeclaration.getBase(binding))
        + "["
        + ArrayDeclaration.getDims(binding)
        + "]";
    } else if (op instanceof PrimitiveType) {
      // return "<"+op.class.getName()+">";
      return "<" + op + ">";
    } else
      throw new FluidError("Unexpected " + op);
  }

  /// copyType (from a binding)
  public static IRNode copyType(IBinder b, IRNode binding) {
    Operator op = jtree.getOperator(binding);
    if (op instanceof TypeDeclInterface) {
      String qname = JavaNames.getFullTypeName(binding);
      qname = CommonStrings.pool(qname);
      return NamedType.createNode(qname);
    } else if (op instanceof ArrayDeclaration) {
      return ArrayType.createNode(
        copyType(b, ArrayDeclaration.getBase(binding)),
        ArrayDeclaration.getDims(binding));
    } else if (op instanceof PrimitiveType) {
      return ((JavaOperator) op).jjtCreate();
    } else
      throw new FluidError("Unexpected " + op);
  }

  /// copyTypeNodes (from existing syntax)
  public static IRNode copyTypeNodes(IRNode x) {
    return JJNode.copyTree(x); // FIX include checks?
    /*
		 * JavaOperator op = (JavaOperator) jtree.getOperator(x); if (op instanceof
		 * NamedType) { String name = NamedType.getType(x); return
		 * NamedType.createNode(name); } else if (op instanceof ArrayType) { IRNode
		 * base = ArrayType.getBase(x); int dims = ArrayType.getDims(x); return
		 * ArrayType.createNode(copyTypeNodes(base), dims); } else if (op
		 * instanceof PrimitiveType) { return op.jjtCreate(); } else throw new
		 * FluidError("Got unknown type node : "+op);
		 */
  }

  /// other stuff

  /**
   * Is the given node a type declaration?  True if the node is a 
   * ClassDeclaration, NestedClassDeclaration, InterfaceDeclaration
   * NestedInterfaceDeclaration, EnumDeclaration, NestedEnumDeclaration
   * AnonClassExpression, or EnumConstantClassDeclaration.
   */
  public static boolean isTypeDecl(final IRNode node) {
    final Operator op = JJNode.tree.getOperator(node);
    return ClassDeclaration.prototype.includes(op)
        || NestedClassDeclaration.prototype.includes(op)
        || InterfaceDeclaration.prototype.includes(op)
        || NestedInterfaceDeclaration.prototype.includes(op)
        || EnumDeclaration.prototype.includes(op)
        || NestedEnumDeclaration.prototype.includes(op)
        || AnonClassExpression.prototype.includes(op)
        || EnumConstantClassDeclaration.prototype.includes(op);
  }
  
  /**
   * Is some member declared to be <code>static</code>.  Works for 
   * MethodDeclaration, VariableDeclarator, FieldDeclaration, ClassInitializer,
   * NewRegionDeclaration, ConstructorDeclaration (always false),
   * InitDeclaration (always false), ClassInitDeclaration (always true).
   * @param node Node to test
   * @return whether the entity is <code>static</code>
   * @exception IllegalArgumentException If the node does not have one of the
   * above operator types.
   */
  public static boolean isStatic(final IRNode node) {
    final Operator op = JJNode.tree.getOperator(node);
    if (VariableDeclarator.prototype.includes(op)) {
      if (isInterface(VisitUtil.getEnclosingType(node))) {
        // Fields in interfaces are always static
        return true;
      } else {
        final IRNode gp = JJNode.tree.getParent(JJNode.tree.getParent(node));
        if (FieldDeclaration.prototype.includes(JJNode.tree.getOperator(gp))) {
          return JavaNode.getModifier(gp, JavaNode.STATIC);
        } else {
          // its a local variable
          return false;
        }
      }
    } else if (FieldDeclaration.prototype.includes(op)
        || NestedClassDeclaration.prototype.includes(op)) {
      if (isInterface(VisitUtil.getEnclosingType(node))) {
        // Fields and class declarations in interfaces are always static
        return true;
      } else {
        return JavaNode.getModifier(node, JavaNode.STATIC);
      }
    } else if (MethodDeclaration.prototype.includes(op)
        || ClassInitializer.prototype.includes(op)){ 
    	// The AST representation of an instance or static initializer block        
        return JavaNode.getModifier(node, JavaNode.STATIC);
    } else if (ClassDeclaration.prototype.includes(op)) {
      return false;
    } else if (NestedInterfaceDeclaration.prototype.includes(op) || NestedEnumDeclaration.prototype.includes(op) ||
    		   NestedAnnotationDeclaration.prototype.includes(op)) { // MUST test before Interface/EnumDeclaration
      return true; // Nested interfaces are always implicitly static
    } else if (InterfaceDeclaration.prototype.includes(op)) {
      return false;
    } else if (AnonClassExpression.prototype.includes(op)) {
      return false; // Anonymous classes are never static
    } else if (ConstructorDeclaration.prototype.includes(op)) {
      // Sort of bogus, but true none-the-less
      return false;
    } else if (InitDeclaration.prototype.includes(op)) { // The PROMISE representation of the "method" that executes the instance init blocks
      // Special Instance initializer declaration is NOT static
      return false;
    } else if (ClassInitDeclaration.prototype.includes(op)) { // The PROMISE representation of the "method" that executes the static init blocks
      return true;
    } else if (EnumConstantDeclaration.prototype.includes(op)) {
      return true;
    } else if (AnnotationElement.prototype.includes(op)) {
      return false;
    } else if (EnumDeclaration.prototype.includes(op)) {
      return false;
    } else if (LambdaExpression.prototype.includes(op)) {
      return false; // Just like ACEs
    } else {
      throw new IllegalArgumentException(
        "Can't test if " + op.name() + " is static.");
    }
  }
  
  /**
   * Is the local variable final or effectively final as descrived in
   * JLS8 &sect;4.12.4?
   * 
   * <p>N.B. There is no point in using this method if you know the 
   * VariableDeclarator is from a FieldDeclaration.
   * 
   * @param varDecl A VariableDeclarator or ParameterDeclaration node
   */
  public static boolean isFinalOrEffectivelyFinal(
      final IRNode varDecl, final IBinder binder,
      final ProvablyUnassignedQuery query) {
    return isJSureFinal(varDecl) || isEffectivelyFinal(varDecl, binder, query);
  }
  
  /**
   * Is the local variable effectively final as described in JLS8 &sect;4.12.4?
   * We don't check that the variable isn't declared <code>final</code> because
   * we assume this method is only called when we already know the field is
   * non-<code>final</code>. 
   * 
   * <p>N.B. There is no point in using this method if you know the 
   * VariableDeclarator is from a FieldDeclaration.
   * 
   * @param varDecl A VariableDeclarator or ParameterDeclaration node
   */
  public static boolean isEffectivelyFinal(final IRNode varDecl, final IBinder binder, final ProvablyUnassignedQuery query) {
    abstract class Visitor extends JavaSemanticsVisitor {
      private boolean isEffectivelyFinal = true;

      public Visitor(final IRNode blockStmt) {
        super(VisitInsideTypes.NO, SkipAnnotations.NO, PromiseUtil.getEnclosingMethod(blockStmt));
      }
      
      @Override
      public final Void visitCrementExpression(final IRNode expr) {
        @SuppressWarnings("static-access") // Index getOp by CrementExpression for clarity
        final IRNode operand = CrementExpression.getOp(expr);
        if (VariableUseExpression.prototype.includes(operand)) {
          final IRNode bindsTo = binder.getBinding(operand);
          if (bindsTo.equals(varDecl)) isEffectivelyFinal &= false;
        }
        return null;
      }
      
      @Override
      public final Void visitAssignmentExpression(final IRNode expr) {
        final IRNode lhs = AssignmentExpression.getOp1(expr);
        if (VariableUseExpression.prototype.includes(lhs)) {
          final IRNode bindsTo = binder.getBinding(lhs);
          if (bindsTo.equals(varDecl)) {
            final boolean f = processAssignment(expr);
            isEffectivelyFinal &= f;
          }
        }
        return null;
      }
      
      protected abstract boolean processAssignment(IRNode assignExpr);
      
      public final boolean isEffectivelyFinal() {
        return isEffectivelyFinal;
      }
    }

    
    
    final class InitializedVisitor extends Visitor {
      public InitializedVisitor(final IRNode blockStmt) {
        super(blockStmt);
      }

      @Override
      protected boolean processAssignment(final IRNode assignExpr) {
        return false;
      }
    }

    final class UninitializedVisitor extends Visitor {
      public UninitializedVisitor(final IRNode blockStmt) {
        super(blockStmt);
      }

      @Override
      protected boolean processAssignment(final IRNode assignExpr) {
        /* Assignment is allowed as long as the variable is provably
         * unassigned.
         */
        final IRNode rhs = AssignmentExpression.getOp2(assignExpr);
        return query.getResultFor(rhs).isProvableUnassigned(varDecl); 
      }
    }

    
    if (JavaNode.getModifier(varDecl, JavaNode.FINAL)) {
      /* Variables that are explicitly final are not effectively final
       */
      return false;
    } else {
      final IRNode blockStmt;
      final Visitor v;
      if (VariableDeclarator.prototype.includes(varDecl)) {
        blockStmt = VisitUtil.getEnclosingBlockStatement(varDecl);
        if (NoInitialization.prototype.includes(VariableDeclarator.getInit(varDecl))) {
          v = new UninitializedVisitor(blockStmt);
        } else {
          v = new InitializedVisitor(blockStmt);
        }
      } else { // ParameterDeclaration
        final IRNode parent = JJNode.tree.getParent(varDecl);
        if (CatchClause.prototype.includes(parent)) {
          blockStmt = CatchClause.getBody(parent);
          v = new InitializedVisitor(blockStmt);
        } else { // Method or Constructor parameter
          final IRNode body = 
              SomeFunctionDeclaration.getBody(JJNode.tree.getParent(parent));
          if (MethodBody.prototype.includes(body)) {
            blockStmt = MethodBody.getBlock(body);
            v = new InitializedVisitor(blockStmt);
          } else { // Method/Constructor has no body, so parameter is not modified!
            return true;
          }
        }
      }
      v.doAccept(blockStmt);
      return v.isEffectivelyFinal();
    }
  }
  
  /**
   * Is the field or variable final according to the Java semantics, or
   * because we have a <code>@Vouch("final")</code> annotation on the field?
   * The fields/variables this is true for are a superset of those for which
   * {@link #isJavaFinal} is <code>true</code>.
   * 
   * @param node A FieldDeclaration, VariableDeclarato,r or ParameterDeclaration node
   */
  public static boolean isJSureFinal(final IRNode node) {
    if (isJavaFinal(node)) {
      return true;
    } else {
      final VouchFieldIsPromiseDrop vouchFinal = LockRules.getVouchFieldIs(node);
      return vouchFinal != null && vouchFinal.isFinal();
    }
  }
  
  /**
   * Is a field or variable implicitly or explicitly <code>final</code>, as
   * described in JLS8 &sect;4.14.4? A field or variable is final if it is
   * declared to be <code>final</code>. &ldquo;Three kinds of variable are
   * implicitly declared <code>final</code>: a field of an interface
   * (&sect;9.3), a local variable which is a resource of a <code>try</code>
   * -with-resources statement (&sect;14.20.3), and an exception parameter of a
   * multi-<code>catch</code> clause (&sect;14.20)
   * 
   * @param node A FieldDeclaration, VariableDeclarato,r or ParameterDeclaration node
   */  
  public static boolean isJavaFinal(final IRNode node) {
    final Operator op = JJNode.tree.getOperator(node);
    if (VariableDeclarator.prototype.includes(op)) {
      final IRNode parent  = JJNode.tree.getParent(node);
      if (VariableResource.prototype.includes(JJNode.tree.getOperator(parent))) {
        return true; // RESOURCES ARE ALWAYS IMPLICITLY FINAL
      }
      final IRNode gparent = JJNode.tree.getParent(parent);
      if (DeclStatement.prototype.includes(JJNode.tree.getOperator(gparent))) {
        return JavaNode.getModifier(gparent, JavaNode.FINAL);
      } else {
        return isFieldDeclarationFinal(gparent);
      }
    } else if (FieldDeclaration.prototype.includes(op)) {
      return isFieldDeclarationFinal(node);
    } else if (ParameterDeclaration.prototype.includes(op)) {
      final IRNode parent  = JJNode.tree.getParent(node);
      if (CatchClause.prototype.includes(JJNode.tree.getOperator(parent))) {
        final IRNode typeNode = ParameterDeclaration.getType(node);
        if (UnionType.prototype.includes(typeNode)) {
          return true; // MULTI-CATCH CLAUSE IS ALWAYS FINAL
        }
      }
      return JavaNode.getModifier(node, JavaNode.FINAL);
    }
    return false;
  }
  
  /**
   * A Field Declarations declares final fields if the declaration
   * is explicitly <code>final</code> or if the field declaration is part
   * of an interface.
   */
  private static boolean isFieldDeclarationFinal(final IRNode node) {
    if (TypeUtil.isInterface(VisitUtil.getEnclosingType(node))) {
      return true; // IMPLICITLY FINAL
    } else {
      return JavaNode.getModifier(node, JavaNode.FINAL);
    }
  }
 
  
  
  /**
   * Is the given VariableDeclarator volatile?
   */
  public static boolean isVolatile(final IRNode node) {
    return JavaNode.getModifier(
        JJNode.tree.getParent(JJNode.tree.getParent(node)),
        JavaNode.VOLATILE);
  }
  
  /**
   * Is the given type abstract?
   */
  public static boolean isAbstract(final IRNode tdecl) {
    return JavaNode.getModifier(tdecl, JavaNode.ABSTRACT);
  }
  
  /**
   * Is the given node an Interface declaration?  This includes 
   * InterfaceDeclaration and NestedInterfaceDeclaration.
   */
  public static boolean isInterface(final IRNode node) {
    final Operator op = JJNode.tree.getOperator(node);
    return InterfaceDeclaration.prototype.includes(op)
        || NestedInterfaceDeclaration.prototype.includes(op) 
        || AnnotationDeclaration.prototype.includes(op);
  }
  
  /**
   * True for outer (top-level) class and interface declaration nodes.
   * @param tdecl type declaration node
   * @return true if defined at top-level
   */
  public static boolean isOuter(final IRNode tdecl) {
    return TypeDeclarations.prototype.includes(jtree.getParent(tdecl));
  }
  
  /**
   * True for nested types, those declared within the body of another type.
   */
  public static boolean isNested(final IRNode tdecl) {
    return !isOuter(tdecl);
  }
  
  /**
   * True for member types, those directly enclosed in another class or interface
   * declaration.
   */
  public static boolean isMember(final IRNode tdecl) {
    return ClassBody.prototype.includes(jtree.getParent(tdecl));
  }
  
  /**
   * True for anonymous classes. 
   */
  public static boolean isAnonymousClass(final IRNode tdecl) {
    return AnonClassExpression.prototype.includes(tdecl);
  }
  
  /**
   * True for inner classes: non-static nested classes.
   */
  public static boolean isInner(final IRNode tdecl) {
    return isNested(tdecl) && !isStatic(tdecl);
  }
  
  /**
   * True for local classes: non-member nested classes.
   */
  public static boolean isLocal(final IRNode tdecl) {
    return isNested(tdecl) && !isMember(tdecl);
  }
  
  /**
   * True for inner clases
   * @param type
   * @return
   */
  
	public static String printType(IRNode type) {
		Operator op = JJNode.tree.getOperator(type);
		if (ClassDeclaration.prototype.includes(op) ||
				InterfaceDeclaration.prototype.includes(op)) {
			return JJNode.getInfoOrNull(type);
		}
		return DebugUnparser.toString(type);
	}
  
  /**
   * Compute the fully qualified name for the type declaration.
   * This method should only be called on <i>outer</i> classes.
   * Other classes must be referred to using 
   * {@link edu.cmu.cs.fluid.java.operator.TypeRef}
   * @param type declaration node.
   * @return fully qualified name of type.
   */
  public static String getQualifiedName(IRNode type) {
    String pstr = getPackage(type);
    String name = TypeDeclaration.getId(type);
    if (pstr.equals("")) return name;
    return pstr+"."+name;
  }
  
  /**
   * Return the string for the current package, or return "" for the default package.
   * @param type a type declaration node
   * @return string for package
   */
  public static String getPackage(IRNode type) {
    IRNode cu  = VisitUtil.getEnclosingCompilationUnit(type);
    if (cu == null) {
      LOG.severe("Couldn't get comp unit for "+DebugUnparser.toString(type));
      return "";
    }
    IRNode pkg = CompilationUnit.getPkg(cu);
    if (JJNode.tree.getOperator(pkg) instanceof NamedPackageDeclaration) {
      return NamedPackageDeclaration.getId(pkg);
    }
    // LOG.fine("package decl is " + DebugUnparser.toString(pkg));
    return "";
  }
  
  /**
   * Does the given expression "occur in a static context" as defined in JLS3
   * &sect;8.1.3?
   * 
   * <p>
   * A statement or expression occurs in a static context if and only if the
   * innermost method, constructor, instance initializer, static initializer,
   * field initializer, or explicit constructor invocation statement enclosing
   * the statement or expression is a static method, a static initializer, the
   * variable initializer of a static variable, or an explicit constructor
   * invocation statement.
   * 
   * @param expr
   *          The node of the expression to test.
   * @return <code>true</code> iff the expression occurs in a static context.
   * @exception IllegalArgumentException
   *              Thrown if no enclosing context for the given node can be
   *              found.
   */
  public static boolean occursInAStaticContext(final IRNode expr) {
    IRNode exprToTest = expr;
    /* Avoid null errors by stopping search if we hit to root of the tree.  This
     * shouldn't happen if we are given a node of an expression.
     */
    while (exprToTest != null) {
      final Operator op = JJNode.tree.getOperator(exprToTest);
      if (MethodDeclaration.prototype.includes(op)) {
        // Context is static if the method is static
        return JavaNode.getModifier(exprToTest, JavaNode.STATIC);
      } else if (ConstructorDeclaration.prototype.includes(op)) {
        // Context is not static
        return false;
      } else if (ClassInitializer.prototype.includes(op)) { // The AST representation of an instance or static initializer block
        // Context is static if this is a static init block
        return JavaNode.getModifier(exprToTest, JavaNode.STATIC);        
      } else if (FieldDeclaration.prototype.includes(op)) {
        // Context is static if the field is static
        return JavaNode.getModifier(exprToTest, JavaNode.STATIC);
      } else if (EnumConstantDeclaration.prototype.includes(op)) {
        // Enumeration constant declarations are like static field declarations
        return true;
      } else if (ConstructorCall.prototype.includes(op)) {
        // Context is static
        return true;
      }
      exprToTest = JJNode.tree.getParentOrNull(exprToTest);
    }
    throw new IllegalArgumentException("Couldn't find context for given expression");
  }

  /**
   * Is the given node part of a compiled binary?
   */
  public static boolean isBinary(IRNode n) {
    final IRNode cu = VisitUtil.getEnclosingCUorHere(n);
    if (cu == null) {
    	Operator op = JJNode.tree.getOperator(n);
    	LOG.warning("Couldn't find CU for "+n+" -- "+op);
    	LOG.warning("Unparse: "+DebugUnparser.toString(n));    
    	throw new IllegalStateException();
    }
    return JavaNode.getModifier(cu, JavaNode.AS_BINARY);
  }

  
  
  public static final class UpperBoundGetter {
    private final ITypeEnvironment typeEnv;
    private final IJavaType javaLangObject;
    
    
    public UpperBoundGetter(final ITypeEnvironment te) {
      typeEnv = te;
      javaLangObject = typeEnv.findJavaTypeByName(SLUtility.JAVA_LANG_OBJECT);
    }
    
    
    
    public IJavaType getUpperBound(final IJavaType type) {
      if (type instanceof IJavaTypeFormal) {
        return getUpperBound(((IJavaTypeFormal) type).getSuperclass(typeEnv));
      } else if (type instanceof IJavaWildcardType) {
        /* I think this case is dead because what we are actually going to see is
         * capture types.
         */
        final IJavaType upperBound = ((IJavaWildcardType) type).getUpperBound();
        return (upperBound == null) ? javaLangObject : getUpperBound(upperBound);
      } else if (type instanceof IJavaCaptureType) {
        final IJavaType upperBound = ((IJavaCaptureType) type).getUpperBound();
        return (upperBound == null) ? javaLangObject : getUpperBound(upperBound);
      } else {
        /* IJavaPrimitiveType, IJavaVoidType, IJavaArrayType,
         * IJavaIntersectionType, IJavaNullType, IJavaDeclaredType.
         */
        // XXX: Is this really correct for intersection type?
        return type;
      }
    }
  }
  
  
  
  /**
   * Given a type formal, return the least declared <em>class</em> that is
   * the upper bound of the formal.  This never returns a declared interface.  
   *  
   * @param tf
   * @return
   */
  public static IJavaDeclaredType typeFormalToDeclaredClass(
      final ITypeEnvironment typeEnv, final IJavaTypeFormal tf) {
    final IJavaDeclaredType javaLangObject =
        (IJavaDeclaredType) typeEnv.findJavaTypeByName(SLUtility.JAVA_LANG_OBJECT);

    IJavaType current = tf;
    while (!(current instanceof IJavaDeclaredType)) {
      if (current instanceof IJavaTypeFormal) {
        current = ((IJavaTypeFormal) current).getSuperclass(typeEnv);
      } else if (current instanceof IJavaWildcardType) {
        /* I think this case is dead because what we are actually going to see is
         * capture types.
         */
        final IJavaType upperBound = ((IJavaWildcardType) current).getUpperBound();
        current = (upperBound == null) ? javaLangObject : upperBound;
      } else if (current instanceof IJavaCaptureType) {
        final IJavaType upperBound = ((IJavaCaptureType) current).getUpperBound();
        current = (upperBound == null) ? javaLangObject : upperBound;
      } else if (current instanceof IJavaIntersectionType) {
        /* Only the first type can be a class, so we return that one.  Then
         * we check below to see if that is in fact a class or an interface.
         */
        current = ((IJavaIntersectionType) current).getPrimarySupertype();
      }
    }
    
    final IJavaDeclaredType declType = (IJavaDeclaredType) current;
    if (TypeUtil.isInterface(declType.getDeclaration())) {
      return javaLangObject;
    } else {
      return declType;
    }
  }
  
  
  
  /**
   * Returns whether <code>t1</code> is an ancestor of <code>t2</code>,
   * or vice versa.
   */
  public static boolean areDirectlyRelated(
    final ITypeEnvironment tEnv, final IJavaType t1, final IJavaType t2) {
    return tEnv.isRawSubType(t1, t2) || tEnv.isRawSubType(t2, t1);
  }
  
  /**
   * Returns whether <code>t1</code> is an ancestor of <code>t2</code>.
   * This uses ITypeEnvironment.isSubType() which 
   * already does the right thing for related IJavaArrayType to IJavaDeclaredType.
   * (This is, arrays are subtypes of java.lang.Object.)
   */
  public static boolean isAncestorOf(
    final ITypeEnvironment tEnv, final IJavaType t1, final IJavaType t2) {
    return tEnv.isRawSubType(t2, t1);
  }
}
