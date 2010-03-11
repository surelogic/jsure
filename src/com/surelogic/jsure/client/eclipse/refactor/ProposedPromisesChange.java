package com.surelogic.jsure.client.eclipse.refactor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop;

public class ProposedPromisesChange {

	private final IJavaProject selectedProject;
	private final List<ProposedPromiseDrop> drops;
	private final IBinder binder;

	public ProposedPromisesChange(final IJavaProject selectedProject,
			final IBinder binder, final List<ProposedPromiseDrop> drops) {
		this.selectedProject = selectedProject;
		this.drops = drops;
		this.binder = binder;
	}

	public IJavaProject getSelectedProject() {
		return selectedProject;
	}

	public List<ProposedPromiseDrop> getDrops() {
		return drops;
	}

	void change(final CompositeChange root) {
		final Map<CU, Set<AnnotationDescription>> map = new HashMap<CU, Set<AnnotationDescription>>();
		for (final ProposedPromiseDrop drop : drops) {
			final ISrcRef srcRef = drop.getSrcRef();
			final CU cu = new CU(srcRef.getPackage(), srcRef.getCUName());
			Set<AnnotationDescription> set = map.get(cu);
			if (set == null) {
				set = new HashSet<AnnotationDescription>();
				map.put(cu, set);
			}
			set.add(new AnnotationDescription(drop, binder));
		}
		final AnnotationRewriter rewrite = new AnnotationRewriter(
				selectedProject);
		try {
			for (final IPackageFragment frag : selectedProject
					.getPackageFragments()) {
				for (final ICompilationUnit unit : frag.getCompilationUnits()) {
					final CU check = new CU(frag.getElementName(), unit
							.getElementName());
					final Set<AnnotationDescription> set = map.get(check);
					if (set != null) {
						rewrite.setCompilationUnit(unit);
						rewrite.rewriteAnnotations(set);
						final TextEdit textEdit = rewrite.getTextEdit();
						final IFile file = (IFile) unit.getResource();
						final TextFileChange change = new TextFileChange(file
								.getName(), file);
						change.setEdit(textEdit);
						root.add(change);
					}
				}
			}
		} catch (final JavaModelException e) {
			throw new IllegalStateException(e);
		}
	}

	private static class CU {
		final String cu;
		final String pakkage;

		CU(final String pakkage, final String cu) {
			this.cu = cu;
			this.pakkage = pakkage;
		}

		@Override
		public String toString() {
			return pakkage + "." + cu;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (cu == null ? 0 : cu.hashCode());
			result = prime * result
					+ (pakkage == null ? 0 : pakkage.hashCode());
			return result;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final CU other = (CU) obj;
			if (cu == null) {
				if (other.cu != null) {
					return false;
				}
			} else if (!cu.equals(other.cu)) {
				return false;
			}
			if (pakkage == null) {
				if (other.pakkage != null) {
					return false;
				}
			} else if (!pakkage.equals(other.pakkage)) {
				return false;
			}
			return true;
		}

	}
}
