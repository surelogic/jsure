package edu.cmu.cs.fluid.java;

import java.util.*;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.*;

/**
 * Some utility routines to help report information about Java nodes by creating
 * well formated strings.
 */
public final class JavaNames {
  /*
  private static final SlotInfo qualifiedNameSI = 
    JavaNode.getVersionedSlotInfo("JavaNames.qualifiedName", IRStringType.prototype, null);
  */
  public static Operator getOperator(final IRNode n) {
    return JJNode.tree.getOperator(n);
  }

  /**
   * Given a field declaration this method sends a string.
   * 
   * @param field
   *          the IRNode for the FieldDeclaration
   * @return a string representation of the field
   */
  public static String getFieldDecl(IRNode field) {
    String result = "(unknown)";
    if (field != null) {
      final Operator op = getOperator(field);
      if (VariableDeclarator.prototype.includes(op)
          || ParameterDeclaration.prototype.includes(op)) {
        result = VariableDeclaration.getId(field);
      } else if (ReceiverDeclaration.prototype.includes(op)) {
        result = "this";
      } else if (ReturnValueDeclaration.prototype.includes(op)) {
        result = "return";
      } else if (FieldDeclaration.prototype.includes(op)) {
        result = DebugUnparser.toString(field);
      }
    }
    return result;
  }

  /**
   * Given a type this method returns the identifier for the type.
   * 
   * @param type
   *          an IRNode which is either a ClassDeclaration or an
   *          InterfaceDeclaration
   * @return the identifier for the type or "(unknown)"
   */
  public static String getTypeName(IRNode type) {
    String result = "(unknown)";
    if (type == null) return result;
    final Operator op = getOperator(type);
    if (ClassDeclaration.prototype.includes(op)) {
      result = ClassDeclaration.getId(type);
    } else if (InterfaceDeclaration.prototype.includes(op)) {
      result = InterfaceDeclaration.getId(type);
    } else if (EnumDeclaration.prototype.includes(op)) {
      result = EnumDeclaration.getId(type);
    } else if (AnnotationDeclaration.prototype.includes(op)) {
      result = AnnotationDeclaration.getId(type);
    } else if (AnonClassExpression.prototype.includes(op)) {
      result = JJNode.getInfoOrNull(type);
    } else if (Type.prototype.includes(op)) {
      result = DebugUnparser.toString(type);
    } else if (VoidType.prototype.includes(op)) {
      result = "void";
    }
    return result;
  }

  private static String getRecursiveTypeName(IRNode type, IRNode last) {
    if (type == null) {
      String pkg = getPackageName(last);
      if (pkg == null) {
        pkg = "";
      }
      return pkg;
    }
    IRNode next = VisitUtil.getEnclosingType(type);
    return getRecursiveTypeName(next, type)+"."+getTypeName(type);
  }

  /**
   * Given a type this method returns the nested identifier for the type.
   * <p>
   * Example: blah.Foo.Nested
   * <p>
   * Same as {@link #getTypeName(IRNode)} But gives nested classes dots.
   * 
   * @param type
   *          an IRNode which is either a ClassDeclaration or an
   *          InterfaceDeclaration
   * @return the identifier for the type or "(unknown)"
   */
  public static String getQualifiedTypeName(IRNode type) {
    String name; // = (String) type.getSlotValue(qualifiedNameSI);
    //if (name == null) {
      name = getRecursiveTypeName(type, null);
    //  type.setSlotValue(qualifiedNameSI, name);
    //}
    return name;
  }

  public static String getQualifiedTypeName(IJavaType type) {
    if (type instanceof IJavaDeclaredType) {
      IJavaDeclaredType t = (IJavaDeclaredType) type;
      return getQualifiedTypeName(t.getDeclaration());
    }
    return "(not a declared type)";
  }
  
  /**
   * From an IRNode representing Parameters generates a list of the types of the
   * foramal parameters suitable for use in the UI.
   * 
   * @param args
   *          an IRNode which should be a Parameters op
   * @return something like "()" or "(int, Object)"
   */
  public static String genArgList(final IRNode args) {
    int paramCount = 0;
    String result = "(";
    final Operator op = getOperator(args);
    if (Parameters.prototype.includes(op)) {
      Iterator<IRNode> e = Parameters.getFormalIterator(args);
      while (e.hasNext()) {
        final IRNode param = e.next();
        final Operator paramOp = getOperator(param);
        if (ParameterDeclaration.prototype.includes(paramOp)) {
          result += (paramCount++ > 0 ? "," : "")
              + getTypeName(ParameterDeclaration.getType(param));
        }
      }
    }
    return result + ")";
  }

