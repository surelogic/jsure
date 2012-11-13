package com.surelogic.jsure.client.eclipse.refactor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;

import com.surelogic.common.core.JDTUtility;
import com.surelogic.common.ref.DeclUtil;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.IDecl.Kind;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.refactor.AnnotationDescription;
import com.surelogic.common.refactor.AnnotationDescription.CU;
import com.surelogic.common.ui.refactor.PromisesAnnotationRewriter;
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.javac.JavacTypeEnvironment;

import edu.cmu.cs.fluid.java.bind.PromiseFramework;

public class ProposedPromisesChange {
  private final List<? extends IProposedPromiseDrop> drops;
  private final String label;

  public ProposedPromisesChange(final List<? extends IProposedPromiseDrop> drops) {
    this.drops = drops;

    // Just compute the label from the projects involved
    //
    // Get the projects involved (no dups)
    Collection<String> projects = new HashSet<String>();
    for (IProposedPromiseDrop p : drops) {
      projects.add(p.getJavaRef().getRealEclipseProjectNameOrNull());
    }
    // Sort them
    projects = new ArrayList<String>(projects);
    Collections.sort((List<String>) projects);
    final StringBuilder sb = new StringBuilder();
    for (String p : projects) {
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

  public List<? extends IProposedPromiseDrop> getDrops() {
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
    for (final IProposedPromiseDrop drop : drops) {
      projects.add(drop.getJavaRef().getRealEclipseProjectNameOrNull());
      projects.add(drop.getAssumptionRef().getRealEclipseProjectNameOrNull());

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
    final PromisesAnnotationRewriter rewrite = new PromisesAnnotationRewriter(PromiseFramework.getInstance()
        .getAllowsMultipleAnnosSet());
    try {
      for (final String proj : projects) {
        if (proj == null || proj.startsWith(JavacTypeEnvironment.JRE_NAME)) {
          // All binaries, so use assumption
          continue;
        }
        final IJavaProject selectedProject = JDTUtility.getJavaProject(proj);
        for (final IPackageFragment frag : selectedProject.getPackageFragments()) {
          for (final ICompilationUnit unit : frag.getCompilationUnits()) {
            final CU check = new CU(proj, frag.getElementName(), unit.getElementName());
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
      for (final String proj : projects) {
        if (proj == null || proj.startsWith(JavacTypeEnvironment.JRE_NAME)) {
          continue;
        }
        final IJavaProject selectedProject = JDTUtility.getJavaProject(proj);
        for (final IPackageFragment frag : selectedProject.getPackageFragments()) {
          for (final ICompilationUnit unit : frag.getCompilationUnits()) {
            final CU check = new CU(proj, frag.getElementName(), unit.getElementName());
            final Set<AnnotationDescription> promiseSet = promiseMap.get(check);
            final Set<AnnotationDescription> assumeSet = assumeMap.get(check);
            if (promiseSet != null || assumeSet != null) {
              rewrite.setCompilationUnit(unit);
              if (promiseSet != null) {
                rewrite.writeAnnotations(promiseSet);
              }
              if (assumeSet != null) {
                rewrite.writeAssumptions(assumeSet);
              }
              final TextEdit textEdit = rewrite.getTextEdit();
              final IFile file = (IFile) unit.getResource();
              final TextFileChange change = new TextFileChange(file.getName(), file);
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
  static AnnotationDescription desc(final IProposedPromiseDrop drop) {
    final IDecl target = drop.getJavaRef().getDeclaration();

    // @Assume cannot be on a field, so we just stick it on the parent type
    // in that case
    IDecl from = drop.getAssumptionRef().getDeclaration();
    if (from.getKind() == Kind.FIELD) {
      from = from.getParent();
    }
    final IDecl assumptionTarget = from;
    final String annotation = drop.getAnnotation();
    final String contents = drop.getContents();
    IJavaRef srcRef = drop.getJavaRef();
    String fileName = DeclUtil.guessSimpleFileName(srcRef.getDeclaration(), srcRef.getWithin());
    final CU cu = new CU(srcRef.getRealEclipseProjectNameOrNull(), srcRef.getPackageName(), fileName);
    srcRef = drop.getAssumptionRef();
    fileName = DeclUtil.guessSimpleFileName(srcRef.getDeclaration(), srcRef.getWithin());
    final CU assumptionCU = new CU(srcRef.getRealEclipseProjectNameOrNull(), srcRef.getPackageName(), fileName);
    return new AnnotationDescription(annotation, contents, drop.getReplacedContents(), target, assumptionTarget, cu, assumptionCU);
  }

  public String getSelectedProjectNames() {
    return label;
  }

}
