package com.surelogic.jsure.client.eclipse.refactor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;

import com.surelogic.common.eclipse.refactor.PromisesAnnotationRewriter;
import com.surelogic.common.refactor.AnnotationDescription;
import com.surelogic.common.refactor.Field;
import com.surelogic.common.refactor.IJavaDeclaration;
import com.surelogic.common.refactor.AnnotationDescription.CU;

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

	/**
	 * Calculate the change that would occur by applying these proposed promises
	 * to the selected project.
	 * 
	 * @param root
	 */
	void change(final CompositeChange root) {
		final Map<CU, Set<AnnotationDescription>> map = new HashMap<CU, Set<AnnotationDescription>>();
		for (final ProposedPromiseDrop drop : drops) {
			final AnnotationDescription ann = desc(drop, binder);
			final CU cu = ann.getCU();
			Set<AnnotationDescription> set = map.get(cu);
			if (set == null) {
				set = new HashSet<AnnotationDescription>();
				map.put(cu, set);
			}
			set.add(ann);
		}
		final Map<CU, Set<AnnotationDescription>> promiseMap = new HashMap<CU, Set<AnnotationDescription>>();
		final Map<CU, Set<AnnotationDescription>> assumeMap = new HashMap<CU, Set<AnnotationDescription>>();
		final PromisesAnnotationRewriter rewrite = new PromisesAnnotationRewriter(
				selectedProject);
		try {
			for (final IPackageFragment frag : selectedProject
					.getPackageFragments()) {
				for (final ICompilationUnit unit : frag.getCompilationUnits()) {
					final CU check = new CU(frag.getElementName(), unit
							.getElementName());
					promiseMap.put(check, map.remove(check));
				}
			}
			for (final Entry<CU, Set<AnnotationDescription>> e : map.entrySet()) {
				for (final AnnotationDescription ann : e.getValue()) {
					final CU cu = ann.getAssumptionCU();
					Set<AnnotationDescription> set = assumeMap.get(cu);
					if (set == null) {
						set = new HashSet<AnnotationDescription>();
						assumeMap.put(cu, set);
					}
					set.add(ann);
				}
			}
			for (final IPackageFragment frag : selectedProject
					.getPackageFragments()) {
				for (final ICompilationUnit unit : frag.getCompilationUnits()) {
					final CU check = new CU(frag.getElementName(), unit
							.getElementName());
					rewrite.setCompilationUnit(unit);
					final Set<AnnotationDescription> promiseSet = promiseMap
							.get(check);
					if (promiseSet != null) {
						rewrite.writeAnnotations(promiseSet);
					}
					final Set<AnnotationDescription> assumeSet = assumeMap
							.get(check);
					if (assumeSet != null) {
						rewrite.writeAssumptions(assumeSet);
					}
					if (promiseSet != null || assumeSet != null) {
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

	/**
	 * Construct an annotation description from a proposed promise drop.
	 * 
	 * @param drop
	 * @param b
	 * @return
	 */
	static AnnotationDescription desc(final ProposedPromiseDrop drop,
			final IBinder b) {
		final IJavaDeclaration target = IRNodeUtil.convert(b, drop.getNode());
		// @Assume cannot be on a field, so we just stick it on the parent type
		// in that case
		IJavaDeclaration from = IRNodeUtil.convert(b, drop.getRequestedFrom());
		if (from instanceof Field) {
			from = ((Field) from).getTypeContext();
		}
		final IJavaDeclaration assumptionTarget = from;
		final String annotation = drop.getAnnotation();
		final String contents = drop.getContents();
		ISrcRef srcRef = drop.getSrcRef();
		final CU cu = new CU(srcRef.getPackage(), srcRef.getCUName());
		srcRef = drop.getAssumptionRef();
		final CU assumptionCU = new CU(srcRef.getPackage(), srcRef.getCUName());
		return new AnnotationDescription(annotation, contents, target,
				assumptionTarget, cu, assumptionCU);
	}

}
