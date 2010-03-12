package com.surelogic.jsure.client.eclipse.refactor;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;

import com.surelogic.common.eclipse.JDTUtility;
import com.surelogic.common.eclipse.SWTUtility;
import com.surelogic.common.i18n.I18N;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.ProjectDrop;

public abstract class ProposedPromisesRefactoringAction extends Action {

	protected abstract List<ProposedPromiseDrop> getProposedDrops();

	@Override
	public void run() {
		final List<ProposedPromiseDrop> selected = getProposedDrops();
		if (selected.isEmpty()) {
			return;
		}
		/*
		 * TODO Proposed the edit to the code in the dialog HERE (you are in the
		 * SWT thread)
		 */
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
			op.run(SWTUtility.getShell(), I18N
					.msg("jsure.eclipse.promises.refactor"));
		} catch (final InterruptedException e) {
			// Operation was cancelled. Whatever floats their boat.
		}
	}

}
