package com.surelogic.jsure.client.eclipse.refactor;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;

import com.surelogic.common.eclipse.*;

import edu.cmu.cs.fluid.sea.IProposedPromiseDropInfo;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop;

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
	protected abstract List<? extends IProposedPromiseDropInfo> getProposedDrops();

	protected abstract String getDialogTitle();

	@Override
	public void run() {
		final List<? extends IProposedPromiseDropInfo> selected = getProposedDrops();
		if (selected.isEmpty()) {
			return;
		}

		final ProposedPromisesChange info = new ProposedPromisesChange(selected);
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