  /**
   * Generates a name from a constructor or method declaration.
   * 
   * @param node
   *          a constructor or method declaration
   * @return a created name
   */
  public static String genMethodConstructorName(final IRNode node) {
    if (node == null) {
      return "(n/a)";
    }
    // add the type we found the method within (could be the promised type)
    IRNode enclosingType = VisitUtil.getEnclosingType(node);
    String typeName      = getFullTypeName(enclosingType);
    StringBuilder sb     = new StringBuilder(typeName);
    addTargetName(sb, node, true);
    return sb.toString();
  }

  public static String genRelativeFunctionName(IRNode node) {
	  if (node == null) {
		  return "(n/a)";
	  }
	  // add the type we found the method within (could be the promised type)
	  IRNode enclosingType = VisitUtil.getEnclosingType(node);
	  String typeName      = getRelativeTypeName(enclosingType);
	  StringBuilder sb     = new StringBuilder(typeName);
	  addTargetName(sb, node, false);
	  return sb.toString();
  }
  
  private static void addTargetName(StringBuilder sb, IRNode node, boolean includeArgs) {
	  String targetName = "(none)";
	  final Operator op = getOperator(node);
	  final IRNode args;
	  if (MethodDeclaration.prototype.includes(op)) {
		  targetName = MethodDeclaration.getId(node);
		  args = MethodDeclaration.getParams(node);
	  } else if (ConstructorDeclaration.prototype.includes(op)) {
		  targetName = ConstructorDeclaration.getId(node);
		  args = ConstructorDeclaration.getParams(node);
	  } else {
		  sb.append("("+op.name()+")");
		  return;
	  }
	  sb.append('.').append(targetName);
	  
	  if (includeArgs) {
		  sb.append(genArgList(args));
	  } else {
		  sb.append("()");
	  }
  }
  
  /**
   * Returns the name of the package that the node is part of.
   * 
   * @param nodeInsideCompUnit
   * @return null if inside the default package
   */
  public static String getPackageName(final IRNode nodeInsideCompUnit) {
    IRNode compUnit = VisitUtil.getEnclosingCompilationUnit(nodeInsideCompUnit);
    if (compUnit == null) {
      compUnit = nodeInsideCompUnit;
    }
    IRNode pkgDecl = CompilationUnit.getPkg(compUnit);
    if (NamedPackageDeclaration.prototype.includes(getOperator(pkgDecl))) {
      return NamedPackageDeclaration.getId(pkgDecl);
    }
    return null;
  }

  /**
   * Produce the x.y.z. part of a qualified name, ready to prepend onto a type
   * and simple-name.
   * 
   * @param nodeInsideCompUnit
   *          an IRBNode somewhere inside a CompUnit
   * @return Either x.y.z. if we are in a non-default package or an empty string
   *         if we are in the default package.
   */
  public static String genPackageQualifier(final IRNode nodeInsideCompUnit) {
    final String packName = getPackageName(nodeInsideCompUnit);
    if (packName == null) {
      return "";
    } else {
      return packName + ".";
    }
  }

  public static String genQualifiedMethodConstructorName(IRNode method) {
//    String pkgQ = genPackageQualifier(method);
//    String mcName = genMethodConstructorName(method);
    return genMethodConstructorName(method);
  }

  /**
   * Produce a complete qualifier ready to have a simpleName appended to it.
   * 
   * @param nodeInsideType
   *          An IRNode somewhere inside a Type.
   * @return either "" or "x.y.z." or "x.y.z.Foo." as appropriate
   */
  public static String genFullQualifier(final IRNode nodeInsideType) {
    IRNode enclosingType = VisitUtil.getEnclosingType(nodeInsideType);
    String typeName;
    if (enclosingType == null) {
      typeName = "";
    } else {
      typeName = getTypeName(enclosingType);
    }
    final String pkgQ = genPackageQualifier(nodeInsideType);
    final String allButDot = pkgQ + typeName;

    if (allButDot.length() > 0) {
      return allButDot + ".";
    } else {
      return "";
    }
  }

  /**
   * Return the simple name portion of a possibly qualified name. raises
   * exception for null arg, but handles empty string correctly.
   * 
   * @param name
   *          A possibly qualified name
   * @return the SimpleName part of name.
   */
  public static String genSimpleName(final String name) {
    // failfast on null arg!
    int posOfLastDot = name.lastIndexOf('.');
    if (posOfLastDot < 0 || (posOfLastDot == name.length() - 1)) {
      return name;
    }

    return name.substring(posOfLastDot + 1);
  }
  
  /**
   * @return the fully qualified name of the type
   */
  public static String getFullTypeName(IRNode decl) {    
    StringBuilder name = new StringBuilder();
    computeFullTypeName(name, decl, true);
    return name.toString();
  }

