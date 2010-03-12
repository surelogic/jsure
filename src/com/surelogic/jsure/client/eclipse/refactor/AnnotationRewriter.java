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
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import com.surelogic.common.logging.SLLogger;

public class AnnotationRewriter {

	private final ASTParser parser;
	private ASTNode ast;
	private ASTRewrite rewrite;

	public AnnotationRewriter(final IJavaProject project) {
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
			final String name = node.getName().getIdentifier();
			if (type == null) {
				type = new TypeContext(name);
			} else if (inMethod == null) {
				type = new TypeContext(type, name);
			} else {
				type = new TypeContext(inMethod, name);
			}
			rewriteNode(node, TypeDeclaration.MODIFIERS2_PROPERTY, targetMap
					.get(type));
			return true;
		}

		@Override
		public boolean visit(final AnnotationTypeDeclaration node) {
			final String name = node.getName().getIdentifier();
			if (type == null) {
				type = new TypeContext(name);
			} else if (inMethod == null) {
				type = new TypeContext(type, name);
			} else {
				type = new TypeContext(inMethod, name);
			}
			rewriteNode(node, AnnotationTypeDeclaration.MODIFIERS2_PROPERTY,
					targetMap.get(type));
			return true;
		}

		@Override
		public boolean visit(final AnonymousClassDeclaration node) {
			final String name = ""; // FIXME
			if (type == null) {
				type = new TypeContext(name);
			} else if (inMethod == null) {
				type = new TypeContext(type, name);
			} else {
				type = new TypeContext(inMethod, name);
			}
			return true;
		}

		@Override
		public boolean visit(final EnumDeclaration node) {
			final String name = node.getName().getIdentifier();
			if (type == null) {
				type = new TypeContext(name);
			} else if (inMethod == null) {
				type = new TypeContext(type, name);
			} else {
				type = new TypeContext(inMethod, name);
			}
			rewriteNode(node, EnumDeclaration.MODIFIERS2_PROPERTY, targetMap
					.get(type));
			return true;
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

		private void rewriteNode(final ASTNode node,
				final ChildListPropertyDescriptor prop,
				final List<AnnotationDescription> list) {
			if (list != null) {
				Collections.sort(list);
				Collections.reverse(list);
				final ListRewrite lrw = rewrite.getListRewrite(node, prop);
				for (final AnnotationDescription desc : list) {
					// Add annotation
					final AST ast = node.getAST();
					if (isAssumption) {
						addImport("Assume");
						final SingleMemberAnnotation ann = ast
								.newSingleMemberAnnotation();
						ann.setTypeName(ast.newName("Assume"));
						final StringLiteral lit = ast.newStringLiteral();
						lit.setLiteralValue(String.format("%s for %s in %s",
								desc.toString(), desc.getTarget().forSyntax(),
								desc.getCU().getPackage()));
						ann.setValue(lit);
						lrw.insertFirst(ann, editGroup);
					} else {
						addImport(desc.getAnnotation());
						if (desc.hasContents()) {
							final SingleMemberAnnotation ann = ast
									.newSingleMemberAnnotation();
							ann.setTypeName(ast.newName(desc.getAnnotation()));
							final StringLiteral lit = ast.newStringLiteral();
							lit.setLiteralValue(desc.getContents());
							ann.setValue(lit);
							lrw.insertFirst(ann, editGroup);
						} else {
							final MarkerAnnotation ann = ast
									.newMarkerAnnotation();
							ann.setTypeName(ast.newName(desc.getAnnotation()));
							lrw.insertFirst(node, editGroup);
						}
					}
				}
			}
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

		void addImport(final String promise) {
			imports.add("com.surelogic." + promise);
		}

		@SuppressWarnings("unchecked")
		@Override
		public void endVisit(final CompilationUnit node) {
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
				SLLogger.getLoggerFor(AnnotationRewriter.class).log(
						Level.SEVERE, "Unexpected method syntax");
			}
			inMethod = null;
		}

		@Override
		public void endVisit(final TypeDeclaration node) {
			if (type.getMethod() != null) {
				inMethod = type.getMethod();
			}
			type = type.getParent();
		}

	}

}
