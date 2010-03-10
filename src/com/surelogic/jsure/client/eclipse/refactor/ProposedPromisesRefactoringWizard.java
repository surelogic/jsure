package com.surelogic.jsure.client.eclipse.refactor;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

public class ProposedPromisesRefactoringWizard extends RefactoringWizard {

	private final ProposedPromisesChange info;

	public ProposedPromisesRefactoringWizard(final Refactoring refactoring,
			final ProposedPromisesChange info) {
		super(refactoring, RefactoringWizard.DIALOG_BASED_USER_INTERFACE);
		this.info = info;
	}

	@Override
	protected void addUserInputPages() {
		setDefaultPageTitle(getRefactoring().getName());
	}

}