  /**
   * @return the name of the type inside this CU (no package)
   */
  public static String getRelativeTypeName(IRNode decl) {    
    StringBuilder name = new StringBuilder();
    computeFullTypeName(name, decl, false);
    return name.toString();
  }  
  
  /**
   * Helper function for getFullTypeName
   */
  private static void computeFullTypeName(StringBuilder name, IRNode decl, boolean includePackage) {
    IRNode enclosingT = VisitUtil.getEnclosingType(decl);
    if (enclosingT == null) {
      if (includePackage) {
    	  String pkg = getPackageName(decl);
    	  if (pkg != null && pkg != "") {
    		  name.append(pkg).append('.');
    	  }
      }
      name.append(getTypeName(decl));
    } else {
      computeFullTypeName(name, enclosingT, includePackage);
      
      IRNode parent = JJNode.tree.getParentOrNull(decl);
      if (TypeDeclarationStatement.prototype.includes(parent)) {
    	name.append('$');
      } else {
    	name.append('.');
      }
      name.append(getTypeName(decl));
    }
  }

  /** Compute the canonical qualifier for the outermost Enclosing Type Or comp unit.
   * See VisitUtil.computeOutermostEnclosingTypeOrCU for details on finding the
   * outermost...
   * @param locInIR  The place we are now
   * @return String representing either a qualified type name, or a package name,
   * or "(default)" if we're in the default package.
   */
  public static String computeQualForOutermostTypeOrCU(IRNode locInIR) {
    // figure out what the CUname should be
    final IRNode idealCU = VisitUtil.computeOutermostEnclosingTypeOrCU(locInIR);
    final Operator op = JJNode.tree.getOperator(idealCU);
  
    final String cuName;
    if (ClassDeclaration.prototype.includes(op)
        || InterfaceDeclaration.prototype.includes(op)) {  
      cuName = getFullTypeName(idealCU);
    } else if (NamedPackageDeclaration.prototype.includes(op)) {  
      cuName = NamedPackageDeclaration.getId(idealCU);
    } else {
      String tCUname = getPackageName(idealCU);
      if (tCUname == null) {
        // getPackageName returns null when we're in the default package.
        cuName = "(default)";
      } else {
        cuName = tCUname; 
      }
    }
    return cuName;
  }
  
  public static Iteratable<String> getQualifiedTypeNames(IRNode cu) {
    final Iterator<IRNode> it = VisitUtil.getTypeDecls(cu);
    if (it.hasNext()) {
      return new SimpleRemovelessIterator<String>() {
        @Override
        protected Object computeNext() {
          if (!it.hasNext()) {
            return IteratorUtil.noElement;
          }
          IRNode n = it.next();
          return getQualifiedTypeName(n);
        }        
      };
    }
    return EmptyIterator.prototype();
  }


  /**
   * @param cu The root of a compilation unit
   * @return The fully qualified name for the primary class
   *         (or the first type that appears)
   */
  public static String genPrimaryTypeName(IRNode cu) {
    IRNode t = null;
    for(IRNode type : VisitUtil.getTypeDecls(cu)) {
      if (t == null) {
        t = type;
      }
      if (JavaNode.getModifier(type, JavaNode.PUBLIC)) {
        return getQualifiedTypeName(type);
      }
    }
    return t == null ? null : getQualifiedTypeName(t);
  }

  public static String getFullName(IRNode node) {
    final Operator op = JJNode.tree.getOperator(node);
    if (SomeFunctionDeclaration.prototype.includes(op)) {
      return genQualifiedMethodConstructorName(node);
    }
    if (op instanceof TypeDeclInterface) {
      return getFullTypeName(node);
    }
    final IRNode type = VisitUtil.getEnclosingType(node);
    return getFullTypeName(type)+'.'+getFieldDecl(node);
  }

  public static String unparseType(IRNode type) {
    if (TypeRef.prototype.includes(type)) {
      return unparseType(TypeRef.getBase(type))+"."+TypeRef.getId(type);
    }
    return DebugUnparser.toString(type);
  }
  
  /**
   * Used to compute a context id for the given node
   */
  public static String computeContextId(IRNode node) {
	  final StringBuilder sb = new StringBuilder(); 
	  //VisitUtil.getEnclosingDecl(node);
	  for(IRNode n : VisitUtil.rootWalk(node)) {
		  final Operator op = getOperator(n);
		  if (Declaration.prototype.includes(op)) {
			  sb.append(getFullName(n));			  
			  break;
		  } else {
			  String info = JJNode.getInfoOrNull(n);
			  if (info != null && !info.isEmpty()) {
				  sb.append(info);
				  sb.append(", ");				  
			  }
		  }
	  }
	  return sb.toString();
  }
}