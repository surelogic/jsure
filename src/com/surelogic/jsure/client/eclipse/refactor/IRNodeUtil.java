package com.surelogic.jsure.client.eclipse.refactor;

import java.util.*;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.*;
import edu.cmu.cs.fluid.tree.*;

public class IRNodeUtil {
	public static IJavaDeclaration convert(IRNode decl) {
		if (decl == null) {
			return null;
		}
		final IRNode parentNode = JJNode.tree.getParentOrNull(decl);
		final IJavaDeclaration parent = convert(parentNode);
		final Operator op = JJNode.tree.getOperator(decl);		
		if (op instanceof Declaration) {
			if (MethodDeclaration.prototype.includes(op)) {
				final String name = MethodDeclaration.getId(decl);
				final String[] paramNames = getParamNames(MethodDeclaration.getParams(decl));
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
				final String[] paramNames = getParamNames(ConstructorDeclaration.getParams(decl));
				return new Method((Type) parent, type.getName(), paramNames);
			}				
			if (ParameterDeclaration.prototype.includes(op)) {
				final IRLocation loc = JJNode.tree.getLocation(decl);
				final int num        = JJNode.tree.childLocationIndex(parentNode, loc);
				return new MethodParameter((Method) parent, num); 
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
	
	private static String[] getParamNames(IRNode params) {
		List<String> names = new ArrayList<String>();
		for(IRNode pd : JJNode.tree.children(params)) {
			names.add(ParameterDeclaration.getId(pd));
		}
		return names.toArray(new String[names.size()]);
	}
}
