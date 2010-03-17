package com.surelogic.jsure.client.eclipse.refactor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import com.surelogic.common.logging.SLLogger;

public class PromisesAnnotationRewriter {

	private static final String ASSUME = "Assume";
	private static final String ASSUMES = "Assumes";
	private final ASTParser parser;
	private ASTNode ast;
	private ASTRewrite rewrite;

	public PromisesAnnotationRewriter(final IJavaProject project) {
		parser = ASTParser.newParser(AST.JLS3);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		parser.setStatementsRecovery(true);
	}

	/**
	 * Set the focused compilation unit. All rewrite methods target this
	 * compilation unit.
	 * 
	 * @param cu
	 */
	public void setCompilationUnit(final ICompilationUnit cu) {
		parser.setSource(cu);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		parser.setStatementsRecovery(true);
		ast = parser.createAST(null);
		rewrite = ASTRewrite.create(ast.getAST());
	}

	public void writeAnnotations(final Set<AnnotationDescription> descs) {
		writeAnnotations(descs, null);
	}

	public void writeAnnotations(final Set<AnnotationDescription> descs,
			final TextEditGroup editGroup) {
		ast.accept(new AnnotationVisitor(descs, editGroup, false));
	}

	public void writeAssumptions(final Set<AnnotationDescription> descs) {
		ast.accept(new AnnotationVisitor(descs, null, true));
	}

	/**
	 * Produce a TextEdit representing the set of rewrites made to this
	 * compilation unit
	 * 
	 * @return
	 */
	public TextEdit getTextEdit() {
		try {
			return rewrite.rewriteAST();
		} catch (final JavaModelException e) {
			throw new IllegalStateException(e);
		} catch (final IllegalArgumentException e) {
			throw new IllegalStateException(e);
		}
	}

	private class AnnotationVisitor extends ASTVisitor {
		private final Map<Object, List<AnnotationDescription>> targetMap;
		private final TextEditGroup editGroup;
		private Method inMethod;
		private TypeContext type;
		private final boolean isAssumption;
		private final Set<String> imports;

		public AnnotationVisitor(final Collection<AnnotationDescription> descs,
				final TextEditGroup editGroup, final boolean isAssumption) {
			targetMap = new HashMap<Object, List<AnnotationDescription>>();
			for (final AnnotationDescription desc : descs) {
				final Object target = isAssumption ? desc.getAssumptionTarget()
						: desc.getTarget();
				List<AnnotationDescription> list = targetMap.get(target);
				if (list == null) {
					list = new ArrayList<AnnotationDescription>();
					targetMap.put(target, list);
				}
				list.add(desc);
			}
			this.isAssumption = isAssumption;
			this.editGroup = editGroup;
			this.imports = new HashSet<String>();
		}

		@Override
		public boolean visit(final TypeDeclaration node) {
			type = typeContext(node.getName().getIdentifier());
			rewriteNode(node, TypeDeclaration.MODIFIERS2_PROPERTY, targetMap
					.get(type));
			return true;
		}

		@Override
		public boolean visit(final AnnotationTypeDeclaration node) {
			type = typeContext(node.getName().getIdentifier());
			rewriteNode(node, AnnotationTypeDeclaration.MODIFIERS2_PROPERTY,
					targetMap.get(type));
			return true;
		}

		@Override
		public boolean visit(final AnonymousClassDeclaration node) {
			final String name = "ANON"; // FIXME
			type = typeContext(name);
			return true;
		}

		@Override
		public boolean visit(final EnumDeclaration node) {
			type = typeContext(node.getName().getIdentifier());
			rewriteNode(node, EnumDeclaration.MODIFIERS2_PROPERTY, targetMap
					.get(type));
			return true;
		}

		private TypeContext typeContext(final String name) {
			if (type == null) {
				type = new TypeContext(name);
			} else if (inMethod == null) {
				type = new TypeContext(type, name);
			} else {
				type = new TypeContext(inMethod, name);
				inMethod = null;
			}
			return type;
		}

