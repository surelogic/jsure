package edu.cmu.cs.fluid.java;

import java.util.Iterator;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.AnnotationDeclaration;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.CompilationUnit;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.Declaration;
import edu.cmu.cs.fluid.java.operator.EnumConstantClassDeclaration;
import edu.cmu.cs.fluid.java.operator.EnumDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.InterfaceDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.NamedPackageDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.java.operator.SomeFunctionDeclaration;
import edu.cmu.cs.fluid.java.operator.Type;
import edu.cmu.cs.fluid.java.operator.TypeDeclInterface;
import edu.cmu.cs.fluid.java.operator.TypeDeclarationStatement;
import edu.cmu.cs.fluid.java.operator.TypeRef;
import edu.cmu.cs.fluid.java.operator.VariableDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableDeclarators;
import edu.cmu.cs.fluid.java.operator.VoidType;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.EmptyIterator;
import edu.cmu.cs.fluid.util.Iteratable;
import edu.cmu.cs.fluid.util.IteratorUtil;
import edu.cmu.cs.fluid.util.SimpleRemovelessIterator;

/**
 * Some utility routines to help report information about Java nodes by creating
 * well formated strings.
 */
public final class JavaNames {
	/*
	 * private static final SlotInfo qualifiedNameSI =
	 * JavaNode.getVersionedSlotInfo("JavaNames.qualifiedName",
	 * IRStringType.prototype, null);
	 */
	public static Operator getOperator(final IRNode n) {
		return JJNode.tree.getOperator(n);
	}

