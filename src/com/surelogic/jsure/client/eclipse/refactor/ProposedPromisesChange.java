package com.surelogic.jsure.client.eclipse.refactor;

import java.util.*;
import java.util.Map.Entry;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;

import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.JavaProjects;
import com.surelogic.common.eclipse.JDTUtility;
import com.surelogic.common.eclipse.refactor.PromisesAnnotationRewriter;
import com.surelogic.common.refactor.AnnotationDescription;
import com.surelogic.common.refactor.Field;
import com.surelogic.common.refactor.IJavaDeclaration;
import com.surelogic.common.refactor.AnnotationDescription.CU;
import com.surelogic.fluid.javac.JavacTypeEnvironment;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.*;

public class ProposedPromisesChange {
	private final List<? extends IProposedPromiseDropInfo> drops;
	private final String label;
	
	public ProposedPromisesChange(final List<? extends IProposedPromiseDropInfo> drops) {
		this.drops = drops;
		
		// Just compute the label from the projects involved
		//
		// Get the projects involved (no dups)
		Collection<String> projects = new HashSet<String>();
		for(IProposedPromiseDropInfo p : drops) {
			projects.add(p.getTargetProjectName());
		}
		// Sort them
		projects = new ArrayList<String>(projects);
		Collections.sort((List<String>) projects);
		final StringBuilder sb = new StringBuilder();
		for(String p : projects) {
			final int len = sb.length(); 
			if (len > 0) {
				if (len > 50) {
					sb.append(" ...");
					break;
				}
				sb.append(", ");
			}
			sb.append(p);
		}
		label = sb.toString();
	}

	public List<? extends IProposedPromiseDropInfo> getDrops() {
		return drops;
	}

	/**
	 * Calculate the change that would occur by applying these proposed promises
	 * to the selected project.
	 * 
	 * @param root
	 */
	void change(final CompositeChange root) {
		final Set<String> projects = new HashSet<String>();
		final Map<CU, Set<AnnotationDescription>> map = new HashMap<CU, Set<AnnotationDescription>>();
		for (final IProposedPromiseDropInfo drop : drops) {	
			projects.add(drop.getTargetProjectName());
			projects.add(drop.getFromProjectName());
			
			final AnnotationDescription ann = desc(drop);
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
		final PromisesAnnotationRewriter rewrite = new PromisesAnnotationRewriter();
		try {
			for(final String proj : projects) {
				if (proj.startsWith(JavacTypeEnvironment.JRE_NAME)) {
					// All binaries, so use assumption
					continue;
				}
				final IJavaProject selectedProject = JDTUtility.getJavaProject(proj);
				for (final IPackageFragment frag : selectedProject
						.getPackageFragments()) {
					for (final ICompilationUnit unit : frag.getCompilationUnits()) {
						final CU check = new CU(proj, frag.getElementName(), unit
								.getElementName());
						promiseMap.put(check, map.remove(check));
					}
				}
			}
			// Not removed by the code above
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
			for(final String proj : projects) {
				if (proj.startsWith(JavacTypeEnvironment.JRE_NAME)) {
					continue;
				}
				final IJavaProject selectedProject = JDTUtility.getJavaProject(proj);
				for (final IPackageFragment frag : selectedProject
						.getPackageFragments()) {
					for (final ICompilationUnit unit : frag.getCompilationUnits()) {
						final CU check = new CU(proj, frag.getElementName(), unit.getElementName());
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
	static AnnotationDescription desc(final IProposedPromiseDropInfo drop) {	
		//final IBinder b = proj.getTypeEnv().getBinder();
		final IJavaDeclaration target = drop.getTargetInfo();//IRNodeUtil.convert(b, drop.getNode());
		
		// @Assume cannot be on a field, so we just stick it on the parent type
		// in that case
		IJavaDeclaration from = drop.getFromInfo();// = IRNodeUtil.convert(b, drop.getRequestedFrom());
		//System.out.println("Target = "+target);
		//System.out.println("From   = "+from);
		if (from instanceof Field) {
			from = ((Field) from).getTypeContext();
		}
		final IJavaDeclaration assumptionTarget = from;
		final String annotation = drop.getAnnotation();
		final String contents = drop.getContents();
		ISrcRef srcRef = drop.getSrcRef();
		final CU cu = new CU(drop.getTargetProjectName(), srcRef.getPackage(), srcRef.getRelativePath());
		srcRef = drop.getAssumptionRef();
		final CU assumptionCU = new CU(drop.getFromProjectName(), srcRef.getPackage(), srcRef.getRelativePath());
		return new AnnotationDescription(annotation, contents, target,
				assumptionTarget, cu, assumptionCU);
	}

	public String getSelectedProjectNames() {
		return label;
	}

}