		@Override
		public boolean visit(final MethodDeclaration node) {
			final IMethodBinding mB = node.resolveBinding();
			final ITypeBinding[] paramDecls = mB.getParameterTypes();
			final String[] params = new String[paramDecls.length];
			for (int i = 0; i < params.length; i++) {
				params[i] = fromType(paramDecls[i]);
			}
			inMethod = new Method(type, node.getName().getIdentifier(), params);
			rewriteNode(node, MethodDeclaration.MODIFIERS2_PROPERTY, targetMap
					.get(inMethod));
			return true;
		}

		/**
		 * Rewrite the given node property to include this list of annotations
		 * 
		 * @param node
		 * @param prop
		 * @param list
		 */
		private void rewriteNode(final ASTNode node,
				final ChildListPropertyDescriptor prop,
				final List<AnnotationDescription> list) {
			if (list != null) {
				Collections.sort(list);
				Collections.reverse(list);
				final List<List<AnnotationDescription>> anns = new ArrayList<List<AnnotationDescription>>();
				List<AnnotationDescription> cur = null;
				String curAnn = null;
				for (final AnnotationDescription d : list) {
					if (!d.getAnnotation().equals(curAnn)) {
						cur = new ArrayList<AnnotationDescription>();
						anns.add(cur);
						curAnn = d.getAnnotation();
					}
					cur.add(d);
				}
				final ListRewrite lrw = rewrite.getListRewrite(node, prop);
				for (final List<AnnotationDescription> ann : anns) {
					mergeAnnotations(node.getAST(), ann, lrw);
				}
			}
		}

		/**
		 * Merge the list of annotations into the given rewrite. This can
		 * incorporate or replace annotations that currently exist in the
		 * compilation unit.
		 * 
		 * @param ann
		 * @param lrw
		 */
		@SuppressWarnings("unchecked")
		private void mergeAnnotations(final AST ast,
				final List<AnnotationDescription> ann, final ListRewrite lrw) {
			final List<ASTNode> nodes = lrw.getRewrittenList();
			final Mergeable m = merge(ann);
			for (final ASTNode aNode : nodes) {
				if (aNode instanceof Annotation) {
					final Annotation a = (Annotation) aNode;
					if (m.match(a)) {
						lrw.replace(aNode, m.merge(ast, a, imports), editGroup);
						return;
					}
				}
			}
			lrw.insertFirst(m.merge(ast, null, imports), editGroup);
		}

