package com.surelogic.xml;

import com.surelogic.annotation.parse.AnnotationVisitor;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public class PromisesXMLAnnotator {
	final AnnotationVisitor v;
	
	private PromisesXMLAnnotator(ITypeEnvironment tEnv, String xmlPath) {
		v = new AnnotationVisitor(tEnv, "XML Parser for "+ xmlPath);
	}
	
	/**
	 * @param root
	 *            A CompilationUnit
	 * @param xml
	 *            The name of the promises.xml file to parse in
	 * @return The number of annotations added
	 */
	public static int process(ITypeEnvironment tEnv, IRNode root, String xmlPath) {
		/*
		 * if (xmlPath.startsWith("java/lang/Object")) {
		 * System.out.println("Looking up "+xmlPath); }
		 */
		if (root == null) {
			return 0;
		}
		PackageElement p = PromisesXMLParser.load(xmlPath);
		if (p == null) {
			return 0;
		}
		PromisesXMLAnnotator a = new PromisesXMLAnnotator(tEnv, xmlPath);
		return a.applyPromisesToPkg(p, root);
	}

	/**
	 * @return The number of annotations added
	 */
	private int applyPromises(AnnotatedJavaElement elt, final IRNode here) {
		if (here == null) {
			return 0;
		}
		int count = 0;
		//for (Map.Entry<String, List<AnnotationElement>> e : elt.order.entrySet()) {
		//	// Reconstituted in the same order in the clone
		//	for (AnnotationElement a : e.getValue()) {
		for(AnnotationElement a : elt.getPromises(true)) { // TODO is this the same?
				count += applyPromise(a, here);
		//	}
		}
		return count;
	}
	
	private int applyPromise(final AnnotationElement a, final IRNode annotatedNode) {
		if (a.isBad() || a.isToBeDeleted()) {
			return 0;
		}
		boolean added = v.handleXMLPromise(annotatedNode, a.getPromise(), a.getContents(), a.getAttributeMap(), JavaNode.ALL_FALSE);
		return added ? 1 : 0;
	}
	
	/**
	 * @return The number of annotations added
	 */
	private int applyPromisesToPkg(PackageElement elt, IRNode cu) {
		final IRNode pkg = CompilationUnit.getPkg(cu);
		int added = applyPromises(elt, pkg);
		if (elt.getClassElement() != null) {
			added += applyPromisesToType(elt.getClassElement(), cu);
		}
		return added;
	}

	/**
	 * @return The number of annotations added
	 */
	private int applyPromisesToType(final ClassElement elt, final IRNode cuOrType) {
		if (cuOrType == null) {
			return 0;
		}
		final IRNode t = findType(cuOrType, elt.getName());
		if (t == null) {			
			return 0;
		}
		int added = applyPromises(elt, t);
		if (elt.getClassInit() != null) {
			added += applyPromises(elt.getClassInit(), JavaPromise.getClassInitOrNull(t));
		}
		for(FieldElement f : elt.getFields()) {
			added += applyPromises(f, TreeAccessor.findField(f.getName(), t));
		}
		for(ConstructorElement c : elt.getConstructors()) {
			added += applyPromisesToFunc(c, TreeAccessor.findConstructor(c.getParams(), t, v.getTypeEnv()));
		}
		for(MethodElement m : elt.getMethods()) {
			added += applyPromisesToFunc(m, TreeAccessor.findMethod(t, m.getName(), m.getParams(), v.getTypeEnv()));
		}
		for(NestedClassElement n : elt.getNestedClasses()) {
			added += applyPromisesToType(n, TreeAccessor.findNestedClass(n.getName(), t));
		}
		return added;
	}
	
	private static IRNode findType(final IRNode cuOrType, final String name) {
		final Operator op = JJNode.tree.getOperator(cuOrType);
		if (TypeDeclaration.prototype.includes(op)) {
			final String tName = JJNode.getInfo(cuOrType);
			if (!tName.equals(name)) {
				throw new IllegalStateException("Got a type with different name: "+tName+" vs "+name);
			}
			return cuOrType;
		} else {
			for(IRNode t : VisitUtil.getTypeDecls(cuOrType)) {
				if (JJNode.getInfo(t).equals(name)) {
					return t;
				}
			}
		}
		return null;
	}

	/**
	 * @return The number of annotations added
	 */
	private int applyPromisesToFunc(final AbstractFunctionElement elt, final IRNode func) {
		if (func == null) {
			return 0;
		}
		int added = applyPromises(elt, func);
		final IRNode params = SomeFunctionDeclaration.getParams(func);
		for(FunctionParameterElement p : elt.getParameters()) {
			if (p != null) {
				added += applyPromises(p, Parameters.getFormal(params, p.getIndex()));
			}
		}
		return added;
	}
}

