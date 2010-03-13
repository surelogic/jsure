package com.surelogic.jsure.client.eclipse.refactor;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;

import com.surelogic.common.eclipse.JDTUtility;
import com.surelogic.common.eclipse.SWTUtility;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.ProjectDrop;

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
	protected abstract List<ProposedPromiseDrop> getProposedDrops();

	protected abstract String getDialogTitle();

	@Override
	public void run() {
		final List<ProposedPromiseDrop> selected = getProposedDrops();
		if (selected.isEmpty()) {
			return;
		}
		final IBinder b = IDE.getInstance().getTypeEnv(
				ProjectDrop.getDrop().getIIRProject()).getBinder();
		final ProposedPromisesChange info = new ProposedPromisesChange(
				JDTUtility.getJavaProject(ProjectDrop.getProject()), b,
				selected);
		final ProposedPromisesRefactoring refactoring = new ProposedPromisesRefactoring(
				info);
		final ProposedPromisesRefactoringWizard wizard = new ProposedPromisesRefactoringWizard(
				refactoring, info);
		final RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(
				wizard);
		try {
			op.run(SWTUtility.getShell(), getDialogTitle());
		} catch (final InterruptedException e) {
			// Operation was canceled. Whatever floats their boat.
		}
	}

}