		private String fromType(final ITypeBinding t) {
			return t.getQualifiedName().replaceAll("<.*>", "");
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean visit(final FieldDeclaration node) {
			final List<VariableDeclarationFragment> fragments = node
					.fragments();
			// Handle when we have more than one field in the same declaration
			final List<AnnotationDescription> list = new ArrayList<AnnotationDescription>();
			for (final VariableDeclarationFragment frag : fragments) {
				final Field f = new Field(type, frag.getName().getIdentifier());
				final List<AnnotationDescription> list2 = targetMap.get(f);
				if (list2 != null) {
					list.addAll(list2);
				}
			}
			if (!list.isEmpty()) {
				rewriteNode(node, FieldDeclaration.MODIFIERS2_PROPERTY, list);
			}
			return false;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void endVisit(final CompilationUnit node) {
			// Add any new imports
			final List<ImportDeclaration> importNodes = node.imports();
			for (final ImportDeclaration i : importNodes) {
				imports.remove(i.getName().getFullyQualifiedName());
			}
			if (imports.size() > 0) {
				final AST ast = node.getAST();
				final ListRewrite lrw = rewrite.getListRewrite(node,
						CompilationUnit.IMPORTS_PROPERTY);
				for (final String i : imports) {
					final ImportDeclaration d = ast.newImportDeclaration();
					d.setName(ast.newName(i));
					lrw.insertLast(d, null);
				}
			}
			super.endVisit(node);
		}

		@Override
		public void endVisit(final MethodDeclaration node) {
			if (inMethod == null) {
				SLLogger.getLoggerFor(PromisesAnnotationRewriter.class).log(
						Level.SEVERE, "Unexpected method syntax");
			}
			inMethod = null;
		}

		private void endTypeContext() {
			if (type.getMethod() != null) {
				inMethod = type.getMethod();
			}
			type = type.getParent();
		}

		@Override
		public void endVisit(final TypeDeclaration node) {
			endTypeContext();
		}

		@Override
		public void endVisit(final AnnotationTypeDeclaration node) {
			endTypeContext();
		}

		@Override
		public void endVisit(final AnonymousClassDeclaration node) {
			endTypeContext();
		}

		@Override
		public void endVisit(final EnumDeclaration node) {
			endTypeContext();
		}

		Mergeable merge(final List<AnnotationDescription> descs) {
			if (isAssumption) {
				return new AssumptionMergeStrategy(descs);
			} else {
				return new DefaultMergeStrategy(descs);
			}
		}

	}

	class AssumptionMergeStrategy implements Mergeable {

		final List<AnnotationDescription> newAnnotations;

		AssumptionMergeStrategy(final List<AnnotationDescription> anns) {
			this.newAnnotations = new ArrayList<AnnotationDescription>(anns);
		}

		@SuppressWarnings("unchecked")
		public Annotation merge(final AST ast, final Annotation cur,
				final Set<String> imports) {
			final Set<String> existing = new HashSet<String>();
			if (cur != null) {
				final Expression e = extractValue(cur);
				if (e instanceof StringLiteral) {
					final String lit = ((StringLiteral) e).getLiteralValue();
					existing.add(lit);
				} else if (e instanceof ArrayInitializer) {
					final ArrayInitializer init = (ArrayInitializer) e;
					final List<Expression> es = init.expressions();
					for (final Expression ex : es) {
						if (ex instanceof Annotation) {
							final Expression val = extractValue((Annotation) ex);
							if (val instanceof StringLiteral) {
								final String lit = ((StringLiteral) val)
										.getLiteralValue();
								existing.add(lit);
							}
						}
					}
				}
			}
			for (final AnnotationDescription desc : newAnnotations) {
				addImport(ASSUME, imports);
				existing.add(String
						.format("%s for %s in %s", desc.toString(), desc
								.getTarget().forSyntax(), desc.getCU()
								.getPackage()));
			}
			final List<String> sortedAnns = new ArrayList<String>(existing);
			Collections.sort(sortedAnns);
			final List<Annotation> anns = new ArrayList<Annotation>(sortedAnns
					.size());
			for (final String s : sortedAnns) {
				final SingleMemberAnnotation ann = ast
						.newSingleMemberAnnotation();
				ann.setTypeName(ast.newName(ASSUME));
				final StringLiteral lit = ast.newStringLiteral();
				lit.setLiteralValue(s);
				ann.setValue(lit);
				anns.add(ann);
			}
			return createWrappedAssume(ast, anns, imports);
		}

		public boolean match(final Annotation a) {
			final String aName = a.resolveTypeBinding().getName();
			return ASSUME.equals(aName) || ASSUMES.equals(aName);
		}
	}

	class DefaultMergeStrategy implements Mergeable {

		final String name;
		final String wrapper;
		final List<AnnotationDescription> newAnnotations;

		DefaultMergeStrategy(final List<AnnotationDescription> anns) {
			this.newAnnotations = anns;
			this.name = anns.get(0).getAnnotation();
			this.wrapper = name + "s";
		}

		@SuppressWarnings("unchecked")
		public Annotation merge(final AST ast, final Annotation cur,
				final Set<String> imports) {
			if (cur != null) {
				final Expression e = extractValue(cur);
				if (e instanceof StringLiteral) {
					final String lit = ((StringLiteral) e).getLiteralValue();
					newAnnotations.add(new AnnotationDescription(name, lit));
				} else if (e instanceof ArrayInitializer) {
					final ArrayInitializer init = (ArrayInitializer) e;
					final List<Expression> es = init.expressions();
					for (final Expression ex : es) {
						if (ex instanceof Annotation) {
							final Expression val = extractValue((Annotation) ex);
							if (val instanceof StringLiteral) {
								final String lit = ((StringLiteral) e)
										.getLiteralValue();
								newAnnotations.add(new AnnotationDescription(
										name, lit));
							}
						}
					}
				}
			}
			return createWrappedAnnotation(ast, wrapper, newAnnotations,
					imports);
		}

		public boolean match(final Annotation a) {
			final String aName = a.resolveTypeBinding().getName();
			return name.equals(aName) || wrapper.equals(aName);
		}
	}

	interface Mergeable {
		/**
		 * Whether or not this mergeable object matches.
		 * 
		 * @param a
		 * @return
		 */
		boolean match(Annotation a);

		/**
		 * Produce a new annotation containing the annotations held in this
		 * mergeable object, as well as the given annotation.
		 * 
		 * @param a
		 *            The annotation to merge in. May be null.
		 * @param imports
		 *            the set of imports used. This should be updated as .
		 *            necessary.
		 * @return
		 */
		Annotation merge(AST ast, Annotation a, Set<String> imports);
	}

	@SuppressWarnings("unchecked")
	private Annotation createWrappedAnnotation(final AST ast,
			final String wrapperName, final List<AnnotationDescription> anns,
			final Set<String> imports) {
		final int len = anns.size();
		if (len == 1) {
			return ann(ast, anns.get(0), imports);
		} else if (len > 1) {
			final SingleMemberAnnotation a = ast.newSingleMemberAnnotation();
			a.setTypeName(ast.newName(wrapperName));
			addImport(wrapperName, imports);
			final ArrayInitializer arr = ast.newArrayInitializer();
			final List<Expression> expressions = arr.expressions();
			for (final AnnotationDescription desc : anns) {
				expressions.add(ann(ast, desc, imports));
			}
			a.setValue(arr);
			return a;
		}
		throw new IllegalArgumentException("List cannot be empty");
	}

	@SuppressWarnings("unchecked")
	private Annotation createWrappedAssume(final AST ast,
			final List<Annotation> anns, final Set<String> imports) {
		final int len = anns.size();
		if (len == 1) {
			return anns.get(0);
		} else if (len > 1) {
			final SingleMemberAnnotation a = ast.newSingleMemberAnnotation();
			a.setTypeName(ast.newName(ASSUMES));
			addImport(ASSUMES, imports);
			final ArrayInitializer arr = ast.newArrayInitializer();
			final List<Expression> expressions = arr.expressions();
			for (final Annotation ann : anns) {
				expressions.add(ann);
			}
			a.setValue(arr);
			return a;
		}
		throw new IllegalArgumentException("List cannot be empty");
	}

	/**
	 * Create an annotation matching the given description
	 * 
	 * @param ast
	 * @param desc
	 * @return
	 */

	private Annotation ann(final AST ast, final AnnotationDescription desc,
			final Set<String> imports) {
		addImport(desc.getAnnotation(), imports);
		if (desc.hasContents()) {
			final SingleMemberAnnotation ann = ast.newSingleMemberAnnotation();
			ann.setTypeName(ast.newName(desc.getAnnotation()));
			final StringLiteral lit = ast.newStringLiteral();
			lit.setLiteralValue(desc.getContents());
			ann.setValue(lit);
			return ann;
		} else {
			final MarkerAnnotation ann = ast.newMarkerAnnotation();
			ann.setTypeName(ast.newName(desc.getAnnotation()));
			return ann;
		}
	}

	static void addImport(final String promise, final Set<String> imports) {
		imports.add("com.surelogic." + promise);
	}

	/**
	 * Returns the value of an annotation, or null if no value exists
	 * 
	 * @param a
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static Expression extractValue(final Annotation a) {
		if (a.isNormalAnnotation()) {
			final NormalAnnotation na = (NormalAnnotation) a;
			final List<MemberValuePair> ps = na.values();
			for (final MemberValuePair p : ps) {
				if (p.getName().getIdentifier().equals("value")) {
					return p.getValue();
				}
			}
		} else if (a.isSingleMemberAnnotation()) {
			final SingleMemberAnnotation sa = (SingleMemberAnnotation) a;
			return sa.getValue();
		}
		return null;
	}

	private static String join(final List<String> names, final char delim) {
		if (names.isEmpty()) {
			return "";
		}
		final StringBuilder b = new StringBuilder();
		for (final String name : names) {
			b.append(name);
			b.append(delim);
		}
		return b.substring(0, b.length() - 1);
	}

	/*
	 * join together a list of names with the '.' separator
	 */
	private static String join(final List<String> names) {
		return join(names, '.');
	}

}
