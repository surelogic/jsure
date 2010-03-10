package com.surelogic.jsure.client.eclipse.refactor;

import java.util.*;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.*;
import edu.cmu.cs.fluid.tree.*;

public class IRNodeUtil {
	public static IJavaDeclaration convert(IBinder binder, IRNode decl) {
		if (decl == null) {
			return null;
		}
		final IRNode parentNode = JJNode.tree.getParentOrNull(decl);
		final IJavaDeclaration parent = convert(binder, parentNode);
		final Operator op = JJNode.tree.getOperator(decl);		
		if (op instanceof Declaration) {
			if (MethodDeclaration.prototype.includes(op)) {
				final String name = MethodDeclaration.getId(decl);
				final String[] paramNames = getParamTypes(binder, MethodDeclaration.getParams(decl));
				return new Method((Type) parent, name, paramNames);
			}
			if (TypeDeclaration.prototype.includes(op)) {
				// TODO fix to handle method/initializer as parent
				if (parent == null) {
					return new Type(TypeDeclaration.getId(decl));
				}
				if (parent instanceof Method) {
					return new Type((Method) parent, TypeDeclaration.getId(decl));
				}				
				return new Type((Type) parent, TypeDeclaration.getId(decl));
			}
			if (VariableDeclarator.prototype.includes(op)) {
				final IRNode gparent = JJNode.tree.getParentOrNull(parentNode);
				if (FieldDeclaration.prototype.includes(gparent)) {
					return new Field((Type) parent, VariableDeclarator.getId(decl));
				}
				return parent;
			}
			if (ConstructorDeclaration.prototype.includes(op)) {
				Type type = (Type) parent;
				final String[] paramNames = getParamTypes(binder, ConstructorDeclaration.getParams(decl));
				return new Method((Type) parent, type.getName(), paramNames);
			}				
			if (ParameterDeclaration.prototype.includes(op)) {
				final IRLocation loc = JJNode.tree.getLocation(decl);
				final int num        = JJNode.tree.childLocationIndex(parentNode, loc);
				return new MethodParameter((Method) parent, num); 
			}
			if (PackageDeclaration.prototype.includes(op) || ClassInitializer.prototype.includes(op)) {
				// Ignoring this "declaration"
				return parent;
			} 
			System.out.println("Unexpected declaration: "+DebugUnparser.toString(decl));
		} 
		else if (AnonClassExpression.prototype.includes(op)) {
			// TODO is this the right name?
			if (parent instanceof Method) {
				return new Type((Method) parent, JJNode.getInfoOrNull(decl));
			}				
			return new Type((Type) parent, JJNode.getInfoOrNull(decl));
		}
		return parent;
	}
	
	private static String[] getParamTypes(IBinder binder, IRNode params) {
		List<String> names = new ArrayList<String>();
		for(final IRNode pd : JJNode.tree.children(params)) {
			final IRNode type = ParameterDeclaration.getType(pd);
			final IJavaType t = binder.getJavaType(type);
			final String name = getParamType(binder, t);
			names.add(name);
		}
		return names.toArray(new String[names.size()]);
	}
	
	private static String getParamType(IBinder binder, IJavaType t) {
		if (t instanceof IJavaTypeFormal) {
			IJavaTypeFormal tf = (IJavaTypeFormal) t;
			return getParamType(binder, tf.getSuperclass(binder.getTypeEnvironment()));
		} 
		else if (t instanceof IJavaArrayType) {
			IJavaArrayType at = (IJavaArrayType) t;
			StringBuilder sb = new StringBuilder(getParamType(binder, at.getBaseType()));
			for(int i=0; i<at.getDimensions(); i++) {
				sb.append("[]");
			}
			return sb.toString();
		}
		else if (t instanceof IJavaDeclaredType) {
			IJavaDeclaredType dt = (IJavaDeclaredType) t;
			return JavaNames.getFullTypeName(dt.getDeclaration());
		}
		return t.getName();
	}
}
