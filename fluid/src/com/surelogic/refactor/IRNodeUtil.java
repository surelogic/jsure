/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.refactor;

import java.util.*;

import com.surelogic.common.SLUtility;
import com.surelogic.common.refactor.*;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.InitDeclaration;
import edu.cmu.cs.fluid.parse.*;
import edu.cmu.cs.fluid.tree.*;

public class IRNodeUtil {
	public static IJavaDeclaration convert(final IBinder binder,
			final IRNode decl) {
		if (decl == null) {
			return null;
		}
		final IRNode parentNode = JavaPromise.getParentOrPromisedFor(decl);
		final IJavaDeclaration parent = convert(binder, parentNode);
		final Operator op = JJNode.tree.getOperator(decl);
		if (op instanceof Declaration) {
			if (MethodDeclaration.prototype.includes(op)) {
				final String name = MethodDeclaration.getId(decl);
				final String[] paramNames = getParamTypes(binder,
						MethodDeclaration.getParams(decl));
				return new Method((TypeContext) parent, name, paramNames, JavaNode.wasImplicit(decl));
			}
			if (TypeDeclaration.prototype.includes(op)) {
				// TODO fix to handle method/initializer as parent
				final String id = JJNode.getInfoOrNull(decl);
				if (parent == null) {
					return new TypeContext(id);
				}
				if (parent instanceof Method) {
					return new TypeContext((Method) parent, id);
				}				
				return new TypeContext((TypeContext) parent, id);
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
						paramNames, JavaNode.wasImplicit(decl));
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
			if (parent instanceof Field) {
				return new TypeContext((Field) parent, JJNode
						.getInfoOrNull(decl));
			}
			if (!(parent instanceof TypeContext)) {
				throw new IllegalStateException();
			}
			return new TypeContext((TypeContext) parent, JJNode
					.getInfoOrNull(decl));
		} else if (InitDeclaration.prototype.includes(op)) {
			return new Method((TypeContext) parent, "<init>", SLUtility.EMPTY_STRING_ARRAY, true);
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

