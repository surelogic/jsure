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

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop;

public class ProposedPromisesChange {
	private final List<ProposedPromiseDrop> drops;
	private final String label;
	
	public ProposedPromisesChange(final List<ProposedPromiseDrop> drops) {
		this.drops = drops;
		
		// Just compute the label from the projects involved
		//
		// Get the projects involved (no dups)
		Collection<IIRProject> projects = new HashSet<IIRProject>();
		for(ProposedPromiseDrop p : drops) {
			projects.add(JavaProjects.getEnclosingProject(p.getNode()));
		}
		// Sort them
		projects = new ArrayList<IIRProject>(projects);
		Collections.sort((List<IIRProject>) projects, new Comparator<IIRProject>() {
			public int compare(IIRProject o1, IIRProject o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		final StringBuilder sb = new StringBuilder();
		for(IIRProject p : projects) {
			final int len = sb.length(); 
			if (len > 0) {
				if (len > 50) {
					sb.append(" ...");
					break;
				}
				sb.append(", ");
			}
			sb.append(p.getName());
		}
		label = sb.toString();
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
		final Set<IIRProject> projects = new HashSet<IIRProject>();
		final Map<CU, Set<AnnotationDescription>> map = new HashMap<CU, Set<AnnotationDescription>>();
		for (final ProposedPromiseDrop drop : drops) {
			final IIRProject proj = JavaProjects.getEnclosingProject(drop.getNode());	
			projects.add(proj);
			
			final AnnotationDescription ann = desc(drop, proj);
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
			for(final IIRProject proj : projects) {
				final IJavaProject selectedProject = JDTUtility.getJavaProject(proj.getName());
				for (final IPackageFragment frag : selectedProject
						.getPackageFragments()) {
					for (final ICompilationUnit unit : frag.getCompilationUnits()) {
						final CU check = new CU(proj.getName(), frag.getElementName(), unit
								.getElementName());
						promiseMap.put(check, map.remove(check));
					}
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
			for(final IIRProject proj : projects) {
				final IJavaProject selectedProject = JDTUtility.getJavaProject(proj.getName());
				for (final IPackageFragment frag : selectedProject
						.getPackageFragments()) {
					for (final ICompilationUnit unit : frag.getCompilationUnits()) {
						final CU check = new CU(proj.getName(), frag.getElementName(), unit.getElementName());
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
	static AnnotationDescription desc(final ProposedPromiseDrop drop, final IIRProject proj) {	
		final IBinder b = proj.getTypeEnv().getBinder();
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
		final CU cu = new CU(proj.getName(), srcRef.getPackage(), srcRef.getRelativePath());
		srcRef = drop.getAssumptionRef();
		final IIRProject fromProj = JavaProjects.getEnclosingProject(drop.getAssumptionNode());	
		final CU assumptionCU = new CU(fromProj.getName(), srcRef.getPackage(), srcRef.getRelativePath());
		return new AnnotationDescription(annotation, contents, target,
				assumptionTarget, cu, assumptionCU);
	}

	public String getSelectedProjectNames() {
		return label;
	}

}
