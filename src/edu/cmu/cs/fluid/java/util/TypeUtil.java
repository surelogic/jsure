// $Header: /var/cvs/fluid/code/fluid/java/bind/TypeUtil.java,v 1.4 2002/07/25
// 15:14:57 chance Exp $
package edu.cmu.cs.fluid.java.util;

import java.util.logging.Logger;

import com.surelogic.annotation.rules.*;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.CommonStrings;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaOperator;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.ClassInitDeclaration;
import edu.cmu.cs.fluid.java.promise.InitDeclaration;
import edu.cmu.cs.fluid.java.promise.NewRegionDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.promises.VouchFieldIsPromiseDrop;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.version.VersionedSlotFactory;

public class TypeUtil implements JavaGlobals {
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
        || ClassInitializer.prototype.includes(op) // The AST representation of an instance or static initializer block
        || NewRegionDeclaration.prototype.includes(op)) {
        return JavaNode.getModifier(node, JavaNode.STATIC);
    } else if (ClassDeclaration.prototype.includes(op)) {
      return false;
    } else if (NestedInterfaceDeclaration.prototype.includes(op)) { // MUST test before InterfaceDeclaration
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
    } else {
      throw new IllegalArgumentException(
        "Can't test if " + op.name() + " is static.");
    }
  }

  /**
   * Is the field declaration a final field. Returns true if the field has a
   * final modifier, or a assumeFinal promise, or if the field is declared in an *
   * interface.
   * 
   * @param node
   *          A VariableDeclarator
   */
  public static boolean isFinal(final IRNode node) {
    final Operator op = JJNode.tree.getOperator(node);
    if (VariableDeclarator.prototype.includes(op)) {
      if (TypeUtil.isInterface(VisitUtil.getEnclosingType(node))) {
        return true; // declared in an interface
      } else if (JavaNode.getModifier(JJNode.tree.getParent(JJNode.tree
          .getParent(node)), JavaNode.FINAL)) {
        return true; // declared final
      } else { // Check for @Vouch("final")
        final VouchFieldIsPromiseDrop vouchFinal = LockRules.getVouchFieldIs(node);
        if (vouchFinal != null && vouchFinal.isFinal()) {
          // We have an @Vouch("final")
          return true;
        }
      }      
      return false;
    } else if (ParameterDeclaration.prototype.includes(op)) {
      return JavaNode.getModifier(node, JavaNode.FINAL);
    } else {
      return false;
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
   * Is the given node an Interface declaration?  This includes 
   * InterfaceDeclaration and NestedInterfaceDeclaration.
   */
  public static boolean isInterface(final IRNode node) {
    final Operator op = JJNode.tree.getOperator(node);
    return InterfaceDeclaration.prototype.includes(op)
        || NestedInterfaceDeclaration.prototype.includes(op);
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
   * Given a type declaration, return the ClassBody node of the type.
   * 
   * @param typeDecl
   *          A {@link AnonClassExpression}, {@link ClassDeclaration},
   *          {@link NestedClassDeclaration},
   *          {@link EnumConstantClassDeclaration}, {@link EnumDeclaration},
   *          {@link NestedEnumDeclaration}, {@link InterfaceDeclaration}, or
   *          {@link NestedInterfaceDeclaration} node.
   * @return The {@link ClassBody} node of the given type declaration.
   * @throws IllegalArgumentException
   *           if <code>typeDecl</code> is not one of the above nodes.
   */
  public static final IRNode getTypeBody(final IRNode typeDecl) {
    final Operator op = JJNode.tree.getOperator(typeDecl);
    if (AnonClassExpression.prototype.includes(op)) {
      return AnonClassExpression.getBody(typeDecl);
    } else if (ClassDeclaration.prototype.includes(op)) {
      return ClassDeclaration.getBody(typeDecl);
    } else if (EnumConstantClassDeclaration.prototype.includes(op)) {
      return EnumConstantClassDeclaration.getBody(typeDecl);
    } else if (EnumDeclaration.prototype.includes(op)) {
      return EnumDeclaration.getBody(typeDecl);
    } else if (InterfaceDeclaration.prototype.includes(op)) {
      return InterfaceDeclaration.getBody(typeDecl);
    } else {
      throw new IllegalArgumentException(
          "Unknown type declaration " + op.name());
    }
  }
}
