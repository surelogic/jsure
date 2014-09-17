package com.surelogic.jsure.client.eclipse.refactor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;

import com.surelogic.common.core.JDTUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;
import com.surelogic.jsure.client.eclipse.handlers.AddUpdatePromisesLibraryHandler;
import com.surelogic.jsure.core.JSureUtility;

public abstract class ProposedPromisesRefactoringAction extends Action {

  /**
   * Gets the list of proposed promise drops for the source code modification.
   * Duplicates should not be in this result. Use
   * {@link ProposedPromiseDrop#filterOutDuplicates(java.util.Collection)} if
   * you need to filter out duplicates.
   * 
   * @return the list of proposed promise drops for the source code
   *         modification. Should not contain duplicate.
   */
  protected abstract List<IProposedPromiseDrop> getProposedDrops();

  protected abstract String getDialogTitle();

  @Override
  public void run() {
    final List<IProposedPromiseDrop> selected = getProposedDrops();
    if (selected.isEmpty()) {
      return;
    }
    final List<IJavaProject> missing = findProjectsWithoutPromises(selected);
    if (!missing.isEmpty()) {
      new AddUpdatePromisesLibraryHandler().runActionOn(missing);

      StringBuilder sb = new StringBuilder();
      for (IJavaProject p : missing) {
        if (sb.length() > 0) {
          sb.append(", ");
        }
        sb.append(p.getElementName());
      }
      MessageDialog.openInformation(EclipseUIUtility.getShell(), I18N.msg("jsure.eclipse.dialog.promises.addRequiredJars.title"),
          I18N.msg("jsure.eclipse.dialog.promises.addRequiredJars.msg", sb));
      return;
    }
    final List<IProposedPromiseDrop> valid = findValidProposals(selected);
    if (valid.isEmpty()) {
      MessageDialog.openInformation(EclipseUIUtility.getShell(), I18N.msg("jsure.eclipse.dialog.proposals.noneValid.title"),
    		  I18N.msg("jsure.eclipse.dialog.proposals.noneValid.msg"));
      return;
    }
    
    final ProposedPromisesChange info = new ProposedPromisesChange(valid);
    final ProposedPromisesRefactoring refactoring = new ProposedPromisesRefactoring(info);
    final ProposedPromisesRefactoringWizard wizard = new ProposedPromisesRefactoringWizard(refactoring, info);
    final RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
    try {
      op.run(EclipseUIUtility.getShell(), getDialogTitle());
    } catch (final InterruptedException canceled) {
      // Operation was canceled. Whatever floats their boat.
    }
  }

  private List<IProposedPromiseDrop> findValidProposals(List<IProposedPromiseDrop> proposals) {
	List<IProposedPromiseDrop> valid = new ArrayList<IProposedPromiseDrop>(proposals.size());
	for(IProposedPromiseDrop p : proposals) {
		if (isValid(p)) {
			valid.add(p);
		}
	}
	return valid;
  }
  
  private boolean isValid(IProposedPromiseDrop p) {
	final IJavaRef ref = p.getJavaRef();
	/*
	final IJavaProject proj = JDTUtility.getJavaProject(ref.getRealEclipseProjectNameOrNull());
	if (proj == null) {
		return false;
	}
	*/
	IJavaElement elt = JDTUtility.findJavaElementOrNull(ref);
	if (elt != null) {
		switch (elt.getElementType()) {
		case IJavaElement.COMPILATION_UNIT:
			return true;
		case IJavaElement.CLASS_FILE:
			IJavaElement srcElt = JDTUtility.findJavaElementOrNull(p.getAssumptionRef());
			return srcElt != null && srcElt.getElementType() == IJavaElement.COMPILATION_UNIT;
		default:
			throw new IllegalStateException("Unexpected Java element: "+elt);
		}
	}
	return false;
  }

/**
   * Find projects that don't have the promises jar
   */
  private List<IJavaProject> findProjectsWithoutPromises(List<IProposedPromiseDrop> proposals) {
    final Set<String> projects = new HashSet<String>();
    for (IProposedPromiseDrop p : proposals) {
      // Check the target project for promises
      projects.add(p.getJavaRef().getRealEclipseProjectNameOrNull());
    }
    final List<IJavaProject> missing = new ArrayList<IJavaProject>();
    for (String name : projects) {
      if (name == null) {
    	  continue; // Skip this since it's the JRE
      }
      final IJavaProject proj = JDTUtility.getJavaProject(name);
      if (proj == null) {
          continue; // Skip this since it's the JRE
        }
      if (!JSureUtility.checkForRegionLockPromiseOnClasspathOf(proj)) {
        missing.add(proj);
      }
    }
    return missing;
  }
}