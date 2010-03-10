package com.surelogic.jsure.client.eclipse.refactor;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.ClassInitializer;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.Declaration;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.PackageDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public class IRNodeUtil {
	public static IJavaDeclaration convert(final IBinder binder,
			final IRNode decl) {
		if (decl == null) {
			return null;
		}
		final IRNode parentNode = JJNode.tree.getParentOrNull(decl);
		final IJavaDeclaration parent = convert(binder, parentNode);
		final Operator op = JJNode.tree.getOperator(decl);
		if (op instanceof Declaration) {
			if (MethodDeclaration.prototype.includes(op)) {
				final String name = MethodDeclaration.getId(decl);
				final String[] paramNames = getParamTypes(binder,
						MethodDeclaration.getParams(decl));
				return new Method((TypeContext) parent, name, paramNames);
			}
			if (TypeDeclaration.prototype.includes(op)) {
				// TODO fix to handle method/initializer as parent
				if (parent == null) {
					return new TypeContext(TypeDeclaration.getId(decl));
				}
				if (parent instanceof Method) {
					return new TypeContext((Method) parent, TypeDeclaration
							.getId(decl));
				}
				return new TypeContext((TypeContext) parent, TypeDeclaration
						.getId(decl));
			}
			if (VariableDeclarator.prototype.includes(op)) {
				final IRNode gparent = JJNode.tree.getParentOrNull(parentNode);
				if (FieldDeclaration.prototype.includes(gparent)) {
					return new Field((TypeContext) parent, VariableDeclarator
							.getId(decl));
				}
				return parent;
			}
			if (ConstructorDeclaration.prototype.includes(op)) {
				final TypeContext type = (TypeContext) parent;
				final String[] paramNames = getParamTypes(binder,
						ConstructorDeclaration.getParams(decl));
				return new Method((TypeContext) parent, type.getName(),
						paramNames);
			}
			if (ParameterDeclaration.prototype.includes(op)) {
				final IRLocation loc = JJNode.tree.getLocation(decl);
				final int num = JJNode.tree.childLocationIndex(parentNode, loc);
				return new MethodParameter((Method) parent, num);
			}
			if (PackageDeclaration.prototype.includes(op)
					|| ClassInitializer.prototype.includes(op)) {
				// Ignoring this "declaration"
				return parent;
			}
			System.out.println("Unexpected declaration: "
					+ DebugUnparser.toString(decl));
		} else if (AnonClassExpression.prototype.includes(op)) {
			// TODO is this the right name?
			if (parent instanceof Method) {
				return new TypeContext((Method) parent, JJNode
						.getInfoOrNull(decl));
			}
			return new TypeContext((TypeContext) parent, JJNode
					.getInfoOrNull(decl));
		}
		return parent;
	}

	private static String[] getParamTypes(final IBinder binder,
			final IRNode params) {
		final List<String> names = new ArrayList<String>();
		for (final IRNode pd : JJNode.tree.children(params)) {
			final IRNode type = ParameterDeclaration.getType(pd);
			final IJavaType t = binder.getJavaType(type);
			final String name = getParamType(binder, t);
			names.add(name);
		}
		return names.toArray(new String[names.size()]);
	}

	private static String getParamType(final IBinder binder, final IJavaType t) {
		if (t instanceof IJavaTypeFormal) {
			final IJavaTypeFormal tf = (IJavaTypeFormal) t;
			return getParamType(binder, tf.getSuperclass(binder
					.getTypeEnvironment()));
		} else if (t instanceof IJavaArrayType) {
			final IJavaArrayType at = (IJavaArrayType) t;
			final StringBuilder sb = new StringBuilder(getParamType(binder, at
					.getBaseType()));
			for (int i = 0; i < at.getDimensions(); i++) {
				sb.append("[]");
			}
			return sb.toString();
		} else if (t instanceof IJavaDeclaredType) {
			final IJavaDeclaredType dt = (IJavaDeclaredType) t;
			return JavaNames.getFullTypeName(dt.getDeclaration());
		}
		return t.getName();
	}
}
