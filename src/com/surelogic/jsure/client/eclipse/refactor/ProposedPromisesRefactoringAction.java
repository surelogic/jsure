package com.surelogic.jsure.client.eclipse.refactor;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.common.eclipse.JDTUtility;
import com.surelogic.common.eclipse.SWTUtility;
import com.surelogic.common.i18n.I18N;

//FIXME this may be an unecessary class
public class ProposedPromisesRefactoringAction implements
		IObjectActionDelegate, IWorkbenchWindowActionDelegate {

	private IJavaProject f_javaProject = null;

	public void setActivePart(final IAction action,
			final IWorkbenchPart targetPart) {
		// Do nothing
	}

	public void run(final IAction action) {

		final ProposedPromisesInfo info = new ProposedPromisesInfo(JDTUtility
				.getJavaProjects(), null);
		info.setSelectedProject(f_javaProject);
		final ProposedPromisesRefactoring refactoring = new ProposedPromisesRefactoring(
				info);
		final ProposedPromisesRefactoringWizard wizard = new ProposedPromisesRefactoringWizard(
				refactoring, info);
		final RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(
				wizard);
		try {
			op.run(SWTUtility.getShell(), I18N
					.msg("flashlight.recommend.refactor.regionIsThis"));
		} catch (final InterruptedException e) {
			// Operation was cancelled. Whatever floats their boat.
		}
	}

	public void selectionChanged(final IAction action,
			final ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			final Object o = ((IStructuredSelection) selection)
					.getFirstElement();
			if (o instanceof IJavaProject) {
				f_javaProject = (IJavaProject) o;
				// FIXME action.setEnabled(noCompilationErrors(f_javaProject));
			}
		}
	}

	public void dispose() {
		// Do nothing
	}

	public void init(final IWorkbenchWindow window) {
		// Do nothing
	}
}