	/**
	 * Given a field declaration this method sends a string.
	 * 
	 * @param field
	 *            the IRNode for the FieldDeclaration
	 * @return a string representation of the field
	 */
	public static String getFieldDecl(final IRNode field) {
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
				//result = DebugUnparser.toString(field);
				IRNode vdecls = FieldDeclaration.getVars(field);
				StringBuilder sb = new StringBuilder();
				// Optimized for the common case of one decl
				String first = null;
				int num = 0;
				for(IRNode vd : VariableDeclarators.getVarIterator(vdecls)) {
					String id = VariableDeclaration.getId(vd);
					if (sb.length() == 0) {
						first = id;
					} else {
						sb.append(", ");
					}
					sb.append(id);					
					num++;
				}
				if (num == 1) {
					return first;
				}
				return sb.toString();
			} else if (QualifiedReceiverDeclaration.prototype.includes(op)) {
				result = DebugUnparser.toString(field);
			}
		}
		return result;
	}

	/**
	 * Given a type this method returns the identifier for the type.
	 * 
	 * @param type
	 *            an IRNode which is either a ClassDeclaration or an
	 *            InterfaceDeclaration
	 * @return the identifier for the type or "(unknown)"
	 */
	public static String getTypeName(final IRNode type) {
		String result = "(unknown)";
		if (type == null) {
			return result;
		}
		// This should be the same as below
		/*
		final String temp = JJNode.getInfoOrNull(type);
		if (temp != null) {
			return temp;
		}
        */
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
		} else if (EnumConstantClassDeclaration.prototype.includes(op)) {
			result = EnumConstantClassDeclaration.getId(type);
		} else if (Type.prototype.includes(op)) {
			result = DebugUnparser.toString(type);
		} else if (VoidType.prototype.includes(op)) {
			result = "void";
		}
		return result;
	}

	private static String getRecursiveTypeName(final IRNode type,
			final IRNode last) {
		if (type == null) {
			String pkg = getPackageName(last);
			if (pkg == null) {
				pkg = "";
			}
			return pkg;
		}
		final IRNode next = VisitUtil.getEnclosingType(type);
		return getRecursiveTypeName(next, type) + "." + getTypeName(type);
	}

	/**
	 * Given a type this method returns the nested identifier for the type.
	 * <p>
	 * Example: blah.Foo.Nested
	 * <p>
	 * Same as {@link #getTypeName(IRNode)} But gives nested classes dots.
	 * 
	 * @param type
	 *            an IRNode which is either a ClassDeclaration or an
	 *            InterfaceDeclaration
	 * @return the identifier for the type or "(unknown)"
	 */
	public static String getQualifiedTypeName(final IRNode type) {
		String name; // = (String) type.getSlotValue(qualifiedNameSI);
		// if (name == null) {
		name = getRecursiveTypeName(type, null);
		// type.setSlotValue(qualifiedNameSI, name);
		// }
		return name;
	}

	public static String getQualifiedTypeName(final IJavaType type) {
		if (type instanceof IJavaDeclaredType) {
			final IJavaDeclaredType t = (IJavaDeclaredType) type;
			return getQualifiedTypeName(t.getDeclaration());
		}
		return "(not a declared type)";
	}

	/**
	 * From an IRNode representing Parameters generates a list of the types of
	 * the foramal parameters suitable for use in the UI.
	 * 
	 * @param args
	 *            an IRNode which should be a Parameters op
	 * @return something like "()" or "(int, Object)"
	 */
	private static String genArgList(final IRNode args, boolean useFullTypes) {
		int paramCount = 0;
		StringBuilder result = new StringBuilder("(");
		final Operator op = getOperator(args);
		if (Parameters.prototype.includes(op)) {
			final Iterator<IRNode> e = Parameters.getFormalIterator(args);
			while (e.hasNext()) {
				final IRNode param = e.next();
				final Operator paramOp = getOperator(param);
				if (ParameterDeclaration.prototype.includes(paramOp)) {
					if (paramCount > 0) {
						result.append(',');
					}
					paramCount++;
					
					final String pType = getTypeName(ParameterDeclaration.getType(param));
					if (useFullTypes) {
						result.append(pType);
					} else {
						final int lastDot = pType.lastIndexOf('.');
						if (lastDot < 0) {
							result.append(pType);
						} else {
							result.append(pType.substring(lastDot+1));
						}
					}
				}
			}
		}
		result.append(')');
		return result.toString();
	}

	/**
	 * Generate an unqualified name from a constructor or method declaration.
   * 
   * @param node
   *            a constructor or method declaration
   * @return a created name
	 */
	public static String genSimpleMethodConstructorName(final IRNode node) {
    final StringBuilder sb = new StringBuilder();
    addTargetName(sb, node, false);
    return sb.toString();
	}
	
	/**
	 * Generates a name from a constructor or method declaration.
	 * 
	 * @param node
	 *            a constructor or method declaration
	 * @return a created name
	 */
	public static String genMethodConstructorName(final IRNode node) {
		if (node == null) {
			return "(n/a)";
		}
		// add the type we found the method within (could be the promised type)
		final IRNode enclosingType = VisitUtil.getEnclosingType(node);
		final String typeName = getFullTypeName(enclosingType);
		final StringBuilder sb = new StringBuilder(typeName);
		addTargetName(sb.append('.'), node, true);
		return sb.toString();
	}

	public static String genRelativeFunctionName(final IRNode node) {
		if (node == null) {
			return "(n/a)";
		}
		// add the type we found the method within (could be the promised type)
		final IRNode enclosingType = VisitUtil.getEnclosingType(node);
		final String typeName = getRelativeTypeName(enclosingType);
		final StringBuilder sb = new StringBuilder(typeName);
		addTargetName(sb.append('.'), node, false);
		return sb.toString();
	}

	private static void addTargetName(final StringBuilder sb,
			final IRNode node, final boolean useFullParamTypes) {
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
			sb.append("(" + op.name() + ")");
			return;
		}
		sb.append(targetName);

		if (true) {
			sb.append(genArgList(args, useFullParamTypes));
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
		IRNode compUnit = VisitUtil.findCompilationUnit(nodeInsideCompUnit);
		if (compUnit == null) {
			return "";
		}
		final IRNode pkgDecl = CompilationUnit.getPkg(compUnit);
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
	 *            an IRBNode somewhere inside a CompUnit
	 * @return Either x.y.z. if we are in a non-default package or an empty
	 *         string if we are in the default package.
	 */
	public static String genPackageQualifier(final IRNode nodeInsideCompUnit) {
		final String packName = getPackageName(nodeInsideCompUnit);
		if (packName == null) {
			return "";
		} else {
			return packName + ".";
		}
	}

	public static String genQualifiedMethodConstructorName(final IRNode method) {
		// String pkgQ = genPackageQualifier(method);
		// String mcName = genMethodConstructorName(method);
		return genMethodConstructorName(method);
	}

	/**
	 * Produce a complete qualifier ready to have a simpleName appended to it.
	 * 
	 * @param nodeInsideType
	 *            An IRNode somewhere inside a Type.
	 * @return either "" or "x.y.z." or "x.y.z.Foo." as appropriate
	 */
	public static String genFullQualifier(final IRNode nodeInsideType) {
		final IRNode enclosingType = VisitUtil.getEnclosingType(nodeInsideType);
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
	 *            A possibly qualified name
	 * @return the SimpleName part of name.
	 */
	public static String genSimpleName(final String name) {
		// failfast on null arg!
		final int posOfLastDot = name.lastIndexOf('.');
		if (posOfLastDot < 0 || posOfLastDot == name.length() - 1) {
			return name;
		}

		return name.substring(posOfLastDot + 1);
	}
	
	/**
	 * Uses ':' to designate local types
	 */
	public static String getFullTypeName_local(final IRNode decl) {
		return new TypeNameBuilder(true, ':').build(decl);
	}
	
	/**
	 * @return the fully qualified name of the type
	 */
	public static String getFullTypeName(final IRNode decl) {
		return new TypeNameBuilder(true, '$').build(decl);
	}

	/**
	 * @return the name of the type inside this CU (no package)
	 */
	public static String getRelativeTypeName(final IRNode decl) {
		return new TypeNameBuilder(false, '$').build(decl);
	}

	private static class TypeNameBuilder {
		final StringBuilder name = new StringBuilder();
		final boolean includePackage;
		final char localSeparator;
		
		TypeNameBuilder(boolean needPkg, char sep) {
			includePackage = needPkg;
			localSeparator = sep;
		}

		String build(final IRNode decl) {
			computeFullTypeName(name, decl, includePackage);
			return name.toString();
		}
	
		/**
		 * Helper function for getFullTypeName
		 */
		void computeFullTypeName(final StringBuilder name,
				final IRNode decl, final boolean includePackage) {
			final IRNode enclosingT = VisitUtil.getEnclosingType(decl);
			if (enclosingT == null) {
				if (includePackage) {
					final String pkg = getPackageName(decl);
					if (pkg != null && pkg != "") {
						name.append(pkg).append('.');
					}
				}
				name.append(getTypeName(decl));
			} else {
				computeFullTypeName(name, enclosingT, includePackage);

				final IRNode parent = JJNode.tree.getParentOrNull(decl);
				if (TypeDeclarationStatement.prototype.includes(parent)) {
					name.append(localSeparator);
				} else {
					name.append('.');
				}
				name.append(getTypeName(decl));
			}
		}
	}

	/**
	 * Compute the canonical qualifier for the outermost Enclosing Type Or comp
	 * unit. See VisitUtil.computeOutermostEnclosingTypeOrCU for details on
	 * finding the outermost...
	 * 
	 * @param locInIR
	 *            The place we are now
	 * @return String representing either a qualified type name, or a package
	 *         name, or "(default)" if we're in the default package.
	 */
	public static String computeQualForOutermostTypeOrCU(final IRNode locInIR) {
		// figure out what the CUname should be
		final IRNode idealCU = VisitUtil
				.computeOutermostEnclosingTypeOrCU(locInIR);
		final Operator op = JJNode.tree.getOperator(idealCU);

		final String cuName;
		if (ClassDeclaration.prototype.includes(op)
				|| InterfaceDeclaration.prototype.includes(op)) {
			cuName = getFullTypeName(idealCU);
		} else if (NamedPackageDeclaration.prototype.includes(op)) {
			cuName = NamedPackageDeclaration.getId(idealCU);
		} else {
			final String tCUname = getPackageName(idealCU);
			if (tCUname == null) {
				// getPackageName returns null when we're in the default
				// package.
				cuName = "(default)";
			} else {
				cuName = tCUname;
			}
		}
		return cuName;
	}

	public static Iteratable<String> getQualifiedTypeNames(final IRNode cu) {
		final Iterator<IRNode> it = VisitUtil.getTypeDecls(cu);
		if (it.hasNext()) {
			return new SimpleRemovelessIterator<String>() {
				@Override
				protected Object computeNext() {
					if (!it.hasNext()) {
						return IteratorUtil.noElement;
					}
					final IRNode n = it.next();
					return getQualifiedTypeName(n);
				}
			};
		}
		return new EmptyIterator<String>();
	}

	/**
	 * @param cu
	 *            The root of a compilation unit
	 * @return The fully qualified name for the primary class (or the first type
	 *         that appears)
	 */
	public static String genPrimaryTypeName(final IRNode cu) {
		IRNode t = null;
		for (final IRNode type : VisitUtil.getTypeDecls(cu)) {
			if (t == null) {
				t = type;
			}
			if (JavaNode.getModifier(type, JavaNode.PUBLIC)) {
				return getQualifiedTypeName(type);
			}
		}
		return t == null ? null : getQualifiedTypeName(t);
	}

	public static String getFullName(final IRNode node) {
		if (node == null) {
			return "Null";
		}
		if (node.identity() == IRNode.destroyedNode) {
			return "Destroyed";
		}
		final Operator op = JJNode.tree.getOperator(node);
		if (SomeFunctionDeclaration.prototype.includes(op)) {
			return genQualifiedMethodConstructorName(node);
		}
		if (op instanceof TypeDeclInterface) {
			return getFullTypeName(node);
		}
		if (NamedPackageDeclaration.prototype.includes(op)) {
			return NamedPackageDeclaration.getId(node);
		}
		final IRNode type = VisitUtil.getEnclosingType(node);
		if (type == null) {
			return getFieldDecl(node);
		}
		return getFullTypeName(type) + '.' + getFieldDecl(node);
	}
	
	public static String getRelativeName(final IRNode node) {
		if (node == null) {
			return "Null";
		}
		if (node.identity() == IRNode.destroyedNode) {
			return "Destroyed";
		}
		final Operator op = JJNode.tree.getOperator(node);
		if (SomeFunctionDeclaration.prototype.includes(op)) {
			return genRelativeFunctionName(node);
		}
		if (op instanceof TypeDeclInterface) {
			return getRelativeTypeName(node);
		}
		if (NamedPackageDeclaration.prototype.includes(op)) {
			return NamedPackageDeclaration.getId(node);
		}
		final IRNode type = VisitUtil.getEnclosingType(node);
		if (type == null) {
			return getFieldDecl(node);
		}
		return getRelativeTypeName(type) + '.' + getFieldDecl(node);
	}

	public static String unparseType(final IRNode type) {
		if (TypeRef.prototype.includes(type)) {
			return unparseType(TypeRef.getBase(type)) + "."
					+ TypeRef.getId(type);
		}
		return DebugUnparser.toString(type);
	}

	/**
	 * Used to compute a context id for the given node
	 */
	public static String computeContextId(final IRNode node) {
		final StringBuilder sb = new StringBuilder();
		// VisitUtil.getEnclosingDecl(node);
		for (final IRNode n : VisitUtil.rootWalk(node)) {
			final Operator op = getOperator(n);
			if (Declaration.prototype.includes(op)) {
				sb.append(getFullName(n));
				break;
			} else {
				final String info = JJNode.getInfoOrNull(n);
				if (info != null && !(info.length() == 0)) {
					sb.append(info);
					sb.append(", ");
				}
			}
		}
		return sb.toString();
	}
}